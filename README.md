Spreadsheet Simulation
This project implements a simple spreadsheet system in Java, supporting text, numbers, and formulas. It provides functionality to create a table of cells, evaluate formulas, and load/save the sheet to/from a file. The spreadsheet also calculates dependency depths to handle cell evaluations in the correct order, taking into account references to other cells.

Files
1. Ex2Sheet.java
This file contains the implementation of the Ex2Sheet class, which represents the spreadsheet itself. It includes methods for:

Creating and initializing a new spreadsheet with default or specified dimensions.
Storing cell data and evaluating it based on dependencies.
Handling various cell types (text, number, formula).
Saving and loading the spreadsheet from a file.
Key Methods:

value(int x, int y): Returns the evaluated value of a cell.
set(int col, int row, String val): Sets the value of a cell.
eval(): Evaluates all cells based on dependencies.
depth(): Calculates the dependency depth of each cell.
save(String fileName): Saves the spreadsheet to a file.
load(String fileName): Loads a spreadsheet from a file.
2. CellEntry.java
This file contains the CellEntry class, which represents a cell’s coordinates and provides conversion between spreadsheet notation (e.g., "A0") and array indices.

Key Methods:

toString(): Converts the coordinates to a string representation (e.g., "A0").
isValid(): Checks if the coordinates are valid.
getX() and getY(): Return the column and row indices, respectively.
Dependencies
Java 8 or higher: The project uses features introduced in Java 8 (such as lambda expressions and streams).
Usage
Create a new spreadsheet:

java
Copy code
Ex2Sheet sheet = new Ex2Sheet(10, 10); // creates a 10x10 sheet
Set cell values:

java
Copy code
sheet.set(0, 0, "5");
sheet.set(1, 0, "=A0+3");
Get evaluated value:

java
Copy code
String result = sheet.value(1, 0); // Retrieves the evaluated value of cell B0
Save and load from file:

java
Copy code
sheet.save("sheet.txt");
sheet.load("sheet.txt");
Example
For a spreadsheet with 3 rows and 3 columns:

Set A0 = 5, B0 = "=A0+3", and C0 = "=B0*2".
The cell C0 will contain 16 after evaluation.
