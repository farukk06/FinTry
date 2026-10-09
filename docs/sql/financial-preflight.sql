-- Read-only preflight. Run only against an explicitly verified target after review.
select current_database(), current_schema(), version();
select table_name,column_name,data_type,numeric_precision,numeric_scale,is_nullable
from information_schema.columns where table_schema=current_schema()
and table_name in ('users','instruments','virtual_accounts','portfolio_assets','transactions','balance_requests')
order by table_name,ordinal_position;
select c.conrelid::regclass as table_name,c.conname,pg_get_constraintdef(c.oid),c.convalidated
from pg_constraint c join pg_namespace n on n.oid=c.connamespace
where n.nspname=current_schema()
order by table_name,c.conname;
select user_id,instrument_id,count(*) from portfolio_assets group by user_id,instrument_id having count(*)>1;
select * from instruments where price is null or price<=0 or price='NaN'::numeric;
select * from virtual_accounts where balance is null or balance<0 or balance='NaN'::numeric;
select * from portfolio_assets where quantity is null or quantity<=0 or quantity='NaN'::numeric
or average_price is null or average_price<=0 or average_price='NaN'::numeric;
select * from transactions where quantity is null or quantity<=0 or quantity='NaN'::numeric
or price is null or price<=0 or price='NaN'::numeric
or total_amount is null or total_amount<=0 or total_amount='NaN'::numeric
or total_amount<>price*quantity or type is null or transaction_time is null;
select * from balance_requests where requested_amount is null or requested_amount<=0
or requested_amount='NaN'::numeric or status is null or created_at is null;
select a.* from virtual_accounts a left join users r on a.user_id=r.id where r.id is null;
select a.* from portfolio_assets a left join users r on a.user_id=r.id where r.id is null;
select a.* from portfolio_assets a left join instruments r on a.instrument_id=r.id where r.id is null;
select a.* from transactions a left join users r on a.user_id=r.id where r.id is null;
select a.* from transactions a left join instruments r on a.instrument_id=r.id where r.id is null;
select a.* from balance_requests a left join users r on a.user_id=r.id where r.id is null;
