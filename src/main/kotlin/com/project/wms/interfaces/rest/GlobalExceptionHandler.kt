package com.project.wms.interfaces.rest

import com.project.wms.domain.inventory.InsufficientReservationException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.IllegalReservationStateException
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.ReservationConflictException
import com.project.wms.domain.inventory.ReservationNotFoundException
import com.project.wms.domain.user.UsernameAlreadyExistsException
import com.project.wms.infrastructure.idempotency.IdempotencyConflictException
import com.project.wms.infrastructure.idempotency.IdempotencyKeyReuseException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.AuthenticationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException::class)
    fun handleNotFound(e: InventoryNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "재고 없음")

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(e: InsufficientStockException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "재고 부족")

    @ExceptionHandler(InsufficientReservationException::class)
    fun handleInsufficientReservation(e: InsufficientReservationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "예약 부족")

    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmount(e: InvalidAmountException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "잘못된 수량")

    @ExceptionHandler(ReservationNotFoundException::class)
    fun handleReservationNotFound(e: ReservationNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "예약 없음")

    @ExceptionHandler(ReservationConflictException::class)
    fun handleReservationConflict(e: ReservationConflictException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "예약 충돌")

    @ExceptionHandler(IllegalReservationStateException::class)
    fun handleIllegalReservationState(e: IllegalReservationStateException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "잘못된 예약 상태 전이")

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(e: IdempotencyConflictException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "중복 요청 처리 중")

    /** 같은 Idempotency-Key로 다른 내용의 요청 — 멱등 계약 위반. */
    @ExceptionHandler(IdempotencyKeyReuseException::class)
    fun handleIdempotencyKeyReuse(e: IdempotencyKeyReuseException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Idempotency-Key 재사용 충돌")

    /** 낙관적 락 충돌(동시 수정) — 재시도 가능 신호. */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(e: OptimisticLockingFailureException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "동시 수정 충돌입니다. 다시 시도하세요.")

    /** 같은 reservationId 동시 예약 등 무결성 위반 — 재시도하면 멱등 처리된다. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(e: DataIntegrityViolationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "동시 요청 충돌입니다. 다시 시도하세요.")

    /** 회원가입 시 username 중복. */
    @ExceptionHandler(UsernameAlreadyExistsException::class)
    fun handleUsernameExists(e: UsernameAlreadyExistsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "이미 사용 중인 사용자명")

    /** 로그인 실패(자격증명 불일치 등). 어떤 부분이 틀렸는지는 노출하지 않는다. */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(e: AuthenticationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message)
    }
}
