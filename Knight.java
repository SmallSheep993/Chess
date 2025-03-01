package chess;

public class Knight extends Piece {
    public Knight(Color color) { super(color); }

    @Override
    public boolean isValidMove(int startFile, int startRank,
                               int endFile, int endRank,
                               Board board) {
        int dx = Math.abs(endFile - startFile);
        int dy = Math.abs(endRank - startRank);
        return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
    }
}
