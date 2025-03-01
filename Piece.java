package chess;

public abstract class Piece {
    public enum Color { 
        WHITE, BLACK;
        public Color opponent() {
            return (this == WHITE) ? BLACK : WHITE;
        }
    }

    protected Color color;
    protected boolean hasMoved = false;

    public Piece(Color color) {
        this.color = color;
    }

    public abstract boolean isValidMove(int startFile, int startRank,
                                       int endFile, int endRank,
                                       Board board);

    public Color getColor() {
        return color;
    }

    public void markAsMoved() {
        hasMoved = true;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public String getType() {
        if (this instanceof Pawn)   return "P";
        if (this instanceof Rook)   return "R";
        if (this instanceof Knight) return "N";
        if (this instanceof Bishop) return "B";
        if (this instanceof Queen)  return "Q";
        if (this instanceof King)   return "K";
        return "?";
    }

    // Utility methods to find piece’s position on a given board (used for King checks)
    public int getFile(Board board) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                if (board.getPiece(f, r) == this) {
                    return f;
                }
            }
        }
        return -1;
    }

    public int getRank(Board board) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                if (board.getPiece(f, r) == this) {
                    return r;
                }
            }
        }
        return -1;
    }
}
