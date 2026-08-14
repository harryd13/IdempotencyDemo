package com.harman.idempotencyDemo.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.harman.idempotencyDemo.checkout.CreateCheckoutRequest;
import org.junit.jupiter.api.Test;

class RequestHasherTest {

	private static final String PRICE_ID = "price_demo_123";

	private final RequestHasher requestHasher = new RequestHasher(PRICE_ID);

	@Test
	void sameRequestProducesSameHash() {
		CreateCheckoutRequest first = new CreateCheckoutRequest("ORDER-1001", 1L);
		CreateCheckoutRequest second = new CreateCheckoutRequest("ORDER-1001", 1L);

		assertEquals(requestHasher.hash(first), requestHasher.hash(second));
	}

	@Test
	void differentOrderProducesDifferentHash() {
		CreateCheckoutRequest first = new CreateCheckoutRequest("ORDER-1001", 1L);
		CreateCheckoutRequest second = new CreateCheckoutRequest("ORDER-2002", 1L);

		assertNotEquals(requestHasher.hash(first), requestHasher.hash(second));
	}

	@Test
	void differentQuantityProducesDifferentHash() {
		CreateCheckoutRequest first = new CreateCheckoutRequest("ORDER-1001", 1L);
		CreateCheckoutRequest second = new CreateCheckoutRequest("ORDER-1001", 2L);

		assertNotEquals(requestHasher.hash(first), requestHasher.hash(second));
	}

}
