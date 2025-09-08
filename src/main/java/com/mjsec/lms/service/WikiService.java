package com.mjsec.lms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WikiService {

    @Value("${wiki.domain:wiki.mjsec.kr}")
    private String wikiDomain;

    @Value("${wiki.api.token}")
    private String apiToken;

    @Value("${wiki.group.id:10}")
    private Integer groupId;

    private final RestTemplate restTemplate;

    public WikiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Wiki.js에 사용자 계정을 생성
     *
     * @param email 사용자 이메일 (Wiki.js 아이디로 사용)
     * @param name 사용자 이름
     * @param password 초기 비밀번호 (이메일 @ 앞부분)
     * @return 성공 여부
     */
    @SuppressWarnings("unchecked")
    public boolean createWikiUser(String email, String name, String password) {
        try {
            log.info("Creating Wiki user for email: {}", email);

            String url = String.format("https://%s/graphql", wikiDomain);

            // GraphQL 뮤테이션 쿼리
            String mutation = """
                mutation($email:String!, $name:String!, $pass:String!, $groups:[Int]!) {
                  users {
                    create(
                      email: $email,
                      name: $name,
                      passwordRaw: $pass,
                      providerKey: "local",
                      groups: $groups,
                      mustChangePassword: true,
                      sendWelcomeEmail: false
                    ) {
                      responseResult { succeeded message slug }
                      user { id }
                    }
                  }
                }
                """;

            // 요청 변수 설정
            Map<String, Object> variables = new HashMap<>();
            variables.put("email", email);
            variables.put("name", name);
            variables.put("pass", password);
            variables.put("groups", List.of(groupId));

            // 요청 페이로드 구성
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", mutation);
            payload.put("variables", variables);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // GraphQL API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();

                if (responseBody == null) {
                    log.error("Received null response body from Wiki.js API");
                    return false;
                }

                // GraphQL 에러 체크
                if (responseBody.containsKey("errors")) {
                    List<Map<String, Object>> errors = (List<Map<String, Object>>) responseBody.get("errors");
                    if (errors != null && !errors.isEmpty()) {
                        String errorMessage = errors.get(0).get("message").toString();
                        log.error("GraphQL error while creating Wiki user: {}", errorMessage);
                        return false;
                    }
                }

                // 응답 데이터 파싱 (단계별 null 체크)
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data == null) {
                    log.error("No data field in Wiki.js API response");
                    return false;
                }

                Map<String, Object> users = (Map<String, Object>) data.get("users");
                if (users == null) {
                    log.error("No users field in Wiki.js API response");
                    return false;
                }

                Map<String, Object> create = (Map<String, Object>) users.get("create");
                if (create == null) {
                    log.error("Create operation returned null for user: {}", email);
                    return false;
                }

                Map<String, Object> responseResult = (Map<String, Object>) create.get("responseResult");
                if (responseResult == null) {
                    log.error("No responseResult in Wiki.js API response");
                    return false;
                }

                Boolean succeededObj = (Boolean) responseResult.get("succeeded");
                boolean succeeded = succeededObj != null ? succeededObj : false;
                String message = (String) responseResult.get("message");
                if (message == null) {
                    message = "no message";
                }

                if (succeeded) {
                    Map<String, Object> user = (Map<String, Object>) create.get("user");
                    if (user != null) {
                        Integer userId = (Integer) user.get("id");
                        log.info("Successfully created Wiki user: {} (ID: {})", email, userId);
                    } else {
                        log.info("Successfully created Wiki user: {} (no user ID returned)", email);
                    }
                    return true;
                } else {
                    log.error("Failed to create Wiki user {}: {}", email, message);
                    return false;
                }
            } else {
                log.error("HTTP error while creating Wiki user: {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            log.error("Network error while creating Wiki user {}: {}", email, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while creating Wiki user {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 이메일에서 초기 비밀번호를 추출
     *
     * @param email 사용자 이메일
     * @return 이메일 @ 앞부분
     */
    public String extractPasswordFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            log.warn("Invalid email format: {}", email);
            return email; // fallback
        }
        return email.substring(0, email.indexOf("@"));
    }
}