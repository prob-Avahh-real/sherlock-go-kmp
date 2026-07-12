// GameScreen.kt — Go game Board + AI (Compose Canvas)
package com.sherlockgo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherlockgo.ai.GoAI
import com.sherlockgo.model.GoGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun GameScreen() {
    val game = remember { GoGame(19) }
    val ai = remember { GoAI(game) }
    var refresh by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("黑方先行") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("221B Weiqi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5D4037),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {

            // Status bar
            Text(status, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3E2723), modifier = Modifier.padding(8.dp))

            // Board
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp)
                    .pointerInput(game) {
                        detectTapGestures { offset ->
                            val boardSize = min(size.width, size.height)
                            val padding = boardSize * 0.06f
                            val cellSize = (boardSize - padding * 2) / (game.size - 1)
                            val c = ((offset.x - padding) / cellSize).roundToInt()
                            val r = ((offset.y - padding) / cellSize).roundToInt()
                            if (r in 0 until game.size && c in 0 until game.size) {
                                val result = game.placeStone(r, c)
                                if (result != null) {
                                    refresh++
                                    if (!game.gameOver) {
                                        // AI response
                                        kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                                            val aiMove = ai.getBestMove(game.currentPlayer)
                                            aiMove?.let { game.placeStone(it.first, it.second) }
                                            withContext(Dispatchers.Main) { refresh++ }
                                        }
                                    }
                                    status = if (game.gameOver)
                                        if (game.winner == GoGame.BLACK) "黑胜" else "白胜"
                                    else if (game.currentPlayer == GoGame.BLACK) "黑方先行" else "白方落子"
                                }
                            }
                        }
                    }
            ) {
                val boardSize = min(size.width, size.height)
                val pad = boardSize * 0.06f
                val cellSize = (boardSize - pad * 2) / (game.size - 1)

                // Wood background
                drawRect(Color(0xFFDEB887))

                // Grid
                val gridColor = Color(0xFF3E2723)
                for (i in 0 until game.size) {
                    val pos = pad + i * cellSize
                    drawLine(gridColor, Offset(pad, pos), Offset(boardSize - pad, pos), 1f)
                    drawLine(gridColor, Offset(pos, pad), Offset(pos, boardSize - pad), 1f)
                }

                // Star points
                val stars = listOf(3, 9, 15)
                for (sr in stars) for (sc in stars) {
                    drawCircle(gridColor, 3f,
                        Offset(pad + sr * cellSize, pad + sc * cellSize))
                }

                // Stones
                for (r in 0 until game.size) for (c in 0 until game.size) {
                    if (game.board[r][c] == GoGame.EMPTY) continue
                    val color = if (game.board[r][c] == GoGame.BLACK) Color.Black else Color.White
                    val cx = pad + c * cellSize; val cy = pad + r * cellSize
                    drawCircle(color, cellSize * 0.43f, Offset(cx, cy))
                    if (color == Color.White) {
                        drawCircle(Color.Black, cellSize * 0.43f, Offset(cx, cy), style = Stroke(1f))
                    }
                }
            }

            // Controls
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { game.pass(); status = "Pass"; refresh++ }) {
                    Text("Pass")
                }
                OutlinedButton(onClick = {
                    // Reset
                    for (r in 0 until game.size) for (c in 0 until game.size) game.board[r][c] = GoGame.EMPTY
                    game.currentPlayer = GoGame.BLACK; game.moveCount = 0; game.gameOver = false
                    game.winner = null; refresh++; status = "黑方先行"
                }) {
                    Text("新局")
                }
            }
        }
    }
}
