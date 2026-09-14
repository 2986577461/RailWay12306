-- KEYS[1]   : lock record key, inventory:locked:{orderId}
-- KEYS[2..] : every station segment key for the requested travel interval
-- ARGV[1]   : requested ticket quantity
-- ARGV[2]   : lock record TTL in seconds (>= order payment window)
--
-- The lock record makes locking idempotent PER ORDER: a duplicate event for the same
-- orderId (idempotent double-submit, or RocketMQ at-least-once redelivery) hits the
-- marker and skips decrementing, so one order never deducts inventory twice.
-- Because the whole script runs atomically in Redis, even two consumers racing on the
-- same order end up with exactly one DECRBY.
--
-- Returns: 0 = insufficient stock (nothing deducted), 1 = newly locked, 2 = already locked.
local quantity = tonumber(ARGV[1])
if redis.call('EXISTS', KEYS[1]) == 1 then
    return 2
end
for i = 2, #KEYS do
    local remaining = tonumber(redis.call('GET', KEYS[i]) or '-1')
    if remaining < quantity then
        return 0
    end
end
for i = 2, #KEYS do
    redis.call('DECRBY', KEYS[i], quantity)
end
redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[2]))
return 1
