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
        jdbcTemplate.update("""
            INSERT INTO outbox_event (id, aggregate_type, aggregate_id, type, payload, created_at, sent)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, event.getId(), event.getAggregateType(), event.getAggregateId(),
                event.getType(), event.getPayload(), event.getCreatedAt(), event.isSent());
    }

    public List<OutboxEvent> fetchUnsentEvents(int limit) {
        return jdbcTemplate.query("""
            SELECT * FROM outbox_event
            WHERE sent = FALSE
            ORDER BY created_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """, new Object[]{limit}, (rs, rowNum) -> new OutboxEvent(
                UUID.fromString(rs.getString("id")),
                rs.getString("aggregate_type"),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("type"),
                rs.getString("payload"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("sent")
        ));
    }

    public void markAsSent(UUID id) {
        jdbcTemplate.update("UPDATE outbox_event SET sent = TRUE WHERE id = ?", id);
    }
}

