package com.project.wms.infrastructure.persistence

import com.project.wms.domain.inventory.Reservation
import com.project.wms.domain.port.ReservationRepository
import org.springframework.stereotype.Repository

@Repository
class ReservationPersistenceAdapter(
    private val jpaRepository: ReservationJpaRepository
) : ReservationRepository {

    override fun findById(reservationId: String): Reservation? =
        jpaRepository.findById(reservationId).orElse(null)

    override fun save(reservation: Reservation): Reservation =
        jpaRepository.save(reservation)
}
