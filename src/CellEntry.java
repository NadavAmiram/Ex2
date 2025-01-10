
public class CellEntry implements Index2D {
    private final int x;
    private final int y;
    
    public CellEntry(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public String toString() {
        char ch =' ';
        ch = (char) (ch + x);
        return ch + "" + (y);
    }
    
    @Override
    public boolean isValid() {
        return x >= 0 && x < 9 && y >= 0 && y < 17;
    }
    
    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}