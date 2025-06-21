package com.bank.system.payment_service.repository;

import com.bank.system.payment_service.domain.ProcessedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ProcessedEvent> rowMapper = new RowMapper<>() {
        @Override
        public ProcessedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProcessedEvent(
                    UUID.fromString(rs.getString("event_id")),
                    rs.getString("event_type"),
                    rs.getString("payload"),
                    rs.getTimestamp("processed_at").toInstant()
            );
        }
    };

    public Optional<ProcessedEvent> findById(UUID eventId) {
        String sql = "SELECT * FROM processed_event WHERE event_id = ?";
        try {
            ProcessedEvent event = jdbcTemplate.queryForObject(sql, rowMapper, eventId.toString());
            return Optional.ofNullable(event);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean existsById(UUID eventId) {
        String sql = "SELECT 1 FROM processed_event WHERE event_id = ?";
        try {
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, eventId.toString());
            return result != null && result == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public void save(ProcessedEvent event) {
        String sql = "INSERT INTO processed_event (event_id, event_type, payload, processed_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(
                sql,
                event.getEventId().toString(),
                event.getEventType(),
                event.getPayload(),
                event.getProcessedAt()
        );
    }
}