// MctsAi.kt — MCTS Go AI (KMP port from Dart)
package com.sherlockgo.ai

import com.sherlockgo.model.GoGame
import kotlin.math.*
import kotlin.random.Random

class MctsNode(
    val parent: MctsNode?,
    val move: Pair<Int, Int>?,
    val color: Int?
) {
    val children = mutableListOf<MctsNode>()
    var visits: Int = 0
    var wins: Double = 0.0
    var untriedMoves: MutableList<Pair<Int, Int>?>? = null

    fun ucb1(totalVisits: Int, exploration: Double = 1.4): Double =
        if (visits == 0) Double.POSITIVE_INFINITY
        else (wins / visits) + exploration * sqrt(ln(totalVisits.toDouble()) / visits)
}

class GoAI(
    private val game: GoGame,
    private val iterations: Int = 2000,
    private val exploration: Double = 1.4,
    private val usePatterns: Boolean = true
) {
    private val rng = Random.Default

    fun getBestMove(color: Int): Pair<Int, Int>? {
        if (game.moveCount == 0) {
            val corners = listOf(
                game.size - 4 to game.size - 4, 3 to game.size - 4,
                game.size - 4 to 3, 3 to 3, game.size - 4 to game.size / 2
            )
            return corners[rng.nextInt(corners.size)]
        }

        val root = MctsNode(null, null, color)
        root.untriedMoves = getMovesForColor(game, color).toMutableList()
        if (root.untriedMoves!!.isEmpty()) return null

        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        for (i in 0 until iterations) {
            if (i % 100 == 0 && kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime > 3000) break

            val simGame = game.clone()
            var node = root
            var simColor = color

            // Selection
            while ((node.untriedMoves == null || node.untriedMoves!!.isEmpty()) && node.children.isNotEmpty()) {
                node = selectBestChild(node)
                node.move?.let { simGame.placeStone(it.first, it.second) } ?: simGame.pass()
                simColor = if (simColor == GoGame.BLACK) GoGame.WHITE else GoGame.BLACK
            }

            // Expansion
            if (node.untriedMoves != null && node.untriedMoves!!.isNotEmpty()) {
                val move = node.untriedMoves!!.removeLast()
                val child = MctsNode(node, move, simColor)
                node.children.add(child)
                node = child
                move?.let { simGame.placeStone(it.first, it.second) } ?: simGame.pass()
                simColor = if (simColor == GoGame.BLACK) GoGame.WHITE else GoGame.BLACK
            }

            // Simulation
            val result = simulate(simGame, simColor)
            backpropagate(node, result, color)
        }

        if (root.children.isEmpty()) return null
        return root.children.maxByOrNull { it.visits }?.move
    }

    private fun getMovesForColor(game: GoGame, color: Int): List<Pair<Int, Int>?> {
        val prev = game.currentPlayer
        game.currentPlayer = color
        val moves = game.getValidMoves().map { it as Pair<Int, Int>? }.toMutableList()
        game.currentPlayer = prev
        moves.add(null) // pass
        moves.shuffle(rng)
        return moves
    }

    private fun selectBestChild(node: MctsNode): MctsNode {
        var best: MctsNode? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (child in node.children) {
            val score = child.ucb1(node.visits, exploration)
            if (score > bestScore) { bestScore = score; best = child }
        }
        return best!!
    }

    private fun simulate(simGame: GoGame, color: Int): Int {
        val maxMoves = simGame.size * simGame.size
        var simColor = color
        var moveCount = 0
        while (!simGame.gameOver && moveCount < maxMoves) {
            simGame.currentPlayer = simColor
            val validMoves = simGame.getValidMoves()
            if (validMoves.isEmpty()) {
                simGame.pass()
                if (simGame.gameOver) break
                simColor = if (simColor == GoGame.BLACK) GoGame.WHITE else GoGame.BLACK
                moveCount++
                continue
            }
            val move = if (usePatterns && rng.nextDouble() < 0.7)
                heuristicMove(simGame, validMoves, simColor)
            else validMoves[rng.nextInt(validMoves.size)]

            simGame.placeStone(move.first, move.second)
            simColor = if (simColor == GoGame.BLACK) GoGame.WHITE else GoGame.BLACK
            moveCount++
        }
        return if (simGame.gameOver) simGame.winner!! else evaluateBoard(simGame)
    }

    private fun heuristicMove(game: GoGame, validMoves: List<Pair<Int, Int>>, color: Int): Pair<Int, Int> {
        val opp = if (color == GoGame.BLACK) GoGame.WHITE else GoGame.BLACK
        val scored = validMoves.map { move ->
            var score = 0.0
            val (r, c) = move
            if (game.moveCount < 10) {
                val center = (game.size - 1) / 2.0
                score += (game.size - abs(r - center) - abs(c - center)) * 0.5
                val distFromEdge = listOf(r, c, game.size - 1 - r, game.size - 1 - c).min()
                if (distFromEdge in 2.0..4.0) score += 3.0
            }
            game.board[r][c] = color
            var captureCount = 0
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (game.inBounds(nr, nc) && game.board[nr][nc] == opp) {
                    val group = game.getGroup(nr, nc)
                    if (game.getLiberties(group).isEmpty()) captureCount += group.size
                }
            }
            game.board[r][c] = GoGame.EMPTY
            score += captureCount * 10.0
            val edgeDist = listOf(r, c, game.size - 1 - r, game.size - 1 - c).min()
            if (edgeDist == 0) score -= 5.0
            var nearOwn = 0; var nearOpp = 0
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (game.inBounds(nr, nc)) {
                    if (game.board[nr][nc] == color) nearOwn++
                    if (game.board[nr][nc] == opp) nearOpp++
                }
            }
            score += nearOwn * 1.5 - nearOpp * 0.5 + rng.nextDouble() * 2.0
            move to score
        }.sortedByDescending { it.second }

        val topN = minOf(5, scored.size)
        return scored[rng.nextInt(topN)].first
    }

    private fun evaluateBoard(game: GoGame): Int {
        val territory = game.calculateTerritory()
        val blackScore = territory.getOrDefault(GoGame.BLACK, 0) + game.captures.getOrDefault(GoGame.BLACK, 0)
        val whiteScore = territory.getOrDefault(GoGame.WHITE, 0) + game.captures.getOrDefault(GoGame.WHITE, 0) + 6.5
        return if (blackScore > whiteScore) GoGame.BLACK else GoGame.WHITE
    }

    private fun backpropagate(node: MctsNode, winner: Int, aiColor: Int) {
        var n: MctsNode? = node
        while (n != null) {
            n.visits++
            if (n.color != null && winner == n.color) n.wins++
            n = n.parent
        }
    }
}
