package com.project.wms.interfaces.graphql

import com.project.wms.domain.inventory.InventoryNotFoundException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

/**
 * GraphQL은 조회 전용(Query만)이므로 조회 경로에서 실제 발생 가능한 예외만 매핑한다.
 * 재고 부족·예약 충돌·동시성 등 쓰기 관련 예외는 GraphQL에서 발생하지 않아 다루지 않는다.
 * (그 예외들은 REST GlobalExceptionHandler에서 처리)
 */
@Component
class GraphqlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? =
        when (ex) {
            is InventoryNotFoundException -> build(ex.message, env, ErrorType.NOT_FOUND)
            else -> null
        }

    private fun build(message: String?, env: DataFetchingEnvironment, type: ErrorType): GraphQLError =
        GraphqlErrorBuilder.newError(env)
            .errorType(type)
            .message(message)
            .build()
}
