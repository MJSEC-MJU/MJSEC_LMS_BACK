# Java 실행 환경 업데이트

2026-11-17 기준으로 빌드·테스트 JDK와 앱 JRE를 현재 배포된 Eclipse Temurin 17.0.21+9 Jammy 이미지의 digest로 고정함
Java 17, Spring Boot, Gradle wrapper 버전은 유지하며 Docker 빌드도 저장소의 wrapper를 사용함
보안 분석 워크플로도 앱의 Java 17 toolchain을 찾을 수 있도록 Temurin 17.0.21+9를 설치함

11월 16일 사전 권고문에 나온 OpenJDK RCE는 해결 버전이 아직 확인되지 않았으므로 이번 변경으로 조치 완료 처리하면 안 됨
Oracle의 11월 공지는 확인 당시 사전 공지이고 17.0.21.1은 배포 예정 상태임
벤더가 수정 버전을 공개하면 JDK·JRE·Compose 테스트 이미지의 태그와 digest를 함께 갱신하고 다시 검증해야 함

- [Oracle 11월 보안 공지](https://www.oracle.com/security-alerts/cspunov2026.html)
- [JDK 배포 현황](https://ops.java/releases/)
- [Temurin 10월 릴리스](https://adoptium.net/en-GB/news/2026/10/eclipse-temurin-17021-21013-2505-2701-available)

배포 전에는 별도의 MySQL·Redis와 테스트용 설정에서 `docker build --pull -f dockerfile -t mjsec-lms-java-check .` 및 기존 Gradle 테스트를 실행함
기존 테스트는 DB 날짜 보정 작업도 검증하므로 운영 DB를 연결하면 안 됨

배포 후 실제 서버에서 아래 명령으로 실행 JVM과 이미지 ID를 확인해야 함
CI에서 확인한 버전만으로 운영 반영을 판단하지 않음

```sh
docker compose exec -T app java -XshowSettings:properties -version
docker inspect "$(docker compose ps -q app)" --format '{{.Image}} {{.Config.Image}}'
```

컨테이너 재생성 전에 기존 앱 이미지의 digest·배포 설정과 DB 백업을 보관함
문제가 생기면 보관한 이미지와 설정으로 되돌리고 DB 변경 여부도 확인함
Java 실행 파일을 교체해도 이미 실행 중인 JVM에는 적용되지 않으므로 앱 재기동이 필요함
