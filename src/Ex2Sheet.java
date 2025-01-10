import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a spreadsheet with support for text, numbers, and formulas.
 * Provides functionality for cell evaluation, depth calculation, and file I/O.
 */
public class Ex2Sheet implements Sheet {
    private Cell[][] table;

    /**
     * Creates a new spreadsheet with specified dimensions
     * @param x Width of the spreadsheet
     * @param y Height of the spreadsheet
     */
    public Ex2Sheet(int x, int y) {
        table = new SCell[x][y];
        initializeTable(x, y);
        eval();
    }

    /**
     * Creates a new spreadsheet with default dimensions
     */
    public Ex2Sheet() {
        this(Ex2Utils.WIDTH, Ex2Utils.HEIGHT);
    }

    /**
     * Initializes the spreadsheet with empty cells
     * @param x Width of the spreadsheet
     * @param y Height of the spreadsheet
     */
    private void initializeTable(int x, int y) {
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                table[i][j] = new SCell(Ex2Utils.EMPTY_CELL, this, getCellName(i, j));
            }
        }
    }

    /**
     * Generates a cell name from its coordinates (e.g., "A0")
     * @param col Column index
     * @param row Row index
     * @return Cell name in spreadsheet notation
     */
    private String getCellName(int col, int row) {
        return String.valueOf((char)('A' + col)) + row;
    }

    /**
     * Gets the evaluated value of a cell at specified coordinates
     */
    @Override
    public String value(int x, int y) {
        if (!isIn(x, y)) {
            return Ex2Utils.EMPTY_CELL;
        }

        Cell cell = table[x][y];
        if (cell instanceof SCell sCell) {
            String evaluatedValue = sCell.getEvaluatedValue();
            if (evaluatedValue != null) {
                return evaluatedValue;
            }
            evaluateCell(x, y);
            return sCell.getEvaluatedValue();
        }
        return "";
    }

    @Override
    public Cell get(int x, int y) {
        if (isIn(x, y)) {
            return table[x][y];
        }
        return null;
    }

    @Override
    public Cell get(String cords) {
        try {
            int[] xy = parseCoordinates(cords);
            return get(xy[0], xy[1]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int width() {
        return table.length;
    }

    @Override
    public int height() {
        return table[0].length;
    }

    /**
     * Sets the value of a cell at specified coordinates
     */
    @Override
    public void set(int col, int row, String val) {
        if (!isIn(col, row)) return;

        if (val == null || val.trim().isEmpty()) {
            table[col][row] = new SCell(Ex2Utils.EMPTY_CELL, this, getCellName(col, row));
            return;
        }

        table[col][row] = new SCell(val, this, getCellName(col, row));
    }

    /**
     * Evaluates all cells in the spreadsheet based on their dependencies
     */
    @Override
    public void eval() {
        int[][] depths = depth();

        // Reset previously evaluated values
        for (int i = 0; i < width(); i++) {
            for (int j = 0; j < height(); j++) {
                if (table[i][j] instanceof SCell) {
                    ((SCell) table[i][j]).setEvaluatedValue(null);
                }
            }
        }

        // Evaluate cells by depth order
        for (int currentDepth = 0; currentDepth <= getMaxDepth(depths); currentDepth++) {
            for (int i = 0; i < width(); i++) {
                for (int j = 0; j < height(); j++) {
                    if (depths[i][j] == currentDepth) {
                        evaluateCell(i, j);
                    }
                }
            }
        }
    }

    /**
     * Finds the maximum depth in the dependency tree
     */
    private int getMaxDepth(int[][] depths) {
        int maxDepth = 0;
        for (int[] row : depths) {
            for (int depth : row) {
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            }
        }
        return maxDepth;
    }

    /**
     * Evaluates a single cell at specified coordinates
     */
    private void evaluateCell(int x, int y) {
        if (!isIn(x, y)) return;

        Cell cell = table[x][y];
        if (!(cell instanceof SCell sCell)) return;

        String data = sCell.getData();

        // Empty cell
        if (data == null || data.trim().isEmpty()) {
            sCell.setEvaluatedValue("");
            return;
        }

        // Formula
        if (data.startsWith("=")) {
            // בדיקת תלות מעגלית
            int depth = calculateCellDepth(x, y, new boolean[width()][height()]);
            if (depth == Ex2Utils.ERR_CYCLE_FORM) {
                sCell.setType(Ex2Utils.ERR_CYCLE_FORM);  // עדכון הטיפוס
                sCell.setEvaluatedValue(Ex2Utils.ERR_CYCLE);
                return;
            }

            Double result = sCell.computeForm(data);
            if (result != null) {
                sCell.setType(Ex2Utils.FORM);  // נוסחה תקינה
                sCell.setEvaluatedValue(String.format("%.1f", result));
            } else {
                sCell.setType(Ex2Utils.ERR_FORM_FORMAT);  // שגיאת פורמט
                sCell.setEvaluatedValue(Ex2Utils.ERR_FORM);
            }
            return;
        }

        // Number
        if (sCell.isNumber()) {
            try {
                double val = Double.parseDouble(data);
                sCell.setType(Ex2Utils.NUMBER);
                sCell.setEvaluatedValue(String.format("%.1f", val));
            } catch (NumberFormatException e) {
                sCell.setType(Ex2Utils.TEXT);
                sCell.setEvaluatedValue(data);
            }
            return;
        }

        // Text
        sCell.setType(Ex2Utils.TEXT);
        sCell.setEvaluatedValue(data);
    }

    /**
     * Checks if coordinates are within spreadsheet bounds
     */
    @Override
    public boolean isIn(int xx, int yy) {
        return xx >= 0 && yy >= 0 && xx < width() && yy < height();
    }

    /**
     * Calculates dependency depths for all cells
     */
    @Override
    public int[][] depth() {
        int[][] depths = new int[width()][height()];
        boolean[][] visited = new boolean[width()][height()];

        for (int i = 0; i < width(); i++) {
            for (int j = 0; j < height(); j++) {
                if (!visited[i][j]) {
                    depths[i][j] = calculateCellDepth(i, j, new boolean[width()][height()]);
                }
            }
        }
        return depths;
    }
    private int calculateCellDepth(int row, int col, boolean[][] visited) {
        if (!isIn(row, col)) {
            return Ex2Utils.ERR_CYCLE_FORM;
        }

        Cell cell = table[row][col];
        if (cell == null) {
            return Ex2Utils.ERR_CYCLE_FORM;
        }

        String data = cell.getData();
        if (data == null || data.trim().isEmpty()) {
            return 0;
        }

        if (!data.startsWith("=")) {
            return 0;
        }

        // בדיקה אם זה מספר בפורמט מדעי
        String content = data.substring(1).trim();
        if (content.matches("^-?\\d*\\.?\\d+[eE][-+]?\\d+$")) {
            return 0;
        }

        // בדיקת מעגליות - אבל קודם נבדוק אם זו נוסחה שמאפסת את עצמה
        if (visited[row][col]) {
            // בדיקה האם זו נוסחה מהצורה A1-A1
            Pattern selfNegatingPattern = Pattern.compile("([A-Za-z][0-9]+)-\\1");
            Matcher selfNegatingMatcher = selfNegatingPattern.matcher(content);
            if (selfNegatingMatcher.matches()) {
                return 0;  // במקרה של A1-A1, התוצאה תמיד תהיה 0
            }
            return Ex2Utils.ERR_CYCLE_FORM;
        }

        Pattern pattern = Pattern.compile("[A-Za-z][0-9]+");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.find()) {
            return 0;  // אין תלויות בתאים אחרים
        }

        visited[row][col] = true;  // מסמן את התא כמבוקר
        matcher.reset();
        int maxDepth = 0;

        try {
            while (matcher.find()) {
                String ref = matcher.group();
                int nextCol = Character.toUpperCase(ref.charAt(0)) - 'A';
                int nextRow = Integer.parseInt(ref.substring(1));

                // בדיקת תקינות התא המאוזכר
                if (!isIn(nextCol, nextRow)) {
                    return Ex2Utils.ERR_CYCLE_FORM;
                }

                Cell dependentCell = table[nextCol][nextRow];
                if (dependentCell == null) {
                    return Ex2Utils.ERR_CYCLE_FORM;
                }

                // אם התא ריק, נחשיב אותו כ-0
                String dependentData = dependentCell.getData();
                if (dependentData == null || dependentData.trim().isEmpty()) {
                    continue;
                }

                int depth = calculateCellDepth(nextCol, nextRow, visited);
                if (depth == Ex2Utils.ERR_CYCLE_FORM) {
                    return Ex2Utils.ERR_CYCLE_FORM;
                }
                maxDepth = Math.max(maxDepth, depth);
            }

            return maxDepth + 1;
        } finally {
            visited[row][col] = false;  // תמיד נשחרר את הסימון בסוף
        }
    }

    /**
     * Evaluates a specific cell and returns its value
     */
    @Override
    public String eval(int x, int y) {
        if (!isIn(x, y)) {
            return null;
        }

        eval();

        Cell cell = get(x, y);
        if (cell instanceof SCell) {
            String evaluatedValue = ((SCell) cell).getEvaluatedValue();
            if (evaluatedValue != null && !evaluatedValue.isEmpty()) {
                return evaluatedValue;
            }
            return cell.getData();
        }

        return null;
    }

    /**
     * Saves the spreadsheet to a file
     */
    @Override
    public void save(String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(width() + "," + height() + "\n");

            for (int i = 0; i < width(); i++) {
                for (int j = 0; j < height(); j++) {
                    Cell cell = table[i][j];
                    String cellData = cell.getData();
                    if (cellData == null || cellData.trim().isEmpty()) {
                        cellData = "EMPTY";
                    }
                    cellData = cellData.replace(",", "\\,").replace("\n", "\\n");
                    writer.write(cellData);

                    if (j < height() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
        }
    }

    /**
     * Loads the spreadsheet from a file
     */
    @Override
    public void load(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String[] dimensions = reader.readLine().split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);

            table = new SCell[width][height];

            for (int i = 0; i < width; i++) {
                String[] row = reader.readLine().split("(?<!\\\\),");
                for (int j = 0; j < height; j++) {
                    String cellData = row[j]
                            .replace("\\,", ",")
                            .replace("\\n", "\n");

                    if (cellData.equals("EMPTY")) {
                        cellData = "";
                    }

                    table[i][j] = new SCell(cellData, this, getCellName(i, j));
                }
            }
            eval();
        }
    }

    /**
     * Parses cell coordinates from string format (e.g., "A0")
     */
    private int[] parseCoordinates(String cords) {
        if (cords == null || cords.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid coordinates: empty input");
        }

        cords = cords.trim();  // Remove toUpperCase() to accept lowercase
        if (!cords.matches("[A-Za-z][0-9]+")) {  // Modified regex to accept both cases
            throw new IllegalArgumentException("Invalid coordinates format: " + cords);
        }

        // Convert to uppercase for calculation regardless of input case
        int col = Character.toUpperCase(cords.charAt(0)) - 'A';
        int row = Integer.parseInt(cords.substring(1));

        if (!isIn(col, row)) {
            throw new IllegalArgumentException("Invalid coordinates: " + cords);
        }

        return new int[] { col, row };
    }
}