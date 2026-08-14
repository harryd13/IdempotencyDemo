package com.harman.idempotencyDemo.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
		name = "idempotency_records",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_idempotency_operation_key",
				columnNames = {"operation", "idempotency_key"}
		)
)
@Getter
@Setter
public class IdempotencyRecord {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String operation;

	@Column(name = "idempotency_key", nullable = false, length = 255)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 255)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IdempotencyStatus status;

	@Column(name = "response_status")
	private Integer responseStatus;

	@Column(name = "response_body", columnDefinition = "text")
	private String responseBody;

	@Column(name = "stripe_session_id", length = 255)
	private String stripeSessionId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
