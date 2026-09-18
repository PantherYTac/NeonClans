package com.neonclans.model;

public enum ClanRole {
    OWNER,
    MODERATOR,
    MEMBER;

    public static ClanRole fromString(String roleStr) {
        try {
            return ClanRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
