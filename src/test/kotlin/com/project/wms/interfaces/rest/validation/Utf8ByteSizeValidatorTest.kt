package com.project.wms.interfaces.rest.validation

import jakarta.validation.Validation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Utf8ByteSizeValidatorTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `ASCII 72자는 72바이트라 허용된다`() {
        val value = PasswordInput("a".repeat(72))

        assertTrue(validator.validate(value).isEmpty())
    }

    @Test
    fun `ASCII 73자는 72바이트 초과라 거부된다`() {
        val value = PasswordInput("a".repeat(73))

        assertFalse(validator.validate(value).isEmpty())
    }

    @Test
    fun `멀티바이트 문자는 문자 수가 72 이하여도 72바이트를 넘으면 거부된다`() {
        val value = PasswordInput("가".repeat(25))

        assertFalse(validator.validate(value).isEmpty())
    }

    private data class PasswordInput(
        @field:Utf8ByteSize(max = 72)
        val password: String,
    )
}
