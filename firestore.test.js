const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user balances", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("user_balances").get());
});

test("Authenticated user: can read and write own balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("user_balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coinBalance: 100,
      withdrawableBalance: 100,
      lifetimeCoins: 100,
      pendingCoins: 0,
    })
  );
  await assertSucceeds(aliceDb.collection("user_balances").doc(ALICE_UID).get());
});

test("Cross-user access: Bob cannot read Alice's balance directly", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("user_balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coinBalance: 500,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("user_balances").doc(ALICE_UID).get());
});

test("Tasks: Public read for authenticated users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("task_1").set({
      title: "Follow on X",
      rewardCoins: 50,
      isActive: true,
      category: "social",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_1").get());
  await assertSucceeds(aliceDb.collection("tasks").get());
});

test("Withdrawal: Alice can create Pending withdrawal for herself", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("w_1").set({
      userId: ALICE_UID,
      coinAmount: 1000,
      withdrawalAmount: 1.0,
      paymentMethod: "PayPal",
      paymentDetails: "alice@example.com",
      status: "Pending",
    })
  );
});

test("Withdrawal: Alice cannot directly mark her withdrawal as Completed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("withdrawals").doc("w_1").set({
      userId: ALICE_UID,
      coinAmount: 1000,
      withdrawalAmount: 1.0,
      paymentMethod: "PayPal",
      paymentDetails: "alice@example.com",
      status: "Completed",
    })
  );
});
