package com.project.wms.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

/**
 * 통합 테스트 공통 베이스. 실제 Postgres·Redis를 Testcontainers로 띄워
 * Flyway 마이그레이션·JPA·Redis 멱등을 실DB 환경에서 검증한다.
 *
 * 싱글톤 컨테이너 패턴: @Container/@Testcontainers는 클래스마다 컨테이너를
 * start/stop 하므로, 여러 통합 테스트 클래스가 돌면 앞 클래스가 컨테이너를
 * 꺼버려 뒤 클래스가 죽은 DB를 만난다. 대신 수동 start()로 한 번만 띄우고
 * JVM 종료 시 Ryuk이 정리하게 해 모든 클래스가 안전하게 공유한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase {

    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17").apply { start() }

        @JvmStatic
        val redis = GenericContainer("redis:7-alpine").withExposedPorts(6379).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            // 운영은 env(APP_JWT_SECRET)로 주입하므로 application.yml엔 기본값이 없다.
            // 테스트 컨텍스트 로드를 위해 HS256용 32바이트 이상 더미 키를 주입한다.
            registry.add("app.jwt.secret") { "integration-test-only-secret-32bytes-minimum" }
        }
    }
}
