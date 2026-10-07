package com.fitgear.backend.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Delivery address snapshot stored on the order.
 * Embedded in the orders table - an order keeps its address even if
 * the customer later changes details elsewhere.
 */
@Embeddable
public class OrderAddress {

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "address_line1", nullable = false)
    private String line1;

    @Column(name = "address_line2")
    private String line2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    public OrderAddress() {
    }

    public OrderAddress(String receiverName, String phone, String line1, String line2,
                        String city, String state, String pincode) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getReceiverName() { return receiverName; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
}
