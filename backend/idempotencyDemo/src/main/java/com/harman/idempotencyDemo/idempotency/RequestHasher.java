package com.harman.idempotencyDemo.idempotency;

import com.harman.idempotencyDemo.checkout.CreateCheckoutRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestHasher {

	private final String priceId;

	public RequestHasher(@Value("${stripe.price-id}") String priceId) {
		this.priceId = priceId;
	}

	public String hash(CreateCheckoutRequest request) {
		String canonical = canonicalize(request);
		return sha256Hex(canonical);
	}

	String canonicalize(CreateCheckoutRequest request) {
		return "orderId=" + request.orderId()
				+ "|quantity=" + request.quantity()
				+ "|priceId=" + priceId;
	}

	private String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return toHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte current : bytes) {
			builder.append(String.format("%02x", current));
		}
		return builder.toString();
	}

}
