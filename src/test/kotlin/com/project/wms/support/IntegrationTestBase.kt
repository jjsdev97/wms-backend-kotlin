package com.project.wms.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트 공통 베이스. 실제 Postgres·Redis를 Testcontainers로 띄워
 * Flyway 마이그레이션·JPA·Redis 멱등을 실DB 환경에서 검증한다.
 *
 * 컨테이너는 static으로 한 번만 기동돼 모든 하위 테스트가 공유한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class IntegrationTestBase {

    companion object {
        /** 통합 테스트 일괄 비활성화 사유(@Disabled에서 참조). */
        const val DISABLED_REASON =
            "로컬 Docker Desktop named pipe가 Testcontainers docker-java 요청에 400을 반환(CLI는 정상). " +
                "Docker TCP 데몬 노출 또는 호환 CI 환경에서 활성화. 자세한 건 docs/code-walkthrough.md 참고."

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")

        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7-alpine").withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }
}
