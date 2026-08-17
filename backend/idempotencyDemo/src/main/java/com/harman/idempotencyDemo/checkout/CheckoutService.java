package com.harman.idempotencyDemo.checkout;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.harman.idempotencyDemo.idempotency.IdempotencyAcquireResult;
import com.harman.idempotencyDemo.idempotency.IdempotencyAcquireStatus;
import com.harman.idempotencyDemo.idempotency.IdempotencyService;
import com.harman.idempotencyDemo.idempotency.RequestHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CheckoutService {

	private static final String CHECKOUT_OPERATION = "CHECKOUT_SESSION_CREATE";
	private static final long MAX_DEMO_DELAY_MS = 5000L;

	private final String priceId;
	private final String frontendUrl;
	private final RequestHasher requestHasher;
	private final IdempotencyService idempotencyService;

	public CheckoutService(
			@Value("${stripe.price-id}") String priceId,
			@Value("${app.frontend-url}") String frontendUrl,
			RequestHasher requestHasher,
			IdempotencyService idempotencyService
	) {
		this.priceId = priceId;
		this.frontendUrl = frontendUrl;
		this.requestHasher = requestHasher;
		this.idempotencyService = idempotencyService;
	}

	public CheckoutResponse createCheckoutSession(
			CreateCheckoutRequest request,
			String idempotencyKey,
			int requestNumber,
			Long demoDelayMs
	) throws StripeException {
		SessionCreateParams params = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setSuccessUrl(frontendUrl + "/?payment=success&session_id={CHECKOUT_SESSION_ID}")
				.setCancelUrl(frontendUrl + "/?payment=cancelled")
				.putMetadata("orderId", request.orderId())
				.addLineItem(
						SessionCreateParams.LineItem.builder()
								.setPrice(priceId)
								.setQuantity(request.quantity())
								.build()
				)
				.build();

		boolean idempotencyUsed = StringUtils.hasText(idempotencyKey);
		String normalizedIdempotencyKey = idempotencyUsed ? idempotencyKey.trim() : null;

		if (!idempotencyUsed) {
			return createStripeCheckoutResponse(
					requestNumber,
					normalizedIdempotencyKey,
					false,
					"CREATED",
					Session.create(params)
			);
		}

		// Calculate the request hash
		String requestHash = requestHasher.hash(request);
		// Acquire the idempotency key
		IdempotencyAcquireResult acquireResult = idempotencyService.acquire(
				CHECKOUT_OPERATION,
				normalizedIdempotencyKey,
				requestHash
		);
		// Check if the request is a replay
		if (acquireResult.status() == IdempotencyAcquireStatus.REPLAY) {
			CheckoutResponse replayResponse = acquireResult.replayResponse();
			return new CheckoutResponse(
					requestNumber,
					replayResponse.stripeSessionId(),
					replayResponse.checkoutUrl(),
					true,
					normalizedIdempotencyKey,
					"REPLAYED"
			);
		}

		if (acquireResult.status() == IdempotencyAcquireStatus.IN_PROGRESS) {
			return new CheckoutResponse(
					requestNumber,
					null,
					null,
					true,
					normalizedIdempotencyKey,
					"PROCESSING"
			);
		}

		if (acquireResult.status() == IdempotencyAcquireStatus.CONFLICT) {
			return new CheckoutResponse(
					requestNumber,
					null,
					null,
					true,
					normalizedIdempotencyKey,
					"CONFLICT"
			);
		}

		if (acquireResult.status() == IdempotencyAcquireStatus.RECOVERY_REQUIRED) {
			return new CheckoutResponse(
					requestNumber,
					null,
					null,
					true,
					normalizedIdempotencyKey,
					"RECOVERY_REQUIRED"
			);
		}

		applyDemoDelayIfEnabled(demoDelayMs);

		Session session = Session.create(
				params,
				RequestOptions.builder()
						.setIdempotencyKey(normalizedIdempotencyKey)
						.build()
		);

		CheckoutResponse response = createStripeCheckoutResponse(
				requestNumber,
				normalizedIdempotencyKey,
				true,
				"CREATED",
				session
		);
		idempotencyService.saveSucceeded(acquireResult.record(), 200, response);
		return response;
	}

	private CheckoutResponse createStripeCheckoutResponse(
			int requestNumber,
			String idempotencyKey,
			boolean idempotencyUsed,
			String requestStatus,
			Session session
	) {
		return new CheckoutResponse(
				requestNumber,
				session.getId(),
				session.getUrl(),
				idempotencyUsed,
				idempotencyKey,
				requestStatus
		);
	}

	private void applyDemoDelayIfEnabled(Long demoDelayMs) {
		if (demoDelayMs == null || demoDelayMs <= 0) {
			return;
		}

		long boundedDelayMs = Math.min(demoDelayMs, MAX_DEMO_DELAY_MS);

		try {
			Thread.sleep(boundedDelayMs);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Demo delay was interrupted", exception);
		}
	}

}
