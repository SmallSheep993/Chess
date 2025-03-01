package chess;

public class Queen extends Piece {
    public Queen(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        // Queen moves like Rook or Bishop
        boolean rookMove = new Rook(color).isValidMove(startFile, startRank, endFile, endRank, board);
        boolean bishopMove = new Bishop(color).isValidMove(startFile, startRank, endFile, endRank, board);
        if (!(rookMove || bishopMove)) {
            return false;
        }
        // Ensure not capturing own piece
        Piece destPiece = board.getPiece(endFile, endRank);
        return destPiece == null || destPiece.getColor() != color;
    }
}
