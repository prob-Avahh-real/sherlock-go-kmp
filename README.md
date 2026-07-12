# sherlock-go-kmp
A Go (Weiqi/Baduk) board game with MCTS AI, built with Kotlin Multiplatform + Compose Multiplatform.

## Structure
- `shared/` — shared game engine: GoGame (rules), MctsAi (AI)
- `composeApp/` — Compose Multiplatform board UI
- `iosApp/` — iOS entry point

## Build
```bash
./gradlew :composeApp:assembleDebug
```

## Features
- Full Go rules: groups, liberties, capture, ko, territory scoring
- 19x19 board with star points
- MCTS AI with 2000 iterations
- Heuristic move selection (captures, patterns, territory evaluation)
- Pass and reset controls
