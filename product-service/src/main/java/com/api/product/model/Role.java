package com.api.product.model;

/**
 * Role
 */
public enum Role {
    CLIENT,
    SELLER;

    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}