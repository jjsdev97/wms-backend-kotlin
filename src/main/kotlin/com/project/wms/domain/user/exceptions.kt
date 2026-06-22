package com.project.wms.domain.user

/** 이미 존재하는 username으로 회원가입을 시도한 경우. */
class UsernameAlreadyExistsException(username: String) :
    RuntimeException("이미 사용 중인 사용자명입니다: $username")
