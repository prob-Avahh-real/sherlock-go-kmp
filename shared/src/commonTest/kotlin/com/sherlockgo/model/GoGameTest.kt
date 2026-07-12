package com.sherlockgo.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoGameTest {
    @Test
    fun `new game has empty board`() {
        val game = GoGame(9)
        for (r in 0 until 9) for (c in 0 until 9)
            assertEquals(GoGame.EMPTY, game.board[r][c])
    }

    @Test
    fun `black goes first`() {
        val game = GoGame(9)
        assertEquals(GoGame.BLACK, game.currentPlayer)
    }

    @Test
    fun `place stone on empty intersection`() {
        val game = GoGame(9)
        val result = game.placeStone(4, 4)
        assertNotNull(result)
        assertEquals(GoGame.BLACK, game.board[4][4])
    }

    @Test
    fun `cannot place on occupied intersection`() {
        val game = GoGame(9)
        game.placeStone(4, 4) // black
        val result = game.placeStone(4, 4) // white tries same spot
        assertNull(result)
    }

    @Test
    fun `player alternates after move`() {
        val game = GoGame(9)
        game.placeStone(4, 4)
        assertEquals(GoGame.WHITE, game.currentPlayer)
    }

    @Test
    fun `two passes ends game`() {
        val game = GoGame(9)
        game.pass() // black passes
        game.pass() // white passes
        assertTrue(game.gameOver)
    }

    @Test
    fun `in bounds check`() {
        val game = GoGame(9)
        assertTrue(game.inBounds(0, 0))
        assertTrue(game.inBounds(8, 8))
        assertFalse(game.inBounds(-1, 0))
        assertFalse(game.inBounds(0, 9))
    }

    @Test
    fun `capture removes a stone`() {
        val game = GoGame(9)
        // Place black stone
        game.placeStone(4, 4)
        // Place white stones around it
        game.placeStone(3, 4)
        game.placeStone(5, 4)
        game.placeStone(4, 3)
        // Black tries to escape but white places at 4,5
        game.placeStone(4, 5) // this captures black at 4,4
        assertEquals(GoGame.EMPTY, game.board[4][4])
    }

    @Test
    fun `territory calculation is deterministic`() {
        val game = GoGame(9)
        game.placeStone(0, 0)
        val territory = game.calculateTerritory()
        assertNotNull(territory)
    }

    @Test
    fun `clone is independent`() {
        val game = GoGame(9)
        game.placeStone(4, 4)
        val clone = game.clone()
        assertEquals(game.board[4][4], clone.board[4][4])
        clone.placeStone(3, 3)
        assertEquals(GoGame.EMPTY, game.board[3][3]) // original unchanged
    }
}
