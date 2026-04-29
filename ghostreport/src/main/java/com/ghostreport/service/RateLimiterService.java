package com.ghostreport.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 10;

    public void checkLimit(String ip) {

        attempts.put(ip, attempts.getOrDefault(ip, 0) + 1);

        if (attempts.get(ip) > MAX_ATTEMPTS) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas tentativas. Tente mais tarde."
            );
        }
    }
}