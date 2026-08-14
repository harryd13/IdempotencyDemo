package com.harman.idempotencyDemo.idempotency;

import com.harman.idempotencyDemo.checkout.CheckoutResponse;
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

	private final IdempotencyRepository idempotencyRepository;

	public IdempotencyService(IdempotencyRepository idempotencyRepository) {
		this.idempotencyRepository = idempotencyRepository;
	}

	@Transactional
	public IdempotencyAcquireResult acquire(
			String operation,
			String idempotencyKey,
			String requestHash
	) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(RECORD_TTL);

		int inserted = idempotencyRepository.insertProcessingRecord(
				UUID.randomUUID(),
				operation,
				idempotencyKey,
				requestHash,
				IdempotencyStatus.PROCESSING.name(),
				now,
				now,
				expiresAt
		);

		if (inserted == 1) {
			IdempotencyRecord record = idempotencyRepository
					.findByOperationAndIdempotencyKey(operation, idempotencyKey)
					.orElseThrow(() -> new IllegalStateException("Inserted idempotency record was not found"));
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
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.CONFLICT,
					record,
					null
			);
		}

		if (record.getStatus() == IdempotencyStatus.SUCCEEDED) {
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.REPLAY,
					record,
					deserialize(record.getResponseBody())
			);
		}

		if (record.getStatus() == IdempotencyStatus.PROCESSING) {
			return new IdempotencyAcquireResult(
					IdempotencyAcquireStatus.IN_PROGRESS,
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
