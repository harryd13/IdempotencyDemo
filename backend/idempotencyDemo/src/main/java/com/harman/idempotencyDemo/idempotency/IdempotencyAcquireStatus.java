package com.harman.idempotencyDemo.idempotency;

public enum IdempotencyAcquireStatus {
	ACQUIRED,
	REPLAY,
	IN_PROGRESS,
	CONFLICT
}
