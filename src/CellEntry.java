/**
 * Represents a cell's coordinates and provides conversion between
 * spreadsheet notation (e.g., "A0") and array indices.
 */
public class CellEntry implements Index2D {
    private final int x;
    private final int y;

    /**
     * Creates a new cell entry with given coordinates
     * @param x Column index (0-25 for A-Z)
     * @param y Row index (0-99)
     */
    public CellEntry(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Converts cell coordinates to spreadsheet notation
     * @return String representation of the cell (e.g., "A0")
     */
    @Override
    public String toString() {
        char letter = 'A';
        letter = (char) (letter + x);
        return letter + "" + (y);
    }

    /**
     * Checks if the coordinates are within valid range
     * @return true if coordinates are valid (A-Z for columns, 0-99 for rows)
     */
    @Override
    public boolean isValid() {
        return x >= 0 && x < 26 && y >= 0 && y < 100;
    }


    /**
     * Gets the column index
     * @return Column index (0-25)
     */
    @Override
    public int getX() {
        return x;
    }

    /**
     * Gets the row index
     * @return Row index (0-99)
     */
    @Override
    public int getY() {
        return y;
    }
}