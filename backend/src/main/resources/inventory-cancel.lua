-- KEYS[1]   : lock record key, inventory:locked:{orderId}
-- KEYS[2..] : every station segment key for the order's travel interval
-- ARGV[1]   : ticket quantity
-- ARGV[2]   : record TTL in seconds (same as the lock record)
--
-- Cancel of an UNPAID order. Stock is returned only when this order's lock really deducted it
-- (record == '1'). When no lock has run yet, a RELEASED tombstone is written so a late lock event
-- hits inventory-lock.lua's EXISTS check and deducts nothing.
-- Returns: 1 = stock returned, 0 = nothing to return.
local state = redis.call('GET', KEYS[1])
if state == '1' then
    for i = 2, #KEYS do
        redis.call('INCRBY', KEYS[i], ARGV[1])
    end
    redis.call('SET', KEYS[1], 'RELEASED', 'EX', tonumber(ARGV[2]))
    return 1
end
if not state then
    redis.call('SET', KEYS[1], 'RELEASED', 'EX', tonumber(ARGV[2]))
end
return 0
