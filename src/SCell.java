/**
 * Represents a cell in a spreadsheet that can contain numbers, text, or formulas.
 * Implements the Cell interface to provide basic cell functionality.
 */
public class SCell implements Cell {
    private String line;
    private int type;
    private String evaluatedValue;
    private final Ex2Sheet sheet;
    private final String cellName;

    /**
     * Creates a new cell with initial value, parent sheet and cell name.
     * @param s Initial cell content
     * @param sheet Parent spreadsheet
     * @param cellName The cell's reference name (e.g., "A0")
     */
    public SCell(String s, Ex2Sheet sheet, String cellName) {
        this.sheet = sheet;
        this.cellName = cellName;
        setData(s);
        setType(Ex2Utils.TEXT);
    }

    /**
     * Checks if the cell's content represents a valid number
     * @return true if the cell contains a valid number, false otherwise
     */
    public boolean isNumber() {
        String data = getData();
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(data.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the cell's content represents a String
     * @return true if the cell contains a String which isn't a formula and not a number, false otherwise
     */
    public boolean isText() {
        return !isNumber() && !isForm();
    }

    /**
     * Checks if the cell contains a formula (starts with '=')
     * @return true if the cell contains a formula, false otherwise
     */

    public boolean isForm() {
        String data = getData();
        return data != null && data.startsWith("=");
    }
    /**
     * Validates if a character is a valid mathematical operator
     * @param c Character to check
     * @return true if the character is a valid operator, false otherwise
     */

    private boolean isValidOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    /**
     * Checks for balanced parentheses in a formula string
     * @param str The string to check
     * @return true if parentheses are balanced, false otherwise
     */
    private boolean isBalancedParentheses(String str) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == '(') count++;
            if (c == ')') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }

