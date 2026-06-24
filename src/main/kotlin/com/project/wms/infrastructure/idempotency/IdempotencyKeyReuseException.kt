package com.project.wms.infrastructure.idempotency

/**
 * 이미 완료된 Idempotency-Key를 **다른 내용의 요청**으로 재사용했을 때 발생한다.
 * (같은 키 = 같은 요청이어야 한다는 멱등 계약 위반. reserve의 자연키 충돌과 같은 의미)
 */
class IdempotencyKeyReuseException(key: String) :
    RuntimeException("같은 Idempotency-Key로 다른 내용의 요청이 왔습니다: key=$key")
