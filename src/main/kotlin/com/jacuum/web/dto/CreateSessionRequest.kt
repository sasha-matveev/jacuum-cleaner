package com.jacuum.web.dto

data class CreateSessionRequest(
    val hash: String?,
    val size: String?,
    val algoName: String?,
    val username: String?,
    val avatar: String?,
    val iterations: Int
)
