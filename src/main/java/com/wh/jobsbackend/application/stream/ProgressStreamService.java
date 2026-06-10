package com.wh.jobsbackend.application.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Slf4j
@Service
public class ProgressStreamService {
    private static final long NO_TIMEOUT = 0L;

    private final ObjectMapper objectMapper;
    private final Supplier<SseEmitter> emitterFactory;
    private final Map<UserTopic, CopyOnWriteArrayList<SseEmitter>> emittersByTopic = new ConcurrentHashMap<>();

    @Autowired
    public ProgressStreamService(ObjectMapper objectMapper) {
        this(objectMapper, () -> new SseEmitter(NO_TIMEOUT));
    }

    ProgressStreamService(ObjectMapper objectMapper, Supplier<SseEmitter> emitterFactory) {
        this.objectMapper = objectMapper;
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter open(Long userId, String topic, Object connectedPayload) {
        UserTopic userTopic = new UserTopic(userId, topic);
        SseEmitter emitter = emitterFactory.get();
        emittersByTopic.computeIfAbsent(userTopic, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userTopic, emitter));
        emitter.onTimeout(() -> remove(userTopic, emitter));
        emitter.onError(error -> remove(userTopic, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(connectedPayload));
        } catch (IOException e) {
            log.debug("Failed to send SSE connected event for topic {}", topic, e);
            removeAndComplete(userTopic, emitter);
        }
        return emitter;
    }

    public void publish(Long userId, String topic, String eventName, Object payload) {
        UserTopic userTopic = new UserTopic(userId, topic);
        String data = toJson(payload);
        for (SseEmitter emitter : subscribers(userTopic)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                handleSendFailure(userTopic, emitter, eventName, e);
            }
        }
    }

    public void heartbeat(Long userId, String topic) {
        UserTopic userTopic = new UserTopic(userId, topic);
        for (SseEmitter emitter : subscribers(userTopic)) {
            try {
                emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
            } catch (Exception e) {
                handleSendFailure(userTopic, emitter, "ping", e);
            }
        }
    }

    public void heartbeatAll(String topic) {
        for (UserTopic userTopic : emittersByTopic.keySet()) {
            if (userTopic.topic().equals(topic)) {
                heartbeat(userTopic.userId(), topic);
            }
        }
    }

    int subscriberCount(Long userId, String topic) {
        return emittersByTopic.getOrDefault(new UserTopic(userId, topic), new CopyOnWriteArrayList<>()).size();
    }

    private List<SseEmitter> subscribers(UserTopic userTopic) {
        return emittersByTopic.getOrDefault(userTopic, new CopyOnWriteArrayList<>());
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize SSE payload", e);
        }
    }

    private void handleSendFailure(UserTopic userTopic, SseEmitter emitter, String eventName, Exception e) {
        if (isDisconnected(e)) {
            log.debug("SSE client disconnected for topic {} while sending {}", userTopic.topic(), eventName);
        } else {
            log.error("Failed to send SSE event {} for topic {}", eventName, userTopic.topic(), e);
        }
        removeAndComplete(userTopic, emitter);
    }

    private boolean isDisconnected(Exception e) {
        return e instanceof AsyncRequestNotUsableException
                || e instanceof ClientAbortException
                || e.getCause() instanceof ClientAbortException
                || e instanceof IOException;
    }

    private void removeAndComplete(UserTopic userTopic, SseEmitter emitter) {
        remove(userTopic, emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private void remove(UserTopic userTopic, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTopic.get(userTopic);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByTopic.remove(userTopic);
        }
    }

    private record UserTopic(Long userId, String topic) {
    }
}
