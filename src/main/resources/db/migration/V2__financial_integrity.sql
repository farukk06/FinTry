-- Transactional, fail-closed upgrade. Never deletes, merges, or repairs financial data.
set local lock_timeout = '5s';
lock table users, instruments, virtual_accounts, portfolio_assets, transactions, balance_requests in access exclusive mode;
do $$
begin
 if (select count(*) from information_schema.columns
     where table_schema=current_schema() and data_type='numeric'
      and numeric_precision=38 and numeric_scale=2
      and (table_name,column_name) in (('instruments','price'),('virtual_accounts','balance'),('portfolio_assets','quantity'),('portfolio_assets','average_price'),('transactions','quantity'),('transactions','price'),('transactions','total_amount'),('balance_requests','requested_amount'))) <> 8 then
  raise exception 'FinTry preflight: unexpected legacy numeric schema; review before baseline';
 end if;
 if not exists(select 1 from pg_constraint c where c.conrelid='virtual_accounts'::regclass
    and c.contype='u' and c.convalidated
    and c.conkey=array[(select attnum from pg_attribute where attrelid='virtual_accounts'::regclass and attname='user_id')]::smallint[]) then
  raise exception 'FinTry preflight: account user uniqueness missing; review before baseline';
 end if;
 if exists(select 1 from portfolio_assets group by user_id, instrument_id having count(*) > 1) then
  raise exception 'FinTry preflight: duplicate portfolio pair; manual review required';
 end if;
 if exists(select 1 from instruments where price is null or price <= 0 or price = 'NaN'::numeric)
 or exists(select 1 from virtual_accounts where balance is null or balance < 0 or balance = 'NaN'::numeric)
 or exists(select 1 from portfolio_assets where quantity is null or quantity <= 0 or quantity = 'NaN'::numeric
   or average_price is null or average_price <= 0 or average_price = 'NaN'::numeric)
 or exists(select 1 from transactions where quantity is null or quantity <= 0 or quantity = 'NaN'::numeric
   or price is null or price <= 0 or price = 'NaN'::numeric
   or total_amount is null or total_amount <= 0 or total_amount = 'NaN'::numeric
   or total_amount <> price * quantity or type is null or transaction_time is null)
 or exists(select 1 from balance_requests where requested_amount is null or requested_amount <= 0
   or requested_amount = 'NaN'::numeric or status is null or created_at is null) then
  raise exception 'FinTry preflight: invalid financial values or inconsistent trade total; manual review required';
 end if;
 if exists(select 1 from virtual_accounts a left join users u on u.id=a.user_id where u.id is null)
 or exists(select 1 from portfolio_assets a left join users u on u.id=a.user_id left join instruments i on i.id=a.instrument_id where u.id is null or i.id is null)
 or exists(select 1 from transactions a left join users u on u.id=a.user_id left join instruments i on i.id=a.instrument_id where u.id is null or i.id is null)
 or exists(select 1 from balance_requests a left join users u on u.id=a.user_id where u.id is null) then
  raise exception 'FinTry preflight: orphan or null relation; manual review required';
 end if;
end $$;
alter table instruments alter column price type numeric(44,8), alter column price set not null;
alter table instruments add constraint ck_instruments_price check (price > 0 and price <> 'NaN'::numeric);
alter table virtual_accounts alter column balance type numeric(52,16), alter column balance set not null;
alter table virtual_accounts add constraint ck_virtual_accounts_balance check (balance >= 0 and balance <> 'NaN'::numeric);
alter table portfolio_assets alter column quantity type numeric(44,8), alter column quantity set not null;
alter table portfolio_assets add constraint ck_portfolio_assets_quantity check (quantity > 0 and quantity <> 'NaN'::numeric);
alter table portfolio_assets alter column average_price type numeric(44,8), alter column average_price set not null;
alter table portfolio_assets add constraint ck_portfolio_assets_average_price check (average_price > 0 and average_price <> 'NaN'::numeric);
alter table transactions alter column quantity type numeric(44,8), alter column quantity set not null;
alter table transactions add constraint ck_transactions_quantity check (quantity > 0 and quantity <> 'NaN'::numeric);
alter table transactions alter column price type numeric(44,8), alter column price set not null;
alter table transactions add constraint ck_transactions_price check (price > 0 and price <> 'NaN'::numeric);
alter table transactions alter column total_amount type numeric(52,16), alter column total_amount set not null;
alter table transactions add constraint ck_transactions_total_amount check (total_amount > 0 and total_amount <> 'NaN'::numeric);
alter table balance_requests alter column requested_amount type numeric(52,16), alter column requested_amount set not null;
alter table balance_requests add constraint ck_balance_requests_requested_amount check (requested_amount > 0 and requested_amount <> 'NaN'::numeric);
alter table portfolio_assets add constraint uk_portfolio_user_instrument unique (user_id, instrument_id);
-- Stabilize the existing account constraint name without rebuilding or replacing its index.
do $$
declare existing_name text;
begin
 select c.conname into existing_name from pg_constraint c
 where c.conrelid='virtual_accounts'::regclass and c.contype='u'
 and c.conkey=array[(select attnum from pg_attribute where attrelid='virtual_accounts'::regclass and attname='user_id')]::smallint[];
 if existing_name <> 'uk_virtual_account_user' then
  execute format('alter table virtual_accounts rename constraint %I to uk_virtual_account_user', existing_name);
 end if;
