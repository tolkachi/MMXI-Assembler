package cse560;

import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Stores the abstract representation of an MMXI assembly program.
 * <p>
 * <h1>Parser view</h1>
 * <p>
 * TODO: Explain Parser's view of the Program component.
 * <h1>Assembler view</h1>
 * <p>
 * TODO: Explain Assembler's view of the Program component.
 * <p>
 * <b>Model:</b>
 * <ul>
 * <li>{@code string segmentName} - The name of the segment.</li>
 * <li>{@code boolean isRelocatable} - True iff this is a relocatable program.</li>
 * <li>{@code integer firstAddress} - The first address of the segment.</li>
 * <li>{@code integer length} - The number of words in the segment.</li>
 * <li>{@code integer execAddress} - The address at which to begin execution.</li>
 * <li>{@code SourceRecord records} - The parsed body of the program as an
 * ordered list of {@link SourceRecord}s.</li>
 * <li>{@code symbolTable} - The symbol table as a map from labels to two-tuples
 * {@code (v, r)}, where {@code v} is the value of the symbol and {@code r} is a
 * boolean indicating whether symbol is relocatable.</li>
 * <li>{@code literalTable} - The literals table as a map from the literal to
 * the address at which it is stored.</li>
 * <li>{@code entrySymbol} - A set of strings indicating the symbols in this
 * program that can be referenced externally.</li>
 * </ul>
 * <p>
 * <b>Constraints:</b>
 * <ul>
 * <li>The elements of {@code records} are in increasing order by line number.</li>
 * <li>For all {@code (s, n)} in {@code symbolTable}, {@code 0 <= n <= 0xffff}
 * and {@code s} is a valid symbol.</li>
 * <li>For all {@code (m, n)} in {@code literalTable}, {@code 0 <= m <= 0xffff}.
 * </li>
 * <li>{@code segmentName} meets the requirements for segment names as outlined
 * in the simulator specification.</li>
 * <li>{@code 0 <= firstAddress <= 0xffff}</li>
 * <li>{@code firstAddress <= execAddress <= 0xffff}</li>
 * </ul>
 *
 * @author Igor Tolkachev
 *
 */
public interface Program {
    /**
     * Add a symbol to the list of externally accessible symbols.
     * <p>
     * <b>Requires:</b> {@code symbol} be added to the symbol table by the end
     * of parsing.
     *
     * @param symbol
     *            The symbol to add to the list of entry symbols.
     */
    void addEntrySymbol(String symbol);

    /**
     * Adds an entry to the list of external symbols.
     *
     * @param symbol The symbol that will be defined outside of this program.
     */
    void addExternalSymbol(String symbol);

    /**
     * Adds a literal to the literal table with an invalid address. Address will
     * be set when {@code startLiteralsAt} is called.
     * <p>
     * <b>Requires:</b> {@code literal} is not yet in the literal table.
     * <p>
     * <b>Ensures:</b> {@code literalTable = #literalTable union (literal,
     * -1)}
     *
     * @param literal
     *            The literal to add to the table.
     */
    void addLiteral(int literal);

    /**
     * Adds a record to the end of {@code records}.
     * <p>
     * <b>Requires:</b> {@code record.lineNumber > records[i].lineNumber} for
     * all {@code i < |records}.
     * <p>
     * <b>Ensures:</b> {@code records = #records * <record>}
     *
     * @param record
     *            The record to add to the end {@code records}.
     */
    void addRecord(SourceRecord record);

    /**
     * Adds a non-relocatable value to the symbol table.
     * <p>
     * <b>Requires:</b>
     * <ul>
     * <li>{@code (symbol, n)} is not in {@code symbolTable} for all {@code n}.</li>
     * <li>{@code symbol} is a valid string for a symbol.</li>
     * <li>{@code 0 <= value <= 0xffff}.</li>
     * </ul>
     * <p>
     * <b>Ensures:</b>
     * {@code symbolTable = #symbolTable union (symbol, (value, false))}.
     *
     * @param symbol
     *            The name of the symbol.
     * @param value
     *            The value the symbol represents.
     */
    void addSymbol(String symbol, int value);

    /**
     * Adds a value to the symbol table.
     * <p>
     * <b>Requires:</b>
     * <ul>
     * <li>{@code (symbol, n)} is not in {@code symbolTable} for all {@code n}.</li>
     * <li>{@code symbol} is a valid string for a symbol.</li>
     * <li>{@code 0 <= value <= 0xffff}.</li>
     * </ul>
     * <p>
     * <b>Ensures:</b>
     * {@code symbolTable = #symbolTable union (symbol, (value, isRelocatable))}.
     *
     * @param symbol
     *            The name of the symbol.
     * @param value
     *            The value the symbol represents.
     * @param isRelocatable
     *            True iff the symbol is relative.
     */
    void addSymbol(String symbol, int value, boolean isRelocatable);

    /**
     * Get the set of entry symbols for this program.
     *
     * @return {@code this.entrySymbols}
     */
    Set<String> getEntrySymbols();

    /**
     * Returns the address of the first instruction to execute.
     *
     * @return {@code this.execAddress}
     */
    int getExecAddress();

    /**
     * Returns the address of the beginning of the segment.
     *
     * @return {@code this.firstAddress}
     */
    int getFirstAddress();

