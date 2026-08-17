package com.harman.idempotencyDemo.webhook;

import com.stripe.exception.SignatureVerificationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

	private final StripeWebhookService stripeWebhookService;

	public StripeWebhookController(StripeWebhookService stripeWebhookService) {
		this.stripeWebhookService = stripeWebhookService;
	}

	@PostMapping
	public ResponseEntity<Void> handleStripeWebhook(
			@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String stripeSignature
	) throws SignatureVerificationException {
		stripeWebhookService.handleWebhook(payload, stripeSignature);
		return ResponseEntity.ok().build();
	}
}
