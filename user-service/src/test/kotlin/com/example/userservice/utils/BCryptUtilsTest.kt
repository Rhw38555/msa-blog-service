package com.example.userservice.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Assertions.*

class BCryptUtilsTest : StringSpec({

    "비밀번호 암호화가 정상적으로 동작하면 기존 비밀번호와 다르다" {
        val password = "dev1234"
        val result = BCryptUtils.hash(password)
        result shouldNotBe  password
    }

    "비밀번호 복호화가 정상적으로 동작하면 기존 비밀번호와 같다"{
        val password = "dev1234"
        val encodePassword = "\$2a\$12\$sYBpzPS8F0tdqJoEk.TBwevg2s8OABILbx5iHxS14BAK7LwJ45sTC"
        val result = BCryptUtils.verify(password, encodePassword)
        result shouldBe true
    }
})