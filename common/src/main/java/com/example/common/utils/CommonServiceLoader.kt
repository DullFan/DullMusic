package com.example.common.utils

import java.util.*

object CommonServiceLoader {
    fun <S> load(service: Class<S>?): S? {
        val iterator = ServiceLoader.load(service).iterator()

        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }
    }
}