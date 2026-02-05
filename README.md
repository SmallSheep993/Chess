# Two-Player Chess (Java)

A complete two-player chess engine implemented in Java, featuring full rule enforcement, modular design, and a text-based interface suitable for automated testing environments.

This project emphasizes correctness, clean object-oriented architecture, and strict adherence to official chess rules.

---

## Features

- Full implementation of standard chess rules:
  - Legal move validation for all pieces
  - Check and checkmate detection
  - Castling (king-side and queen-side)
  - En passant
  - Pawn promotion (Q, R, B, N)
- Game termination handling:
  - Checkmate
  - Resignation
  - Draw offers
- Turn-based enforcement (White always moves first)
- Robust illegal move handling (illegal moves do not advance turns)
- Board state reset support for multiple consecutive games

---

## Project Structure

chess/
├── Chess.java # Game controller and entry point for gameplay logic
├── Board.java # Board representation and state management
├── Piece.java # Abstract base class for all chess pieces
├── Pawn.java
├── Rook.java
├── Knight.java
├── Bishop.java
├── Queen.java
├── King.java
└── PlayChess.java # Console-based test driver (not required for submission)


---

## Design Overview

The system is built using a modular, object-oriented approach:

- Each chess piece encapsulates its own movement rules.
- The `Board` class manages piece placement, move execution, and state validation.
- The `Chess` class coordinates gameplay, enforces turn order, and handles game-ending conditions.
- Illegal moves are detected by simulating moves and verifying king safety before committing state changes.

This separation of concerns improves readability, maintainability, and correctness under automated evaluation.

---

## Running the Project

### Requirements
- Java 21 or later

### Compile
```bash
javac chess/*.java


Run (Console Test Mode)
java chess.PlayChess

The console interface allows players to enter moves using algebraic coordinates, such as:

e2 e4
g8 h6
g1 f3 draw?
resign


Notes

This project is designed to support automated grading systems and does not rely on console output for correctness.

Game state is returned programmatically through structured objects rather than printed output.

The implementation prioritizes rule correctness over UI or AI components.
