
public class SCell implements Cell {
    private String line; //השורה שמזינים לתא
    private int type; // סוג התא (טקסט , מספר , נוסחה)
    private String value; // ערך התא
    private final Ex2Sheet sheet;
    private final String cellName; // שם התא


    public SCell(String s, Ex2Sheet sheet, String cellName) {
        this.sheet = sheet;
        this.cellName = cellName;
        setData(s); // הגדרת הנתונים ההתחלתיים של התא
        setType(Ex2Utils.TEXT); // קביעת הטיפוס כהתחלתי לטקסט
    }

    public boolean isNumber() {
        String data = getData();
        if (data == null || data.isEmpty()) {
            return false;  //אם הוא Null או ריק
        }
        try {
            Double.parseDouble(data.trim());
            return true;  // אם הפורמט של המבנה נכון
        } catch (NumberFormatException e) {
            return false; // אם הפורמט של המבנה לא נכון
        }
    }

    public boolean isForm() {
        String str = getData();
        return str != null && str.startsWith("="); // בדיקה אם התוכן מתחיל בסימן שווה
    }

    // מחזיר את סימן הפעולה
    private boolean isOperator(char character) {
        return character == '+' || character == '-' || character == '*' || character == '/'; // בדיקה אם התו הוא אופרטור מתמטי
    }

    public boolean isText() {
        return !isNumber() && !isForm();
    } // אני בודק את זה בפונקציה אחרת

    // עושה את פעולת החיבור כפל חיסור חילוק לפי היסמן פעולה שהגיע
    private Double calculate(Double leftOperand, Double rightOperand, char operation) {
        return switch (operation) {
            case '+' -> leftOperand + rightOperand;
            case '-' -> leftOperand - rightOperand;
            case '*' -> leftOperand * rightOperand;
            case '/' -> rightOperand != 0 ? leftOperand / rightOperand : null; // מניעת חלוקה באפס
            default -> null;
        };
    }

