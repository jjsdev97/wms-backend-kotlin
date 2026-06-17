package com.project.wms.interfaces.rest

import com.project.wms.domain.inventory.InsufficientReservationException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.InventoryNotFoundException
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message)
    }
}
