package chess;

public class King extends Piece {
    public King(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        int dx = Math.abs(endFile - startFile);
        int dy = Math.abs(endRank - startRank);
        // Normal one-square move in any direction
        if (dx <= 1 && dy <= 1) {
            if (dx == 0 && dy == 0) {
                return false; // no move
            }
            // Cannot land on a square occupied by own piece
            Piece destPiece = board.getPiece(endFile, endRank);
            if (destPiece != null && destPiece.getColor() == color) {
                return false;
            }
            // King cannot move into check
            return !board.isSquareUnderAttack(endFile, endRank, color.opponent());
        }
        // Castling move (two squares horizontally)
        if (dx == 2 && dy == 0) {
            return canCastle(startFile, startRank, endFile, board);
        }
        return false;
    }

    private boolean canCastle(int startFile, int startRank, int endFile, Board board) {
        if (hasMoved) return false;
        int direction = (endFile > startFile) ? 1 : -1;
        int rookFile = (direction == 1) ? 7 : 0;
        Piece rook = board.getPiece(rookFile, startRank);
        if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) {
            return false;
        }
        // Path between king and rook must be clear
        for (int i = startFile + direction; i != rookFile; i += direction) {
            if (board.getPiece(i, startRank) != null) {
                return false;
            }
        }
        // None of the squares king passes through or lands on are under attack
        for (int i = startFile; i != endFile + direction; i += direction) {
            if (board.isSquareUnderAttack(i, startRank, color.opponent())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getType() {
        return "K";
    }
}

