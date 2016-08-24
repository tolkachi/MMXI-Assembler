package cse560;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ProgramImp implements Program {
    /**
     * Structure containing the value and relativity flag for a single symbol.
     * Used by the symbol table.
     *
     * @author Igor Tolkachev
     *
     */
    private static final class Symbol {
        /** Value of this symbol. */
        public int value;

        /** Whether or not this symbol is relative. */
        public boolean isRelative;

        /**
         * Initializes a new symbol with the given value and relativity flag.
         */
        public Symbol(int value, boolean isRelative) {
            this.value = value;
            this.isRelative = isRelative;
        }
    }

    /** Name of this segment. */
    private String segmentName;

    /** Address of the start of this segment. */
    private int firstAddress;

    /** Address at which to begin execution. */
    private int execAddress;

    /** Length of the segment in words. */
    private int length = -1;

    /** True iff the program is relocatable. */
    private boolean isRelocatable = false;

    /** The body of the program. */
    private final Queue<SourceRecord> records = new LinkedList<SourceRecord>();

    /**
     * The literal table, a mapping of integer values to the addresses where
     * they reside.
     */
    private final Map<Integer, Integer> literalTable =
            new HashMap<Integer, Integer>();

    /**
     * The symbol table. Associates a symbol (a string) its value and whether it
     * is relative.
     */
    private final Map<String, Symbol> symbolTable =
            new HashMap<String, Symbol>();

    /** Set of external symbols. */
    private final Set<String> externalSymbols = new HashSet<String>();

    /**
     * Entry table. The subset of the symbol table which can be referenced by
     * other segments.
     */
    private final Set<String> entryPoints = new HashSet<String>();

    @Override
    public void addEntrySymbol(String symbol) {
        this.entryPoints.add(symbol);
    }

    @Override
    public void addExternalSymbol(String symbol) {
        this.externalSymbols.add(symbol);
    }

    @Override
    public void addLiteral(int literal) {
        this.literalTable.put(literal, -1);
    }

    @Override
    public void addRecord(SourceRecord record) {
        records.add(record);
    }

    @Override
    public void addSymbol(String symbol, int value) {
        // Do not try to add a duplicate symbol.
        assert !this.symbolTable.containsKey(symbol);

        this.symbolTable.put(symbol, new Symbol(value, false));
    }

    @Override
    public void addSymbol(String symbol, int value, boolean isRelative) {
        this.symbolTable.put(symbol, new Symbol(value, isRelative));
    }

    @Override
    public Set<String> getEntrySymbols() {
        return this.entryPoints;
    }

    @Override
    public int getExecAddress() {
        return this.execAddress;
    }

    @Override
    public int getFirstAddress() {
        return this.firstAddress;
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Override
    public int getLiteralAddress(int literal) {
        return this.literalTable.get(literal);
    }

    @Override
    public Map<Integer, Integer> getLiteralTable() {
        return this.literalTable;
    }

    @Override
    public SourceRecord getNextRecord() {
        return records.remove();
    }

    @Override
    public String getSegmentName() {
        return this.segmentName;
    }

    @Override
    public int getSymbolValue(String symbol) {
        return this.symbolTable.get(symbol).value;
    }

    @Override
    public boolean hasExternalSymbol(String symbol) {
        return this.externalSymbols.contains(symbol);
    }

    @Override
    public boolean hasLiteral(int literal) {
        return this.literalTable.containsKey(literal);
    }

    @Override
    public boolean hasSymbol(String symbol) {
        return this.symbolTable.containsKey(symbol);
    }

    @Override
    public boolean isRelative(String symbol) {
        return this.symbolTable.get(symbol).isRelative;
    }

    @Override
    public boolean isRelocatable() {
        return this.isRelocatable;
    }

    @Override
    public void isRelocatable(boolean value) {
        this.isRelocatable = value;
    }

    @Override
    public int numberOfLiterals() {
        return literalTable.size();
    }

    @Override
    public int numberOfRecords() {
        return records.size();
    }

    @Override
    public int numberOfSymbols() {
        return symbolTable.size();
    }

    @Override
    public void setExecAddress(int execAddress) {
        this.execAddress = execAddress;
    }

    @Override
    public void setFirstAddress(int startAddress) {
        this.firstAddress = startAddress;
    }

    @Override
    public void setLength(int length) {
        this.length = length;

        assert 0 <= this.length && this.length <= 0xffff;
    }

    @Override
    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    @Override
    public void startLiteralsAt(int address) {
        for (Map.Entry<Integer, Integer> entry : literalTable.entrySet()) {
            entry.setValue(address);
            ++address;
        }
    }

    @Override
    public void writeStateTo(OutputStream stream) {
        PrintWriter out =
                new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        stream)), true);

        // ...Print program description

        out.printf("segmentName   = '%s'\n", this.segmentName);
        out.printf("isRelocatable = %b\n", this.isRelocatable);
        out.printf("firstAddress  = 0x%04x\n", this.firstAddress);
        out.printf("length        = 0x%04x\n", this.length);
        out.printf("execAddress   = 0x%04x\n", this.execAddress);

        // ...Print external symbols

        out.println("\n# EXTERNAL SYMBOLS #\n");

        for (String entry : this.externalSymbols) {
            out.println(entry);
        }

        // ...Print entry points

        out.println("\n# ENTRY POINTS #\n");

        for (String entry : this.entryPoints) {
            out.println(entry);
        }

        // ...Print symbol table

        out.println("\n# SYMBOL TABLE #\n");

        // Print each entry with the format "Symb   0xabcd true"
        for (Map.Entry<String, Symbol> entry : symbolTable.entrySet()) {
            Symbol symbol = entry.getValue();

            out.printf("%-6s 0x%04x %b\n", entry.getKey(), symbol.value,
                    symbol.isRelative);
        }

        // ...Print literal table

        out.println("\n# LITERAL TABLE #\n");

        // Print each entry with the format "0x1234 0xabcd"
        // First column is the "name", second is the address.
        for (Map.Entry<Integer, Integer> entry : literalTable.entrySet()) {
            out.printf("%4x %4x\n", entry.getKey(), entry.getValue());
        }

        // ...Print records

        out.println("\n# PROGRAM #\n");

        for (int i = 0; i < this.records.size(); ++i) {
            SourceRecord record = this.records.remove();
            out.write(record.toString());
            out.write('\n');
            this.records.add(record);
        }

        out.close();
    }

    @Override
    public Set<String> getExternalSymbols() {
        return this.externalSymbols;
    }

}
