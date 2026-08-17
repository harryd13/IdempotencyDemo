package com.harman.idempotencyDemo.webhook;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

	@Modifying
	@Query(
			value = """
					INSERT INTO webhook_events (
						id,
						provider,
						provider_event_id,
						received_at
					)
					VALUES (
						:id,
						:provider,
						:providerEventId,
						:receivedAt
					)
					ON CONFLICT (provider, provider_event_id) DO NOTHING
					""",
			nativeQuery = true
	)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("provider") String provider,
			@Param("providerEventId") String providerEventId,
			@Param("receivedAt") Instant receivedAt
	);

	Optional<WebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
