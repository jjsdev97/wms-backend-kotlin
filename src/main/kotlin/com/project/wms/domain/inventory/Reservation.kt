package com.project.wms.domain.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

enum class ReservationStatus { RESERVED, CONFIRMED, CANCELLED }

/**
 * 예약 자연키 엔티티. `reservationId`(클라이언트 제공)가 예약을 식별하므로,
 * 같은 키의 재호출은 도메인 차원에서 멱등하게 처리된다(프로토콜 무관).
 *
 * 상태 전이는 RESERVED → CONFIRMED 또는 RESERVED → CANCELLED만 허용한다.
 */
@Entity
@Table(name = "reservation")
class Reservation(
    @Id
    @Column(name = "reservation_id", length = 100)
    val reservationId: String,

    @Column(name = "inventory_id", nullable = false)
    val inventoryId: Long,

    @Column(nullable = false)
    val amount: Int,
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.RESERVED
        protected set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    fun confirm() {
        if (status != ReservationStatus.RESERVED) {
            throw IllegalReservationStateException(reservationId, status, "확정")
        }
        status = ReservationStatus.CONFIRMED
        touch()
    }

    fun cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw IllegalReservationStateException(reservationId, status, "취소")
        }
        status = ReservationStatus.CANCELLED
        touch()
    }

    private fun touch() {
        updatedAt = Instant.now()
    }
}
