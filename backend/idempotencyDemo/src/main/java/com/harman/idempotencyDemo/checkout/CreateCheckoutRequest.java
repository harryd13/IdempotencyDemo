package com.harman.idempotencyDemo.checkout;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutRequest(
		@NotBlank String orderId,
		@NotNull @Min(1) @Max(10) Long quantity
) {
}
