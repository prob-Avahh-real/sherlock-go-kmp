// GoGame.kt — Go rules engine (KMP port from Dart)
package com.sherlockgo.model

class GoGame(val size: Int = 19) {
    companion object {
        const val EMPTY = 0
        const val BLACK = 1
        const val WHITE = 2
    }

    val board: Array<IntArray> = Array(size) { IntArray(size) { EMPTY } }
    var currentPlayer: Int = BLACK
    val captures: MutableMap<Int, Int> = mutableMapOf(BLACK to 0, WHITE to 0)
    var koPoint: Pair<Int, Int>? = null
    var moveCount: Int = 0
    var passesInRow: Int = 0
    var gameOver: Boolean = false
    var winner: Int? = null

    // Per-player valid-move caches (incremental update)
    private var validBlack: List<Pair<Int, Int>>? = null
    private var validWhite: List<Pair<Int, Int>>? = null

    fun inBounds(r: Int, c: Int) = r in 0 until size && c in 0 until size

    fun opponent(color: Int) = if (color == BLACK) WHITE else BLACK

    // ── Group/Liberty ──

    fun getGroup(r: Int, c: Int): List<Pair<Int, Int>> {
        if (!inBounds(r, c) || board[r][c] == EMPTY) return emptyList()
        val color = board[r][c]
        val group = mutableListOf<Pair<Int, Int>>()
        val visited = mutableSetOf<Int>()
        val stack = mutableListOf(r to c)
        while (stack.isNotEmpty()) {
            val (cr, cc) = stack.removeLast()
            val key = cr * size + cc
            if (key in visited) continue
            visited += key
            group += cr to cc
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = cr + dr; val nc = cc + dc
                if (inBounds(nr, nc) && board[nr][nc] == color && (nr * size + nc) !in visited)
                    stack += nr to nc
            }
        }
        return group
    }

    fun getLiberties(group: List<Pair<Int, Int>>): Set<Int> {
        val libs = mutableSetOf<Int>()
        for ((r, c) in group) {
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (inBounds(nr, nc) && board[nr][nc] == EMPTY) libs += nr * size + nc
            }
        }
        return libs
    }

    fun isValidMove(r: Int, c: Int, playerColor: Int = currentPlayer): Boolean {
        if (gameOver || !inBounds(r, c) || board[r][c] != EMPTY) return false
        if (koPoint == r to c) return false
        val opp = opponent(playerColor)
        board[r][c] = playerColor
        val wouldCapture = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).any { (dr, dc) ->
            val nr = r + dr; val nc = c + dc
            inBounds(nr, nc) && board[nr][nc] == opp && getLiberties(getGroup(nr, nc)).isEmpty()
        }
        val ownLibs = getLiberties(getGroup(r, c))
        board[r][c] = EMPTY
        return !(ownLibs.isEmpty() && !wouldCapture)
    }

    // ── Incremental valid-move cache ──

    fun getValidMoves(): List<Pair<Int, Int>> {
        val cache = if (currentPlayer == BLACK) validBlack else validWhite
        if (cache != null) return cache
        val moves = computeAllValidMoves()
        if (currentPlayer == BLACK) validBlack = moves else validWhite = moves
        return moves
    }

    private fun computeAllValidMoves(): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size)
            if (isValidMove(r, c, currentPlayer)) moves += r to c
        return moves
    }

    private fun clearCaches() { validBlack = null; validWhite = null }

    private fun updateCachesAfterMove(r: Int, c: Int, captured: List<Pair<Int, Int>>) {
        val affected = mutableSetOf<Int>()
        fun add(pr: Int, pc: Int) { if (inBounds(pr, pc)) affected += pr * size + pc }
        add(r, c)
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) add(r + dr, c + dc)
        for ((sr, sc) in captured) {
            add(sr, sc)
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) add(sr + dr, sc + dc)
        }
        validBlack = incrUpdate(validBlack, affected, BLACK)
        validWhite = incrUpdate(validWhite, affected, WHITE)
    }

    private fun incrUpdate(cache: List<Pair<Int, Int>>?, affected: Set<Int>, color: Int): List<Pair<Int, Int>>? {
        if (cache == null) return null
        val result = mutableListOf<Pair<Int, Int>>()
        for (move in cache)
            if ((move.first * size + move.second) !in affected) result += move
        for (key in affected) {
            val ar = key / size; val ac = key % size
            if (board[ar][ac] == EMPTY && isValidMove(ar, ac, color)) result += ar to ac
        }
        return result
    }

    // ── Place stone ──

    data class MoveResult(val captured: Int, val move: Pair<Int, Int>)

    fun placeStone(r: Int, c: Int): MoveResult? {
        if (!isValidMove(r, c)) return null
        val color = currentPlayer; val opp = opponent(color)
        board[r][c] = color; passesInRow = 0; moveCount++

        val allCaptured = mutableListOf<Pair<Int, Int>>()
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nr = r + dr; val nc = c + dc
            if (inBounds(nr, nc) && board[nr][nc] == opp) {
                val group = getGroup(nr, nc)
                if (getLiberties(group).isEmpty()) {
                    for (stone in group) { board[stone.first][stone.second] = EMPTY; allCaptured += stone }
                }
            }
        }

        // Ko detection
        koPoint = if (allCaptured.size == 1 && getLiberties(getGroup(r, c)).size == 1) {
            val cap = allCaptured[0]
            board[cap.first][cap.second] = opp
            val wouldRecapture = getLiberties(getGroup(r, c)).isEmpty() && getGroup(r, c).size == 1
            board[cap.first][cap.second] = EMPTY
            if (wouldRecapture) cap else null
        } else null

        captures[color] = captures.getOrDefault(color, 0) + allCaptured.size
        updateCachesAfterMove(r, c, allCaptured)
        currentPlayer = opp
        return MoveResult(allCaptured.size, r to c)
    }

    fun pass(): Boolean {
        if (gameOver) return false
        passesInRow++; moveCount++; koPoint = null; clearCaches()
        if (passesInRow >= 2) { gameOver = true; winner = determineWinner() }
        else currentPlayer = opponent(currentPlayer)
        return true
    }

    fun determineWinner(): Int {
        val territory = calculateTerritory()
        val blackScore = territory.getOrDefault(BLACK, 0) + captures.getOrDefault(BLACK, 0)
        val whiteScore = territory.getOrDefault(WHITE, 0) + captures.getOrDefault(WHITE, 0) + 6.5
        return if (blackScore > whiteScore) BLACK else WHITE
    }

    private fun calculateTerritory(): Map<Int, Int> {
        val visited = mutableSetOf<Int>()
        val territory = mutableMapOf(BLACK to 0, WHITE to 0)
        for (r in 0 until size) for (c in 0 until size) {
            if (board[r][c] != EMPTY || (r * size + c) in visited) continue
            val region = mutableListOf<Pair<Int, Int>>()
            val borders = mutableSetOf<Int>()
            val stack = mutableListOf(r to c)
            while (stack.isNotEmpty()) {
                val (cr, cc) = stack.removeLast()
                val key = cr * size + cc
                if (key in visited) continue
                if (board[cr][cc] != EMPTY) { borders += board[cr][cc]; continue }
                visited += key; region += cr to cc
                for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                    val nr = cr + dr; val nc = cc + dc
                    if (inBounds(nr, nc) && (nr * size + nc) !in visited) stack += nr to nc
                }
            }
            if (borders.size == 1) {
                val owner = borders.first()
                territory[owner] = territory.getOrDefault(owner, 0) + region.size
            }
        }
        return territory
    }

    fun clone(): GoGame {
        val g = GoGame(size)
        for (r in 0 until size) g.board[r] = board[r].copyOf()
        g.currentPlayer = currentPlayer; g.captures.putAll(captures)
        g.koPoint = koPoint?.copy(); g.moveCount = moveCount; g.passesInRow = passesInRow
        g.gameOver = gameOver; g.winner = winner
        g.validBlack = validBlack?.map { it.copy() }
        g.validWhite = validWhite?.map { it.copy() }
        return g
    }
}
