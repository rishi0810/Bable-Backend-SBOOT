package com.bable.b_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
public class RedisConfig {

    @Bean
    public RedisClient redisClient(RedisConnectionFactory factory, ObjectMapper mapper) {
        return new RedisClient(new StringRedisTemplate(factory), mapper);
    }

    @RequiredArgsConstructor
    public static class RedisClient {

        private final StringRedisTemplate redis;
        private final ObjectMapper mapper;

        public void set(String key, String value) {
            redis.opsForValue().set(key, value);
        }

        public String get(String key) {
            return redis.opsForValue().get(key);
        }

        public <T> void setObject(String key, T value) {
            try {
                redis.opsForValue().set(key, mapper.writeValueAsString(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T getObject(String key, Class<T> entityClass) {
            try {
                String json = redis.opsForValue().get(key);
                if (json == null) {
                    return null;
                }
                return mapper.readValue(json, entityClass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void del(String key) {
            redis.delete(key);
        }

       public boolean exists(String key) {
           return redis.hasKey(key);
        }

        public <T> void setList(String key, List<T> value) {
            try {
                redis.opsForValue().set(key, mapper.writeValueAsString(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public <T> List<T> getList(String key, Class<T> entityClass) {
            try {
                String json = redis.opsForValue().get(key);
                if (json == null) return null;
                JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, entityClass);
                return mapper.readValue(json, type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
