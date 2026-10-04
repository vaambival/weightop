package com.weightop.web;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("comments_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    static {
        CONTAINER.start();
    }

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        return CONTAINER;
    }
}
