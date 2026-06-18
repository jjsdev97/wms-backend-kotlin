package com.project.wms.domain.inventory

class InventoryNotFoundException(id: Long) :
    RuntimeException("재고 없음: id=$id")

class InsufficientStockException(id: Long, current: Int, delta: Int) :
    RuntimeException("재고 부족: id=$id, 현재=$current, 요청=$delta")

class InsufficientReservationException(id: Long, reserved: Int, amount: Int) :
    RuntimeException("예약 부족: id=$id, 예약=$reserved, 요청=$amount")

class InvalidAmountException(amount: Int) :
    RuntimeException("수량은 1 이상이어야 합니다: 요청=$amount")

class ReservationNotFoundException(reservationId: String) :
    RuntimeException("예약 없음: reservationId=$reservationId")

/** 같은 reservationId로 내용이 다른 예약이 이미 존재할 때. */
class ReservationConflictException(reservationId: String) :
    RuntimeException("동일 reservationId의 다른 예약이 이미 존재합니다: reservationId=$reservationId")

/** RESERVED가 아닌 예약에 확정·취소를 시도할 때. */
class IllegalReservationStateException(reservationId: String, status: ReservationStatus, action: String) :
    RuntimeException("$action 불가: reservationId=$reservationId, 현재 상태=$status")
