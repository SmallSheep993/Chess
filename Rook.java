package chess;

public class Rook extends Piece {
    public Rook(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        if (startFile != endFile && startRank != endRank) {
            return false;
        }
        int stepFile = Integer.compare(endFile, startFile);
        int stepRank = Integer.compare(endRank, startRank);
        int steps = Math.max(Math.abs(endFile - startFile), Math.abs(endRank - startRank));
        // Check that all squares between start and end are empty
        for (int i = 1; i < steps; i++) {
            int file = startFile + i * stepFile;
            int rank = startRank + i * stepRank;
            if (board.getPiece(file, rank) != null) {
                return false;
            }
        }
        // Rook move is valid if destination is free or has an enemy piece
        Piece destPiece = board.getPiece(endFile, endRank);
        return destPiece == null || destPiece.getColor() != color;
    }
}
