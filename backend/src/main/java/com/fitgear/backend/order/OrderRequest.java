package com.fitgear.backend.order;

import com.fitgear.backend.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Checkout request: delivery address plus the chosen payment method.
 * Prices/totals are NEVER accepted from the client - they are always
 * recalculated from the server-side cart.
 */
public record OrderRequest(

        @Valid @NotNull(message = "Delivery address is required")
        AddressRequest address,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {

    public record AddressRequest(
            @NotBlank(message = "Receiver name is required")
            @Size(max = 255)
            String receiverName,

            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^[0-9+\\-\\s]{8,15}$", message = "Phone number must be 8-15 digits")
            String phone,

            @NotBlank(message = "Address line is required")
            @Size(max = 255)
            String line1,

            @Size(max = 255)
            String line2,

            @NotBlank(message = "City is required")
            @Size(max = 100)
            String city,

            @NotBlank(message = "State is required")
            @Size(max = 100)
            String state,

            @NotBlank(message = "Pincode is required")
            @Pattern(regexp = "^[0-9]{4,10}$", message = "Pincode must be 4-10 digits")
            String pincode
    ) {
        public OrderAddress toOrderAddress() {
            return new OrderAddress(
                    receiverName.trim(),
                    phone.trim(),
                    line1.trim(),
                    line2 == null ? null : line2.trim(),
                    city.trim(),
                    state.trim(),
                    pincode.trim()
            );
        }
    }
}
