select * from xaction limit 1;

select x.ledger_id, l.name, sum(x.amount) as balance
  from xaction x
  join ledger l on l.id = x.ledger_id
 group by x.ledger_id, l.name
 order by l.name

select * from ledger;