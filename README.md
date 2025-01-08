# Ex2Sheet - Spreadsheet Implementation

## Overview
`Ex2Sheet` is a Java implementation of a spreadsheet with support for text, numbers, and formulas. It provides basic functionality for:
- Storing and evaluating cells.
- Handling different data types (text, numbers, formulas).
- Saving and loading spreadsheets to/from a file.

## Features
- **Cell Evaluation:** Handles the evaluation of text, numbers, and formulas.
- **Dependencies:** Supports cell dependencies and ensures there are no circular references.
- **File I/O:** Allows loading and saving spreadsheets in a text-based format.
- **Spreadsheet Dimensions:** Supports custom width and height for spreadsheets.

## Classes
- **Ex2Sheet:** The main class representing the spreadsheet.
- **SCell:** Represents a single cell in the spreadsheet (text, number, or formula).
- **CellEntry:** Represents a cell's coordinates and provides conversion between spreadsheet notation (e.g., "A0") and array indices.
- **Sheet:** Interface defining the methods for a spreadsheet.

## Methods

### `Ex2Sheet(int x, int y)`
Constructor that initializes the spreadsheet with the specified dimensions (width `x` and height `y`).

### `Ex2Sheet()`
Constructor that initializes the spreadsheet with default dimensions.

### `void set(int col, int row, String val)`
Sets the value of the cell at the given column (`col`) and row (`row`).

### `String value(int x, int y)`
Returns the evaluated value of the cell at the specified coordinates (`x`, `y`).

### `Cell get(int x, int y)`
Returns the cell at the specified coordinates (`x`, `y`).

### `int width()`
Returns the width (number of columns) of the spreadsheet.

### `int height()`
Returns the height (number of rows) of the spreadsheet.

### `void eval()`
Evaluates all cells in the spreadsheet based on their dependencies.

### `boolean isIn(int xx, int yy)`
Checks if the specified coordinates are within the bounds of the spreadsheet.

### `int[][] depth()`
Calculates the dependency depths for all cells in the spreadsheet.

### `String eval(int x, int y)`
Evaluates a specific cell and returns its value.

### `void save(String fileName) throws IOException`
Saves the spreadsheet to a file with the given filename.

### `void load(String fileName) throws IOException`
Loads the spreadsheet from a file with the given filename.

### `private int[] parseCoordinates(String cords)`
Parses cell coordinates from string format (e.g., "A0") and returns the corresponding column and row indices.

### Picture of the project 


![image](https://github.com/user-attachments/assets/3a6d39b6-7694-4b60-9897-fa471e56514f)

