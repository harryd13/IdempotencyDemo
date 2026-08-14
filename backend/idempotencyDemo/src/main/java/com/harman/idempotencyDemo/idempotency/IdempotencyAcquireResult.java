package com.harman.idempotencyDemo.idempotency;

import com.harman.idempotencyDemo.checkout.CheckoutResponse;

public record IdempotencyAcquireResult(
		IdempotencyAcquireStatus status,
		IdempotencyRecord record,
		CheckoutResponse replayResponse
) {
}
