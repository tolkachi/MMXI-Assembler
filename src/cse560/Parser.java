package cse560;

import java.io.BufferedReader;

/**
 * Opens the input file, stores the records in memory, and creates the symbols
 * and literals tables. Checks for various errors in syntax of the input file.
 *
 * <b>Model:</b>
 * <ul>
 * <li>{@code Program program} - Stores records, symbols, literals.</li>
 * <li>{@code integer LC} - Location Counter, an integer that represents the
 *     address of the current instruction.</li>
 * <li>{@code integer lineNumber} - Keeps track of the current line number
 *     of a record.</li>
 * </ul>
 *
 * @author Igor Tolkachev
 *
 */
public interface Parser {
	/**
	 * Parses up to {@code maxRecords} lines from the input file pointed to by
	 * {@code input} and returns a {@code Program} object.
	 * <p>
	 * <b>Requires:</b>
	 * <ul>
	 * <li>input exists and is valid input file</li>
	 * <li>maxRecords > 0</li>
	 * </ul>
	 *
	 * @param input
	 *
	 * @param maxRecords
	 *
	 * @return {@code this.program}
	 */
	Program parse(BufferedReader input, int maxRecords);
}