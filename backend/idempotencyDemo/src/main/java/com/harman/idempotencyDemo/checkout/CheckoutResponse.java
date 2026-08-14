package com.harman.idempotencyDemo.checkout;

public record CheckoutResponse(
		int requestNumber,
		String stripeSessionId,
		String checkoutUrl,
		boolean idempotencyUsed,
		String idempotencyKey,
		String requestStatus
) {
}
