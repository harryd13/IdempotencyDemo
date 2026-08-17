package com.harman.idempotencyDemo.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StripeWebhookService {

	private static final String STRIPE_PROVIDER = "stripe";
	private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";

	private final WebhookEventRepository webhookEventRepository;
	private final String webhookSecret;

	public StripeWebhookService(
			WebhookEventRepository webhookEventRepository,
			@Value("${stripe.webhook-secret}") String webhookSecret
	) {
		this.webhookEventRepository = webhookEventRepository;
		this.webhookSecret = webhookSecret;
	}

	@Transactional
	public void handleWebhook(String payload, String signatureHeader) throws SignatureVerificationException {
		Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);

		int inserted = webhookEventRepository.insertIfAbsent(
				UUID.randomUUID(),
				STRIPE_PROVIDER,
				event.getId(),
				Instant.now()
		);

		if (inserted == 0) {
			return;
		}

		processEvent(event);

		WebhookEvent webhookEvent = webhookEventRepository
				.findByProviderAndProviderEventId(STRIPE_PROVIDER, event.getId())
				.orElseThrow(() -> new IllegalStateException("Inserted webhook event was not found"));
		webhookEvent.setProcessedAt(Instant.now());
		webhookEventRepository.save(webhookEvent);
	}

	private void processEvent(Event event) {
		if (!CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
			return;
		}

		StripeObject stripeObject = event.getDataObjectDeserializer()
				.getObject()
				.orElseThrow(() -> new IllegalStateException("Unable to deserialize Stripe event payload"));

		if (stripeObject instanceof Session session) {
			log.info("Received checkout.session.completed for session {}", session.getId());
		}
	}
}
