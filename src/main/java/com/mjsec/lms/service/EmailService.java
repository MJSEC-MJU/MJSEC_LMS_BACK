package com.mjsec.lms.service;

import com.mjsec.lms.domain.AssignmentNotSubmittedInfo;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.UserRole;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    private final UserRepository userRepository;

    EmailService(JavaMailSender mailSender, UserRepository userRepository) {

        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    private List<String> getAdminEmails() {

        List<String> adminEmails = userRepository.findEmailsByRole(UserRole.ROLE_ADMIN);

        if (adminEmails.isEmpty()) {
            log.warn("No admin users found in database!");
            throw new RestApiException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("Found {} admin email(s): {}", adminEmails.size(), adminEmails);

        return adminEmails;
    }
    

    public void sendWeeklyAssignmentReport(Map<String, List<AssignmentNotSubmittedInfo>> reportData,
                                           LocalDateTime reportDate) {
        try {
            if (reportData == null || reportData.isEmpty()) {
                log.info("No assignment reports to send");
                return;
            }

            List<String> adminEmails = getAdminEmails();

            for (String adminEmail : adminEmails) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(adminEmail);
                helper.setSubject("[LMS 주간 리포트] 과제 미제출자 통합 알림 - " +
                        reportDate.toLocalDate());

                String htmlContent = buildWeeklyReportContent(reportData, reportDate);

                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Weekly assignment report sent successfully to admin: {}",
                        maskEmail(adminEmail));
            }

        } catch (MessagingException e) {
            log.error("Failed to send weekly assignment report", e);
            throw new RuntimeException("주간 리포트 이메일 발송에 실패했습니다.", e);
        }
    }

    public void sendAbsenceAlert(String studyGroupName,
                                 List<AttendanceAlertService.AbsenceInfo> absenceList,
                                 LocalDate targetDate) {
        try {
            List<String> adminEmails = getAdminEmails();

            for (String adminEmail : adminEmails) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(adminEmail);
                helper.setSubject("[LMS 결석 알림] " + studyGroupName + " - 10일 경과 결석자 알림 (" +
                        targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ")");

                String htmlContent = buildAbsenceAlertContent(studyGroupName, absenceList, targetDate);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Absence alert sent successfully to admin: {}", maskEmail(adminEmail));
            }

        } catch (MessagingException e) {
            log.error("Failed to send absence alert email for study group: {}", studyGroupName, e);
            throw new RuntimeException("결석 알림 이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 결석 알림 이메일 HTML 내용 생성
     */
    private String buildAbsenceAlertContent(String studyGroupName,
                                            List<AttendanceAlertService.AbsenceInfo> absenceList,
                                            LocalDate targetDate) {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        DateTimeFormatter currentTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head><meta charset='UTF-8'></head>")
                .append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>")
                .append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>")

                // 헤더
                .append("<h1 style='color: #e74c3c; text-align: center; border-bottom: 3px solid #e74c3c; padding-bottom: 15px;'>")
                .append("🚨 LMS 결석자 알림</h1>")

                // 알림 정보
                .append("<div style='background-color: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #ffc107;'>")
                .append("<h2 style='margin-top: 0; color: #856404;'>알림 정보</h2>")
                .append("<p style='margin: 5px 0;'><strong>스터디 그룹:</strong> ").append(studyGroupName).append("</p>")
                .append("<p style='margin: 5px 0;'><strong>결석 날짜:</strong> ").append(targetDate.format(dateFormatter)).append("</p>")
                .append("<p style='margin: 5px 0;'><strong>경과 일수:</strong> 10일</p>")
                .append("<p style='margin: 5px 0;'><strong>결석자 수:</strong> ").append(absenceList.size()).append("명</p>")
                .append("</div>")

                // 결석자 목록
                .append("<div style='background-color: #f8d7da; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #dc3545;'>")
                .append("<h3 style='margin-top: 0; color: #721c24;'>📋 결석자 명단</h3>")
                .append("<div style='background-color: white; padding: 15px; border-radius: 5px;'>");

        // 결석자 목록을 표 형태로 생성
        html.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>")
                .append("<thead>")
                .append("<tr style='background-color: #dc3545; color: white;'>")
                .append("<th style='padding: 10px; text-align: left; border: 1px solid #ddd;'>학번</th>")
                .append("<th style='padding: 10px; text-align: left; border: 1px solid #ddd;'>이름</th>")
                .append("<th style='padding: 10px; text-align: left; border: 1px solid #ddd;'>주차</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        for (int i = 0; i < absenceList.size(); i++) {
            AttendanceAlertService.AbsenceInfo absence = absenceList.get(i);
            String rowStyle = i % 2 == 0 ? "background-color: #f8f9fa;" : "background-color: white;";

            html.append("<tr style='").append(rowStyle).append("'>")
                    .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(absence.getStudentNumber()).append("</td>")
                    .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(absence.getStudentName()).append("</td>")
                    .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(absence.getWeek() != null ? absence.getWeek() : "-").append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody>")
                .append("</table>")
                .append("</div>")
                .append("</div>")

                // 안내 문구
                .append("<div style='background-color: #d1ecf1; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #bee5eb;'>")
                .append("<h4 style='margin-top: 0; color: #0c5460;'>💡 안내사항</h4>")
                .append("<ul style='margin: 10px 0; padding-left: 20px; color: #0c5460;'>")
                .append("<li>위 학생들은 ").append(targetDate.format(dateFormatter)).append("에 결석한 후 10일이 경과되었습니다.</li>")
                .append("<li>해당 학생들의 출석 상태를 확인하고 필요시 개별 연락을 취해주세요.</li>")
                .append("<li>지속적인 결석이 우려되는 경우 적절한 조치를 취해주시기 바랍니다.</li>")
                .append("</ul>")
                .append("</div>")

                // 푸터
                .append("<div style='margin-top: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 8px; text-align: center;'>")
                .append("<p style='margin: 0; font-size: 14px; color: #6c757d;'>")
                .append("이 알림은 LMS 시스템에서 자동으로 발송되었습니다.</p>")
                .append("<p style='margin: 5px 0 0 0; font-size: 12px; color: #adb5bd;'>")
                .append("발송 시간: ").append(LocalDateTime.now().format(currentTimeFormatter))
                .append(" | 문의사항이 있으시면 시스템 관리자에게 연락해주세요.")
                .append("</p>")
                .append("</div>")

                .append("</div>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }

    private String buildWeeklyReportContent(Map<String, List<AssignmentNotSubmittedInfo>> reportData,
                                            LocalDateTime reportDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        // 통계 계산
        int totalStudyGroups = reportData.size();
        int totalAssignments = reportData.values().stream()
                .mapToInt(List::size)
                .sum();
        int totalNotSubmitted = reportData.values().stream()
                .flatMap(List::stream)
                .mapToInt(AssignmentNotSubmittedInfo::getNotSubmittedCount)
                .sum();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head><meta charset='UTF-8'></head>")
                .append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>")
                .append("<div style='max-width: 800px; margin: 0 auto; padding: 20px;'>")

                // 헤더
                .append("<h1 style='color: #2c3e50; text-align: center; border-bottom: 3px solid #3498db; padding-bottom: 15px;'>")
                .append("LMS 주간 과제 미제출자 리포트</h1>")

                // 요약 정보
                .append("<div style='background-color: #ecf0f1; padding: 20px; border-radius: 8px; margin: 20px 0;'>")
                .append("<h2 style='margin-top: 0; color: #2c3e50;'>리포트 요약</h2>")
                .append("<div style='display: flex; justify-content: space-between; flex-wrap: wrap;'>")
                .append("<div style='text-align: center; margin: 10px;'>")
                .append("<div style='font-size: 24px; font-weight: bold; color: #e74c3c;'>").append(totalStudyGroups).append("</div>")
                .append("<div style='font-size: 14px; color: #7f8c8d;'>관련 스터디 그룹</div>")
                .append("</div>")
                .append("<div style='text-align: center; margin: 10px;'>")
                .append("<div style='font-size: 24px; font-weight: bold; color: #f39c12;'>").append(totalAssignments).append("</div>")
                .append("<div style='font-size: 14px; color: #7f8c8d;'>미제출 과제</div>")
                .append("</div>")
                .append("<div style='text-align: center; margin: 10px;'>")
                .append("<div style='font-size: 24px; font-weight: bold; color: #e67e22;'>").append(totalNotSubmitted).append("</div>")
                .append("<div style='font-size: 14px; color: #7f8c8d;'>총 미제출 건수</div>")
                .append("</div>")
                .append("</div>")
                .append("<p style='margin-bottom: 0; font-size: 14px; color: #7f8c8d; text-align: center;'>")
                .append("리포트 생성일: ").append(reportDate.format(dateFormatter)).append("</p>")
                .append("</div>");

        // 각 스터디 그룹별 상세 정보
        for (Map.Entry<String, List<AssignmentNotSubmittedInfo>> entry : reportData.entrySet()) {
            String studyGroupName = entry.getKey();
            List<AssignmentNotSubmittedInfo> assignments = entry.getValue();

            int groupTotalNotSubmitted = assignments.stream()
                    .mapToInt(AssignmentNotSubmittedInfo::getNotSubmittedCount)
                    .sum();

            html.append("<div style='background-color: #ffffff; border: 1px solid #ddd; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>")
                    .append("<div style='background-color: #3498db; color: white; padding: 15px; border-radius: 8px 8px 0 0;'>")
                    .append("<h3 style='margin: 0; font-size: 18px;'>")
                    .append("📚 ").append(studyGroupName)
                    .append(" <span style='font-size: 14px; opacity: 0.9;'>(")
                    .append(assignments.size()).append("개 과제, ")
                    .append(groupTotalNotSubmitted).append("건 미제출)</span>")
                    .append("</h3>")
                    .append("</div>")
                    .append("<div style='padding: 20px;'>");

            // 각 과제별 미제출자 정보
            for (int i = 0; i < assignments.size(); i++) {
                AssignmentNotSubmittedInfo assignmentInfo = assignments.get(i);

                html.append("<div style='margin-bottom: 25px; padding-bottom: 15px;")
                        .append(i < assignments.size() - 1 ? " border-bottom: 1px solid #ecf0f1;" : "")
                        .append("'>")

                        .append("<h4 style='color: #e74c3c; margin: 0 0 10px 0; font-size: 16px;'>")
                        .append("📝 ").append(assignmentInfo.getPlanTitle())
                        .append("</h4>")

                        .append("<p style='margin: 5px 0; font-size: 14px; color: #7f8c8d;'>")
                        .append("<strong>마감일:</strong> ").append(assignmentInfo.getEndDate().format(formatter))
                        .append("</p>")

                        .append("<div style='background-color: #fff5f5; padding: 15px; border-left: 4px solid #e74c3c; border-radius: 0 5px 5px 0;'>")
                        .append("<p style='margin: 0 0 10px 0; font-weight: bold; color: #e74c3c;'>")
                        .append("미제출자 (").append(assignmentInfo.getNotSubmittedCount()).append("명):")
                        .append("</p>")
                        .append("<ul style='margin: 0; padding-left: 20px;'>");

                for (String student : assignmentInfo.getNotSubmittedStudents()) {
                    html.append("<li style='margin: 3px 0; font-size: 14px;'>").append(student).append("</li>");
                }

                html.append("</ul>")
                        .append("</div>")
                        .append("</div>");
            }

            html.append("</div>")
                    .append("</div>");
        }

        // 푸터
        html.append("<div style='margin-top: 40px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; text-align: center;'>")
                .append("<p style='margin: 0; font-size: 14px; color: #2c3e50;'>")
                .append("이 리포트는 LMS 시스템에서 자동으로 생성되어 매주 월요일에 발송됩니다.</p>")
                .append("<p style='margin: 10px 0 0 0; font-size: 12px; color: #7f8c8d;'>")
                .append("발송 시간: ").append(LocalDateTime.now().format(formatter))
                .append(" | 문의사항이 있으시면 시스템 관리자에게 연락해주세요.")
                .append("</p>")
                .append("</div>")

                .append("</div>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }
    
    
    //출석체크 미완료자 알람용 - 일단 그냥 냅둠
    private String buildAttendanceAlertEmailContent(String studyGroupName,
                                                    List<String> absentStudents,
                                                    LocalDateTime alertDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head><meta charset='UTF-8'></head>")
                .append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>")
                .append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>")
                .append("<h2 style='color: #f39c12; border-bottom: 2px solid #f39c12; padding-bottom: 10px;'>")
                .append("출석체크 미완료자 알림</h2>")

                .append("<div style='background-color: #fefdf5; padding: 15px; border-radius: 5px; margin: 20px 0;'>")
                .append("<h3 style='margin-top: 0; color: #2c3e50;'>출석체크 정보</h3>")
                .append("<p><strong>스터디 그룹:</strong> ").append(studyGroupName).append("</p>")
                .append("<p><strong>알림 날짜:</strong> ").append(alertDate.format(formatter)).append("</p>")
                .append("</div>")

                .append("<div style='margin: 20px 0;'>")
                .append("<h3 style='color: #f39c12;'>미출석자 목록 (").append(absentStudents.size()).append("명)</h3>")
                .append("<ul style='background-color: #fffbf0; padding: 15px; border-left: 4px solid #f39c12;'>");

        for (String student : absentStudents) {
            html.append("<li style='margin: 5px 0;'>").append(student).append("</li>");
        }

        html.append("</ul>")
                .append("</div>")

                .append("<div style='margin-top: 30px; padding: 15px; background-color: #e8f4f8; border-radius: 5px;'>")
                .append("<p style='margin: 0; font-size: 14px; color: #2c3e50;'>")
                .append("이 알림은 LMS 시스템에서 자동으로 발송되었습니다.</p>")
                .append("<p style='margin: 5px 0 0 0; font-size: 14px; color: #7f8c8d;'>")
                .append("발송 시간: ").append(LocalDateTime.now().format(formatter)).append("</p>")
                .append("</div>")

                .append("</div>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }

    /**
     * 이메일 주소 마스킹 처리
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        String maskedLocal = localPart.charAt(0) +
                "*".repeat(Math.max(0, localPart.length() - 2)) +
                (localPart.length() > 1 ? localPart.charAt(localPart.length() - 1) : "");

        return maskedLocal + domainPart;
    }

}

