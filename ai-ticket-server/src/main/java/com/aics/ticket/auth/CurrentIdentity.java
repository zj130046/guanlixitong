package com.aics.ticket.auth;

import cn.dev33.satoken.stp.StpUtil;

public final class CurrentIdentity {

    private CurrentIdentity() {
    }

    public static Long currentIdOrDefault(String identityType, Long fallbackId) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return fallbackId;
        }
        String text = loginId.toString();
        String prefix = identityType + ":";
        if (!text.startsWith(prefix)) {
            return fallbackId;
        }
        try {
            return Long.parseLong(text.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return fallbackId;
        }
    }
}
