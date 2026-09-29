package com.mjsec.lms.common.util;

import com.mjsec.lms.assignment.repository.PlanCommentRepository;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.studygroup.repository.StudyActivityRepository;
import com.mjsec.lms.studygroup.repository.StudyGroupRepository;
import com.mjsec.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("과제 제출 블로그 도메인 검증 테스트")
class ValidationUtilsSubmissionDomainTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudyGroupRepository studyGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private PlanCommentRepository planCommentRepository;
    @Mock
    private StudyActivityRepository studyActivityRepository;

    private ValidationUtils validationUtils;

    @BeforeEach
    void setUp() {
        validationUtils = new ValidationUtils(
                userRepository,
                studyGroupRepository,
                groupMemberRepository,
                attendanceRepository,
                planRepository,
                submissionRepository,
                planCommentRepository,
                studyActivityRepository
        );
    }

    @ParameterizedTest
    @DisplayName("기존 허용 도메인은 계속 통과한다")
    @ValueSource(strings = {
            "https://velog.io/@mjsec/week1",
            "https://mjsec.tistory.com/12",
            "https://blog.naver.com/mjsec/223000000000",
            "https://m.blog.naver.com/mjsec/223000000000"
    })
    void existingDomainsStillAllowed(String url) {
        assertThatCode(() -> validationUtils.validateSubmissionContent(url))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("새로 추가한 블로그 도메인은 통과한다")
    @ValueSource(strings = {
            "https://mjsec.github.io/posts/week1",
            "https://github.com/mjsec/writeups/blob/main/week1.md",
            "https://mjsec.notion.site/week1-abc123",
            "https://www.notion.so/mjsec/week1-abc123",
            "https://mjsec.oopy.io/week1",
            "https://medium.com/@mjsec/week1-abc123",
            "https://brunch.co.kr/@mjsec/1",
            "https://mjsec.blogspot.com/2026/09/week1.html",
            "https://mjsec.gitbook.io/writeups/week1",
            "https://MJSEC.GITHUB.IO/week1",
            "https://mjsec.github.io"
    })
    void addedDomainsAllowed(String url) {
        assertThatCode(() -> validationUtils.validateSubmissionContent(url))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("목록에 없는 도메인과 이름만 비슷한 도메인은 거절한다")
    @ValueSource(strings = {
            "https://example.com/week1",
            "https://evilgithub.io/week1",
            "https://github.io.evil.com/week1",
            "https://notvelog.io/@mjsec/week1",
            "https://github.com@evil.com/week1"
    })
    void unknownDomainsRejected(String url) {
        assertThatThrownBy(() -> validationUtils.validateSubmissionContent(url))
                .isInstanceOf(RestApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_DOMAIN);
    }

    @Test
    @DisplayName("여러 링크 중 하나라도 허용되지 않으면 거절한다")
    void rejectsWhenAnyLinkIsNotAllowed() {
        String content = "https://mjsec.github.io/week1 https://example.com/week1";

        assertThatThrownBy(() -> validationUtils.validateSubmissionContent(content))
                .isInstanceOf(RestApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_DOMAIN);
    }
}
