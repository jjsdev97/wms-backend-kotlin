package com.project.wms.domain.port

import com.project.wms.domain.inventory.Reservation

interface ReservationRepository {
    fun findById(reservationId: String): Reservation?
    fun save(reservation: Reservation): Reservation
}
