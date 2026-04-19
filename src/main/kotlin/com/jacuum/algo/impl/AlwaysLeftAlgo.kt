package com.jacuum.algo.impl

import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.algo.RobotAlgorithm
import com.jacuum.algo.Tile
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Always Left")
class AlwaysLeftAlgo : RobotAlgo {
    private val preference = listOf(Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH)
    override fun next(tile: Tile): Direction =
        preference.firstOrNull { !tile.hasWall(it) } ?: Direction.WEST
}
