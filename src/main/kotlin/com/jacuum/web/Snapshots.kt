package com.jacuum.web

import com.jacuum.map.GameMap
import com.jacuum.web.dto.MapSnapshot

interface Snapshots {
    fun of(map: GameMap): MapSnapshot
}
