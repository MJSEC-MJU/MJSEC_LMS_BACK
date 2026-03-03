package com.mjsec.lms.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JsonArrayUtils {

    // JSON 배열 문자열을 String List로 변환
    // @param jsonArray JSON 배열 형태의 문자열 (예: ["url1", "url2", "url3"])
    // @return String 리스트, 파싱 실패 시 빈 리스트 반환
    public List<String> parseStringArray(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            log.debug("JSON array is null or empty, returning empty list");
            return new ArrayList<>();
        }

        try {
            return parseJsonStringArray(jsonArray.trim());
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {} - Error: {}", jsonArray, e.getMessage());
            return new ArrayList<>();
        }
    }

    // String List를 JSON 배열 문자열로 변환
    // @param stringList 문자열 리스트
    // @return JSON 배열 형태의 문자열, 비어있으면 null 반환
    public String toStringArray(List<String> stringList) {
        if (stringList == null || stringList.isEmpty()) {
            log.debug("String list is null or empty, returning null");
            return null;
        }

        try {
            return buildJsonStringArray(stringList);
        } catch (Exception e) {
            log.error("Failed to convert list to JSON array: {} - Error: {}", stringList, e.getMessage());
            return null;
        }
    }

    // JSON 배열이 유효한 형식인지 검증
    // @param jsonArray 검증할 JSON 배열 문자열
    // @return 유효하면 true, 그렇지 않으면 false
    public boolean isValidJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return true; // null이나 빈 문자열은 유효한 것으로 간주
        }

        try {
            String trimmed = jsonArray.trim();
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                return false;
            }

            // 간단한 구문 검증 - 실제 파싱해보기
            parseJsonStringArray(trimmed);
            return true;
        } catch (Exception e) {
            log.debug("JSON array validation failed: {} - Error: {}", jsonArray, e.getMessage());
            return false;
        }
    }

    // JSON 배열의 요소 개수를 반환
    public int getArrayLength(String jsonArray) {
        List<String> parsed = parseStringArray(jsonArray);
        return parsed.size();
    }

    // 두 JSON 배열이 같은 내용을 가지고 있는지 비교
    // @return 같으면 true, 다르면 false
    public boolean arraysEqual(String jsonArray1, String jsonArray2) {
        List<String> list1 = parseStringArray(jsonArray1);
        List<String> list2 = parseStringArray(jsonArray2);
        return list1.equals(list2);
    }

    /*
    private Methods
     */

    // 실제 JSON 배열 파싱 로직
    // 간단한 구현으로 큰따옴표로 둘러싸인 문자열들을 콤마로 분리
    private List<String> parseJsonStringArray(String jsonArray) {
        List<String> result = new ArrayList<>();

        if (!jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array format: must start with [ and end with ]");
        }

        // 대괄호 제거
        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();

        if (content.isEmpty()) {
            return result; // 빈 배열
        }

        // 콤마로 분리하되, 큰따옴표 안의 콤마는 무시
        List<String> tokens = splitRespectingQuotes(content);

        for (String token : tokens) {
            String cleanToken = token.trim();
            if (!cleanToken.isEmpty()) {
                // 큰따옴표 제거
                if (cleanToken.startsWith("\"") && cleanToken.endsWith("\"") && cleanToken.length() >= 2) {
                    cleanToken = cleanToken.substring(1, cleanToken.length() - 1);
                }

                // 빈 문자열이 아닌 경우만 추가
                if (!cleanToken.isEmpty()) {
                    result.add(cleanToken);
                }
            }
        }

        return result;
    }

    // String List를 JSON 배열로 변환하는 실제 로직
    private String buildJsonStringArray(List<String> stringList) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < stringList.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }

            String value = stringList.get(i);
            if (value == null) {
                sb.append("null");
            } else {
                // JSON 문자열로 이스케이프 처리
                String escapedValue = escapeJsonString(value);
                sb.append("\"").append(escapedValue).append("\"");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    // 큰따옴표를 고려하여 콤마로 문자열 분리
    private List<String> splitRespectingQuotes(String content) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
                current.append(c);
            } else if (c == ',' && !insideQuotes) {
                // 큰따옴표 밖의 콤마이면 분리
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 마지막 토큰 추가
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    // JSON 문자열 이스케이프 처리
    private String escapeJsonString(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("\\", "\\\\")  // 백슬래시
                .replace("\"", "\\\"")   // 큰따옴표
                .replace("\n", "\\n")    // 개행
                .replace("\r", "\\r")    // 캐리지 리턴
                .replace("\t", "\\t");   // 탭
    }
}