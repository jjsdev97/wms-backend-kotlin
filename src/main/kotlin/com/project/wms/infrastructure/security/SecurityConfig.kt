package com.project.wms.infrastructure.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class, SecurityExposureProperties::class)
class SecurityConfig(
    private val properties: JwtProperties,
    private val exposure: SecurityExposureProperties,
) {

    private val secretKey = SecretKeySpec(properties.secret.toByteArray(), "HmacSHA256")

    // 프로토콜별 접근 정책 (이 프로젝트의 멀티프로토콜 보안 모델):
    //  - REST 재고(inventory)
    //      · 조회(GET)  → 공개. 누구나 재고를 읽을 수 있다.
    //      · 변경(POST: adjust/reserve/confirm/cancel) → 인증 필요(JWT Bearer).
    //        재고 쓰기를 인증된 주체로 제한하는 것이 이번 보안 작업의 유일한 기능 목표다.
    //  - REST 인증(auth)  → 공개. 회원가입·로그인은 토큰을 받기 전이라 인증 불가.
    //  - GraphQL          → 공개. 조회 전용(Mutation 없음)이라 쓰기 구멍이 없다.
    //  - MCP              → 기본 인증 필요. 로컬 학습 환경에서만 설정으로 공개할 수 있다.
    //  - 문서/운영(swagger, actuator, graphiql) → 기본 인증 필요. 로컬 학습 환경에서만 설정으로 공개할 수 있다.
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // REST/GraphQL/MCP 모두 stateless 토큰 기반. 세션·CSRF 불필요.
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    // --- 공개: 인증 진입점 ---
                    .requestMatchers("/api/v1/auth/logout-all").authenticated()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    // --- REST: 조회는 공개, 쓰기는 인증 ---
                    .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()
                    .requestMatchers("/api/v1/inventory/**").authenticated()
                    // --- GraphQL: 조회 전용이라 공개 ---
                    .requestMatchers("/graphql").permitAll()
                    // --- MCP: 기본 보호, 로컬 학습 환경에서만 공개 가능 ---
                    .apply {
                        if (exposure.mcpPublic) {
                            requestMatchers("/mcp/**").permitAll()
                        } else {
                            requestMatchers("/mcp/**").authenticated()
                        }
                    }
                    // --- 문서/운영: 기본 보호, 로컬 학습 환경에서만 공개 가능 ---
                    .apply {
                        val devEndpoints = arrayOf(
                            "/graphiql/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/actuator/**",
                        )
                        if (exposure.devEndpointsPublic) {
                            requestMatchers(*devEndpoints).permitAll()
                        } else {
                            requestMatchers(*devEndpoints).authenticated()
                        }
                    }
                    .anyRequest().authenticated()
            }
            // Bearer JWT를 검증하는 리소스 서버. 위 authenticated 경로에 적용된다.
            .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) } }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** 로그인 시 username/password를 검증하는 매니저. (AuthService가 주입받아 사용) */
    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationManager {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return ProviderManager(provider)
    }

    /** HS256 대칭키로 JWT 서명(발급). */
    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey))

    /** HS256 대칭키로 JWT 검증(파싱). */
    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()

    /**
     * JWT의 'scope' 클레임을 권한으로 변환한다. 발급 시 값에 이미 "ROLE_" 접두사를 넣으므로
     * 기본 변환기의 "SCOPE_" 접두사를 비워, hasRole("USER")/hasAuthority("ROLE_ADMIN")가
     * 그대로 동작하도록 한다. (기본값이면 권한이 "SCOPE_ROLE_USER"가 되어 RBAC가 어긋난다)
     */
    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authorities = JwtGrantedAuthoritiesConverter().apply {
            setAuthorityPrefix("")
            setAuthoritiesClaimName("scope")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authorities)
        }
    }
}
