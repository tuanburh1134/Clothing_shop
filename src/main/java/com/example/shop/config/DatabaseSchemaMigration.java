package com.example.shop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseSchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigration.class);

    @Bean
    public CommandLineRunner migrateCustomerOrderStatusColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Ensure order status column can store newly added values like DELIVERED.
                jdbcTemplate.execute("ALTER TABLE customer_orders MODIFY COLUMN status VARCHAR(20) NOT NULL");
                log.info("Schema migration applied: customer_orders.status -> VARCHAR(20)");
            } catch (Exception ex) {
                // Do not stop application startup if the table does not exist yet or is already compatible.
                log.debug("Skip status column migration: {}", ex.getMessage());
            }
        };
    }
}
