package com.project.wms.domain.inventory

class InventoryNotFoundException(id: Long) :
    RuntimeException("재고 없음: id=$id")

class InsufficientStockException(id: Long, current: Int, delta: Int) :
    RuntimeException("재고 부족: id=$id, 현재=$current, 요청=$delta")

class InsufficientReservationException(id: Long, reserved: Int, amount: Int) :
    RuntimeException("예약 부족: id=$id, 예약=$reserved, 요청=$amount")

class InvalidAmountException(amount: Int) :
    RuntimeException("수량은 1 이상이어야 합니다: 요청=$amount")
