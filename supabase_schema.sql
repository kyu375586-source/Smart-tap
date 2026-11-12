-- =========================================================================
-- SMART TAP: SUPABASE POSTGRESQL SCHEMA & ROW LEVEL SECURITY (RLS) POLICIES
-- Paste this script into your Supabase project's SQL Editor and execute.
-- =========================================================================

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- 1. USER PROFILES TABLE
create table if not exists public.user_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null,
    username text not null,
    avatar_url text default '',
    referral_code text unique,
    referred_by text default '',
    is_admin boolean default false,
    created_at timestamptz default timezone('utc'::text, now()) not null
);

-- 2. USER BALANCES & GAME STATE TABLE
create table if not exists public.user_balances (
    user_id uuid primary key references auth.users(id) on delete cascade,
    current_balance bigint default 0 not null,
    total_earned bigint default 0 not null,
    energy_current int default 1000 not null,
    energy_max int default 1000 not null,
    energy_regen_rate int default 2 not null,
    coins_per_tap int default 1 not null,
    total_taps bigint default 0 not null,
    multitap_level int default 1 not null,
    energy_boost_level int default 1 not null,
    last_tap_timestamp bigint default 0 not null,
    updated_at timestamptz default timezone('utc'::text, now()) not null
);

-- 3. TASKS TABLE
create table if not exists public.tasks (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text not null,
    category text default 'DAILY' not null,
    reward_amount bigint default 500 not null,
    requirements text default '',
    action_url text default '',
    is_active boolean default true not null,
    created_at timestamptz default timezone('utc'::text, now()) not null
);

-- 4. TASK SUBMISSIONS TABLE
create table if not exists public.task_submissions (
    id uuid primary key default gen_random_uuid(),
    task_id uuid not null references public.tasks(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    user_email text not null,
    task_title text not null,
    proof_text text not null,
    status text default 'PENDING' not null, -- PENDING, APPROVED, REJECTED
    reward_amount bigint default 0 not null,
    created_at timestamptz default timezone('utc'::text, now()) not null
);

-- 5. WITHDRAWAL REQUESTS TABLE
create table if not exists public.withdrawal_requests (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    user_email text not null,
    amount_coins bigint not null,
    amount_usd numeric(10, 2) not null,
    payment_method text not null, -- PayPal, USDT, Bank Transfer
    account_details text not null,
    status text default 'PENDING' not null, -- PENDING, UNDER_REVIEW, APPROVED, COMPLETED, REJECTED
    admin_notes text default '',
    created_at timestamptz default timezone('utc'::text, now()) not null
);

-- 6. APP REMOTE CONFIGURATION TABLE
create table if not exists public.app_configuration (
    id text primary key,
    coins_per_tap int default 1 not null,
    base_energy int default 1000 not null,
    energy_regen_rate int default 2 not null,
    withdrawal_rate numeric(10, 6) default 0.0001 not null,
    min_withdrawal_coins bigint default 50000 not null,
    maintenance_mode boolean default false not null,
    banner_announcement text default 'Welcome to SMART TAP! Tap to earn digital rewards.',
    updated_at timestamptz default timezone('utc'::text, now()) not null
);

-- 7. ANNOUNCEMENTS TABLE
create table if not exists public.announcements (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    message text not null,
    type text default 'info' not null, -- info, alert, reward
    is_active boolean default true not null,
    created_at timestamptz default timezone('utc'::text, now()) not null
);

-- Insert default app configuration if not exists
insert into public.app_configuration (id, coins_per_tap, base_energy, energy_regen_rate, withdrawal_rate, min_withdrawal_coins, maintenance_mode, banner_announcement)
values ('default', 1, 1000, 2, 0.0001, 50000, false, 'Welcome to SMART TAP! Tap to earn digital rewards.')
on conflict (id) do nothing;

-- Insert starter tasks if empty
insert into public.tasks (title, description, category, reward_amount, requirements)
values 
  ('Join Official Telegram', 'Subscribe to the Smart Tap announcement channel', 'SOCIAL', 2500, 'Submit your Telegram handle'),
  ('Follow on X (Twitter)', 'Follow @SmartTapApp and repost pinned message', 'SOCIAL', 2000, 'Submit your X @handle'),
  ('Daily Tapping Sprint', 'Complete at least 500 taps in a single day', 'DAILY', 1000, 'Automatic upon reaching 500 taps'),
  ('Invite 3 Friends', 'Bring 3 active players to the Smart Tap arena', 'REFERRAL', 10000, 'Submit user emails or friend referral tags')
on conflict do nothing;

-- =========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =========================================================================

alter table public.user_profiles enable row level security;
alter table public.user_balances enable row level security;
alter table public.tasks enable row level security;
alter table public.task_submissions enable row level security;
alter table public.withdrawal_requests enable row level security;
alter table public.app_configuration enable row level security;
alter table public.announcements enable row level security;

-- Profiles: Authenticated users can read all profiles (for leaderboard/friends), update own
create policy "Allow read user_profiles" on public.user_profiles for select using (true);
create policy "Allow insert own profile" on public.user_profiles for insert with check (auth.uid() = id);
create policy "Allow update own profile" on public.user_profiles for update using (auth.uid() = id or auth.jwt()->>'email' = 'kyu375586@gmail.com');

-- Balances: Read all (leaderboard), insert own, update own or admin
create policy "Allow read user_balances" on public.user_balances for select using (true);
create policy "Allow insert own balance" on public.user_balances for insert with check (auth.uid() = user_id);
create policy "Allow update own balance" on public.user_balances for update using (auth.uid() = user_id or auth.jwt()->>'email' = 'kyu375586@gmail.com');

-- Tasks: Everyone can read active tasks, admin can manage
create policy "Allow read active tasks" on public.tasks for select using (true);
create policy "Allow admin manage tasks" on public.tasks for all using (auth.jwt()->>'email' = 'kyu375586@gmail.com');

-- Submissions: User can insert and read own, admin can read and update all
create policy "Allow read own submissions" on public.task_submissions for select using (auth.uid() = user_id or auth.jwt()->>'email' = 'kyu375586@gmail.com');
create policy "Allow insert own submission" on public.task_submissions for insert with check (auth.uid() = user_id);
create policy "Allow admin update submissions" on public.task_submissions for update using (auth.jwt()->>'email' = 'kyu375586@gmail.com');

-- Withdrawals: User can insert and read own, admin can read and update all
create policy "Allow read own withdrawals" on public.withdrawal_requests for select using (auth.uid() = user_id or auth.jwt()->>'email' = 'kyu375586@gmail.com');
create policy "Allow insert own withdrawal" on public.withdrawal_requests for insert with check (auth.uid() = user_id);
create policy "Allow admin update withdrawals" on public.withdrawal_requests for update using (auth.jwt()->>'email' = 'kyu375586@gmail.com');

-- App Config & Announcements: Everyone can read, admin can update
create policy "Allow read app_configuration" on public.app_configuration for select using (true);
create policy "Allow admin update app_configuration" on public.app_configuration for all using (auth.jwt()->>'email' = 'kyu375586@gmail.com');

create policy "Allow read announcements" on public.announcements for select using (true);
create policy "Allow admin manage announcements" on public.announcements for all using (auth.jwt()->>'email' = 'kyu375586@gmail.com');
