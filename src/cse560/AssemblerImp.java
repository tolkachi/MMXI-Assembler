package cse560;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * An implementation of the Assembler interface.
 *
 * <h1>Implementation</h1>
 *
 * <pre>
 * read head record
 * write head record to object file
 * write listing
 *
 * while more than one records remain
 *      get next record
 *      if pseudo-op then
 *           handle appropriately
 *      else handle as machine op
 * process .END record
 * </pre>
 *
 * @author Igor Tolkachev
 */
public final class AssemblerImp implements Assembler {
    /**
     * The index of the bit in certain instructions that indicates an immediate
     * for the last argument.
     */
    private static final int IMM_FLAG = 5;

    /** The base of hexadecimal. */
    private static final int BASE_16 = 16;

    /** PrintWriter connected to the object file being generated. */
    private PrintWriter objWriter;

    /** PrintWriter for the listing output. */
    private PrintWriter listWriter;

    @Override
    public void assemble(BufferedWriter objectOut, BufferedWriter listingOut,
            Program program) throws IOException {
        SourceRecord record = program.getNextRecord();
        this.objWriter = new PrintWriter(objectOut, true);
        this.listWriter = new PrintWriter(listingOut, true);

        // ...Write the header record

        this.objWriter.printf("H%-6s%04X%04X%n", program.getSegmentName(),
                program.getFirstAddress(), program.getLength());
        //this.objWriter.println();
        printListingLine(record, -1, -1, true);

        // ...Write the entry points as 'E' records

        for (String entrySymbol : program.getEntrySymbols()) {
            // Write the symbol and value
            this.objWriter.printf("N%-6s%04X", entrySymbol,
                    program.getSymbolValue(entrySymbol));

            // If the symbol is relative, terminate line with an 'R'.
            // Else, terminate with an 'A' for absolute.
            if (program.isRelative(entrySymbol)) {
                this.objWriter.printf("R%n");
            } else {
                this.objWriter.printf("A%n");
            }
            
        }

        // ...Write the external symbols used here as 'X' records

        for (String externalSymbol : program.getExternalSymbols()) {
            this.objWriter.printf("X%s%n", externalSymbol);
        }

        // ...For each record, generate a line of the object file (if necessary)
        // and a line of the listing. Stop just before the last, which will be
        // the .END record.
        while (program.numberOfRecords() > 1) {
            // Start with the instruction as 0xffffffff so that it's clear to
            // the listing writer whether this is meant to be a real MMXI value.
            int instr = -1;
            record = program.getNextRecord();

            // ... Handle the pseudo-ops.

            if (record.getOpCode().charAt(0) == '.') {
                // ...Handle .FILL, which sets a cell in memory to a given
                // value.
                if (record.getOpCode().equals(".FILL")) {
                    String relocationRecord = "";
                    String arg = record.getArgAt(0);
                    ArgType type = ArgFormat.getArgType(arg);

                    // ...Set the value of instr and the relocation record based
                    // on the argument type.
                    instr = getArgValue(program, record, 0);

                    if (program.isRelocatable()
                            && (type == ArgType.LITERAL || (type == ArgType.SYMBOL && program
                                    .isRelative(arg)))) {
                        relocationRecord = "M1";
                    } else if (type == ArgType.SYMBOL
                            && program.hasExternalSymbol(arg)) {
                        relocationRecord = String.format("X%X%s", 0xf, arg);
                    }

                    // Write to object file
                    this.objWriter.printf("T%04X%04X%s%n",
                            record.getLocation(), instr, relocationRecord);

                    // Write listing line
                    printListingLine(record, record.getLocation(), instr, true);
                } else if (record.getOpCode().equals(".STRZ")) {
                    // ...Handle .STRZ pseudo-op, which fills memory with a
                    // given sequence of characters followed by a null.

                    String str = record.getArgAt(0);
                    int location = record.getLocation();
                    boolean firstChar = true;

                    // ...Generate one text record for each character plus the
                    // null.

                    for (int i = 1; i < str.length() - 1; ++i) {
                        this.objWriter.printf("T%04X%04X%n", location,
                                str.codePointAt(i));

                        printListingLine(record, location, str.codePointAt(i),
                                firstChar);

                        ++location;

                        firstChar = false;
                    }

                    this.objWriter.printf("T%04X0000%n", location);

                    printListingLine(record, location, 0, firstChar);
                } else {
                    // ...If not .FILL or .STRZ, just write an output line.
                    printListingLine(record, -1, -1, true);
                }
            } else {
                // ...Handle the machine ops.

                String opCode = record.getOpCode();
                instr = MachineOpTable.getTemplate(opCode);
                String relocationRecord = "";

                // Loop over the args
                for (int i = 0; i < record.getArgCount(); ++i) {
                    String arg = record.getArgAt(i);
                    int argValue = getArgValue(program, record, i);
                    ArgFormat argFormat =
                            MachineOpTable.getArgFormat(opCode, i);
                    ArgType type = ArgFormat.getArgType(arg);
                    int argLen = argFormat.getLength();
                    int argPos = argFormat.getPosition();

                    // Ensure that the argument is within proper bounds
                    if (type == ArgType.IMMEDIATE
                            && isArgOutOfBounds(argValue, argLen, arg.charAt(0))) {
                        ErrorHandler.fatalError(
                                "Immediate out of bounds for argument", 0,
                                record.getLineNumber());
                    }

                    if (type == ArgType.SYMBOL
                            && isArgOutOfBounds(argValue, argLen, 'x')) {
                        ErrorHandler.fatalError(
                                "Symbol out of bounds for argument", 1,
                                record.getLineNumber());
                    }

                    // External symbols can only appear in the last arg slot.

                    if (type == ArgType.SYMBOL
                            && program.hasExternalSymbol(arg)
                            && i + 1 != record.getArgCount()) {
                        ErrorHandler
                                .fatalError(
                                        "External symbol found in non-final argument slot",
                                        4, record.getLineNumber());
                    }

                    // If argument is an address, verify the page number matches
                    // that of the record's location.
                    if (argLen == 9) {
                        // Current PC is record location + 1
                        int recordPage = 1 + record.getLocation() >> 9;
                        int argPage = argValue >> 9;

                        if (argPage != recordPage) {
                            ErrorHandler.fatalError("Page number mismatch", 2,
                                    record.getLineNumber());
                        }
                    }

                    // If we're on the last argument, we need to check to
                    // see if we have a relocatable symbol.
                    if (program.isRelocatable()
                            && i == record.getArgCount() - 1) {
                        // If it's a literal, you always use a relocation
                        // record of M1.
                        // If it's a relocatable symbol, we need to check
                        // the length of the arg.
                        if (type == ArgType.LITERAL) {
                            relocationRecord = "M1";
                        } else if (type == ArgType.SYMBOL
                                && program.hasExternalSymbol(arg)) {
                            relocationRecord =
                                    String.format("X%X%s", argLen, arg);
                        } else if (type == ArgType.SYMBOL
                                && program.isRelative(arg)) {
                            relocationRecord = "M0";
                            if (argLen == 9) {
                                relocationRecord = "M1";
                            }
                        }
                    }

                    instr = orBitsAt(instr, argValue, argLen, argPos);

                    // Special case: If the instruction is AND or OR and the
                    // third argument is not a register, we need to flip on bit
                    // six to indicate an immediate.
                    if (i == MachineOpTable.MAX_ARGS - 1
                            && (opCode.equals("AND") || opCode.equals("ADD"))) {
                        if (record.getArgAt(i).charAt(0) != 'R') {
                            instr = instr | (1 << AssemblerImp.IMM_FLAG);
                        }
                    }
                }

                this.objWriter.printf("T%04X%04X%s%n", record.getLocation(),
                        instr, relocationRecord);

                printListingLine(record, record.getLocation(), instr, true);
            }

        }

        // ...Write the .END record to the *listing*

        record = program.getNextRecord();

        printListingLine(record, -1, -1, true);

        // ...Write literals to memory and listing

        for (Map.Entry<Integer, Integer> literal : program.getLiteralTable()
                .entrySet()) {
            this.objWriter.printf("T%04X%04X%n", literal.getValue(),
                    literal.getKey());

            printLiteralListingLine(literal.getKey(), literal.getValue());
        }

        // ...Write the .END record to the object file

        this.objWriter.printf("E%04X%n", program.getExecAddress());
    }

