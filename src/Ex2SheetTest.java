import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Ex2SheetTest {

    @Test
    void testTextCells() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "Hello");      // A0
        sheet.set(1, 0, "2a");         // B0
        sheet.set(2, 0, "{2}");        // C0
        sheet.set(3, 0, "@123");       // D0

        assertEquals("Hello", sheet.value(0, 0));
        assertEquals("2a", sheet.value(1, 0));
        assertEquals("{2}", sheet.value(2, 0));
        assertEquals("@123", sheet.value(3, 0));
    }

    @Test
    void testValidFormulas() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=1");             // A0
        sheet.set(1, 0, "=1.2");           // B0
        sheet.set(2, 0, "=(0.2)");         // C0
        sheet.set(3, 0, "=1+2");           // D0
        sheet.set(4, 0, "=1+2*3");         // E0
        sheet.set(5, 0, "=(1+2)*((3))-1"); // F0
        sheet.eval();

        assertEquals("1.0", sheet.value(0, 0));
        assertEquals("1.2", sheet.value(1, 0));
        assertEquals("0.2", sheet.value(2, 0));
        assertEquals("3.0", sheet.value(3, 0));
        assertEquals("7.0", sheet.value(4, 0));
        assertEquals("8.0", sheet.value(5, 0));
    }

    @Test
    void testInvalidFormulaTypes() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "a");          // A0
        sheet.set(1, 0, "AB");         // B0
        sheet.set(2, 0, "@2");         // C0
        sheet.set(3, 0, "2+)");        // D0
        sheet.set(4, 0, "(3+1*2)-");   // E0
        sheet.set(5, 0, "=()");        // F0
        sheet.set(6, 0, "=5**");       // G0
        sheet.eval();

        assertEquals("a", sheet.value(0, 0));
        assertEquals("AB", sheet.value(1, 0));
        assertEquals("@2", sheet.value(2, 0));
        assertEquals("2+)", sheet.value(3, 0));
        assertEquals("(3+1*2)-", sheet.value(4, 0));
        assertEquals("ERR_FORM!", sheet.value(5, 0));
        assertEquals("ERR_FORM!", sheet.value(6, 0));
    }

    @Test
    void testComplexCyclicReferences() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=B0+1");      // A0
        sheet.set(1, 0, "=C0+2");      // B0
        sheet.set(2, 0, "=D0+3");      // C0
        sheet.set(3, 0, "=A0+4");      // D0

        int[][] depths = sheet.depth();
        for (int i = 0; i < 4; i++) {
            assertEquals(-1, depths[i][0]);
        }
    }

    @Test
    void testComplexDepthCalculation() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "5");          // A0: depth 0
        sheet.set(1, 0, "3");          // B0: depth 0
        sheet.set(2, 0, "=A0+B0");     // C0: depth 1
        sheet.set(3, 0, "=C0*2");      // D0: depth 2
        sheet.set(4, 0, "=D0+A0");     // E0: depth 3
        sheet.set(0, 1, "=E0/2");      // A1: depth 4

        int[][] depths = sheet.depth();
        assertEquals(0, depths[0][0]); // A0
        assertEquals(0, depths[1][0]); // B0
        assertEquals(1, depths[2][0]); // C0
        assertEquals(2, depths[3][0]); // D0
        assertEquals(3, depths[4][0]); // E0
        assertEquals(4, depths[0][1]); // A1
    }

    @Test
    void testMathOperations() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=10/2");      // A0
        sheet.set(1, 0, "=10*2");      // B0
        sheet.set(2, 0, "=10+2");      // C0
        sheet.set(3, 0, "=10-2");      // D0
        sheet.eval();

        assertEquals("5.0", sheet.value(0, 0));
        assertEquals("20.0", sheet.value(1, 0));
        assertEquals("12.0", sheet.value(2, 0));
        assertEquals("8.0", sheet.value(3, 0));
    }

    @Test
    void testDivisionByZero() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=1/0");       // A0
        sheet.set(1, 0, "=10/(5-5)");  // B0
        sheet.eval();

        assertEquals("ERR_FORM!", sheet.value(0, 0));
        assertEquals("ERR_FORM!", sheet.value(1, 0));
    }

    @Test
    void testLargeFormulas() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=((1+2)*(3+4))/(2+3)"); // A0
        sheet.eval();
        assertEquals("4.2", sheet.value(0, 0));
    }

    @Test
    void testSaveLoadComplex() throws IOException {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "5");          // A0
        sheet.set(1, 0, "=A0+2");      // B0
        sheet.set(2, 0, "=B0*2");      // C0
        sheet.set(3, 0, "Some Text");  // D0
        sheet.eval();

        String tempFile = "test_complex_sheet.csv";
        sheet.save(tempFile);

        Sheet loadedSheet = new Ex2Sheet();
        loadedSheet.load(tempFile);
        loadedSheet.eval();

        assertEquals(sheet.value(0, 0), loadedSheet.value(0, 0));
        assertEquals(sheet.value(1, 0), loadedSheet.value(1, 0));
        assertEquals(sheet.value(2, 0), loadedSheet.value(2, 0));
        assertEquals(sheet.value(3, 0), loadedSheet.value(3, 0));

        assertArrayEquals(sheet.depth(), loadedSheet.depth());

        //noinspection ResultOfMethodCallIgnored
        new File(tempFile).delete();
    }

    @Test
    void testNestedCellReferences() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "5");          // A0
        sheet.set(0, 1, "=A0");        // A1
        sheet.set(0, 2, "=A1");        // A2
        sheet.set(0, 3, "=A2+A0");     // A3
        sheet.eval();

        assertEquals("5.0", sheet.value(0, 0));
        assertEquals("5.0", sheet.value(0, 1));
        assertEquals("5.0", sheet.value(0, 2));
        assertEquals("10.0", sheet.value(0, 3));
    }

    @Test
    void testMixedOperationsWithCellReferences() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "10");         // A0
        sheet.set(1, 0, "=A0*2");      // B0
        sheet.set(2, 0, "=(B0+A0)/2"); // C0
        sheet.eval();

        assertEquals("10.0", sheet.value(0, 0));
        assertEquals("20.0", sheet.value(1, 0));
        assertEquals("15.0", sheet.value(2, 0));
    }

    @Test
    void testEdgeCaseMathOperations() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=0.1+0.2");   // A0
        sheet.set(1, 0, "=1e5");       // B0
        sheet.eval();

        assertEquals("0.3", sheet.value(0, 0));
        assertEquals("100000.0", sheet.value(1, 0));
    }

    @Test
    void testMultipleUpdates() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=1+1");
        sheet.eval();
        assertEquals("2.0", sheet.value(0, 0));

        sheet.set(0, 0, "=2+2");
        sheet.eval();
        assertEquals("4.0", sheet.value(0, 0));

        sheet.set(0, 0, "Not a formula");
        sheet.eval();
        assertEquals("Not a formula", sheet.value(0, 0));
    }

    @Test
    void testComplexParentheses() {
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=(((1)))");    // A0
        sheet.set(1, 0, "=((2)+(3))");  // B0
        sheet.set(2, 0, "=(1+(2+(3)))"); // C0
        sheet.eval();

        assertEquals("1.0", sheet.value(0, 0));
        assertEquals("5.0", sheet.value(1, 0));
        assertEquals("6.0", sheet.value(2, 0));
    }

    @Test
    public void testSimpleCircularDependency() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=B0");      // A0 depends on B0
        sheet.set(1, 0, "=A0");      // B0 depends on A0

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
    }

    @Test
    public void testComplexCircularDependency() {
        Ex2Sheet sheet = new Ex2Sheet(4, 4);
        sheet.set(0, 0, "=B0");      // A0 -> B0
        sheet.set(1, 0, "=C0");      // B0 -> C0
        sheet.set(2, 0, "=D0");      // C0 -> D0
        sheet.set(3, 0, "=A0");      // D0 -> A0 (creates cycle)

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
        assertEquals("ERR_CYCLE!", sheet.value(2, 0));
        assertEquals("ERR_CYCLE!", sheet.value(3, 0));
    }

    @Test
    public void testSelfCircularDependency() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=A0");      // Cell depends on itself

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
    }

    @Test
    public void testCircularDependencyWithCalculations() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=B0+1");    // A0 depends on B0
        sheet.set(1, 0, "=A0*2");    // B0 depends on A0

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
    }

    @Test
    public void testCircularDependencyInLargerFormula() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=5+B0*2");  // A0 depends on B0
        sheet.set(1, 0, "=A0/2");    // B0 depends on A0

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
    }

    @Test
    public void testPartialCircularDependency() {
        Ex2Sheet sheet = new Ex2Sheet(4, 4);
        sheet.set(0, 0, "=B0");      // A0 -> B0
        sheet.set(1, 0, "=C0");      // B0 -> C0
        sheet.set(2, 0, "=A0");      // C0 -> A0 (creates cycle)
        sheet.set(3, 0, "=A0+1");    // D0 depends on cell in cycle

        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
        assertEquals("ERR_CYCLE!", sheet.value(2, 0));
        assertEquals("ERR_CYCLE!", sheet.value(3, 0));
    }

    private void verifyArrayEquality(String message, int[][] expected, int[][] actual) {
        if (!Arrays.deepEquals(expected, actual)) {
            throw new AssertionError(message + "\nExpected: " + Arrays.deepToString(expected) + "\nActual: " + Arrays.deepToString(actual));
        }
    }

    @Test
    public void testEmptyCellDependency() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=B0+1");    // A0 depends on B0 (empty)

        int[][] result = sheet.depth();

        assertEquals(-1, result[0][0]); // A0 has invalid depth due to dependency on an empty cell
        assertEquals(0, result[0][1]); // B0 is empty, so depth is 0
    }

    @Test
    public void testLongDependencyChain() {
        Ex2Sheet sheet = new Ex2Sheet(5, 5);
        sheet.set(0, 0, "1");        // A0
        sheet.set(1, 0, "=A0+1");    // B0
        sheet.set(2, 0, "=B0+2");    // C0
        sheet.set(3, 0, "=C0+3");    // D0
        sheet.set(4, 0, "=D0+4");    // E0

        int[][] result = sheet.depth();

        assertEquals(0, result[0][0]);
        assertEquals(1, result[1][0]);
        assertEquals(2, result[2][0]);
        assertEquals(3, result[3][0]);
        assertEquals(4, result[4][0]);
    }

    @Test
    public void testBlankSheetDepth() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        int[][] result = sheet.depth();

        assertEquals(0, result[0][0]);
        assertEquals(0, result[1][1]);
        assertEquals(0, result[2][2]);
    }

    @Test
    public void testMixedFormulaTypes() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "10");
        sheet.set(0, 1, "=A0+5");
        sheet.set(1, 0, "=10+20");
        sheet.set(1, 1, "15");

        int[][] result = sheet.depth();

        assertEquals(0, result[0][0]);
        assertEquals(1, result[0][1]);
        assertEquals(0, result[1][0]);
        assertEquals(0, result[1][1]);
    }

    @Test
    public void testConstantFormula() {
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "5");
        sheet.set(0, 1, "=5*2");

        int[][] result = sheet.depth();

        assertEquals(0, result[0][0]);
        assertEquals(0, result[0][1]);
    }
}