package com.jacuum.algo

interface Algorithms {
    fun names(): List<String>
    @Throws(Exception::class)
    fun instantiate(name: String): RobotAlgo
}
