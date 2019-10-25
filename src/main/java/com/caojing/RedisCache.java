package com.caojing;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

import java.io.Serializable;
import java.util.*;

import static com.caojing.RedisCache.RedisConfig.DEV;
import static com.caojing.RedisCache.RedisConfig.TEST;


/**
 * redis工具类
 *
 * @author CaoJing
 * @date 2019/3/28 17:30
 */
@Data
@Slf4j
public class RedisCache {

    public enum RedisConfig {

        /**
         * dev 8 号库
         * test 9 号库
         */
        DEV(
            "192.168.255.2",
            6379,
            null,
            200,
            10000,
            8
        ),

        /**
         * dev 8 号库
         * test 9 号库
         */
        TEST(
            "192.168.255.2",
            6379,
            null,
            200,
            10000,
            9
        );

        private String host;
        private int port;
        private String password;
        private int maxIdle;
        private int maxWait;
        private int datebase;

        RedisConfig(String host, int port, String password, int maxIdle, int maxWait, int datebase) {
            this.host = host;
            this.port = port;
            this.password = password;
            this.maxIdle = maxIdle;
            this.maxWait = maxWait;
            this.datebase = datebase;
        }
    }

    private static String host = DEV.host;

    private static int port = DEV.port;

    private static String password = DEV.password;

    private static int maxIdle = DEV.maxIdle;

    private static int maxWait = DEV.maxWait;

    private static int datebase = DEV.datebase;

    private static JedisPool jedisPool;

    /**
     * 根据环境变量获取 RedisConfig
     */
    private static RedisConfig getRedisConfigByEnv(String env) {
        if ("dev".equals(env)) {
            return DEV;
        } else if ("test".equals(env)) {
            return TEST;
        }
        return DEV;
    }

    private static void initialPool() {

        try {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxIdle(maxIdle);
            config.setMaxWaitMillis(maxWait);
            config.setTestOnReturn(false);
            config.setTestWhileIdle(false);
            config.setBlockWhenExhausted(true);
            config.setLifo(false);
            config.setSoftMinEvictableIdleTimeMillis(120000);

            jedisPool = new JedisPool(config, host, port, 100000, StringUtils.isBlank(password) ? null : password, datebase);

        } catch (Exception e) {
            log.error("redis连接池初始化失败", e);
        }
    }

    /**
     * 根据环境变量初始化Redis
     */
    public static void initialPool(String env) {
        RedisConfig redisConfig = getRedisConfigByEnv(env);
        try {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxIdle(redisConfig.maxIdle);
            config.setMaxWaitMillis(redisConfig.maxWait);
            config.setTestOnReturn(false);
            config.setTestWhileIdle(false);
            config.setBlockWhenExhausted(true);
            config.setLifo(false);
            config.setSoftMinEvictableIdleTimeMillis(120000);

            jedisPool = new JedisPool(
                config, redisConfig.host, redisConfig.port,
                100000, StringUtils.isBlank(redisConfig.password) ? null : redisConfig.password,
                redisConfig.datebase
            );

        } catch (Exception e) {
            log.error("redis连接池初始化失败", e);
        }
    }

    /**
     * 获取Jedis实例
     *
     * @return Jedis
     */
    public static synchronized Jedis getJedis() {

        if (jedisPool == null) {
            initialPool();
        }

        return Optional.ofNullable(jedisPool).map(JedisPool::getResource).orElse(null);
    }

