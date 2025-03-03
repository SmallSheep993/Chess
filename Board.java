package chess;

import java.util.HashMap;
import java.util.Map;
import chess.Chess.Player;

public class Board {
    private final Piece[][] squares = new Piece[8][8];
    private Player currentPlayer = Player.white;
    private Map<String, Boolean> castlingRights = new HashMap<>();
    private String enPassantTarget = null;

    public Board() {
        initializeBoard();
        initializeCastlingRights();
    }

    // Copy constructor to clone board state
    public Board(Board other) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                if (other.squares[file][rank] != null) {
                    // Clone reference (pieces are immutable in terms of movement rules)
                    this.squares[file][rank] = other.squares[file][rank];
                }
            }
        }
        this.currentPlayer = other.currentPlayer;
        this.castlingRights = new HashMap<>(other.castlingRights);
        this.enPassantTarget = other.enPassantTarget;
    }

    private void initializeBoard() {
        // Place White pieces
        squares[0][0] = new Rook(Piece.Color.WHITE);
        squares[1][0] = new Knight(Piece.Color.WHITE);
        squares[2][0] = new Bishop(Piece.Color.WHITE);
        squares[3][0] = new Queen(Piece.Color.WHITE);
        squares[4][0] = new King(Piece.Color.WHITE);
        squares[5][0] = new Bishop(Piece.Color.WHITE);
        squares[6][0] = new Knight(Piece.Color.WHITE);
        squares[7][0] = new Rook(Piece.Color.WHITE);
        for (int i = 0; i < 8; i++) {
            squares[i][1] = new Pawn(Piece.Color.WHITE);
        }
        // Place Black pieces
        squares[0][7] = new Rook(Piece.Color.BLACK);
        squares[1][7] = new Knight(Piece.Color.BLACK);
        squares[2][7] = new Bishop(Piece.Color.BLACK);
        squares[3][7] = new Queen(Piece.Color.BLACK);
        squares[4][7] = new King(Piece.Color.BLACK);
        squares[5][7] = new Bishop(Piece.Color.BLACK);
        squares[6][7] = new Knight(Piece.Color.BLACK);
        squares[7][7] = new Rook(Piece.Color.BLACK);
        for (int i = 0; i < 8; i++) {
            squares[i][6] = new Pawn(Piece.Color.BLACK);
        }
    }
    

    private void initializeCastlingRights() {
        castlingRights.put("whiteKingSide", true);
        castlingRights.put("whiteQueenSide", true);
        castlingRights.put("blackKingSide", true);
        castlingRights.put("blackQueenSide", true);
    }

    /**
     * Attempt to move a piece from one position to another.
     * @param move A string like "e2 e4" (or including promotion e.g., "e7 e8 Q")
     * @return true if the move is executed successfully, false if it’s illegal.
     */
    public boolean movePiece(String move) {
        String[] parts = move.trim().split("\\s+");
        if (parts.length < 2) return false;
        int[] from = parsePosition(parts[0]);
        int[] to = parsePosition(parts[1]);
        if (from == null || to == null) return false;

        Piece piece = getPiece(from[0], from[1]);
        if (piece == null || piece.getColor() != currentPlayerColor()) {
            return false; // No piece there or not this player's piece
        }

        // Castling move
        if (piece instanceof King && Math.abs(to[0] - from[0]) == 2) {
            return handleCastling(from, to);
        } 
        // Pawn special moves (including promotion handled here)
        else if (piece instanceof Pawn) {
            String promotion = (parts.length == 3) ? parts[2] : "";
            return handlePawnMove(from, to, promotion);
        }

        // Normal move for Knight, Bishop, Rook, Queen, King (non-castling)
        if (!piece.isValidMove(from[0], from[1], to[0], to[1], this)) {
            return false;
        }
        executeMove(from, to);
        switchPlayer();
        return true;
    }

    private boolean handlePawnMove(int[] from, int[] to, String promotion) {
        if (!(squares[from[0]][from[1]] instanceof Pawn)) {
            return false;
        }
        Pawn pawn = (Pawn) squares[from[0]][from[1]];

        // Pawn promotion check
        
        if (pawn.canPromote(to[1])) {
            if (promotion.isEmpty()) promotion = "Q"; // default to Queen
            squares[to[0]][to[1]] = createPromotionPiece(promotion, pawn.getColor());
            squares[from[0]][from[1]] = null;
            enPassantTarget = null;  // no en passant possible after a promotion
            switchPlayer();
            return true;
        }
        /*
        if (pawn.canPromote(to[1])) {
            if (promotion.isEmpty()) promotion = "Q"; // default to Queen
            squares[to[0]][to[1]] = createPromotionPiece(promotion, pawn.getColor());
            squares[from[0]][from[1]] = null;
            switchPlayer();
            return true;
        }
        */

        // Normal pawn move (including diagonal capture and en passant moves)
        if (!pawn.isValidMove(from[0], from[1], to[0], to[1], this)) {
            return false;
        }

        // Execute pawn move
        executeMove(from, to);

        // If moved two squares, set en passant target; otherwise clear it
        if (Math.abs(to[1] - from[1]) == 2) {
            enPassantTarget = toPosition(new int[]{ from[0], (from[1] + to[1]) / 2 });
        } else {
            enPassantTarget = null;
        }

        switchPlayer();
        return true;
    }

    /** Moves a piece on the board without validation (internal use). */
    private void executeMove(int[] from, int[] to) {
        Piece piece = squares[from[0]][from[1]];
        squares[to[0]][to[1]] = piece;
        squares[from[0]][from[1]] = null;
        if (piece != null) {
            piece.markAsMoved();
        }
    }

    /**
     * Determine if a square is under attack by any piece of the given color.
     */
    public boolean isSquareUnderAttack(int file, int rank, Piece.Color attackerColor) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = squares[f][r];
                if (p != null && p.getColor() == attackerColor) {
                    if (p.isValidMove(f, r, file, rank, this)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int[] parsePosition(String pos) {
        if (pos == null || pos.length() != 2) return null;
        int file = pos.charAt(0) - 'a';
        int rank = Character.getNumericValue(pos.charAt(1)) - 1;
        if (file < 0 || file > 7 || rank < 0 || rank > 7) return null;
        return new int[]{ file, rank };
    }

    /** Convert array indices back to chess notation (for internal use) */
    public String toPosition(int[] pos) {
        return (char) ('a' + pos[0]) + String.valueOf(pos[1] + 1);
    }

    public Piece getPiece(int file, int rank) {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) return null;
        return squares[file][rank];
    }

    private Piece createPromotionPiece(String type, Piece.Color color) {
        return switch (type.toUpperCase()) {
            case "Q" -> new Queen(color);
            case "R" -> new Rook(color);
            case "B" -> new Bishop(color);
            case "N" -> new Knight(color);
            default -> new Queen(color); // default to Queen
        };
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == Player.white) ? Player.black : Player.white;
    }

    public String getEnPassantTarget() {
        return enPassantTarget;
    }

    public Map<String, Boolean> getCastlingRights() {
        return castlingRights;
    }

    /**
     * Perform castling (king and rook moves) if legal. Assumes the move is a king moving two squares.
     */
    private boolean handleCastling(int[] from, int[] to) {
        King king = (King) getPiece(from[0], from[1]);
        if (king == null || king.hasMoved()) {
            return false;
        }
        int direction = (to[0] > from[0]) ? 1 : -1;
        int rookFile = (direction == 1) ? 7 : 0;
        Rook rook = (Rook) getPiece(rookFile, from[1]);
        if (rook == null || rook.hasMoved()) {
            return false;
        }
        // Check that squares between king and rook are empty
        for (int i = from[0] + direction; i != rookFile; i += direction) {
            if (getPiece(i, from[1]) != null) {
                return false;
            }
        }
        // Ensure king is not moving through or into check
        Piece.Color opponentColor = king.getColor().opponent();
        if (isSquareUnderAttack(from[0], from[1], opponentColor) ||
            isSquareUnderAttack(from[0] + direction, from[1], opponentColor) ||
            isSquareUnderAttack(to[0], from[1], opponentColor)) {
            return false;
        }
        // Perform castling: move king and rook
        executeMove(from, to);
        executeMove(new int[]{ rookFile, from[1] }, new int[]{ from[0] + direction, from[1] });
        switchPlayer();
        return true;
    }

    /**
     * Validate whether a castling move is legal (without executing it).
     */
    public boolean isValidCastling(int[] from, int[] to, Player currentPlayer) {
        Piece piece = getPiece(from[0], from[1]);
        if (!(piece instanceof King)) return false;
        King king = (King) piece;
        if (king.hasMoved()) return false;
        // Must move two files on same rank
        if (Math.abs(to[0] - from[0]) != 2 || from[1] != to[1]) return false;
        // Determine rook and direction
        int direction = (to[0] > from[0]) ? 1 : -1;
        int rookFile = (direction == 1) ? 7 : 0;
        Piece rookPiece = getPiece(rookFile, from[1]);
        if (!(rookPiece instanceof Rook)) return false;
        Rook rook = (Rook) rookPiece;
        if (rook.hasMoved()) return false;
        // Check path is clear
        for (int x = from[0] + direction; x != rookFile; x += direction) {
            if (getPiece(x, from[1]) != null) return false;
        }
        // Check king not in check or moving through check
        Piece.Color opponentColor = king.getColor().opponent();
        if (isSquareUnderAttack(from[0], from[1], opponentColor) ||
            isSquareUnderAttack(from[0] + direction, from[1], opponentColor) ||
            isSquareUnderAttack(to[0], to[1], opponentColor)) {
            return false;
        }
        return true;
    }

    /**
     * Execute an en passant capture. Assumes the move is identified as en passant.
     */
    public void executeEnPassant(int[] from, int[] to) {
        Piece pawn = getPiece(from[0], from[1]);
        if (!(pawn instanceof Pawn)) {
            return;
        }
        // Determine position of pawn to be captured
        int capturedPawnFile = to[0];
        int capturedPawnRank = ((Pawn) pawn).getColor() == Piece.Color.WHITE 
                                ? to[1] - 1 
                                : to[1] + 1;
        Piece capturedPawn = getPiece(capturedPawnFile, capturedPawnRank);
        // Only proceed if target square is empty and the adjacent pawn is present
        if (getPiece(to[0], to[1]) == null && capturedPawn instanceof Pawn) {
            // Remove the captured pawn
            setPiece(capturedPawnFile, capturedPawnRank, null);
            // Move the pawn performing en passant
            setPiece(to[0], to[1], pawn);
            setPiece(from[0], from[1], null);
            pawn.markAsMoved();
            enPassantTarget = null;
            switchPlayer();
        }
    }

    public void setPiece(int file, int rank, Piece piece) {
        squares[file][rank] = piece;
    }

    private Piece.Color currentPlayerColor() {
        return (currentPlayer == Player.white) ? Piece.Color.WHITE : Piece.Color.BLACK;
    }
}
