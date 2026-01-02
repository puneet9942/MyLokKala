package com.example.museapp.util

import java.util.UUID

object RequestIdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
