package com.project.wms.infrastructure.persistence

import com.project.wms.domain.inventory.Reservation
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationJpaRepository : JpaRepository<Reservation, String>
