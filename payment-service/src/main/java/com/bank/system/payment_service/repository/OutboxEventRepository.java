package com.bank.system.payment_service.repository;

import com.bank.system.payment_service.domain.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public OutboxEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(OutboxEvent event) {
        event.setId(UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        event.setVersion(0);

        jdbcTemplate.update("""
            INSERT INTO outbox_event (
                id, aggregate_type, aggregate_id, type, payload,
                created_at, sent, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, event.getId(), event.getAggregateType(), event.getAggregateId(),
                event.getType(), event.getPayload(), event.getCreatedAt(), event.isSent(), event.getVersion());
    }

    public List<OutboxEvent> fetchUnsentEvents(int limit) {
        return jdbcTemplate.query("""
            SELECT * FROM outbox_event
            WHERE sent = FALSE
            ORDER BY created_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """, new Object[]{limit}, (rs, rowNum) -> {
            OutboxEvent event = new OutboxEvent(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("aggregate_type"),
                    rs.getObject("aggregate_id", UUID.class),
                    rs.getString("type"),
                    rs.getString("payload"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getBoolean("sent")
            );
            event.setVersion(rs.getInt("version"));
            return event;
        });
    }

    public boolean markAsSent(UUID id, int currentVersion) {
        int updated = jdbcTemplate.update("""
            UPDATE outbox_event
            SET sent = TRUE, version = version + 1
            WHERE id = ? AND version = ?
        """, id, currentVersion);
        return updated > 0;
    }
}
