package com.wh.jobsbackend.application.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressStreamServiceTest {

    @Test
    void openShouldRegisterEmitterAndPublishJsonPayload() {
        List<RecordingEmitter> emitters = new ArrayList<>();
        ProgressStreamService service = new ProgressStreamService(
                new ObjectMapper(),
                () -> {
                    RecordingEmitter emitter = new RecordingEmitter();
                    emitters.add(emitter);
                    return emitter;
                }
        );

        service.open(42L, "boss-progress", Map.of("message", "connected"));
        service.publish(42L, "boss-progress", "progress", Map.of("platform", "boss", "status", "running"));

        assertEquals(1, service.subscriberCount(42L, "boss-progress"));
        assertEquals(2, emitters.get(0).events.size());
        assertTrue(emitters.get(0).events.get(0).contains("event:connected"));
        assertTrue(emitters.get(0).events.get(1).contains("event:progress"));
        assertTrue(emitters.get(0).events.get(1).contains("\"platform\":\"boss\""));
        assertTrue(emitters.get(0).events.get(1).contains("\"status\":\"running\""));
    }

    @Test
    void heartbeatShouldIgnoreEmptyTopic() {
        ProgressStreamService service = new ProgressStreamService(new ObjectMapper(), RecordingEmitter::new);

        service.heartbeat(42L, "missing-topic");

        assertEquals(0, service.subscriberCount(42L, "missing-topic"));
    }

    @Test
    void publishShouldRemoveEmitterWhenSendFails() {
        FailingEmitter failingEmitter = new FailingEmitter();
        ProgressStreamService service = new ProgressStreamService(new ObjectMapper(), () -> failingEmitter);

        service.open(42L, "login-status", Map.of("message", "connected"));
        service.publish(42L, "login-status", "login-status", Map.of("platform", "boss"));

        assertEquals(0, service.subscriberCount(42L, "login-status"));
        assertEquals(1, failingEmitter.completeCalls);
    }

    @Test
    void heartbeatShouldRemoveEmitterWhenClientDisconnects() {
        FailingEmitter failingEmitter = new FailingEmitter();
        ProgressStreamService service = new ProgressStreamService(new ObjectMapper(), () -> failingEmitter);

        service.open(42L, "login-status", Map.of("message", "connected"));
        service.heartbeat(42L, "login-status");

        assertEquals(0, service.subscriberCount(42L, "login-status"));
        assertEquals(1, failingEmitter.completeCalls);
    }

    @Test
    void publishShouldOnlySendToSameUserTopic() {
        List<RecordingEmitter> emitters = new ArrayList<>();
        ProgressStreamService service = new ProgressStreamService(
                new ObjectMapper(),
                () -> {
                    RecordingEmitter emitter = new RecordingEmitter();
                    emitters.add(emitter);
                    return emitter;
                }
        );

        service.open(1L, "login-status", Map.of("message", "connected"));
        service.open(2L, "login-status", Map.of("message", "connected"));

        service.publish(1L, "login-status", "login-status", Map.of("platform", "boss"));

        assertEquals(2, emitters.get(0).events.size());
        assertEquals(1, emitters.get(1).events.size());
        assertEquals(1, service.subscriberCount(1L, "login-status"));
        assertEquals(1, service.subscriberCount(2L, "login-status"));
    }

    private static class RecordingEmitter extends SseEmitter {
        private final List<String> events = new ArrayList<>();

        RecordingEmitter() {
            super(0L);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            events.add(builder.build().stream()
                    .map(part -> String.valueOf(part.getData()))
                    .collect(Collectors.joining()));
        }
    }

    private static final class FailingEmitter extends RecordingEmitter {
        private int completeCalls;
        private int sendCalls;

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            sendCalls++;
            if (sendCalls > 1) {
                throw new IOException("client disconnected");
            }
            super.send(builder);
        }

        @Override
        public void complete() {
            completeCalls++;
            super.complete();
        }
    }
}
