-- KEYS contains every station segment key for the requested travel interval.
-- ARGV[1] is the requested ticket quantity. A zero or negative remaining value rejects the full request.
local quantity = tonumber(ARGV[1])
for _, key in ipairs(KEYS) do
    local remaining = tonumber(redis.call('GET', key) or '-1')
    if remaining < quantity then
        return 0
    end
end
for _, key in ipairs(KEYS) do
    redis.call('DECRBY', key, quantity)
end
return 1
