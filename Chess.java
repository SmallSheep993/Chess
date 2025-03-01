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
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            return result;
        }

        move = move.trim();
        if (move.equalsIgnoreCase("resign")) {
            // Current player resigns
            result.message = (currentPlayer == Player.white) ? 
                              ReturnPlay.Message.RESIGN_BLACK_WINS 
                            : ReturnPlay.Message.RESIGN_WHITE_WINS;
            gameOver = true;
            return result;
        }

        // Parse move input
        String[] parts = move.split(" ");
        if (parts.length < 2) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            return result;
        }
        int[] from = parsePosition(parts[0]);
        int[] to = parsePosition(parts[1]);
        if (from == null || to == null) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            return result;
        }
        String promotion = (parts.length == 3) ? parts[2] : "";

        // Check for castling
        if (isCastling(from, to)) {
            if (!board.isValidCastling(from, to, currentPlayer)) {
                result.message = ReturnPlay.Message.ILLEGAL_MOVE;
                return result;
            }
        }

        // Check for en passant
        if (isEnPassant(from, to)) {
            board.executeEnPassant(from, to);
            result.piecesOnBoard = getCurrentBoardState();
            previousMove = move;
            // Switch player after en passant
            currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
            return result;
        }

        // Attempt the move
        if (!board.movePiece(parts[0] + " " + parts[1] + (promotion.isEmpty() ? "" : " " + promotion))) {
            result.message = ReturnPlay.Message.ILLEGAL_MOVE;
            return result;
        }

        // Move succeeded – update game state
        previousMove = move;
        currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
        result.piecesOnBoard = getCurrentBoardState();

        // Check for check or checkmate
        if (isCheckmate()) {
            result.message = (currentPlayer == Player.white) 
                              ? ReturnPlay.Message.CHECKMATE_BLACK_WINS 
                              : ReturnPlay.Message.CHECKMATE_WHITE_WINS;
            gameOver = true;
        } else if (isCheck()) {
            result.message = ReturnPlay.Message.CHECK;
        }

        return result;
    }

    private static boolean isCastling(int[] from, int[] to) {
        // True if moving piece is a King moving two squares horizontally
        return board.getPiece(from[0], from[1]) instanceof King 
               && Math.abs(from[0] - to[0]) == 2 
               && from[1] == to[1];
    }

    private static boolean isEnPassant(int[] from, int[] to) {
        Piece movingPiece = board.getPiece(from[0], from[1]);
        if (!(movingPiece instanceof Pawn)) return false;
        // Target must be empty and match the enPassant target square
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
                    rp.pieceRank = 8 - rank;
                    rp.pieceType = ReturnPiece.PieceType.valueOf(
                                   (piece.getColor() == Piece.Color.WHITE ? "W" : "B") 
                                   + piece.getType());
                    pieces.add(rp);
                }
            }
        }
        return pieces;
    }

    private static int[] parsePosition(String pos) {
        if (pos.length() != 2) return null;
        int file = pos.charAt(0) - 'a';
        int rank = 8 - Character.getNumericValue(pos.charAt(1));
        if (file < 0 || file > 7 || rank < 0 || rank > 7) return null;
        return new int[]{ file, rank };
    }

    private static boolean isCheck() {
        King king = findKing(currentPlayer);
        if (king == null) return false;
        // Check if current player's king is under attack by opponent
        Piece.Color opponentColor = (currentPlayer == Player.white) 
                                    ? Piece.Color.BLACK 
                                    : Piece.Color.WHITE;
        return board.isSquareUnderAttack(king.getFile(board), king.getRank(board), opponentColor);
    }

    private static boolean isCheckmate() {
        King king = findKing(currentPlayer);
        if (king == null || !isCheck()) return false;
        // If any move can get the king out of check, it's not checkmate
        Piece.Color currentColor = currentPlayerColor();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.getPiece(file, rank);
                if (piece == null || piece.getColor() != currentColor) continue;
                for (int tFile = 0; tFile < 8; tFile++) {
                    for (int tRank = 0; tRank < 8; tRank++) {
                        if (piece.isValidMove(file, rank, tFile, tRank, board)) {
                            // Try move on a temporary board
                            Board tempBoard = new Board(board);
                            tempBoard.movePiece(board.toPosition(new int[]{file, rank}) + " " 
                                               + board.toPosition(new int[]{tFile, tRank}));
                            King tempKing = findKing(currentPlayer);
                            if (tempKing != null && 
                               !tempBoard.isSquareUnderAttack(tempKing.getFile(tempBoard), 
                                                              tempKing.getRank(tempBoard), 
                                                              currentColor.opponent())) {
                                return false; // Found a move that escapes check
                            }
                        }
                    }
                }
            }
        }
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
}
