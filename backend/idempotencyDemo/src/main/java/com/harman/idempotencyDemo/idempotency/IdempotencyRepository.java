package com.harman.idempotencyDemo.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

	@Modifying
	@Query(
			value = """
					INSERT INTO idempotency_records (
						id,
						operation,
						idempotency_key,
						request_hash,
						status,
						created_at,
						updated_at,
						expires_at
					)
					VALUES (
						:id,
						:operation,
						:idempotencyKey,
						:requestHash,
						:status,
						:createdAt,
						:updatedAt,
						:expiresAt
					)
					ON CONFLICT (operation, idempotency_key) DO NOTHING
					""",
			nativeQuery = true
	)
	int insertProcessingRecord(
			@Param("id") UUID id,
			@Param("operation") String operation,
			@Param("idempotencyKey") String idempotencyKey,
			@Param("requestHash") String requestHash,
			@Param("status") String status,
			@Param("createdAt") Instant createdAt,
			@Param("updatedAt") Instant updatedAt,
			@Param("expiresAt") Instant expiresAt
	);

	Optional<IdempotencyRecord> findByOperationAndIdempotencyKey(
			String operation,
			String idempotencyKey
	);
}
