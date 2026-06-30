package com.project.wms.interfaces.rest.auth

import com.project.wms.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthTokenIntegrationTest : IntegrationTestBase() {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: RestClient by lazy { RestClient.create("http://localhost:$port") }

    @Test
    fun `refresh token은 refresh 요청 때 회전되고 이전 token은 재사용할 수 없다`() {
        val username = "token-user-${UUID.randomUUID()}"
        val password = "token-pw-12345678"
        signup(username, password)
        val first = login(username, password)

        val second = refresh(first.refreshToken)

        assertNotEquals(first.accessToken, second.accessToken)
        assertNotEquals(first.refreshToken, second.refreshToken)
        val ex = assertFailsWith<HttpClientErrorException> {
            refresh(first.refreshToken)
        }
        assertTrue(ex.statusCode.value() == 401)
    }

    @Test
    fun `logout은 refresh token을 폐기한다`() {
        val username = "logout-user-${UUID.randomUUID()}"
        val password = "logout-pw-12345678"
        signup(username, password)
        val tokens = login(username, password)

        client.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"refreshToken": "${tokens.refreshToken}"}""")
            .retrieve().toBodilessEntity()

        val ex = assertFailsWith<HttpClientErrorException> {
            refresh(tokens.refreshToken)
        }
        assertTrue(ex.statusCode.value() == 401)
    }

    private fun signup(username: String, password: String) {
        client.post().uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"username": "$username", "password": "$password"}""")
            .retrieve().toBodilessEntity()
    }

    private fun login(username: String, password: String): TokenResponse =
        client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"username": "$username", "password": "$password"}""")
            .retrieve().body(TokenResponse::class.java)!!

    private fun refresh(refreshToken: String): TokenResponse =
        client.post().uri("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"refreshToken": "$refreshToken"}""")
            .retrieve().body(TokenResponse::class.java)!!
}
