import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ex2Sheet implements Sheet {
    private Cell[][] table;
    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile("[A-Za-z][0-9]+");
    private static final Pattern SELF_NEGATING_PATTERN = Pattern.compile("([A-Za-z][0-9]+)-\\1");
    private static final Pattern SCIENTIFIC_NOTATION_PATTERN = Pattern.compile("^-?\\d*\\.?\\d+[eE][-+]?\\d+$");

    // Constructors
    public Ex2Sheet(int x, int y) {
        initializeSheet(x, y);
        eval();
    }

    public Ex2Sheet() {
        this(Ex2Utils.WIDTH, Ex2Utils.HEIGHT);
    }


    // Private initialization methods
    private void initializeSheet(int cols, int rows) {
        table = new SCell[cols][rows];
        initializeEmptyCells(cols, rows);
    }

    private void initializeEmptyCells(int cols, int rows) {
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                createEmptyCell(col, row);
            }
        }
    }

    private void createEmptyCell(int col, int row) {
        table[col][row] = new SCell(Ex2Utils.EMPTY_CELL, this, generateCellName(col, row));
    }

    private String generateCellName(int col, int row) {
        return String.valueOf((char)('A' + col)) + row;
    }

    // Interface implementation methods
    @Override
    public boolean isIn(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }

    @Override
    public int width() {
        return table.length;
    }

    @Override
    public int height() {
        return table[0].length;
    }

    @Override
    public void set(int col, int row, String val) {
        if (!isIn(col, row)) return;
        String value = normalizeValue(val);
        table[col][row] = new SCell(value, this, generateCellName(col, row));
    }

    private String normalizeValue(String val) {
        return (val == null || val.trim().isEmpty()) ? Ex2Utils.EMPTY_CELL : val;
    }

    @Override
    public Cell get(int x, int y) {
        return isIn(x, y) ? table[x][y] : null;
    }

    @Override
    public Cell get(String entry) {
        try {
            int[] coords = parseCellReference(entry);
            return get(coords[0], coords[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private int[] parseCellReference(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid cell reference: empty input");
        }

        entry = entry.trim().toUpperCase();
        if (!entry.matches("[A-Z][0-9]+")) {
            throw new IllegalArgumentException("Invalid cell reference format: " + entry);
        }

        int col = entry.charAt(0) - 'A';
        int row = Integer.parseInt(entry.substring(1));

        if (!isIn(col, row)) {
            throw new IllegalArgumentException("Cell reference out of bounds: " + entry);
        }

        return new int[]{col, row};
    }

    @Override
    public String value(int x, int y) {
        if (!isIn(x, y)) {
            return Ex2Utils.EMPTY_CELL;
        }

        Cell cell = table[x][y];
        if (!(cell instanceof SCell sCell)) {
            return Ex2Utils.EMPTY_CELL;
        }

        String evalValue = sCell.getEvaluatedValue();
        if (evalValue != null) {
            return evalValue;
        }

        evaluateCell(x, y);
        return sCell.getEvaluatedValue();
    }

    @Override
    public void eval() {
        resetEvaluatedValues();
        int[][] depths = depth();
        evaluateCellsByDepth(depths);
    }

    private void resetEvaluatedValues() {
        for (int col = 0; col < width(); col++) {
            for (int row = 0; row < height(); row++) {
                if (table[col][row] instanceof SCell sCell) {
                    sCell.setEvaluatedValue(null);
                }
            }
        }
    }

    private void evaluateCellsByDepth(int[][] depths) {
        int maxDepth = findMaxDepth(depths);
        for (int depth = 0; depth <= maxDepth; depth++) {
            evaluateCellsAtDepth(depths, depth);
        }
    }

    private int findMaxDepth(int[][] depths) {
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

    private void evaluateCellsAtDepth(int[][] depths, int targetDepth) {
        for (int col = 0; col < width(); col++) {
            for (int row = 0; row < height(); row++) {
                if (depths[col][row] == targetDepth) {
                    evaluateCell(col, row);
                }
            }
        }
    }

    private void evaluateCell(int col, int row) {
        if (!isIn(col, row)) return;

        SCell sCell = (SCell) table[col][row];
        String data = sCell.getData();

        if (isEmptyData(data)) {
            handleEmptyCell(sCell);
            return;
        }

        if (isFormula(data)) {
            handleFormula(col, row, sCell, data);
            return;
        }

        handleNonFormulaCell(sCell, data);
    }

    private boolean isEmptyData(String data) {
        return data == null || data.trim().isEmpty();
    }

    private boolean isFormula(String data) {
        return data.startsWith("=");
    }

    private void handleEmptyCell(SCell cell) {
        cell.setEvaluatedValue("");
    }

    private void handleFormula(int col, int row, SCell cell, String data) {
        int depthResult = calculateDepth(col, row, new boolean[width()][height()]);

        if (depthResult == Ex2Utils.ERR_CYCLE_FORM) {
            markCellAsCyclic(cell);
            return;
        }

        evaluateFormula(cell, data);
    }

    private void markCellAsCyclic(SCell cell) {
        cell.setType(Ex2Utils.ERR_CYCLE_FORM);
        cell.setEvaluatedValue(Ex2Utils.ERR_CYCLE);
    }

    private void evaluateFormula(SCell cell, String formula) {
        Double result = cell.computeForm(formula);
        if (result != null) {
            cell.setType(Ex2Utils.FORM);
            cell.setEvaluatedValue(String.format("%.1f", result));
        } else {
            cell.setType(Ex2Utils.ERR_FORM_FORMAT);
            cell.setEvaluatedValue(Ex2Utils.ERR_FORM);
        }
    }

    private void handleNonFormulaCell(SCell cell, String data) {
        if (isNumeric(data)) {
            handleNumericCell(cell, data);
        } else {
            handleTextCell(cell, data);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleNumericCell(SCell cell, String data) {
        double value = Double.parseDouble(data);
        cell.setType(Ex2Utils.NUMBER);
        cell.setEvaluatedValue(String.format("%.1f", value));
    }

    private void handleTextCell(SCell cell, String data) {
        cell.setType(Ex2Utils.TEXT);
        cell.setEvaluatedValue(data);
    }

    @Override
    public int[][] depth() {
        int[][] depths = new int[width()][height()];
        boolean[][] visited = new boolean[width()][height()];

        for (int col = 0; col < width(); col++) {
            for (int row = 0; row < height(); row++) {
                if (!visited[col][row]) {
                    depths[col][row] = calculateDepth(col, row, new boolean[width()][height()]);
                }
            }
        }
        return depths;
    }

    private int calculateDepth(int col, int row, boolean[][] visited) {
        if (!isValidCell(col, row)) {
            return Ex2Utils.ERR_CYCLE_FORM;
        }

        String data = table[col][row].getData();
        if (isEmptyData(data) || !isFormula(data)) {
            return 0;
        }

        String content = data.substring(1).trim();
        if (isScientificNotation(content)) {
            return 0;
        }

        if (visited[col][row]) {
            return isSelfNegating(content) ? 0 : Ex2Utils.ERR_CYCLE_FORM;
        }

        return calculateDependencyDepth(col, row, visited, content);
    }

    private boolean isValidCell(int col, int row) {
        return isIn(col, row) && table[col][row] != null;
    }

    private boolean isScientificNotation(String content) {
        return SCIENTIFIC_NOTATION_PATTERN.matcher(content).matches();
    }

    private boolean isSelfNegating(String content) {
        return SELF_NEGATING_PATTERN.matcher(content).matches();
    }

    private int calculateDependencyDepth(int col, int row, boolean[][] visited, String content) {
        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(content);
        if (!matcher.find()) {
            return 0;
        }

        visited[col][row] = true;
        int maxDepth = 0;

        try {
            matcher.reset();
            while (matcher.find()) {
                int depth = processReference(matcher.group(), visited);
                if (depth == Ex2Utils.ERR_CYCLE_FORM) {
                    return Ex2Utils.ERR_CYCLE_FORM;
                }
                maxDepth = Math.max(maxDepth, depth);
            }
            return maxDepth + 1;
        } finally {
            visited[col][row] = false;
        }
    }

    private int processReference(String reference, boolean[][] visited) {
        try {
            int[] coords = parseCellReference(reference);
            if (!isValidCell(coords[0], coords[1])) {
                return Ex2Utils.ERR_CYCLE_FORM;
            }

            String dependentData = table[coords[0]][coords[1]].getData();
            if (isEmptyData(dependentData)) {
                return 0;
            }

            return calculateDepth(coords[0], coords[1], visited);
        } catch (Exception e) {
            return Ex2Utils.ERR_CYCLE_FORM;
        }
    }

    @Override
    public String eval(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }

        eval();
        return getEvaluatedCellValue(x, y);
    }

    private boolean isValidPosition(int x, int y) {
        return isIn(x, y);
    }

    private String getEvaluatedCellValue(int x, int y) {
        Cell cell = get(x, y);
        if (!(cell instanceof SCell sCell)) {
            return null;
        }

        String evaluatedValue = sCell.getEvaluatedValue();
        return (evaluatedValue != null && !evaluatedValue.isEmpty()) ?
                evaluatedValue : cell.getData();
    }

    @Override
    public void save(String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writeSheetDimensions(writer);
            writeSheetContent(writer);
        }
    }

    private void writeSheetDimensions(BufferedWriter writer) throws IOException {
        writer.write(width() + "," + height() + "\n");
    }

    private void writeSheetContent(BufferedWriter writer) throws IOException {
        for (int col = 0; col < width(); col++) {
            writeRowContent(writer, col);
        }
    }

    private void writeRowContent(BufferedWriter writer, int col) throws IOException {
        for (int row = 0; row < height(); row++) {
            writeCellContent(writer, col, row);
            if (row < height() - 1) {
                writer.write(",");
            }
        }
        writer.write("\n");
    }

    private void writeCellContent(BufferedWriter writer, int col, int row) throws IOException {
        Cell cell = table[col][row];
        String data = formatCellData(cell);
        writer.write(data);
    }

    private String formatCellData(Cell cell) {
        String data = cell.getData();
        if (isEmptyOrNull(data)) {
            return "EMPTY";
        }
        return data.replace(",", "\\,").replace("\n", "\\n");
    }

    private boolean isEmptyOrNull(String data) {
        return data == null || data.trim().isEmpty();
    }

    @Override
    public void load(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int[] dimensions = readDimensions(reader);
            createNewTable(dimensions[0], dimensions[1]);
            loadTableContent(reader, dimensions[0], dimensions[1]);
            eval();
        }
    }

    private int[] readDimensions(BufferedReader reader) throws IOException {
        String[] dimensions = reader.readLine().split(",");
        return new int[]{
                Integer.parseInt(dimensions[0]),
                Integer.parseInt(dimensions[1])
        };
    }

    private void createNewTable(int width, int height) {
        table = new SCell[width][height];
    }

    private void loadTableContent(BufferedReader reader, int width, int height) throws IOException {
        for (int col = 0; col < width; col++) {
            loadRowContent(reader, col, height);
        }
    }

    private void loadRowContent(BufferedReader reader, int col, int height) throws IOException {
        String[] rowData = reader.readLine().split("(?<!\\\\),");
        for (int row = 0; row < height; row++) {
            createCell(col, row, rowData[row]);
        }
    }

    private void createCell(int col, int row, String data) {
        String cellData = processCellData(data);
        table[col][row] = new SCell(cellData, this, generateCellName(col, row));
    }

    private String processCellData(String data) {
        String processed = data.replace("\\,", ",").replace("\\n", "\n");
        return processed.equals("EMPTY") ? "" : processed;
    }



    private int[] returnXY(String cords) {
        validateCoordinates(cords);
        return parseCoordinates(cords.trim());
    }

    private void validateCoordinates(String cords) {
        if (isEmptyOrNull(cords)) {
            throw new IllegalArgumentException("Invalid coordinates: empty input");
        }

        if (!isValidCoordinateFormat(cords.trim())) {
            throw new IllegalArgumentException("Invalid coordinates format: " + cords);
        }
    }

    private boolean isValidCoordinateFormat(String cords) {
        return cords.matches("[A-Za-z][0-9]+");
    }

    private int[] parseCoordinates(String cords) {
        int colIndex = calculateColumnIndex(cords.charAt(0));
        int rowIndex = Integer.parseInt(cords.substring(1));

        if (!isIn(colIndex, rowIndex)) {
            throw new IllegalArgumentException("Invalid coordinates: " + cords);
        }

        return new int[]{colIndex, rowIndex};
    }

    private int calculateColumnIndex(char colChar) {
        return Character.toUpperCase(colChar) - 'A';
    }
}

