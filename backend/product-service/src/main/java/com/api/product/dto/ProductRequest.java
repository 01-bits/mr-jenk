
package com.api.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ProductRequest
 */
public record ProductRequest(
                @NotBlank @Size(max = 100) String name,
                @Size(max = 2000) String description,
                @NotNull @Positive Double price,
                @NotNull @PositiveOrZero Long quantity) {
}