package com.mjsec.lms.common.config;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// 실제 MySQL이 필요함 (CI는 docker-compose의 db를 씀)
// 보정 UPDATE는 plan 전체를 당기므로 테스트 행 말고 다른 행이 있는 DB에서는 건너뜀
// prod 프로필로 뜨면 컨텍스트 기동 때 실제 보정이 돌아서 local로 고정함
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("기존 계획 날짜 9시간 보정 테스트")
class PlanDateCorrectionTest {

    private static final String TITLE = "plan-date-correction-test";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        Integer otherPlans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plan WHERE title <> ?", Integer.class, TITLE);
        assumeTrue(otherPlans != null && otherPlans == 0, "다른 계획 데이터가 있는 DB라 보정 테스트를 건너뜀");
        cleanUp();
        // 옛 버전(JVM UTC)이 09:00~23:59 입력을 저장한 모양
        jdbcTemplate.update(
                "INSERT INTO plan (title, content, has_assignment, start_date, end_date) VALUES (?, 'c', 1, ?, ?)",
                TITLE, LocalDateTime.of(2026, 10, 1, 18, 0), LocalDateTime.of(2026, 10, 2, 8, 59));
        jdbcTemplate.update(
                "INSERT INTO plan (title, content, has_assignment, start_date, end_date) VALUES (?, 'c', 0, NULL, NULL)",
                TITLE);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM plan WHERE title = ?", TITLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + PlanDateCorrection.BACKUP_TABLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + PlanDateCorrection.MARKER_TABLE);
    }

    private PlanDateCorrection newCorrection() {
        return new PlanDateCorrection(jdbcTemplate, transactionManager);
    }

    private List<String> column(String table, String column) {
        return jdbcTemplate.queryForList(
                "SELECT CAST(" + column + " AS CHAR) FROM " + table + " WHERE title = ? ORDER BY plan_id",
                String.class, TITLE);
    }

    @Test
    @DisplayName("기존 계획의 시작일과 마감일을 9시간 당기고 NULL은 그대로 둔다")
    void shiftsExistingPlanDates() {
        newCorrection().afterSingletonsInstantiated();

        assertThat(column("plan", "start_date")).containsExactly("2026-10-01 09:00:00.000000", null);
        assertThat(column("plan", "end_date")).containsExactly("2026-10-01 23:59:00.000000", null);
    }

    @Test
    @DisplayName("보정 전 값을 백업 테이블에 남긴다")
    void backsUpBeforeShifting() {
        newCorrection().afterSingletonsInstantiated();

        assertThat(column(PlanDateCorrection.BACKUP_TABLE, "end_date"))
                .containsExactly("2026-10-02 08:59:00.000000", null);
    }

    @Test
    @DisplayName("다시 기동해도 한 번만 보정한다")
    void runsOnlyOnce() {
        newCorrection().afterSingletonsInstantiated();
        newCorrection().afterSingletonsInstantiated();

        assertThat(column("plan", "end_date")).containsExactly("2026-10-01 23:59:00.000000", null);
    }

    @Test
    @DisplayName("prod 프로필이 아니면 보정 빈이 등록되지 않는다")
    void notRegisteredOutsideProd() {
        assertThat(applicationContext.getBeanNamesForType(PlanDateCorrection.class)).isEmpty();
    }
}
