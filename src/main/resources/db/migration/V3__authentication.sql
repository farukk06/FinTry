-- Additive upgrade from V2. Legacy identities and all financial relations remain intact.
set local lock_timeout = '5s';
lock table users in access exclusive mode;
-- Never silently merge identities that become ambiguous under case-insensitive login.
do $$
begin
 if exists(select 1 from users group by lower(btrim(email)) having count(*) > 1) then
  raise exception 'FinTry auth preflight: ambiguous email identities; manual review required';
 end if;
end $$;
alter table users add column password_hash varchar(255);
alter table users add column enabled boolean not null default false;
create unique index uk_users_email_normalized on users(lower(btrim(email)));
alter table users add constraint ck_users_auth_enabled check
 (not enabled or (password_hash is not null and length(password_hash) > 0));
-- No credentials, role promotions, account creation, or financial data updates here.
