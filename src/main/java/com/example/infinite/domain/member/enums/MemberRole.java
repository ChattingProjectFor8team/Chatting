package com.example.infinite.domain.member.enums;

public enum MemberRole {
    USER("USER"),
    ARTIST_ADMIN("ARTIST"), // 💡 이름은 ARTIST_ADMIN이지만 시큐리티에선 "ARTIST"로 쓰겠다!
    SUPER_ADMIN("ADMIN");


    private final String securityName;

    MemberRole(String securityName) {
        this.securityName = securityName;
    }

    public String getSecurityName() {
        return securityName;
    }
}
