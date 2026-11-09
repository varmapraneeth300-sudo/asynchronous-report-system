
package com.report.api.config;

import com.report.api.entity.enums.PaymentMethod;
import com.report.api.entity.enums.TransactionStatus;
import com.report.api.entity.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    @Value("${seeder.enabled:false}")
    private boolean seederEnabled;

    @Value("${seeder.target-transactions:5000000}")
    private long targetTransactions;

    private static final int BATCH_SIZE = 5000;

    private final List<String> categories = Arrays.asList(
            "FOOD", "SHOPPING", "TRAVEL", "ENTERTAINMENT",
            "BILLS", "HEALTHCARE", "EDUCATION", "SALARY",
            "RENT", "INVESTMENT"
    );

    private final List<String> merchants = Arrays.asList(
            "Amazon", "Flipkart", "Swiggy", "Zomato", "Uber",
            "Netflix", "BigBasket", "Myntra", "Reliance Digital",
            "BookMyShow"
    );

    private final List<String> cities = Arrays.asList(
            "Bangalore", "Hyderabad", "Chennai", "Mumbai",
            "Delhi", "Pune", "Visakhapatnam"
    );

    private static final String INSERT_SQL = """
            INSERT INTO transactions (
                transaction_reference,
                external_reference,
                user_id,
                amount,
                fee,
                tax_amount,
                total_amount,
                currency,
                type,
                status,
                category,
                description,
                merchant_name,
                merchant_id,
                payment_method,
                payment_reference,
                country_code,
                city,
                transaction_date,
                settlement_date,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void run(String... args) {

        if (!seederEnabled) {
            System.out.println("Data seeder disabled.");
            return;
        }

        long existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions",
                Long.class
        );

        if (existingCount >= targetTransactions) {
            System.out.println("Target already reached. Skipping.");
            return;
        }

        List<Long> userIds = jdbcTemplate.query(
                "SELECT id FROM users",
                (rs, rowNum) -> rs.getLong("id")
        );

        if (userIds.size() != 2000) {
            throw new IllegalStateException(
                    "Expected 2000 users, found " + userIds.size()
            );
        }

        long remaining = targetTransactions - existingCount;

        System.out.println("Existing transactions: " + existingCount);
        System.out.println("Transactions to insert: " + remaining);

        long inserted = 0;
        long startTime = System.currentTimeMillis();

        while (inserted < remaining) {

            int currentBatchSize = (int) Math.min(
                    BATCH_SIZE,
                    remaining - inserted
            );

            insertBatch(userIds, currentBatchSize);

            inserted += currentBatchSize;

            System.out.println(
                    "Inserted " + inserted + " / " + remaining
            );
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Seeding completed.");
        System.out.println(
                "Total time: " + (endTime - startTime) / 1000 + " seconds"
        );
    }

    private void insertBatch(List<Long> userIds, int batchSize) {

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        long userId = userIds.get(
                                random.nextInt(userIds.size())
                        );

                        BigDecimal amount = BigDecimal.valueOf(
                                random.nextDouble(100, 100000)
                        ).setScale(2, RoundingMode.HALF_UP);

                        BigDecimal fee = amount.multiply(
                                BigDecimal.valueOf(0.01)
                        ).setScale(2, RoundingMode.HALF_UP);

                        BigDecimal tax = fee.multiply(
                                BigDecimal.valueOf(0.18)
                        ).setScale(2, RoundingMode.HALF_UP);

                        TransactionType type =
                                TransactionType.values()[
                                        random.nextInt(TransactionType.values().length)
                                        ];

                        TransactionStatus status =
                                TransactionStatus.values()[
                                        random.nextInt(TransactionStatus.values().length)
                                        ];

                        PaymentMethod paymentMethod =
                                PaymentMethod.values()[
                                        random.nextInt(PaymentMethod.values().length)
                                        ];

                        String category = categories.get(
                                random.nextInt(categories.size())
                        );

                        LocalDateTime transactionDate =
                                LocalDateTime.now()
                                        .minusDays(random.nextInt(730))
                                        .minusHours(random.nextInt(24));

                        LocalDateTime createdAt = LocalDateTime.now();

                        LocalDateTime settlementDate =
                                status == TransactionStatus.SUCCESS
                                        ? transactionDate.plusHours(2)
                                        : null;

                        ps.setString(1, UUID.randomUUID().toString());
                        ps.setString(2, "EXT-" + UUID.randomUUID());
                        ps.setLong(3, userId);
                        ps.setBigDecimal(4, amount);
                        ps.setBigDecimal(5, fee);
                        ps.setBigDecimal(6, tax);
                        ps.setBigDecimal(7, amount.add(fee).add(tax));
                        ps.setString(8, "INR");
                        ps.setString(9, type.name());
                        ps.setString(10, status.name());
                        ps.setString(11, category);
                        ps.setString(12, "Transaction for " + category);
                        ps.setString(13, merchants.get(
                                random.nextInt(merchants.size())
                        ));
                        ps.setString(14, "MER-" + random.nextInt(1000, 9999));
                        ps.setString(15, paymentMethod.name());
                        ps.setString(16, "PAY-" + UUID.randomUUID());
                        ps.setString(17, "IN");
                        ps.setString(18, cities.get(
                                random.nextInt(cities.size())
                        ));
                        ps.setTimestamp(19, Timestamp.valueOf(transactionDate));

                        if (settlementDate != null) {
                            ps.setTimestamp(
                                    20,
                                    Timestamp.valueOf(settlementDate)
                            );
                        } else {
                            ps.setNull(20, Types.TIMESTAMP);
                        }

                        ps.setTimestamp(21, Timestamp.valueOf(createdAt));
                        ps.setTimestamp(22, Timestamp.valueOf(createdAt));
                    }

                    @Override
                    public int getBatchSize() {
                        return batchSize;
                    }
                }
        );
    }
}