    /**
     * Performs the specified mathematical operation
     * @param left Left operand
     * @param right Right operand
     * @param operator Mathematical operator
     * @return Result of the operation, or null if division by zero
     */
    private Double performOperation(Double left, Double right, char operator) {
        return switch (operator) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '*' -> left * right;
            case '/' -> right != 0 ? left / right : null;
            default -> null;
        };
    }

    /**
     * Computes the result of a formula
     * @param form The formula to evaluate
     * @return The computed result or null if invalid
     */
    public Double computeForm(String form) {
        if (form == null || form.isEmpty()) {
            return null;
        }

        // הסר את סימן ה-= אם קיים
        if (form.startsWith("=")) {
            form = form.substring(1).trim();
        }

        form = form.replaceAll("\\s+", "");

        // נסיון ישיר לפרסר כמספר, לפני כל בדיקה אחרת!
        try {
            double value = Double.parseDouble(form);
            return value;  // אם הצלחנו לפרסר, זה בטוח מספר (כולל פורמט מדעי)
        } catch (NumberFormatException ignored) {
            // אם לא הצלחנו לפרסר כמספר, נמשיך לשאר הבדיקות
        }

        // Check for invalid double operators
        for (int i = 0; i < form.length() - 1; i++) {
            char current = form.charAt(i);
            char next = form.charAt(i + 1);
            if (isValidOperator(current) && isValidOperator(next)) {
                return null;
            }
        }

        // Handle parentheses
        while (form.startsWith("(") && form.endsWith(")")) {
            String inner = form.substring(1, form.length() - 1);
            if (isBalancedParentheses(inner)) {
                form = inner;
                try {
                    return Double.parseDouble(form);
                } catch (NumberFormatException ignored) {
                }
            } else {
                break;
            }
        }

        // בדיקת הפניית תא - רק אם זה בדיוק אות אחת ואחריה מספרים
        if (form.matches("^[A-Za-z][0-9]+$") && !form.matches(".*\\d+[eE][-+]?\\d+")) {
            // וידוא נוסף שזה לא מספר בפורמט מדעי
            try {
                Double.parseDouble(form);
                return Double.parseDouble(form);  // אם זה מספר תקין, נחזיר אותו
            } catch (NumberFormatException e) {
                // אם זה לא מספר, נטפל בו כהפניית תא
                int col = Character.toUpperCase(form.charAt(0)) - 'A';
                int row = Integer.parseInt(form.substring(1));
                String cellValue = sheet.value(col, row);
                try {
                    return Double.parseDouble(cellValue);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }


        int operatorIndex = -1;
        int parenthesesCount = 0;
        boolean foundOperator = false;

        for (int i = form.length() - 1; i >= 0; i--) {
            char c = form.charAt(i);
            if (c == ')') parenthesesCount++;
            else if (c == '(') parenthesesCount--;
            else if (parenthesesCount == 0 && (c == '+' || c == '-')) {
                if (i > 0 && isValidOperator(form.charAt(i - 1))) {
                    continue;
                }
                operatorIndex = i;
                break;
            } else if (parenthesesCount == 0 && !foundOperator && (c == '*' || c == '/')) {
                operatorIndex = i;
            }
        }

        if (operatorIndex == -1) {
            try {
                return Double.parseDouble(form);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String leftPart = form.substring(0, operatorIndex).trim();
        char operator = form.charAt(operatorIndex);
        String rightPart = form.substring(operatorIndex + 1).trim();

        if (leftPart.isEmpty()) {
            if (operator == '-') {
                try {
                    return -Double.parseDouble(rightPart);
                } catch (NumberFormatException e) {
                    Double rightValue = computeForm(rightPart);
                    return rightValue != null ? -rightValue : null;
                }
            }
            return computeForm(rightPart);
        }

        Double leftValue = computeForm(leftPart);
        Double rightValue = computeForm(rightPart);

        if (leftValue == null || rightValue == null) {
            return null;
        }

        return performOperation(leftValue, rightValue, operator);
    }

    /**
     * Returns a string representation of the cell
     * @return The cell's name or evaluated value
     */
    @Override
    public String toString() {
        if (cellName != null && !cellName.isEmpty()) {
            return cellName;
        }

        if (evaluatedValue != null) {
            try {
                double val = Double.parseDouble(evaluatedValue);
                if (Math.abs(val) >= 1e6 || (Math.abs(val) < 1e-6 && val != 0)) {
                    return String.format("%.1e", val);
                }
                return String.format("%.1f", val);
            } catch (NumberFormatException e) {
                return evaluatedValue;
            }
        }

        String data = getData();
        if (data == null || data.isEmpty()) {
            return "";
        }

        if (isForm()) {
            Double result = computeForm(data);
            if (result != null) {
                if (Math.abs(result) >= 1e6 || (Math.abs(result) < 1e-6 && result != 0)) {
                    return String.format("%.1e", result);
                }
                return String.format("%.1f", result);
            }
            return evaluatedValue;
        }

        if (isNumber()) {
            try {
                double val = Double.parseDouble(data);
                if (Math.abs(val) >= 1e6 || (Math.abs(val) < 1e-6 && val != 0)) {
                    return String.format("%.1e", val);
                }
                return String.format("%.1f", val);
            } catch (NumberFormatException e) {
                return data;
            }
        }

        return data;
    }

    @Override
    public void setData(String s) {
        line = s;

        // קביעת הטיפוס המתאים
        if (s == null || s.trim().isEmpty()) {
            setType(Ex2Utils.TEXT);
        }
        else if (s.startsWith("=")) {
            setType(Ex2Utils.FORM);
        }
        else {
            try {
                Double.parseDouble(s);
                setType(Ex2Utils.NUMBER);
            } catch (NumberFormatException e) {
                setType(Ex2Utils.TEXT);
            }
        }
    }


    @Override
    public String getData() {
        return line;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int t) {
        type = t;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void setOrder(int t) {
    }

    /**
     * Sets the evaluated value of the cell after formula computation
     * @param value The computed value to set
     */
    public void setEvaluatedValue(String value) {
        this.evaluatedValue = value;
    }

    /**
     * Gets the cell's evaluated value after formula computation
     * @return The evaluated value of the cell
     */
    public String getEvaluatedValue() {
        return evaluatedValue;
    }
}