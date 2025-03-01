package chess;

public class Pawn extends Piece {
    public Pawn(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        int direction = (color == Color.WHITE) ? 1 : -1;
        int startRow = (color == Color.WHITE) ? 1 : 6;
        // Standard one-step forward
        if (endFile == startFile && endRank == startRank + direction) {
            return board.getPiece(endFile, endRank) == null;
        }
        // Initial two-step forward
        if (startRank == startRow && endFile == startFile 
            && endRank == startRank + 2 * direction) {
            // Both the square in front and the destination must be empty
            return board.getPiece(startFile, startRank + direction) == null 
                   && board.getPiece(endFile, endRank) == null;
        }
        // Diagonal capture (including en passant, which is treated as a capture move)
        if (Math.abs(endFile - startFile) == 1 && endRank == startRank + direction) {
            return isCaptureMove(startFile, startRank, endFile, endRank, board);
        }
        return false;
    }

    private boolean isCaptureMove(int startFile, int startRank,
                                  int endFile, int endRank, Board board) {
        Piece target = board.getPiece(endFile, endRank);
        if (target != null && target.getColor() != color) {
            // Normal diagonal capture
            return true;
        }
        // En passant capture
        String epTarget = board.getEnPassantTarget();
        if (epTarget != null) {
            int[] epPos = board.parsePosition(epTarget);
            if (epPos != null && epPos[0] == endFile && epPos[1] == endRank) {
                return true;
            }
        }
        return false;
    }

    public boolean canPromote(int endRank) {
        // White promotes at rank index 7 (8th rank), Black at rank index 0 (1st rank)
        return (color == Color.WHITE && endRank == 7) 
               || (color == Color.BLACK && endRank == 0);
    }
}
