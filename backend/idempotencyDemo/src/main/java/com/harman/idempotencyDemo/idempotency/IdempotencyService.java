package com.harman.idempotencyDemo.idempotency;

import com.harman.idempotencyDemo.checkout.CheckoutResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

	private static final Duration RECORD_TTL = Duration.ofHours(24);
	private static final Duration PROCESSING_LEASE_DURATION = Duration.ofMinutes(2);

	private final IdempotencyRepository idempotencyRepository;
	private final Counter createdCounter;
	private final Counter replayedCounter;
	private final Counter processingCounter;
	private final Counter conflictCounter;

	public IdempotencyService(
			IdempotencyRepository idempotencyRepository,
			MeterRegistry meterRegistry
	) {
		this.idempotencyRepository = idempotencyRepository;
		this.createdCounter = meterRegistry.counter("idempotency.created");
		this.replayedCounter = meterRegistry.counter("idempotency.replayed");
		this.processingCounter = meterRegistry.counter("idempotency.processing");
		this.conflictCounter = meterRegistry.counter("idempotency.conflict");
	}

	@Transactional
	public IdempotencyAcquireResult acquire(
			String operation,
			String idempotencyKey,
			String requestHash
	) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(RECORD_TTL);
		Instant lockedUntil = now.plus(PROCESSING_LEASE_DURATION);

		int inserted = idempotencyRepository.insertProcessingRecord(
				UUID.randomUUID(),
				operation,
				idempotencyKey,
				requestHash,
				IdempotencyStatus.PROCESSING.name(),
				lockedUntil,
				now,
				now,
				expiresAt
		);

		if (inserted == 1) {
			IdempotencyRecord record = idempotencyRepository
					.findByOperationAndIdempotencyKey(operation, idempotencyKey)
					.orElseThrow(() -> new IllegalStateException("Inserted idempotency record was not found"));
			createdCounter.increment();
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.ACQUIRED,
					record,
					null
			);
		}

		IdempotencyRecord existingRecord = idempotencyRepository
				.findByOperationAndIdempotencyKey(operation, idempotencyKey)
				.orElseThrow(() -> new IllegalStateException("Existing idempotency record was not found"));

		return resolveExisting(existingRecord, requestHash);
	}

	@Transactional
	public void saveSucceeded(
			IdempotencyRecord record,
			int responseStatus,
			CheckoutResponse response
	) {
		record.setStatus(IdempotencyStatus.SUCCEEDED);
		record.setLockedUntil(null);
		record.setResponseStatus(responseStatus);
		record.setResponseBody(serialize(response));
		record.setStripeSessionId(response.stripeSessionId());
		idempotencyRepository.save(record);
	}

	private IdempotencyAcquireResult resolveExisting(
			IdempotencyRecord record,
			String requestHash
	) {
		if (!record.getRequestHash().equals(requestHash)) {
			conflictCounter.increment();
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.CONFLICT,
					record,
					null
			);
		}

		if (record.getStatus() == IdempotencyStatus.SUCCEEDED) {
			replayedCounter.increment();
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.REPLAY,
					record,
					deserialize(record.getResponseBody())
			);
		}

		if (record.getStatus() == IdempotencyStatus.PROCESSING) {
			Instant lockExpiry = record.getLockedUntil();
			if (lockExpiry != null && lockExpiry.isAfter(Instant.now())) {
				processingCounter.increment();
				return new IdempotencyAcquireResult(
						IdempotencyAcquireStatus.IN_PROGRESS,
						record,
						null
				);
			}

			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.RECOVERY_REQUIRED,
					record,
					null
			);
		}

		return new IdempotencyAcquireResult(
				IdempotencyAcquireStatus.IN_PROGRESS,
				record,
				null
		);
	}

	private String serialize(CheckoutResponse response) {
		return String.join(
				"|",
				encodePart(String.valueOf(response.requestNumber())),
				encodePart(response.stripeSessionId()),
				encodePart(response.checkoutUrl()),
				encodePart(String.valueOf(response.idempotencyUsed())),
				encodePart(response.idempotencyKey()),
				encodePart(response.requestStatus())
		);
	}

	private CheckoutResponse deserialize(String responseBody) {
		String[] parts = responseBody.split("\\|", -1);
		if (parts.length != 6) {
			throw new IllegalStateException("Stored checkout response has an invalid format");
		}

		return new CheckoutResponse(
				Integer.parseInt(decodePart(parts[0])),
				decodePart(parts[1]),
				decodePart(parts[2]),
				Boolean.parseBoolean(decodePart(parts[3])),
				decodeNullablePart(parts[4]),
				decodePart(parts[5])
		);
	}

	private String encodePart(String value) {
		String normalized = value == null ? "" : value;
		return Base64.getUrlEncoder()
				.encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
	}

	private String decodePart(String value) {
		return new String(
				Base64.getUrlDecoder().decode(value),
				StandardCharsets.UTF_8
		);
	}

	private String decodeNullablePart(String value) {
		String decoded = decodePart(value);
		return decoded.isEmpty() ? null : decoded;
	}
}
