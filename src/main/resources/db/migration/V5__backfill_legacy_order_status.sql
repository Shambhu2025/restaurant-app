-- Orders created before the Kafka flow were fully processed synchronously (totals and items present)
-- but were left with status PENDING. Mark them CONFIRMED. The WHERE clause matches only that exact
-- signature, so genuinely queued orders are never touched, and re-running affects nothing.
UPDATE orders o
SET status = 'CONFIRMED'
WHERE o.status = 'PENDING'
  AND o.idempotency_key IS NULL
  AND o.total IS NOT NULL
  AND EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.id);