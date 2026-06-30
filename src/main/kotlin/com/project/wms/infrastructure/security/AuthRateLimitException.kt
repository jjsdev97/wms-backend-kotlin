package com.project.wms.infrastructure.security

class AuthRateLimitException : RuntimeException("인증 요청이 너무 많습니다. 잠시 후 다시 시도하세요.")
