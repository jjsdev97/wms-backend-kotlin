package com.project.wms.interfaces.graphql

import com.project.wms.domain.inventory.InsufficientReservationException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.InventoryNotFoundException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphqlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? =
        when (ex) {
            is InventoryNotFoundException -> build(ex, env, ErrorType.NOT_FOUND)
            is InsufficientStockException -> build(ex, env, ErrorType.BAD_REQUEST)
            is InsufficientReservationException -> build(ex, env, ErrorType.BAD_REQUEST)
            is InvalidAmountException -> build(ex, env, ErrorType.BAD_REQUEST)
            else -> null
        }

    private fun build(ex: Throwable, env: DataFetchingEnvironment, type: ErrorType): GraphQLError =
        GraphqlErrorBuilder.newError(env)
            .errorType(type)
            .message(ex.message)
            .build()
}
