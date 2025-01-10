import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Ex2Sheet implements Sheet {
    private Cell[][] table;

    public Ex2Sheet(int x, int y) {
        table = new SCell[x][y];
        initializeTable(x, y); // אתחול הטבלה בערכים ריקים כברירת מחדל
        eval();
    }

    public Ex2Sheet() {
        this(Ex2Utils.WIDTH, Ex2Utils.HEIGHT); // בנייה של טבלה בגודל ברירת המחדל
    }

    private void initializeTable(int cols, int rows) {
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                table[col][row] = new SCell(Ex2Utils.EMPTY_CELL, this, generateCellName(col, row)); // אתחול תא ריק עם שם התואם למיקומו
            }
        }
    }

    private String generateCellName(int colIndex, int rowIndex) {
        return String.valueOf((char)('A' + colIndex)) + rowIndex; // חישוב שם התא לפי מיקום עמודה ושורה
    }

    @Override
    public String value(int x, int y) {
        if (!isIn(x, y)) {
            return Ex2Utils.EMPTY_CELL; // החזרת ערך ריק אם התא מחוץ לטווח
        }

        Cell cell = table[x][y];
        if (cell instanceof SCell sCell) {
            String evalValue = sCell.getEvaluatedValue(); // בדיקה אם התא כבר קיבל ערך
            if (evalValue != null) {
                return evalValue;
            }
            evaluateCell(x, y); // אם התא לא קיבל ערך עדיין
            return sCell.getEvaluatedValue();
        }
        return "";
    }

    @Override
    public Cell get(int x, int y) {
        return isIn(x, y) ? table[x][y] : null; // החזרת תא עבור מיקום חוקי
    }

    @Override
    public Cell get(String cords) {
        try {
            int[] coordinates = returnXY(cords); // ערכי איקס וואי עבור השם של התא
            return get(coordinates[0], coordinates[1]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int width() {
        return table.length; // רוחב הטבלה
    }

    @Override
    public int height() {
        return table[0].length; // גובה הטבלה
    }

    @Override
    public void set(int col, int row, String val) {
        if (!isIn(col, row)) return;

        String value = (val == null || val.trim().isEmpty()) ? Ex2Utils.EMPTY_CELL : val;
        table[col][row] = new SCell(value, this, generateCellName(col, row)); // הגדרת תא
    }

    @Override
    public void eval() {
        int[][] depths = depth(); // חישוב עומק התלות של כל תא

        // איפוס הערכים שהוערכו כבר
        for (int col = 0; col < width(); col++) {
            for (int row = 0; row < height(); row++) {
                if (table[col][row] instanceof SCell) {
                    ((SCell) table[col][row]).setEvaluatedValue(null);
                }
            }
        }

        // הערכת תאים לפי העומק
        for (int depth = 0; depth <= findMaxDepth(depths); depth++) {
            for (int col = 0; col < width(); col++) {
                for (int row = 0; row < height(); row++) {
                    if (depths[col][row] == depth) {
                        evaluateCell(col, row);
                    }
                }
            }
        }
    }

    private int findMaxDepth(int[][] depths) {
        int maxDepth = 0; // משתנה עבור הערך המקסימלי של התא
        for (int[] rowDepths : depths) {
            for (int depth : rowDepths) {
                maxDepth = Math.max(maxDepth, depth); // מציאת עומק מקסימלי של התלות
            }
        }
        return maxDepth;
    }

    private void evaluateCell(int col, int row) {
        if (!isIn(col, row)) return;

        Cell cell = table[col][row];
        if (!(cell instanceof SCell sCell)) return;

        String data = sCell.getData();

        // תא ריק
        if (data == null || data.trim().isEmpty()) {
            sCell.setEvaluatedValue("");
            return;
        }

        // נוסחה
        if (data.startsWith("=")) {
            // בדיקת תלות מעגלית
            int depth = calculateDepth(col, row, new boolean[width()][height()]);
            if (depth == Ex2Utils.ERR_CYCLE_FORM) {
                sCell.setType(Ex2Utils.ERR_CYCLE_FORM);  // עדכון הטיפוס
                sCell.setEvaluatedValue(Ex2Utils.ERR_CYCLE);
                return;
            }

            Double result = sCell.computeForm(data); // חישוב הנוסחה
            if (result != null) {
                sCell.setType(Ex2Utils.FORM);  // נוסחה תקינה
                sCell.setEvaluatedValue(String.format("%.1f", result));
            } else {
                sCell.setType(Ex2Utils.ERR_FORM_FORMAT);  // שגיאת פורמט
                sCell.setEvaluatedValue(Ex2Utils.ERR_FORM);
            }
            return;
        }

        // מספר
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

        // טקסט
        sCell.setType(Ex2Utils.TEXT);
        sCell.setEvaluatedValue(data);
    }

    @Override
    public boolean isIn(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height(); // בדיקה אם הקואורדינטות תקינות
    }

    @Override
    public int[][] depth() {
        int[][] depths = new int[width()][height()];
        boolean[][] visitedCells = new boolean[width()][height()];

        for (int col = 0; col < width(); col++) {
            for (int row = 0; row < height(); row++) {
                if (!visitedCells[col][row]) {
                    depths[col][row] = calculateDepth(col, row, new boolean[width()][height()]); // חישוב עומק התא
                }
            }
        }
        return depths;
    }

    private int calculateDepth(int col, int row, boolean[][] visited) {
        if (!isIn(col, row)) {
            return Ex2Utils.ERR_CYCLE_FORM; // מעגלי
        }

        Cell cell = table[col][row];
        if (cell == null) {
            return Ex2Utils.ERR_CYCLE_FORM; // מעגלי
        }

        String data = cell.getData();
        if (data == null || data.trim().isEmpty()) {
            return 0; // התא ריק או מאותחל לNull
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
        if (visited[col][row]) {
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

        visited[col][row] = true;  // מסמן את התא כמבוקר
        matcher.reset();
        int maxDepth = 0;

        try {
            while (matcher.find()) {
                String ref = matcher.group();
                int depCol = Character.toUpperCase(ref.charAt(0)) - 'A';
                int depRow = Integer.parseInt(ref.substring(1));

                // בדיקת תקינות התא המאוזכר
                if (!isIn(depCol, depRow)) {
                    return Ex2Utils.ERR_CYCLE_FORM; // מעגלי
                }

                Cell dependentCell = table[depCol][depRow];
                if (dependentCell == null) {
                    return Ex2Utils.ERR_CYCLE_FORM; // מעגלי
                }

                // אם התא ריק, נחשיב אותו כ-0
                String dependentData = dependentCell.getData();
                if (dependentData == null || dependentData.trim().isEmpty()) {
                    continue;
                }

                int depth = calculateDepth(depCol, depRow, visited);
                if (depth == Ex2Utils.ERR_CYCLE_FORM) {
                    return Ex2Utils.ERR_CYCLE_FORM; // מעגלי
                }
                maxDepth = Math.max(maxDepth, depth);
            }

            return maxDepth + 1; // מגדיל את העומק ב-1
        } finally {
            visited[col][row] = false;  // תמיד נשחרר את הסימון בסוף
        }
    }

    @Override
    public String eval(int x, int y) {
        if (!isIn(x, y)) {
            return null;
        }

        eval();

        Cell cell = get(x, y);
        if (cell instanceof SCell) {
            String evalValue = ((SCell) cell).getEvaluatedValue();
            return (evalValue != null && !evalValue.isEmpty()) ? evalValue : cell.getData();
        }

        return null;
    }

    @Override
    public void save(String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(width() + "," + height() + "\n"); // כתיבת ממדי הטבלה

            for (int col = 0; col < width(); col++) {
                for (int row = 0; row < height(); row++) {
                    Cell cell = table[col][row];
                    String data = (cell.getData() == null || cell.getData().trim().isEmpty()) ? "EMPTY" : cell.getData();
                    writer.write(data.replace(",", "\\,").replace("\n", "\\n"));

                    if (row < height() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
        }
    }

    @Override
    public void load(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String[] dimensions = reader.readLine().split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);

            table = new SCell[width][height]; // יצירת טבלה בגודל הנדרש

            for (int col = 0; col < width; col++) {
                String[] rowData = reader.readLine().split("(?<!\\\\),");
                for (int row = 0; row < height; row++) {
                    String cellData = rowData[row].replace("\\,", ",").replace("\\n", "\n");
                    table[col][row] = new SCell(cellData.equals("EMPTY") ? "" : cellData, this, generateCellName(col, row));
                }
            }
            eval();
        }
    }

    private int[] returnXY(String cords) {
        if (cords == null || cords.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid coordinates: empty input");
        }

        cords = cords.trim(); // הסרת רווחים מיותרים מהקלט
        if (!cords.matches("[A-Za-z][0-9]+")) { // בדיקה אם הקלט תואם פורמט קואורדינטות
            throw new IllegalArgumentException("Invalid coordinates format: " + cords);
        }

        int colIndex = Character.toUpperCase(cords.charAt(0)) - 'A';
        int rowIndex = Integer.parseInt(cords.substring(1));

        if (!isIn(colIndex, rowIndex)) {
            throw new IllegalArgumentException("Invalid coordinates: " + cords);
        }

        return new int[] { colIndex, rowIndex };
    }
}
