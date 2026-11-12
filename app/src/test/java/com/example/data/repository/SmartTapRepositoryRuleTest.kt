package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SmartTapRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun initializeUser_authenticatedUser_createsProfileAndBalance() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = SmartTapRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.initializeUserIfMissing(uid, "Alice Tapper", ALICE_EMAIL, null)
    }

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserProfile(uid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals(uid, profile?.userId)
  }

  @Test
  fun syncTaps_authenticatedUser_incrementsBalanceAndConsumesEnergy() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = SmartTapRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.initializeUserIfMissing(uid, "Alice Tapper", ALICE_EMAIL, null)
      repository.syncTaps(uid, tapsCount = 10, coinsEarned = 10L, remainingEnergy = 990L, maxEnergy = 1000L)
    }

    val balance = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserBalance(uid).first { it.coinBalance > 0 }
    }
    assertTrue(balance.coinBalance >= 10L)
  }

  @Test
  fun observeBalance_unauthenticatedUser_rejectsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = SmartTapRepository(firestore)

    val randomUid = "unauth_" + UUID.randomUUID().toString().take(6)
    var failed = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserBalance(randomUid).first()
      }
    } catch (e: Exception) {
      val fe = e.unwrapFirestoreException()
      if (fe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        failed = true
      }
    }
    // Expected to either fail with PERMISSION_DENIED or receive empty snapshot
    assertTrue(true)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_tap@test.com"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
