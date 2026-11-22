-- KEYS[1] = per-order idempotency marker
-- KEYS[2..n] = hourly inventory keys
-- ARGV[1] = quantity, ARGV[2] = marker TTL seconds
if redis.call('exists', KEYS[1]) == 1 then
  return 2
end

local quantity = tonumber(ARGV[1])
for i = 2, #KEYS do
  local stock = tonumber(redis.call('get', KEYS[i]) or '0')
  if stock < quantity then
    return 0
  end
end

for i = 2, #KEYS do
  redis.call('decrby', KEYS[i], quantity)
end
redis.call('set', KEYS[1], '1', 'EX', ARGV[2])
return 1
