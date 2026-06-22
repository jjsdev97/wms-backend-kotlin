package com.project.wms.domain.user

/** 사용자 권한. 현재는 인증만 강제하고 권한별 분기는 두지 않지만, RBAC 확장 여지로 둔다. */
enum class Role {
    USER,
    ADMIN,
}