    /**
     * Returns the value of a provided argument.
     *
     * @param program
     * @param record
     * @param argIndex
     * @return
     */
    private int getArgValue(Program program, SourceRecord record, int argIndex) {
        int value = 0;

        String arg = record.getArgAt(argIndex);
        ArgType type = ArgFormat.getArgType(arg);

        if (type == ArgType.SYMBOL) {
            // If the symbol is locally defined, get it's value.
            // If it's neither locally defined nor an external symbol, throw an
            // error.
            if (program.hasSymbol(arg)) {
                value = program.getSymbolValue(arg);
            } else if (!program.hasExternalSymbol(arg)) {
                ErrorHandler.fatalError("No such symbol \"" + arg + "\"", 3,
                        record.getLineNumber());
            }
        } else {
            value = getValue(arg);

            if (type == ArgType.LITERAL && program.hasLiteral(value)) {
                value = program.getLiteralAddress(value);
            }
        }

        return value;
    }

    /**
     * Converts an MMXI operand into the appropriate integer value. Exits with
     * an error if that value cannot be determined.
     * <p>
     * <b>Requires:</b> The ArgType of {@code arg} be REGISTER, LITERAL, or
     * IMMEDIATE.
     *
     * @param record
     *            The record from which to pull the argument.
     * @param argIndex
     *            The index of the argument to parse.
     * @return The integer value of that argument.
     */
    private int getValue(String arg) {
        int value = Integer.MIN_VALUE;

        switch (arg.charAt(0)) {
        case 'R':
            value = Integer.parseInt(arg.substring(1));
            break;

        case '=':
            value = getValue(arg.substring(1));
            break;

        case 'x':
            value = Integer.parseInt(arg.substring(1), BASE_16);
            break;

        case '#':
            value = Integer.parseInt(arg.substring(1));
            break;

        default:
            assert false : "Invalid argument.";
        }

        return value;
    }

