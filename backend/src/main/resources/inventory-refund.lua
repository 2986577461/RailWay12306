-- KEYS[1]   : lock record key, inventory:locked:{orderId}
-- KEYS[2..] : every station segment key for the order's travel interval
-- ARGV[1]   : ticket quantity
-- ARGV[2]   : tombstone TTL in seconds
--
-- Refund of a PAID order. The order's lock succeeded, so the stock goes back regardless of the
-- record (the record may have expired after 24h, which is why it is not checked). The RELEASED
-- tombstone makes a retried refund a no-op, so the stock is returned exactly once.
-- Refuses to run (-1) while the ledger rebuild holds inventory:rebuilding.
-- Returns: 1 = stock returned, 0 = already returned, -1 = rebuilding, retry later.
if redis.call('EXISTS', 'inventory:rebuilding') == 1 then
    return -1
end
if redis.call('GET', KEYS[1]) == 'RELEASED' then
    return 0
end
for i = 2, #KEYS do
    redis.call('INCRBY', KEYS[i], ARGV[1])
end
redis.call('SET', KEYS[1], 'RELEASED', 'EX', tonumber(ARGV[2]))
return 1
