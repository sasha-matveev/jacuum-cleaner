package com.jacuum.algo.impl

import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.algo.RobotAlgorithm
import com.jacuum.algo.Tile
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import java.util.Random

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Random")
class RandomAlgo : RobotAlgo {
    private val rng = Random()
    override fun next(tile: Tile): Direction {
        val passable = Direction.entries.filter { !tile.hasWall(it) }
        val choices = passable.ifEmpty { Direction.entries }
        return choices[rng.nextInt(choices.size)]
    }
}
