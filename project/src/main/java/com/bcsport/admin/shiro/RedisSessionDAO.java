package com.bcsport.admin.shiro;

import com.bcsport.admin.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * F33: Redis 会话持久化。此前 SessionDAO 为纯内存（MemorySessionDAO），
 * 应用重启全员掉线、无法水平扩展。继承 CachingSessionDAO：Shiro 先查本地缓存，
 * 未命中走本 DAO 的 Redis 读写；session 序列化为 byte[] 存 Redis，
 * TTL 与会话超时对齐（configService 读 security.sessionTimeout，默认 30 分钟）。
 */
@Slf4j
public class RedisSessionDAO extends CachingSessionDAO {

    private static final String KEY_PREFIX = "shiro:session:";

    private final RedisTemplate<String, byte[]> redisTemplate;
    private final ConfigService configService;

    public RedisSessionDAO(RedisTemplate<String, byte[]> redisTemplate, ConfigService configService) {
        this.redisTemplate = redisTemplate;
        this.configService = configService;
    }

    private int timeoutMinutes() {
        return configService.getInt("security.sessionTimeout", 30);
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = session.getId();
        saveToRedis(session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        return readFromRedis(sessionId);
    }

    @Override
    protected void doUpdate(Session session) {
        saveToRedis(session);
    }

    @Override
    protected void doDelete(Session session) {
        try {
            redisTemplate.delete(KEY_PREFIX + session.getId());
        } catch (Exception e) {
            log.error("Redis 删除会话失败, sessionId={}", session.getId(), e);
        }
    }

    private void saveToRedis(Session session) {
        try {
            int minutes = timeoutMinutes();
            redisTemplate.opsForValue().set(KEY_PREFIX + session.getId(), serialize(session),
                    minutes + 10, TimeUnit.MINUTES); // TTL 比会话超时长 10 分钟缓冲
        } catch (Exception e) {
            log.error("Redis 保存会话失败, sessionId={}", session.getId(), e);
        }
    }

    private Session readFromRedis(Serializable sessionId) {
        try {
            byte[] data = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            if (data == null) {
                return null;
            }
            return (Session) deserialize(data);
        } catch (Exception e) {
            log.error("Redis 读取会话失败, sessionId={}", sessionId, e);
            return null;
        }
    }

    private byte[] serialize(Session session) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(session);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("会话序列化失败", e);
        }
    }

    private Session deserialize(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Session) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("会话反序列化失败", e);
        }
    }
}
