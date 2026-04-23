package com.project.wms.domain.inventory

class InventoryNotFoundException(id: Long) :
    RuntimeException("재고 없음: id=$id")

class InsufficientStockException(id: Long, current: Int, delta: Int) :
    RuntimeException("재고 부족: id=$id, 현재=$current, 요청=$delta")
