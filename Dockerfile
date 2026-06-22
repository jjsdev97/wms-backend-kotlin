# 멀티스테이지 빌드: 빌드 스테이지에서 bootJar 생성 → 런타임은 JRE만 포함해 경량화.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# 래퍼/빌드스크립트를 먼저 복사해 의존성 레이어를 캐시하고, 이후 소스를 복사한다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
# Windows에서 만든 gradlew의 실행권한을 부여. 통합 테스트는 Docker 의존이라 빌드 시 제외.
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
# 비밀(APP_JWT_SECRET)·인프라 호스트는 이미지에 굽지 않고 런타임 환경변수로 주입한다.
ENTRYPOINT ["java", "-jar", "app.jar"]
