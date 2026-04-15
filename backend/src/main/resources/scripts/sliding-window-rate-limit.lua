-- Sliding window rate limiter using sorted set
-- KEYS[1] = rate limit key (e.g. rl:ip:127.0.0.1)
-- ARGV[1] = window size in seconds
-- ARGV[2] = max requests allowed in window
-- ARGV[3] = current timestamp in milliseconds

local key = KEYS[1]
local window = tonumber(ARGV[1]) * 1000
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- remove expired entries outside the window
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

local count = redis.call('ZCARD', key)

if count < limit then
    -- unique member = timestamp + random suffix to avoid collisions
    redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
    redis.call('PEXPIRE', key, window)
    -- return remaining quota
    return limit - count - 1
else
    -- blocked, return -1
    return -1
end
