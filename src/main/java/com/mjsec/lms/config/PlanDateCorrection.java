package com.mjsec.lms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// JVM 기본 시간대가 UTC이던 시절 plan의 start_date, end_date는 입력값보다 9시간 늦게 저장됨
// 현재 운영 DB를 직접 건들 수 없는 상황이라 긴급 패치용으로 기동 시 한 번만 9시간 당김
// 웹 서버가 요청을 받기 전에 돌아야 새 버전이 쓴 행까지 당기지 않음
@Slf4j
@Component
@Profile("prod")
public class PlanDateCorrection implements SmartInitializingSingleton {

    static final String MARKER_TABLE = "data_correction";
    static final String MARKER_NAME = "2026-09-30-plan-date-kst";
    static final String BACKUP_TABLE = "plan_backup_20260930";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PlanDateCorrection(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void afterSingletonsInstantiated() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + MARKER_TABLE
                + " (name VARCHAR(100) PRIMARY KEY, applied_at DATETIME(6) NOT NULL)");

        if (isApplied()) {
            log.info("Plan date correction already applied, skipping: {}", MARKER_NAME);
            return;
        }

        try {
            // 표식이 없으면 보정이 커밋된 적이 없으므로 이전 시도의 백업은 버리고 지금 상태로 다시 뜸
            // DDL은 MySQL에서 암묵적으로 커밋되므로 트랜잭션 밖에서 함
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + BACKUP_TABLE);
            jdbcTemplate.execute("CREATE TABLE " + BACKUP_TABLE + " AS SELECT * FROM plan");

            Integer shifted = transactionTemplate.execute(status -> {
                // 인스턴스가 혹시 두 개 겹쳐 떠도 표식을 먼저 넣은 쪽만 UPDATE까지 감
                jdbcTemplate.update("INSERT INTO " + MARKER_TABLE + " (name, applied_at) VALUES (?, NOW(6))",
                        MARKER_NAME);
                return jdbcTemplate.update("UPDATE plan SET start_date = start_date - INTERVAL 9 HOUR, "
                        + "end_date = end_date - INTERVAL 9 HOUR");
            });
            log.info("Plan date correction applied: {} rows shifted by -9 hours, backup table {}",
                    shifted, BACKUP_TABLE);
        } catch (DuplicateKeyException e) {
            log.info("Plan date correction already applied by another instance");
        } catch (RuntimeException e) {
            // 틀린 마감 시각으로 서비스하지 않도록 기동을 멈춤, 표식과 UPDATE는 함께 롤백됨
            log.error("Plan date correction failed, application startup aborted", e);
            throw e;
        }
    }

    private boolean isApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + MARKER_TABLE + " WHERE name = ?", Integer.class, MARKER_NAME);
        return count != null && count > 0;
    }
}
