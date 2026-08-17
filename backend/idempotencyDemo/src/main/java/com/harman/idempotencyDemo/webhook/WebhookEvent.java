package com.harman.idempotencyDemo.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
		name = "webhook_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_webhook_provider_event",
				columnNames = {"provider", "provider_event_id"}
		)
)
@Getter
@Setter
public class WebhookEvent {

	@Id
	private UUID id;

	@Column(nullable = false, length = 50)
	private String provider;

	@Column(name = "provider_event_id", nullable = false, length = 255)
	private String providerEventId;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (receivedAt == null) {
			receivedAt = Instant.now();
		}
	}
}
