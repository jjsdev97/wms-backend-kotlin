package com.project.wms.interfaces.graphql

import com.project.wms.domain.inventory.IllegalReservationStateException
import com.project.wms.domain.inventory.InsufficientReservationException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.ReservationConflictException
import com.project.wms.domain.inventory.ReservationNotFoundException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphqlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? =
        when (ex) {
            is InventoryNotFoundException -> build(ex, env, ErrorType.NOT_FOUND)
            is ReservationNotFoundException -> build(ex, env, ErrorType.NOT_FOUND)
            is InsufficientStockException -> build(ex, env, ErrorType.BAD_REQUEST)
            is InsufficientReservationException -> build(ex, env, ErrorType.BAD_REQUEST)
            is InvalidAmountException -> build(ex, env, ErrorType.BAD_REQUEST)
            is ReservationConflictException -> build(ex, env, ErrorType.BAD_REQUEST)
            is IllegalReservationStateException -> build(ex, env, ErrorType.BAD_REQUEST)
            // 동시성 충돌 — 재시도 가능. GraphQL ErrorType엔 CONFLICT가 없어 BAD_REQUEST + 안내 메시지.
            is OptimisticLockingFailureException -> build("동시 수정 충돌입니다. 다시 시도하세요.", env, ErrorType.BAD_REQUEST)
            is DataIntegrityViolationException -> build("동시 요청 충돌입니다. 다시 시도하세요.", env, ErrorType.BAD_REQUEST)
            else -> null
        }

    private fun build(ex: Throwable, env: DataFetchingEnvironment, type: ErrorType): GraphQLError =
        build(ex.message, env, type)

    private fun build(message: String?, env: DataFetchingEnvironment, type: ErrorType): GraphQLError =
        GraphqlErrorBuilder.newError(env)
            .errorType(type)
            .message(message)
            .build()
}
