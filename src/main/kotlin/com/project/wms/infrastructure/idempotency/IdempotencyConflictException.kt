package com.project.wms.infrastructure.idempotency

/** 동일 Idempotency-Key의 선행 요청이 아직 처리 중일 때 발생한다. */
class IdempotencyConflictException(key: String) :
    RuntimeException("동일한 Idempotency-Key 요청이 처리 중입니다: key=$key")