    /**
     * Returns the length of this segment in words.
     * <p>
     * <b>Requires:</b> {@code setLength} has been called.
     *
     * @return {@code this.length}
     */
    int getLength();

    /**
     * Get the address at which a literal is stored.
     *
     * @param literal
     *            The literal whose address to return.
     * @return {@code literalTable[literal]}
     */
    int getLiteralAddress(int literal);

    /**
     * Returns the literal table as a map. Necessary so that Assembler can
     * iterate over it and write the literals to memory.
     *
     * @return {@code this.literalTable} as a Map.
     */
    Map<Integer, Integer> getLiteralTable();

    /**
     * Returns the next unprocessed record.
     * <p>
     * <b>Requires:</b> {@code |records| > 0}
     * <p>
     * <b>Ensures:</b> {@code #records = <getNextRecord> * records}
     *
     * @return {@code this.records[0]}
     */
    SourceRecord getNextRecord();

    /**
     * Returns the name of this segment.
     *
     * @return {@code this.segmentName}
     */
    String getSegmentName();

    /**
     * Gets the value of the designated symbol.
     * <p>
     * <b>Requires:</b>
     * {@code exist int v, boolean r : (symbol, v, r) in symbolTable}
     *
     * @param symbol
     *            The symbol whose value is desired.
     * @return {@code n for (symbol, n) in symbolTable}
     */
    int getSymbolValue(String symbol);

    boolean hasExternalSymbol(String symbol);

    /**
     * Check to see if a literal is in the literal table.
     *
     * @param literal
     *            The integer to look up in the table.
     * @return True iff {@code literalTable has key literal}
     */
    boolean hasLiteral(int literal);

    /**
     * Checks to see if a symbol is in the symbol table.
     *
     * @param symbol
     *            The symbol to look up in the table.
     * @return True iff {@code symbolTable has key symbol}
     */
    boolean hasSymbol(String symbol);

    /**
     * Check whether a symbol is relative.
     * <p>
     * <b>Requires:</b>
     * {@code exist int v, boolean r : (symbol, v, r) in symbolTable}
     *
     * @param symbol
     *            The symbol whose relativity to check.
     * @return {@code r} for {@code (symbol, v, r) in symbolTable}
     */
    boolean isRelative(String symbol);

    /**
     * Returns true iff the program is set to be relocatable.
     *
     * @return {@code this.isRelocatable}
     */
    boolean isRelocatable();

    /**
     * Sets whether the program is relocatable or not.
     * <p>
     * <b>Requires:</b> The program fits into one page.
     * <p>
     * <b>Ensures:</b> {@code this.isRelocatable = value}
     *
     * @param value
     *            A boolean indicating whether the program is relocatable (true)
     *            or not (false).
     */
    void isRelocatable(boolean value);

    /**
     * Returns the number of literals in the literal table.
     *
     * @return {@code |literalTable|}
     */
    int numberOfLiterals();

    /**
     * Returns the number of unprocessed records remaining.
     *
     * @return {@code |records|}
     */
    int numberOfRecords();

    /**
     * Returns the number of symbols in the symbol table.
     *
     * @return {@code |symbolTable|}
     */
    int numberOfSymbols();

    /**
     * Sets the address of the first instruction to execute.
     * <p>
     * <b>Requires:</b> {@code firstAddress <= execAddress <= 0xffff}
     * <p>
     * <b>Ensures:</b> {@code this.execAddress = execAddress}
     *
     * @param execAddress
     *            The address of the first instruction to execute.
     */
    void setExecAddress(int execAddress);

    /**
     * Sets the address at which the program segment begins.
     * <p>
     * <b>Requires:</b> {@code 0 <= firstAddress <= 0xffff}
     * <p>
     * <b>Ensures:</b> {@code this.firstAddress = firstAddress}
     *
     * @param firstAddress
     *            The address at which the program begins.
     */
    void setFirstAddress(int firstAddress);

    /**
     * Set the length of this segment.
     * <p>
     * <b>Requires:</b> {@code 0 <= length <= 0xffff}
     * <p>
     * <b>Ensures:</b> {@code this.length = length}
     *
     * @param length
     *            The length of this segment in words.
     */
    void setLength(int length);

    /**
     * Sets the segment name of the program.
     * <p>
     * <b>Requires:</b> {@code segmentName} meets the requirements for segment
     * names.
     * <p>
     * <b>Ensures:</b> {@code this.segmentName = segmentName}
     *
     * @param segmentName
     *            The name of the program segment.
     */
    void setSegmentName(String segmentName);

    /**
     * Assigns an address to each literal in the literal table, beginning at
     * {@code address}.
     * <p>
     * <b>Requires:</b> {@code 0 <= address <= (0xffff - |literalTable|)}
     * <p>
     * <b>Ensures:</b> For all {@code (m, n)} in {@code literalTable},
     * {@code address <= n < address + |literalTable|} and all {@code n}s are
     * unique.
     *
     * @param address
     *            The address at which to begin the literals.
     */
    void startLiteralsAt(int address);

    /**
     * Writes a complete, human-readable summary of this object's state to
     * {@code stream}.
     *
     * @param stream
     *            The output stream to write the summary to.
     */
    void writeStateTo(OutputStream stream);

    /**
     * Returns the set of symbols used in this program but not declared here.
     *
     * @return {@code this.externalSymbols}
     */
    Set<String> getExternalSymbols();
}
