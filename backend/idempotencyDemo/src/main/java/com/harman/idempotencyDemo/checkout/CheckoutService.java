package com.harman.idempotencyDemo.checkout;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CheckoutService {

	private final String priceId;
	private final String frontendUrl;

	public CheckoutService(
			@Value("${stripe.price-id}") String priceId,
			@Value("${app.frontend-url}") String frontendUrl
	) {
		this.priceId = priceId;
		this.frontendUrl = frontendUrl;
	}

	public CheckoutResponse createCheckoutSession(
			CreateCheckoutRequest request,
			String idempotencyKey,
			int requestNumber
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

		Session session = idempotencyUsed
				? Session.create(
						params,
						RequestOptions.builder()
								.setIdempotencyKey(normalizedIdempotencyKey)
								.build()
				)
				: Session.create(params);

		return new CheckoutResponse(
				requestNumber,
				session.getId(),
				session.getUrl(),
				idempotencyUsed,
				normalizedIdempotencyKey
		);
	}

}