    /**
     * Verifies that the given argument is within proper bounds for the given
     * argument length.
     *
     * @param argValue
     *            The value to check.
     * @param argLen
     *            The number of bits the argument will be represented by.
     * @param base
     *            Character indicating the base argValue was given by. Uses
     *            standard MMXI conventions for immediates.
     * @return
     */
    private boolean isArgOutOfBounds(int argValue, int argLen, char base) {
        // Nine-bit arguments are addresses. Allowed values: 0 - 0xffff.
        // Otherwise, the argument must be within range of the number of bits
        //  allotted.
        int shift = 16;
        if (argLen != 9) {
            shift = argLen - 1;
        }

        int min = 0;
        int max = (1 << shift) - 1;

        // If the base is decimal, offset all values down by 2^(shift - 2)
        if (base == '#') {
            int offset = 1 << (shift - 1);
            max = max - offset;
            min = min - offset;
        }

        // Determine if arg is out of range.
        if (min <= argValue && argValue <= max) {
            return false;
        }

        return true;
    }

    /**
     * Inserts the rightmost {@code len} bits of {@code src} into {@code dest}
     * starting at {@code pos} using a bitwise OR.
     * <p>
     * <b>Requires:</b>
     * <ul>
     * <li>{@code 0 &lt;= pos &lt;= pos + len &lt; [bits in an int]}</li>
     * </ul>
     * <p>
     * <b>Ensures:</b>
     * <ul>
     * <li>{@code dest[0, pos - 1] = #dest[0, pos - 1]}</li>
     * <li>{@code dest[pos, pos + len] = src[0, len] | #dest[pos, pos + len]}</li>
     * <li>{@code dest[pos + len + 1, [bits in an int] - 1] = #dest[pos
     * + len + 1, [bit in an int] - 1]}</li>
     * </ul>
     */
    private int orBitsAt(int dest, int src, int len, int pos) {
        // Create a mask to isolate the rightmost len bits.
        int mask = ~(~0 << len);

        // Mask off all but the rightmost n bits and position rightmost
        // bit at pos.
        src = (src & mask) << pos;

        // Combine destination and source.
        return dest | src;
    }

