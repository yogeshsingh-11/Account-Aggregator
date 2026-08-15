package com.company.aa.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String newIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
