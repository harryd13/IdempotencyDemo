package com.harman.idempotencyDemo.idempotency;

public enum IdempotencyStatus {
	PROCESSING,
	SUCCEEDED,
	FAILED_FINAL,
	FAILED_RETRYABLE
}
