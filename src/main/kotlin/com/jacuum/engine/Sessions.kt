package com.jacuum.engine

import com.jacuum.map.GameMap

interface Sessions {
    @Throws(Exception::class)
    fun open(map: GameMap, algoName: String, username: String, avatar: String, iterations: Int): String
    @Throws(Exception::class) fun start(id: String)
    @Throws(Exception::class) fun pause(id: String)
    @Throws(Exception::class) fun resume(id: String)
    @Throws(Exception::class) fun stop(id: String)
    @Throws(Exception::class) fun view(id: String): SessionView
}