    /**
     * Prints one line of a program listing for assembly output. Output takes
     * the following format: <blockquote>
     *
     * <pre>
     * (AAAA) BBBB CCCCCCCCCCCCCCCC (DDDD) EEEEEE   FFFFF GGG, GGG, GGG
     * </pre>
     *
     * </blockquote>
     * <p>
     * Where:
     * <ul>
     * <li>A's are the address</li>
     * <li>B's are the contents in hex</li>
     * <li>C's are the contents in binary</li>
     * <li>D's are the line number from the source file</li>
     * <li>E's are the label, if any</li>
     * <li>F's are the opcode</li>
     * <li>G's are the operands</li>
     * </ul>
     *
     * There are a few caveats. If instruction is -1, do not print the address
     * or memory contents. If printSrc is false, do not print the label, opcode,
     * or operands.
     *
     * @param writer
     *            The BufferedWriter to write the line to.
     * @param record
     *            The record to pull data from.
     * @param addr
     *            The address of the data (if applicable).
     * @param instruction
     *            The value of memory. -1 if not applicable.
     * @param printSrc
     *            Indicates whether to print the source code.
     */
    private void printListingLine(SourceRecord record, int addr,
            int instruction, boolean printSrc) {
        String label = record.getLabel();
        String binary = "";

        // ...Generate binary string

        //
        if (instruction != -1) {
            StringBuilder binaryInstr =
                    new StringBuilder(Integer.toBinaryString(instruction));

            while (binaryInstr.length() < AssemblerImp.BASE_16) {
                binaryInstr.insert(0, '0');
            }

            binary = binaryInstr.toString();
        }

        // ...Convert null label to a blank string.

        if (label == null) {
            label = "";
        }

        // ...Write address, hex and binary contents, line #, label, and
        // instruction

        // Only write address and memory contents if instruction generates data.
        if (MachineOpTable.hasOpCode(record.getOpCode())
                || record.getOpCode().equals(".FILL")
                || record.getOpCode().equals(".STRZ")) {
            this.listWriter.printf("(%04X) %04X %s (%4d)", addr, instruction,
                    binary, record.getLineNumber());
        } else {
            this.listWriter.printf("%28s (%4d)", binary,
                    record.getLineNumber(), label);
        }

        // ...Write opcode and operands if requested

        if (printSrc) {
            this.listWriter.printf(" %-8s %-5s ", label, record.getOpCode());

            int argCount = record.getArgCount();
            for (int i = 0; i < argCount; ++i) {
                this.listWriter.printf("%s", record.getArgAt(i));
                if (i < argCount - 1) {
                    this.listWriter.print(", ");
                }
            }
        }

        this.listWriter.println();
    }

    private void printLiteralListingLine(int literal, int addr) {
        StringBuilder binary =
                new StringBuilder(Integer.toBinaryString(literal));

        while (binary.length() < 16) {
            binary.insert(0, '0');
        }

        this.listWriter.printf("(%04X) %04X %s ( lit)%n", addr, literal,
                binary.toString());
    }
}
