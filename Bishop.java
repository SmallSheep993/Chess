package chess;

public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        if (Math.abs(endFile - startFile) != Math.abs(endRank - startRank)) {
            return false;
        }
        int stepFile = Integer.compare(endFile, startFile);
        int stepRank = Integer.compare(endRank, startRank);
        int steps = Math.abs(endFile - startFile);
        // Ensure all intermediate squares on the diagonal are empty
        for (int i = 1; i < steps; i++) {
            int file = startFile + i * stepFile;
            int rank = startRank + i * stepRank;
            if (board.getPiece(file, rank) != null) {
                return false;
            }
        }
        // Bishop can capture an opponent's piece or move to an empty square
        Piece destPiece = board.getPiece(endFile, endRank);
        return destPiece == null || destPiece.getColor() != color;
    }
}