    /**
     * 释放jedis资源
     *
     * @param jedis
     */
    private static void returnResource(final Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 设置 过期时间
     *
     * @param key
     * @param value
     */
    public static String setString(String key, String value) {
        Jedis jedis = getJedis();
        String r = value = StringUtils.isNotEmpty(value) ? value : "";
        if (jedis.exists(key)) {
            jedis.del(key);
        }
        jedis.set(key, value);
        returnResource(jedis);
        return r;
    }

    /**
     * 设置字符串
     *
     * @param key
     * @param seconds 过期时间（以秒为单位）
     * @param value
     */
    public static String setString(String key, String value, int seconds) {
        Jedis jedis = getJedis();
        String r = value = StringUtils.isNotEmpty(value) ? value : "";
        jedis.setex(key, seconds, value);
        returnResource(jedis);
        return r;

    }

    /**
     * 设置 (互斥)
     *
     * @param key
     * @param value
     */
    public static boolean setStringIfNotExist(String key, String value) {
        if (StringUtils.isBlank(key)) {
            return false;
        }

        String valueNew = value;
        if (StringUtils.isBlank(value)) {
            valueNew = "";
        }

        Jedis jedis = getJedis();
        if (jedis == null) {
            return false;
        }

        long ret = jedis.setnx(key, valueNew);
        returnResource(jedis);

        return ret == 1;
    }

    /**
     * 一次性锁（适用于只要求执行一次）
     */
    public static boolean onceLock(String key) {
        if (setStringIfNotExist("autoReleaseLock:" + key, "")) {
            setString("autoReleaseLock:" + key, "", 5 * 60);
            return true;
        }
        return false;
    }

    /**
     * 设置 (互斥)
     *
     * @param key
     * @param value
     * @parm timeout seconds
     */
    public static boolean setStringIfNotExist(String key, String value, int timeout) {
        if (StringUtils.isBlank(key)) {
            return false;
        }

        if (timeout <= 0) {
            return false;
        }

        List<Object> objectList = null;
        String valueNew = value;
        if (StringUtils.isBlank(value)) {
            valueNew = "";
        }

        Jedis jedis = getJedis();
        if (jedis == null) {
            return false;
        }

        Transaction transaction = jedis.multi();

        transaction.setnx(key, value);
        transaction.expire(key, timeout);
        objectList = transaction.exec();
        returnResource(jedis);

        long ret = 0;
        if (objectList == null || objectList.size() <= 0) {
            return false;
        } else {
            ret = (long) objectList.get(0);
        }

        // 成功
        return ret == 1;
        // 失败
    }

    public static void setExpiredTime(String key, Integer seconds) {
        Jedis jedis = getJedis();
        jedis.expire(key, seconds);
        returnResource(jedis);
    }

    public static Long queryRemainingTimes(String key) {
        Jedis jedis = getJedis();
        Long ttl = jedis.ttl(key);
        returnResource(jedis);
        return ttl;
    }

    /**
     * 获取String值
     *
     * @param key
     * @return value
     */
    public static String getString(String key) {
        Jedis jedis = getJedis();
        if (jedis == null || !jedis.exists(key)) {
            returnResource(jedis);
            return null;
        }
        String value = jedis.get(key);
        returnResource(jedis);
        if (null == value || "".equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 将对象存入redis
     *
     * @param key    键名
     * @param object 对象
     */
    public static <T extends Serializable> void writeObject(String key, T object) {
        Jedis jedis = getJedis();
        if (null != object) {
            jedis.set(key.getBytes(), SerializeUtil.serialize(object));
        }
        returnResource(jedis);
    }

    /**
     * 这个支持存集合
     */
    public static void writeObject(String key, Object object) {
        Jedis jedis = getJedis();
        if (null != object) {
            jedis.set(key.getBytes(), SerializeUtil.serialize(object));
        }
        returnResource(jedis);
    }

    /**
     * 从redis中取数据
     *
     * @param key
     * @return
     */
    public static Object getObject(String key) {
        Jedis jedis = getJedis();
        byte[] content = jedis.get((key).getBytes());
        returnResource(jedis);
        if (null == content || content.length == 0) {
            return null;
        }
        return SerializeUtil.unserialize(content);
    }

    public static boolean isExistsScript(String hash) {
        Jedis jedis = getJedis();
        boolean test = jedis.scriptExists(hash);
        returnResource(jedis);
        return test;
    }

    /**
     * 将对象存入redis，并设置过期时间
     *
     * @param key     键名
     * @param seconds 过期秒数
     * @param object  待存对象
     * @return
     */
    public static <T extends Serializable> void writeObject(String key, T object, int seconds) {
        Jedis jedis = getJedis();
        if (null != object) {
            if (seconds <= 0) {
                jedis.set(key.getBytes(), SerializeUtil.serialize(object));
            } else {
                jedis.setex(key.getBytes(), seconds, SerializeUtil.serialize(object));
            }
        }

        returnResource(jedis);
    }

    /**
     * 从redis中取数据
     *
     * @param key
     * @return
     */
    public static <T extends Serializable> T getObject(String key, Class<T> clazz) {
        Jedis jedis = getJedis();
        byte[] content = jedis.get((key).getBytes());
        returnResource(jedis);
        if (null == content || content.length == 0) {
            return null;
        }
        T t = (T) SerializeUtil.unserialize(content);
        return t;
    }

    /**
     * 将对象存入redis，并设置过期时间
     *
     * @param key     键名
     * @param seconds 过期秒数
     * @param object  待存对象
     * @return
     */
    public static <T extends Serializable> void writeObjectS(String key, T object, int seconds) {
        Jedis jedis = getJedis();
        String value = SerializeUtil.serializeBase64(object);
        if (seconds <= 0) {
            jedis.set(key, value);
        } else {
            jedis.setex(key, seconds, value);
        }
        returnResource(jedis);
    }

    /**
     * 从redis中取数据
     *
     * @param key
     * @return
     */
    public static <T extends Serializable> T getObjectS(String key, Class<T> clazz) {
        Jedis jedis = getJedis();
        String content = jedis.get(key);
        returnResource(jedis);
        if (null == content || "".equals(content)) {
            return null;
        }
        T t = (T) SerializeUtil.unserializeBase64(content);
        return t;
    }

    public static void rpushString(String key, String[] values, int seconds) {
        if (values == null || values.length == 0) {
            return;
        }
        Jedis jedis = getJedis();
        try {
            jedis.rpush(key, values);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> void rpush(String key, Object values) {

        Jedis jedis = getJedis();
        try {
            String value = SerializeUtil.serializeBase64(values);
            jedis.rpush(key, value);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> void rpush(String key, T[] values, int seconds) {
        if (values == null || values.length == 0) {
            return;
        }
        Jedis jedis = getJedis();
        try {
            String[] arr = new String[values.length];
            int i = 0;
            for (T t : values) {
                String value = SerializeUtil.serializeBase64(t);
                arr[i] = value;
                i++;
            }
            jedis.rpush(key, arr);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> void lpop(String key) {
        Jedis jedis = getJedis();
        try {
            jedis.lpop(key);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static long listLength(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.llen(key);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void lpushString(String key, String[] values, int seconds) {
        if (values == null || values.length == 0) {
            return;
        }
        Jedis jedis = getJedis();

        try {
            jedis.lpush(key, values);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> List<T> lrange(String key, int a, int b, Class<T> clazz) {
        Jedis jedis = getJedis();
        try {
            List<String> values = jedis.lrange(key, a, b);
            if (values == null || values.size() == 0) {
                return null;
            } else {
                List<T> list = new ArrayList<T>();
                for (String str : values) {
                    T t = (T) SerializeUtil.unserializeBase64(str);
                    list.add(t);
                }
                return list;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static List<String> lrange(String key, int a, int b) {
        Jedis jedis = getJedis();
        try {
            List<String> values = jedis.lrange(key, a, b);
            if (values == null || values.size() == 0) {
                return null;
            } else {
                return values;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void hsetString(String key, String field, String value, int seconds) {
        if (value == null) {
            return;
        }
        Jedis jedis = getJedis();
        try {
            jedis.hset(key, field, value);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void hmset(String key, Map map) {
        Jedis jedis = getJedis();
        jedis.hmset(key, map);
        returnResource(jedis);
    }

    public static <T extends Serializable> void hset(String key, String field, T t, int seconds) {
        if (t == null) {
            return;
        }
        Jedis jedis = getJedis();
        try {
            String value = SerializeUtil.serializeBase64(t);
            jedis.hset(key, field, value);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static String hget(String key, String field) {
        Jedis jedis = getJedis();
        try {
            String value = jedis.hget(key, field);
            if (value == null || "".equals(value)) {
                return null;
            } else {
                return value;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> T hget(String key, String field, Class<T> clazz) {
        Jedis jedis = getJedis();
        try {
            String value = jedis.hget(key, field);
            if (value == null || "".equals(value)) {
                return null;
            } else {
                T t = (T) SerializeUtil.unserializeBase64(value);
                return t;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static List<String> hgetAll(String key) {
        Jedis jedis = getJedis();
        try {
            List<String> list = null;
            Set<String> fields = jedis.hkeys(key);
            if (fields != null) {
                list = new ArrayList<String>();
                for (String field : fields) {
                    String value = jedis.hget(key, field);
                    list.add(value);
                }
            }
            return list;
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static <T extends Serializable> List<T> hgetAll(String key, Class<T> clazz) {
        Jedis jedis = getJedis();
        try {
            List<T> list = null;
            Set<String> fields = jedis.hkeys(key);
            if (fields != null) {
                list = new ArrayList<T>();
                for (String field : fields) {
                    String value = jedis.hget(key, field);
                    T t = (T) SerializeUtil.unserializeBase64(value);
                    list.add(t);
                }
            }
            return list;
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static Set<String> hkeys(String key) {
        Jedis jedis = getJedis();
        try {
            Set<String> set = jedis.hkeys(key);
            return set;
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 检查特定键在redis中是否存在
     */
    public static boolean hexists(String key, String field) {
        Jedis jedis = getJedis();
        if (jedis.hexists(key, field)) {
            returnResource(jedis);
            return true;
        }
        returnResource(jedis);
        return false;
    }

    /**
     * 删除特定redis键
     */
    public static void hdel(String key, String field) {
        Jedis jedis = getJedis();
        jedis.hdel(key, field);
        returnResource(jedis);
    }

    /**
     * 从redis中取数据
     */
    public static Set<String> zrangeByScore(String key, String min, String max) {
        Jedis jedis = getJedis();
        try {
            Set<String> set = jedis.zrangeByScore(key, min, max);
            return set;
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void zrem(String key, String... members) {
        Jedis jedis = getJedis();
        try {
            jedis.zrem(key, members);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void zremrangeByRank(String key, int start, int end) {
        Jedis jedis = getJedis();
        try {
            jedis.zremrangeByRank(key, start, end);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void zadd(String key, double score, String member) {
        Jedis jedis = getJedis();
        try {
            jedis.zadd(key, score, member);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static Set<String> keys(String key) {
        Jedis jedis = getJedis();
        try {
            Set<String> set = jedis.keys(key);
            return set;
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public static void expire(String key, int seconds) {
        Jedis jedis = getJedis();
        jedis.expire(key, seconds);
        returnResource(jedis);
    }

    /**
     * 检查特定键在redis中是否存在
     *
     * @param key
     * @return boolean
     */
    public static boolean isExists(String key) {
        Jedis jedis = getJedis();
        if (jedis.exists(key.getBytes())) {
            returnResource(jedis);
            return true;
        }
        returnResource(jedis);
        return false;
    }

    /**
     * 删除特定redis键
     *
     * @param key
     */
    public static void delObject(String key) {
        Jedis jedis = getJedis();
        jedis.del((key).getBytes());
        returnResource(jedis);
    }

    public static void del(String key) {
        Jedis jedis = getJedis();
        jedis.del(key);
        returnResource(jedis);
    }

    public static String checkConn() {
        int a = jedisPool.getNumActive();
        int b = jedisPool.getNumIdle();
        int c = jedisPool.getNumWaiters();
        return a + "," + b + "," + c;
    }

    public static Long zcard(String key) {
        Jedis jedis = getJedis();
        Long count = jedis.zcard(key);
        returnResource(jedis);
        return count > 0L ? count : 0;
    }


    public static void batchZadd(String key, Map<String, Double> sourceMember) {
        Jedis jedis = getJedis();
        jedis.zadd(key, sourceMember);
        returnResource(jedis);
    }

    public static void sadd(String key, String value) {
        Jedis jedis = getJedis();
        jedis.sadd(key, value);
        returnResource(jedis);
    }

    public static void sdel(String key, String value) {
        Jedis jedis = getJedis();
        jedis.srem(key, value);
        returnResource(jedis);
    }


    public static Set<Tuple> zrangeAll(String key) {
        Jedis jedis = getJedis();
        Set<Tuple> tuple = jedis.zrangeWithScores(key, 0, -1);
        returnResource(jedis);
        return tuple.size() > 0 ? tuple : null;
    }

    /**
     * @param key
     * @param page     页数
     * @param pagesize 每页数量
     * @param sort     0:正序score从小到大,1:倒叙score从大到小
     * @return Set
     */
    public static Set<Tuple> zrange(String key, int page, int pagesize, int sort) {
        //正序
        if (page == 0) {
            //防止出现0
            page = 1;
        }
        if (pagesize == 0) {
            pagesize = 1;
        }
        Jedis jedis = getJedis();
        //获取总数
        Long count = jedis.zcard(key);
        Set<Tuple> tuple = null;
        if (sort == 0) {
            Long pageL = new Long(page);
            Long pagesizeL = new Long(pagesize);
            Long startIndex = ((pageL - 1) * pagesizeL);
            Long stopIndex = (pageL * pagesizeL - 1l);
            tuple = jedis.zrangeWithScores(key, startIndex, stopIndex);
        } else if (sort == 1) {
            //倒叙
            Long pageL = new Long(page);
            Long pagesizeL = new Long(pagesize);
            Long startIndex = ((pageL - 1) * pagesizeL);
            Long stopIndex = (pageL * pagesizeL - 1l);
            tuple = jedis.zrevrangeWithScores(key, startIndex, stopIndex);
        } else {
            //倒叙
        }
        returnResource(jedis);
        return tuple.size() > 0 ? tuple : null;
    }

    public static byte[] getByte(String key) {
        Jedis jedis = getJedis();
        byte[] bs = jedis.get(key.getBytes());
        returnResource(jedis);
        return null != bs && bs.length > 0L ? bs : null;
    }

    public static void writeByte(String key, byte[] value, int seconds) {
        Jedis jedis = getJedis();
        if (null != value && value.length > 0) {
            if (seconds <= 0) {
                jedis.set(key.getBytes(), value);
            } else {
                jedis.setex(key.getBytes(), seconds, value);
            }
        }

        returnResource(jedis);
    }

    public static Object executeScript(String hash, List<String> keys, List<String> args) {
        Jedis jedis = getJedis();
        log.info(keys.toString());
        log.info(args.toString());

        Object o = jedis.evalsha(hash, keys, args);
        returnResource(jedis);
        return o;
    }

    public static void delAllKeys() {
        Jedis jedis = getJedis();
        jedis.flushDB();
        returnResource(jedis);
    }

    public static Object evalSha(String script, int keyCount, String... params) {
        Jedis jedis = getJedis();
        log.info(script);
        for (int i = 0; i < params.length; i++) {
            log.info(params[i]);
        }
        Object o = jedis.evalsha(script, keyCount, params);
        returnResource(jedis);
        return o;
    }

    public static Object lindex(String key, int index) {
        Jedis jedis = getJedis();
        String o = jedis.lindex(key, index);
        returnResource(jedis);
        return o;
    }

    public static boolean existsScript(String hash) {
        Jedis jedis = getJedis();
        boolean o = jedis.scriptExists(hash);
        returnResource(jedis);
        return o;
    }

    public static boolean sexists(String key, String value) {
        boolean flag = false;
        Jedis jedis = getJedis();
        flag = jedis.sismember(key, value);
        returnResource(jedis);
        return flag;
    }

    public static void hmset(String key, Map map, int timeout) {
        Jedis jedis = getJedis();

        Transaction transaction = jedis.multi();
        transaction.hmset(key, map);
        transaction.expire(key, timeout);
        transaction.exec();
        returnResource(jedis);
    }

    public static List<String> hmget(String key, String... field) {
        Jedis jedis = getJedis();
        List<String> rsmap = jedis.hmget(key, field);
        returnResource(jedis);
        return rsmap;
    }

    // 是否存在key为user的记录 返回true
    public static boolean hmexists(String key) {
        Jedis jedis = getJedis();
        boolean b = jedis.exists(key);
        returnResource(jedis);
        return b;
    }

    // 是否存在key为user的记录 返回true
    public static List<String> hvals(String key) {
        Jedis jedis = getJedis();
        List<String> strLst = jedis.hvals(key);
        returnResource(jedis);
        return strLst;
    }

    // 是否存在key为user的记录 返回true
    public static void del(String... key) {
        Jedis jedis = getJedis();
        jedis.del(key);
        returnResource(jedis);
    }

    public static String get(String key) {
        Jedis jedis = getJedis();
        String str = jedis.get(key);
        returnResource(jedis);
        return str;
    }

    // limitNum: 最大个数
    // timeout: 过期时间，秒
    public static void ZADDWithLimit(String key, double score, String member, int limitNum, int timeout) {
        Jedis jedis = getJedis();

        Transaction transaction = jedis.multi();
        transaction.zadd(key, score, member);
        if (limitNum > 0) {
            transaction.zremrangeByRank(key, limitNum, -1);
        }
        if (timeout > 0) {
            transaction.expire(key, timeout);
        }
        transaction.exec();
        returnResource(jedis);
    }

    public static Set<Tuple> zrevrange(String key) {
        Jedis jedis = getJedis();
        Set<Tuple> tuple = jedis.zrangeWithScores(key, 0, -1);
        returnResource(jedis);
        return tuple.size() > 0 ? tuple : null;
    }

}