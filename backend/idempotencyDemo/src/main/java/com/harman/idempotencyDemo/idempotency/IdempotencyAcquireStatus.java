package com.harman.idempotencyDemo.idempotency;

public enum IdempotencyAcquireStatus {
	ACQUIRED,
	REPLAY,
	IN_PROGRESS,
	RECOVERY_REQUIRED,
	CONFLICT
}
