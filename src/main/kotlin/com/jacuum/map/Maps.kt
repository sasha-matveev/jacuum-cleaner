package com.jacuum.map

interface Maps {
    @Throws(Exception::class)
    fun generate(hash: String, size: SizePreset): GameMap
}
