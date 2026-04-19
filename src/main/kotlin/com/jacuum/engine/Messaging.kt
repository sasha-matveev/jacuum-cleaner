package com.jacuum.engine

interface Messaging {
    fun send(destination: String, payload: Any)
}
