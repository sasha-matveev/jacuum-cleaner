package com.jacuum.algo.impl

import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.algo.RobotAlgorithm
import com.jacuum.algo.Tile
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import java.util.ArrayDeque
import java.util.EnumSet

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Frontier BFS")
class FrontierAlgo : RobotAlgo {
    private val walls = HashMap<String, EnumSet<Direction>>()
    private val clean = HashSet<String>()
    private val plan = ArrayDeque<Direction>()
    private var startKey: String? = null

    override fun next(tile: Tile): Direction {
        observe(tile)
        if (plan.isNotEmpty()) return plan.poll()

        val cur = key(tile.x(), tile.y())
        val toFrontier = bfsTo(tile.x(), tile.y(), null)
        if (toFrontier.isNotEmpty()) { plan.addAll(toFrontier); return plan.poll() }

        val sk = startKey
        if (sk != null && !clean.contains(sk) && cur != sk) {
            val toStart = bfsTo(tile.x(), tile.y(), sk)
            if (toStart.isNotEmpty()) { plan.addAll(toStart); return plan.poll() }
        }
        return anyPassable(tile)
    }

    private fun observe(tile: Tile) {
        val k = key(tile.x(), tile.y())
        if (startKey == null) startKey = k
        if (!walls.containsKey(k)) {
            val blocked = EnumSet.noneOf(Direction::class.java)
            for (d in Direction.entries) if (tile.hasWall(d)) blocked.add(d)
            walls[k] = blocked
        }
        if (tile.isClean()) clean.add(k)
    }

    private fun bfsTo(startX: Int, startY: Int, targetKey: String?): List<Direction> {
        val parent = HashMap<String, String?>()
        val stepTo = HashMap<String, Direction>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val sk = key(startX, startY)
        parent[sk] = null; queue.add(startX to startY)

        while (queue.isNotEmpty()) {
            val (px, py) = queue.poll()
            val posKey = key(px, py)
            val blocked = walls.getOrDefault(posKey, EnumSet.noneOf(Direction::class.java))
            for (d in Direction.entries) {
                if (blocked.contains(d)) continue
                val nx = px + d.dx(); val ny = py + d.dy(); val nk = key(nx, ny)
                if (parent.containsKey(nk)) continue
                parent[nk] = posKey; stepTo[nk] = d
                if (targetKey == null) {
                    if (!walls.containsKey(nk)) return reconstruct(nk, parent, stepTo)
                    queue.add(nx to ny)
                } else {
                    if (nk == targetKey) return reconstruct(nk, parent, stepTo)
                    if (walls.containsKey(nk)) queue.add(nx to ny)
                }
            }
        }
        return emptyList()
    }

    private fun reconstruct(
        target: String,
        parent: Map<String, String?>,
        stepTo: Map<String, Direction>
    ): List<Direction> {
        val path = ArrayDeque<Direction>()
        var cur = target
        while (stepTo.containsKey(cur)) { path.addFirst(stepTo[cur]!!); cur = parent[cur]!! }
        return path.toList()
    }

    private fun anyPassable(tile: Tile) =
        Direction.entries.firstOrNull { !tile.hasWall(it) } ?: Direction.NORTH

    private fun key(x: Int, y: Int) = "$x,$y"
}
