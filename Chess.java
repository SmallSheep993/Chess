package chess;

import java.util.ArrayList;

public class Chess {
    enum Player { white, black }
    
    private static Board board;
    private static Player currentPlayer = Player.white;
    private static boolean gameOver = false;
    private static String previousMove = null; // record last move if needed

    public static void start() {
        board = new Board();
        currentPlayer = Player.white;
        gameOver = false;
        previousMove = null;
    }

    public static ReturnPlay play(String move) {
        ReturnPlay result = new ReturnPlay();
        if (gameOver) {
            // The game is over. Any further commands will be considered illegal movement.
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }

        move = move.trim();
        if (move.equalsIgnoreCase("resign")) {
            // Current Player Concedes
            result.message = (currentPlayer == Player.white) 
                    ? ReturnPlay.Message.RESIGN_BLACK_WINS 
                    : ReturnPlay.Message.RESIGN_WHITE_WINS;
            gameOver = true;
            // stay the same
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }

        // check draw（draw?）
        boolean drawOffered = false;
        if (move.endsWith("draw?")) {
            drawOffered = true;
            // Remove the “draw?” part at the end and keep only the move commands.
            move = move.substring(0, move.length() - 5).trim();
        }

        // Analyzing move commands
        String[] parts = move.split("\\s+");
        if (parts.length < 2) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }
        int[] from = parsePosition(parts[0]);
        int[] to = parsePosition(parts[1]);
        if (from == null || to == null) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }
        // Check for up-change sign (if any)
        String promotion = "";
        if (parts.length >= 3 && parts[2].length() == 1 && !"draw?".equalsIgnoreCase(parts[2])) {
            promotion = parts[2];
        }

        // King castling
        if (isCastling(from, to)) {
            if (!board.isValidCastling(from, to, currentPlayer)) {
                result.message = ReturnPlay.Message.ILLEGAL_MOVE;
                result.piecesOnBoard = getCurrentBoardState();
                return result;
            }
            // King castling
            board.movePiece(parts[0] + " " + parts[1]);
            previousMove = parts[0] + " " + parts[1];
            // switch player
            currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
            result.piecesOnBoard = getCurrentBoardState();
            // check/checkmate
            if (isCheckmate()) {
                result.message = (currentPlayer == Player.white) 
                        ? ReturnPlay.Message.CHECKMATE_BLACK_WINS 
                        : ReturnPlay.Message.CHECKMATE_WHITE_WINS;
                gameOver = true;
            } else if (isCheck()) {
                result.message = ReturnPlay.Message.CHECK;
            }
            
            if (!gameOver && drawOffered) {
                result.message = ReturnPlay.Message.DRAW;
                gameOver = true;
            }
            return result;
        }

        // （en passant）
        if (isEnPassant(from, to)) {
            // 
            board.executeEnPassant(from, to);
            previousMove = parts[0] + " " + parts[1];
            // switch player
            currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
            result.piecesOnBoard = getCurrentBoardState();
            // check/checkmate
            if (isCheckmate()) {
                result.message = (currentPlayer == Player.white) 
                        ? ReturnPlay.Message.CHECKMATE_BLACK_WINS 
                        : ReturnPlay.Message.CHECKMATE_WHITE_WINS;
                gameOver = true;
            } else if (isCheck()) {
                result.message = ReturnPlay.Message.CHECK;
            }
            //
            if (!gameOver && drawOffered) {
                result.message = ReturnPlay.Message.DRAW;
                gameOver = true;
            }
            return result;
        }

        // Use a temporary board to simulate moves in order to verify legality (especially to avoid a checkmate for your king)
        Board testBoard = deepCloneBoard(board);
        boolean testMoveSuccess = testBoard.movePiece(parts[0] + " " + parts[1] + (promotion.isEmpty() ? "" : " " + promotion));
        if (!testMoveSuccess) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }
        // Find the position of the current player's king on the simulation board
        Piece.Color movingColor = (currentPlayer == Player.white) ? Piece.Color.WHITE : Piece.Color.BLACK;
        int kingFile = -1, kingRank = -1;
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = testBoard.getPiece(f, r);
                if (p instanceof King && p.getColor() == movingColor) {
                    kingFile = f;
                    kingRank = r;
                    break;
                }
            }
            if (kingFile != -1) break;
        }
        if (kingFile != -1 && testBoard.isSquareUnderAttack(kingFile, kingRank, movingColor.opponent())) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            result.piecesOnBoard = getCurrentBoardState();
            return result;
        }

        board.movePiece(parts[0] + " " + parts[1] + (promotion.isEmpty() ? "" : " " + promotion));
        previousMove = parts[0] + " " + parts[1] + (promotion.isEmpty() ? "" : " " + promotion);
        // switch player
        currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
        result.piecesOnBoard = getCurrentBoardState();

        // checkmate/draw
        if (isCheckmate()) {
            result.message = (currentPlayer == Player.white) 
                    ? ReturnPlay.Message.CHECKMATE_BLACK_WINS 
                    : ReturnPlay.Message.CHECKMATE_WHITE_WINS;
            gameOver = true;
        } else if (isCheck()) {
            result.message = ReturnPlay.Message.CHECK;
        } else if (isStalemate()) {
            result.message = ReturnPlay.Message.STALEMATE;
            gameOver = true;
        }

        // If the move proposes a draw and the game does not end with a checkmate/draw, it ends with a draw.
        if (!gameOver && drawOffered) {
            result.message = ReturnPlay.Message.DRAW;
            gameOver = true;
        }

        return result;
    }

    private static boolean isCastling(int[] from, int[] to) {
        // Determine if it is a king two-frame horizontal move 
        return board.getPiece(from[0], from[1]) instanceof King 
                && Math.abs(from[0] - to[0]) == 2 
                && from[1] == to[1];
    }

    private static boolean isEnPassant(int[] from, int[] to) {
        Piece movingPiece = board.getPiece(from[0], from[1]);
        if (!(movingPiece instanceof Pawn)) return false;
        // The target frame must be empty and coincide with a position where a passerby pawn can be eaten
        if (board.getPiece(to[0], to[1]) != null) return false;
        String epTarget = board.getEnPassantTarget();
        if (epTarget == null) return false;
        int[] epPos = board.parsePosition(epTarget);
        if (epPos == null) return false;
        return (epPos[0] == to[0] && epPos[1] == to[1]);
    }

    private static ArrayList<ReturnPiece> getCurrentBoardState() {
        ArrayList<ReturnPiece> pieces = new ArrayList<>();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.getPiece(file, rank);
                if (piece != null) {
                    ReturnPiece rp = new ReturnPiece();
                    rp.pieceFile = ReturnPiece.PieceFile.values()[file];
                    // Converts internal row numbers to checkerboard row numbers (1-8), with 0 representing 1 row and 7 representing 8 rows.
                    rp.pieceRank = rank + 1;
                    rp.pieceType = ReturnPiece.PieceType.valueOf(
                            (piece.getColor() == Piece.Color.WHITE ? "W" : "B") + piece.getType());
                    pieces.add(rp);
                }
            }
        }
        return pieces;
    }

    private static int[] parsePosition(String pos) {
        if (pos == null || pos.length() != 2) return null;
        int file = pos.charAt(0) - 'a';
        int rank = Character.getNumericValue(pos.charAt(1)) - 1;
        if (file < 0 || file > 7 || rank < 0 || rank > 7) return null;
        return new int[]{ file, rank };
    }

    private static boolean isCheck() {
        // Check if the current player's king is generalized by the opponent
        Piece.Color opponentColor = (currentPlayer == Player.white) ? Piece.Color.BLACK : Piece.Color.WHITE;
        Piece.Color currentColor = currentPlayerColor();
        int kingFile = -1, kingRank = -1;
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = board.getPiece(f, r);
                if (p instanceof King && p.getColor() == currentColor) {
                    kingFile = f;
                    kingRank = r;
                    break;
                }
            }
            if (kingFile != -1) break;
        }
        if (kingFile == -1) return false;
        return board.isSquareUnderAttack(kingFile, kingRank, opponentColor);
    }

    private static boolean isCheckmate() {
        Piece.Color currentColor = currentPlayerColor();
        // If the current player's king is not generalized, it cannot be General Death
        if (!isCheck()) {
            return false;
        }
        // Iterate through each of the current player's pieces, trying every possible move to see if you can get rid of the general
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.getPiece(file, rank);
                if (piece == null || piece.getColor() != currentColor) continue;
                for (int tFile = 0; tFile < 8; tFile++) {
                    for (int tRank = 0; tRank < 8; tRank++) {
                        if (!piece.isValidMove(file, rank, tFile, tRank, board)) continue;
                        Board cloneBoard = deepCloneBoard(board);
                        String moveStr = board.toPosition(new int[]{file, rank}) + " " 
                                       + board.toPosition(new int[]{tFile, tRank});
                        if (!cloneBoard.movePiece(moveStr)) continue;
                        // Find the current position of the player's king on the simulation board
                        int kf = -1, kr = -1;
                        for (int cf = 0; cf < 8; cf++) {
                            for (int cr = 0; cr < 8; cr++) {
                                Piece cp = cloneBoard.getPiece(cf, cr);
                                if (cp instanceof King && cp.getColor() == currentColor) {
                                    kf = cf;
                                    kr = cr;
                                    break;
                                }
                            }
                            if (kf != -1) break;
                        }
                        if (kf != -1 && !cloneBoard.isSquareUnderAttack(kf, kr, currentColor.opponent())) {
                            // There exists a move that can take the king away from the general, not the general's death.
                            return false;
                        }
                    }
                }
            }
        }
        // No any move that avoids a general is a general's death.
        return true;
    }

    private static boolean isStalemate() {
        // Draw (stalemate): The current player has no legal moves and is not in a checkmate state.
        Piece.Color currentColor = currentPlayerColor();
        if (isCheck()) {
            return false;
        }
        // Try all moves of all the current player's pieces, as long as there is a legal move it is not a stalemate
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.getPiece(file, rank);
                if (piece == null || piece.getColor() != currentColor) continue;
                for (int tFile = 0; tFile < 8; tFile++) {
                    for (int tRank = 0; tRank < 8; tRank++) {
                        if (!piece.isValidMove(file, rank, tFile, tRank, board)) continue;
                        Board cloneBoard = deepCloneBoard(board);
                        String moveStr = board.toPosition(new int[]{file, rank}) + " " 
                                       + board.toPosition(new int[]{tFile, tRank});
                        if (!cloneBoard.movePiece(moveStr)) continue;
                        // Find the current position of the player's king on the simulation board
                        int kf = -1, kr = -1;
                        for (int cf = 0; cf < 8; cf++) {
                            for (int cr = 0; cr < 8; cr++) {
                                Piece cp = cloneBoard.getPiece(cf, cr);
                                if (cp instanceof King && cp.getColor() == currentColor) {
                                    kf = cf;
                                    kr = cr;
                                    break;
                                }
                            }
                            if (kf != -1) break;
                        }
                        if (kf != -1 && !cloneBoard.isSquareUnderAttack(kf, kr, currentColor.opponent())) {
                            return false;
                        }
                    }
                }
            }
        }
        // There are no legal moves and no checkmate - draws
        return true;
    }

    private static King findKing(Player player) {
        Piece.Color color = (player == Player.white) ? Piece.Color.WHITE : Piece.Color.BLACK;
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.getPiece(file, rank);
                if (piece instanceof King && piece.getColor() == color) {
                    return (King) piece;
                }
            }
        }
        return null;
    }

    private static Piece.Color currentPlayerColor() {
        return (currentPlayer == Player.white) ? Piece.Color.WHITE : Piece.Color.BLACK;
    }

    // Deep cloning of board objects (including piece state) for simulating moves
    private static Board deepCloneBoard(Board original) {
        Board cloned = new Board(original);
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece origPiece = original.getPiece(f, r);
                if (origPiece != null) {
                    Piece newPiece;
                    Piece.Color color = origPiece.getColor();
                    // Creates a new piece object based on the piece type.
                    if (origPiece instanceof Pawn) {
                        newPiece = new Pawn(color);
                    } else if (origPiece instanceof Rook) {
                        newPiece = new Rook(color);
                    } else if (origPiece instanceof Knight) {
                        newPiece = new Knight(color);
                    } else if (origPiece instanceof Bishop) {
                        newPiece = new Bishop(color);
                    } else if (origPiece instanceof Queen) {
                        newPiece = new Queen(color);
                    } else if (origPiece instanceof King) {
                        newPiece = new King(color);
                    } else {
                        newPiece = null;
                    }
                    if (newPiece != null && origPiece.hasMoved()) {
                        newPiece.markAsMoved();
                    }
                    cloned.setPiece(f, r, newPiece);
                } else {
                    cloned.setPiece(f, r, null);
                }
            }
        }
        return cloned;
    }
}