    public Double computeForm(String form) {
        if (form == null || form.isEmpty()) {
            return null; // הפורמולה ריקה או לא הותחלה
        }

        // הסר את סימן = אם קיים
        if (form.startsWith("=")) {
            form = form.substring(1).trim();
        }

        form = form.replaceAll("\\s+", ""); // הסרת רווחים מיותרים

        // אם מספר
        try {
            double val = Double.parseDouble(form);
            return val;  // אם הצלחנו להפוך לדאבל זה בטוח מספר
        } catch (NumberFormatException ignored) {
            // אם לא הצלחנו להפוך לדאבל זה בטוח מספר , נמשיך לשאר הבדיקות
        }

        for (int i = 0; i < form.length() - 1; i++) {
            char currentChar = form.charAt(i);
            char nextChar = form.charAt(i + 1);
            if (isOperator(currentChar) && isOperator(nextChar)) { // זיהוי אופרטורים כפולים
                return null;
            }
        }

        while (form.startsWith("(") && form.endsWith(")")) {
            String innerContent = form.substring(1, form.length() - 1);
            if (hasBalancedParentheses(innerContent)) { // בדיקת סוגריים פנימית
                form = innerContent;
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
            try {
                Double.parseDouble(form);
                return Double.parseDouble(form);  // אם זה מספר תקין, נחזיר אותו
            } catch (NumberFormatException e) {
                // אם זה לא מספר, נטפל בו כהפניית תא
                int column = Character.toUpperCase(form.charAt(0)) - 'A';
                int row = Integer.parseInt(form.substring(1));
                String referencedCellValue = sheet.value(column, row); // הפניית ערך התא
                try {
                    return Double.parseDouble(referencedCellValue);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }

        int operatorIdx = -1;
        int parenthesesCounter = 0;
        boolean operatorLocated = false;

        for (int i = form.length() - 1; i >= 0; i--) {
            char character = form.charAt(i);
            if (character == ')') parenthesesCounter++;
            else if (character == '(') parenthesesCounter--;
            else if (parenthesesCounter == 0 && (character == '+' || character == '-')) {
                if (i > 0 && isOperator(form.charAt(i - 1))) {
                    continue;
                }
                operatorIdx = i; // זיהוי האופרטור העיקרי בחישוב
                break;
            } else if (parenthesesCounter == 0 && !operatorLocated && (character == '*' || character == '/')) {
                operatorIdx = i;
            }
        }

        if (operatorIdx == -1) {
            try {
                return Double.parseDouble(form);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String leftExpression = form.substring(0, operatorIdx).trim();
        char operation = form.charAt(operatorIdx);
        String rightExpression = form.substring(operatorIdx + 1).trim();

        if (leftExpression.isEmpty()) {
            if (operation == '-') {
                try {
                    return -Double.parseDouble(rightExpression);
                } catch (NumberFormatException e) {
                    Double rightValue = computeForm(rightExpression);
                    return rightValue != null ? -rightValue : null;
                }
            }
            return computeForm(rightExpression);
        }

        Double leftValue = computeForm(leftExpression);
        Double rightValue = computeForm(rightExpression);

        if (leftValue == null || rightValue == null) {
            return null;
        }

        return calculate(leftValue, rightValue, operation);
    }


    // בודק שהכמות סוגריים סוגרים ופותחים מאוזנת
    private boolean hasBalancedParentheses(String str) {
        int balanceCounter = 0;
        for (char character : str.toCharArray()) {
            if (character == '(') balanceCounter++;
            if (character == ')') balanceCounter--;
            if (balanceCounter < 0) return false; // אם יש יותר סוגרים סוגרים
        }
        return balanceCounter == 0; // וידוא שכל הסוגריים מאוזנים
    }

    @Override
    public String toString() {
        if (cellName != null && !cellName.isEmpty()) {
            return cellName; // הצגת שם התא במידה וקיים
        }

        if (value != null) {
            try {
                double numericValue = Double.parseDouble(value);
                if (Math.abs(numericValue) >= 1e6 || (Math.abs(numericValue) < 1e-6 && numericValue != 0)) {
                    return String.format("%.1e", numericValue);
                }
                return String.format("%.1f", numericValue); // הצגת מספר בפורמט עם ספרה אחרי הנקודה
            } catch (NumberFormatException e) {
                return value;
            }
        }

        String data = getData();
        if (data == null || data.isEmpty()) {
            return ""; // תא ריק
        }

        if (isForm()) {
            Double evaluatedResult = computeForm(data);
            if (evaluatedResult != null) {
                if (Math.abs(evaluatedResult) >= 1e6 || (Math.abs(evaluatedResult) < 1e-6 && evaluatedResult != 0)) {
                    return String.format("%.1e", evaluatedResult);
                }
                return String.format("%.1f", evaluatedResult);
            }
            return value;
        }

        if (isNumber()) {
            try {
                double numericValue = Double.parseDouble(data);
                if (Math.abs(numericValue) >= 1e6 || (Math.abs(numericValue) < 1e-6 && numericValue != 0)) {
                    return String.format("%.1e", numericValue);
                }
                return String.format("%.1f", numericValue);
            } catch (NumberFormatException e) {
                return data;
            }
        }

        return data;
    }

    @Override
    public void setData(String s) {
        line = s; // הגדרת תוכן התא

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
                setType(Ex2Utils.NUMBER); // זיהוי מספר כטיפוס התא
            } catch (NumberFormatException e) {
                setType(Ex2Utils.TEXT);
            }
        }
    }

    @Override
    public String getData() {
        return line; // החזרת תוכן התא
    }

    @Override
    public int getType() {
        return type; // החזרת סוג התא
    }

    @Override
    public void setType(int t) {
        type = t;
    }

    @Override
    public int getOrder() { // מחזיר את העומק
        return 0;
    }

    @Override
    public void setOrder(int t) {
    }

    public void setEvaluatedValue(String value) {
        this.value = value; // הגדרת ערך התא לאחר חישוב
    }

    public String getEvaluatedValue() {
        return value; // החזרת הערך המחושב של התא
    }
}
