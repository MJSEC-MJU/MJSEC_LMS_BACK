package com.mjsec.lms.type;

public enum Category {
    WEB("웹 해킹"),
    PWNABLE("포너블"),
    REVERSING("리버싱"),
    FORENSICS("포렌식"),
    CRYPTOGRAPHY("암호학"),
    MOBILE("모바일 해킹"),
    NETWORK("네트워크"),
    HARDWARE("하드웨어 해킹"),
    SYSTEM("시스템 해킹"),
    MISC("기타"),
    DEV("개발"),
    ALGORITHM("알고리즘")
    ;

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
