import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
public class Ex2SheetTest {

    @Test
    void testText() { // בודק שהטקסט תקין
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "aa");
        sheet.set(1, 0, "4tt");
        sheet.set(2, 0, "{2");
        sheet.set(3, 0, "@123");
        assertEquals("aa", sheet.value(0, 0));
        assertEquals("4tt", sheet.value(1, 0));
        assertEquals("{2", sheet.value(2, 0));
        assertEquals("@123", sheet.value(3, 0));
    }

    @Test
    void testWrongForms() { // בודק שהנוסחא לא רשומה בצורה נכונה
        Sheet sheet = new Ex2Sheet();
        sheet.set(1, 0, "AB");
        sheet.set(2, 0, "@%^2");
        sheet.set(3, 0, "1333.3+)");
        sheet.set(4, 0, "(7+1*2)-");
        sheet.set(5, 0, "=()");
        sheet.set(6, 0, "=7**++");
        sheet.eval();
        assertEquals("AB", sheet.value(1, 0));
        assertEquals("@%^2", sheet.value(2, 0));
        assertEquals("1333.3+)", sheet.value(3, 0));
        assertEquals("(7+1*2)-", sheet.value(4, 0));
        assertEquals("ERR_FORM!", sheet.value(5, 0));
        assertEquals("ERR_FORM!", sheet.value(6, 0));
    }

    @Test
    void testCycle() { // בודק מעגליות
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "=B0+1");
        sheet.set(1, 0, "=C0+2");
        sheet.set(2, 0, "=D0+3");
        sheet.set(3, 0, "=A0+4");
        int[][] depths = sheet.depth();
        for (int i = 0; i < 4; i++) {
            assertEquals(-1, depths[i][0]); // מחזיר טעות
        }
    }

    @Test
    void testDivide0() { // בודק שמחזיר טעות עבור חילוק באפס
        Sheet sheet = new Ex2Sheet();
        sheet.set(5, 1, "=22/0");
        sheet.set(1, 7, "=22/(14+4-18)");
        sheet.eval();
        assertEquals("ERR_FORM!", sheet.value(5, 1));
        assertEquals("ERR_FORM!", sheet.value(1, 7));
    }

    @Test
    void testDepth() { //מחשב את העומק של התא
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 1, "=7+0");
        sheet.set(1, 1, "=13");
        sheet.set(2, 0, "=A1+B1");
        sheet.set(3, 0, "=C0/2");
        sheet.set(4, 0, "=D0+A0");
        int[][] depths = sheet.depth();
        assertEquals(0, depths[0][0]);
        assertEquals(0, depths[1][0]);
        assertEquals(1, depths[2][0]);
        assertEquals(2, depths[3][0]);
        assertEquals(3, depths[4][0]);
    }

    @Test
    void testForms() { // מחשב את התוצאה של הנוסחאות
        Sheet sheet = new Ex2Sheet();
        sheet.set(5, 0, "=10*2");
        sheet.set(1, 0, "=10*2");
        sheet.set(3, 0, "=20+7");
        sheet.set(0, 0, "=((1+2)*(3+4))/(2+3)"); // A0
        sheet.set(1, 1, "=1e5");       // B0
        sheet.set(7, 5, "=(((3)))");    // A0
        sheet.set(3, 4, "=(1+(2+(14)))"); // C0
        sheet.eval();
        assertEquals("20.0", sheet.value(5, 0));
        assertEquals("20.0", sheet.value(1, 0));
        assertEquals("27.0", sheet.value(3, 0));
        assertEquals("4.2", sheet.value(0, 0));
        assertEquals("100000.0", sheet.value(1, 1));
        assertEquals("3.0", sheet.value(7, 5));
        assertEquals("17.0", sheet.value(3, 4));
    }

    @Test
    void testSaveLoadComplex() throws IOException { // בודק את השמירה והטעינה של הקבצים
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "5");
        sheet.set(1, 0, "=A0+2");
        sheet.set(2, 0, "=B0*2");
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
    void testOperationsCells() { // בודק סימני פעולה בין תאים
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 0, "17");
        sheet.set(4, 1, "=A0*2");
        sheet.set(2, 2, "=(E1+A0)/2");
        sheet.eval();
        assertEquals("17.0", sheet.value(0, 0));
        assertEquals("34.0", sheet.value(4, 1));
        assertEquals("25.5", sheet.value(2, 2));
    }


    @Test
    void testEvalAll() { // בודק שהeval לכל התאים עובד
        Sheet sheet = new Ex2Sheet();
        sheet.set(0, 1, "=3-1");
        sheet.eval();
        assertEquals("2.0", sheet.value(0, 1));
        sheet.set(0, 1, "=7*3");
        sheet.eval();
        assertEquals("21.0", sheet.value(0, 1));
        sheet.set(0, 1, "=7-3");
        sheet.eval();
        assertEquals("4.0", sheet.value(0, 1));
        sheet.set(0, 1, "hye");
        sheet.eval();
        assertEquals("hye", sheet.value(0, 1));
    }


    @Test
    public void testycle() { // בודק מעגליות
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 0, "=B0");
        sheet.set(1, 0, "=A0");
        sheet.set(0, 1, "=B1");
        sheet.set(1, 1, "=C1");
        sheet.set(2, 1, "=D1");
        sheet.set(3, 1, "=A1");
        sheet.set(0, 2, "=A2");
        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
        assertEquals("ERR_CYCLE!", sheet.value(0, 1));
        assertEquals("ERR_CYCLE!", sheet.value(1, 1));
        assertEquals("ERR_CYCLE!", sheet.value(2, 1));
        assertEquals("ERR_CYCLE!", sheet.value(0, 2));
    }

    @Test
    public void testCycleWithOperations() { // בודק מעגליות עם סימני פעולה בין התאים
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        sheet.set(0, 1, "=17-B0*2");
        sheet.set(1, 0, "=A1/7");
        sheet.eval();
        assertEquals("ERR_CYCLE!", sheet.value(0, 1));
        assertEquals("ERR_CYCLE!", sheet.value(1, 0));
    }

    @Test
    public void testEmpty() { // בודק שהתאים באמת ריקים
        Ex2Sheet sheet = new Ex2Sheet(3, 3);
        int[][] result = sheet.depth();
        assertEquals(0, result[0][0]);
        assertEquals(0, result[1][1]);
        assertEquals(0, result[2][2]);
    }

    @Test
    public void testTypes() { // בודק את הסוג של התאים
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

}