end $$;
alter table transactions alter column type set not null;
alter table transactions add constraint ck_transactions_total_consistent check (total_amount = price * quantity);
alter table balance_requests alter column user_id set not null, alter column status set not null, alter column created_at set not null;
alter table balance_requests add constraint fk_balance_request_user foreign key (user_id) references users(id) on delete no action;
-- Existing Hibernate FK names may differ. Verify semantics instead of adding duplicate FKs.
do $$
begin
 if not exists (
  select 1 from pg_constraint k
  where k.contype='f' and k.conrelid='virtual_accounts'::regclass and k.confrelid='users'::regclass
   and k.conkey=array[(select attnum from pg_attribute where attrelid='virtual_accounts'::regclass and attname='user_id')]::smallint[]
   and k.confkey=array[(select attnum from pg_attribute where attrelid='users'::regclass and attname='id')]::smallint[]
   and k.confdeltype in ('a','r') and k.convalidated
 ) then
  raise exception 'FinTry preflight: missing or unsafe FK virtual_accounts.user_id; review legacy schema before baseline';
 end if;
end $$;
-- Existing Hibernate FK names may differ. Verify semantics instead of adding duplicate FKs.
do $$
begin
 if not exists (
  select 1 from pg_constraint k
  where k.contype='f' and k.conrelid='portfolio_assets'::regclass and k.confrelid='users'::regclass
   and k.conkey=array[(select attnum from pg_attribute where attrelid='portfolio_assets'::regclass and attname='user_id')]::smallint[]
   and k.confkey=array[(select attnum from pg_attribute where attrelid='users'::regclass and attname='id')]::smallint[]
   and k.confdeltype in ('a','r') and k.convalidated
 ) then
  raise exception 'FinTry preflight: missing or unsafe FK portfolio_assets.user_id; review legacy schema before baseline';
 end if;
end $$;
-- Existing Hibernate FK names may differ. Verify semantics instead of adding duplicate FKs.
do $$
begin
 if not exists (
  select 1 from pg_constraint k
  where k.contype='f' and k.conrelid='portfolio_assets'::regclass and k.confrelid='instruments'::regclass
   and k.conkey=array[(select attnum from pg_attribute where attrelid='portfolio_assets'::regclass and attname='instrument_id')]::smallint[]
   and k.confkey=array[(select attnum from pg_attribute where attrelid='instruments'::regclass and attname='id')]::smallint[]
   and k.confdeltype in ('a','r') and k.convalidated
 ) then
  raise exception 'FinTry preflight: missing or unsafe FK portfolio_assets.instrument_id; review legacy schema before baseline';
 end if;
end $$;
-- Existing Hibernate FK names may differ. Verify semantics instead of adding duplicate FKs.
do $$
begin
 if not exists (
  select 1 from pg_constraint k
  where k.contype='f' and k.conrelid='transactions'::regclass and k.confrelid='users'::regclass
   and k.conkey=array[(select attnum from pg_attribute where attrelid='transactions'::regclass and attname='user_id')]::smallint[]
   and k.confkey=array[(select attnum from pg_attribute where attrelid='users'::regclass and attname='id')]::smallint[]
   and k.confdeltype in ('a','r') and k.convalidated
 ) then
  raise exception 'FinTry preflight: missing or unsafe FK transactions.user_id; review legacy schema before baseline';
 end if;
end $$;
-- Existing Hibernate FK names may differ. Verify semantics instead of adding duplicate FKs.
do $$
begin
 if not exists (
  select 1 from pg_constraint k
  where k.contype='f' and k.conrelid='transactions'::regclass and k.confrelid='instruments'::regclass
   and k.conkey=array[(select attnum from pg_attribute where attrelid='transactions'::regclass and attname='instrument_id')]::smallint[]
   and k.confkey=array[(select attnum from pg_attribute where attrelid='instruments'::regclass and attname='id')]::smallint[]
   and k.confdeltype in ('a','r') and k.convalidated
 ) then
  raise exception 'FinTry preflight: missing or unsafe FK transactions.instrument_id; review legacy schema before baseline';
 end if;
end $$;
