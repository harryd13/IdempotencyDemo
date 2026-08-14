package com.harman.idempotencyDemo.checkout;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/sessions")
	public ResponseEntity<CheckoutResponse> createCheckoutSession(
			@Valid @RequestBody CreateCheckoutRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestHeader(name = "X-Demo-Request-Number", defaultValue = "1") int requestNumber
	) throws StripeException {
		CheckoutResponse response = checkoutService.createCheckoutSession(
				request,
				idempotencyKey,
				requestNumber
		);

		if ("PROCESSING".equals(response.requestStatus()) || "CONFLICT".equals(response.requestStatus())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}

		return ResponseEntity.ok(response);
	}

}
