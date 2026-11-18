# LMS Java 보안 버전

2026년 11월 18일 공개된 CVE-2026-114938 수정판에 맞춰 Java 17 런타임을 Amazon Corretto 17.0.21.1.2로 갱신했다. JVM의 런타임 버전은 17.0.21.1+2로 표시된다. 애플리케이션 소스와 Gradle wrapper 버전은 유지한다.

Docker 빌드, 테스트, 실행 이미지는 Amazon Linux 2023 기반의 공식 Corretto 이미지와 검증된 이미지 digest를 사용한다. 보안 분석 워크플로는 같은 Corretto 릴리스의 공식 Linux x64 압축 파일을 SHA-256으로 검증한다. 컨테이너 CI에서 빌드된 앱과 테스트 JVM의 공급자 및 수정 버전을 확인한다.

- [KISA 보안 업데이트 권고](https://krcert.or.kr/kr/bbs/view.do?bbsId=B0000133&menuNo=205020&nttId=72313)
- [Amazon Linux 보안 공지](https://alas.aws.amazon.com/AL2023/ALAS2023-2026-3478.html)
- [Amazon Corretto 17.0.21.1.2 릴리스](https://github.com/corretto/corretto-17/releases/tag/17.0.21.1.2)

운영 배포에서는 검증한 이미지 digest를 사용하고, 실행 JVM과 DB 연결을 확인한 뒤 공개 경로를 연다.
