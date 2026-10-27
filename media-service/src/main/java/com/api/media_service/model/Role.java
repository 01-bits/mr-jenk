package com.api.media_service.model;

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