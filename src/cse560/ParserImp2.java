package cse560;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 *
 * @author Igor Tolkachev
 */
public final class ParserImp2 implements Parser {

    @Override
    public Program parse(BufferedReader input, int maxRecords) {
        Program program = new ProgramImp();
        String inputLine = null;
        int locationCounter = 0;
        int lineNumber = 0;

        SourceRecord record = new SourceRecordImp();
        String label = null;
        String opCode = null;
        String[] args = null;

        boolean isAtEndRecord = false;

        //... Read until first .ORIG line.

        // TODO: Ensure this loop does not fail on empty input.
        try {
            while (inputLine == null || isComment(inputLine)) {
                inputLine = getNextLine(input);
                ++lineNumber;
            }
        } catch (IOException e) {
            ErrorHandler.fatalError(e.getMessage(), 100);
        } catch (DataFormatException e) {
            ErrorHandler.fatalError(e.getMessage(), 101, lineNumber);
        }

        //... Process .ORIG record

        // The first record must be a .ORIG

        try {
            label = parseLabel(inputLine);
            opCode = parseOpCode(inputLine);
            args = parseArgs(inputLine);

            if (!opCode.equals(".ORIG")) {
                ErrorHandler.fatalError("Unexpected record before .ORIG", 103,
                        lineNumber);
            }

            locationCounter = processPseudoOp(program, label, opCode, args);
        } catch (DataFormatException e) {
            ErrorHandler.fatalError(e.getMessage(), 104, lineNumber);
        }

        // Add .ORIG record to program and set segment first address to initial
        // value of location counter.
        record.setLabel(label);
        record.setOpCode(opCode);
        if (args.length != 0) {
            record.addArg(args[0]);
        }
        record.setLineNumber(lineNumber);
        program.addRecord(record);

        //... Read until we encounter a .END record

        while (!isAtEndRecord) {
            // The number of words this record takes up in memory.
            int recordLen = 0;

            // Where the location counter should be after this step.
            int nextLocation = locationCounter;

            // We've read another input line, so increment our count.
            ++lineNumber;


            // Get next line of input if not at .END record
            try {
                inputLine = getNextLine(input);
            } catch (IOException e) {
                ErrorHandler.fatalError(e.getMessage(), 105, lineNumber);
            } catch (DataFormatException e) {
                ErrorHandler.fatalError(e.getMessage(), 106, lineNumber);
            }

            // If this is a comment, skip it.
            if (isComment(inputLine)) {
                continue;
            }

            // Get the op, label and args

            try {
                label = parseLabel(inputLine);
                opCode = parseOpCode(inputLine);
                args = parseArgs(inputLine);
            } catch (DataFormatException e) {
                ErrorHandler.fatalError(e.getMessage(), 107, lineNumber);
            }

            // Only one .ORIG record is allowed per file
            if (opCode.equals(".ORIG")) {
                ErrorHandler.fatalError("Extra .ORIG record", 108, lineNumber);
            }

            // If we've found the .END record, set the flag so we don't try to
            // make another pass.
            if (opCode.equals(".END")) {
                isAtEndRecord = true;
            }

            // If op is a pseudo-op, process it as such.
            // Otherwise, see if it's in the machine op table.
            // If neither is true, explode.
            if (isPseudoOp(opCode)) {
                try {
                    nextLocation +=
                            processPseudoOp(program, label, opCode, args);
                } catch (DataFormatException e) {
                    ErrorHandler.fatalError(e.getMessage(), 109, lineNumber);
                }
            } else if (!MachineOpTable.hasOpCode(opCode)) {
                ErrorHandler.fatalError("Unknown opcode", 110, lineNumber);
            } else {
                // It's a machine-op, so it will take one word of memory.
                ++nextLocation;

                // Make sure the operation has the correct number of arguments.
                if (args.length != MachineOpTable.getNumArgs(opCode)) {
                    ErrorHandler.fatalError("Wrong number of arguments for "
                            + opCode, 111, lineNumber);
                }

                // ...Validate operands for this machine op
                for (int i = 0; i < args.length; ++i) {
                    ArgFormat format = MachineOpTable.getArgFormat(opCode, i);
                    ArgType type = ArgFormat.getArgType(args[i]);

                    // Check that, in general, this argument type is allowed in
                    // this slot.
                    if (!format.allows(type)) {
                        ErrorHandler.fatalError("Invalid argument type for "
                                + opCode, 112, lineNumber);
                    }

                    // If it's a literal, validate it and add it to the table.
                    if (type == ArgType.LITERAL) {
                        // Only LD can use literals
                        if (!opCode.equals("LD")) {
                            ErrorHandler.fatalError(
                                    "Literals are only allowed for LD", 113,
                                    lineNumber);
                        }

                        int literal =
                                ArgFormat.parseImmediate(args[i].substring(1));

                        if (!program.hasLiteral(literal)) {
                            program.addLiteral(literal);
                        }
                    }
                }

            }

            // Now that everything's been processed, update the location
            // counter.
            locationCounter += recordLen;

            // Initialize the new record and add it to our program
            record = new SourceRecordImp();
            record.setLineNumber(lineNumber);
            record.setLabel(label);
            record.setOpCode(opCode);

            for (String arg : args) {
                record.addArg(arg);
            }

            // If the location counter is going to move, we know a few things:
            // * The record has nonzero length and should have its location set.
            // * If there's a label for this record, it should be validated and
            //   added to the symbol table.
            if (locationCounter != nextLocation) {
                record.setLocation(locationCounter);

                if (label != null) {
                    if (program.hasSymbol(label)) {
                        ErrorHandler.fatalError("Duplicate symbol found", 114,
                                lineNumber);
                    } else {
                        program.addSymbol(label, locationCounter);
                    }
                }
            }

            program.addRecord(record);

            // Update location counter
            locationCounter = nextLocation;

            // Validate new location.
            if (locationCounter > 0xffff) {
                ErrorHandler.fatalError("Segment left system memory", 115,
                        lineNumber);
            }
        }

        // ...Configure remaining program variables.

        // Initialize literals.
        program.startLiteralsAt(locationCounter);
        locationCounter += program.numberOfLiterals();

        // The program's length is the current location counter minus the start
        // address of the segment.
        program.setLength(locationCounter - program.getFirstAddress());

        return program;
    }

    /**
     * Check if the provided opcode is a pseudo-op.
     * <p>
     * <b>Requires:</b> {@code |opCode| > 0}
     *
     * @param opCode
     * @return
     */
    private boolean isPseudoOp(String opCode) {
        if (opCode.charAt(0) == '.') {
            return true;
        }

        return false;
    }

    private int processPseudoOp(Program program, String label, String opCode,
            String[] args) throws DataFormatException {
        int size = 0;
        boolean wrongArgCount = false;

        if (opCode.equals(".STRZ")) {
            if (args.length != 1) {
                wrongArgCount = true;
            } else {
                // A .STRZ will take args[0].length - 2 + 1 words. -2 for the
                // quotations marks, + 1 for the null.
                size = args[0].length() - 1;
            }
        } else if (opCode.equals(".BLKW")) {
            if (args.length != 1) {
                wrongArgCount = true;
            } else {
                ArgType type = ArgFormat.getArgType(args[0]);

                switch (type) {
                case IMMEDIATE:
                    size = ArgFormat.parseImmediate(args[0]);
                    break;

                case SYMBOL:
                    if (program.hasSymbol(args[0])) {
                        size = program.getSymbolValue(args[0]);
                    } else {
                        throw new DataFormatException(
                                "Forward reference in .BLKW");
                    }
                }

            }
        } else if (opCode.equals(".END")) {
            int execAddress = program.getFirstAddress();

            if (args.length == 1) {
                ArgType type = ArgFormat.getArgType(args[0]);
                switch (type) {
                case IMMEDIATE:
                    execAddress = ArgFormat.parseImmediate(args[0]);
                    break;

                case SYMBOL:
                    if (program.hasSymbol(args[0])) {
                        execAddress = program.getSymbolValue(args[0]);
                    } else {
                        throw new DataFormatException("No such symbol: "
                                + args[0]);
                    }
                }
            } else if (args.length > 1) {
                wrongArgCount = true;
            }

            program.setExecAddress(execAddress);
        } else if (opCode.equals(".EQU")) {
            int value = 0;

            if (args.length != 1) {
                wrongArgCount = true;
            } else {
                switch (ArgFormat.getArgType(args[0])) {
                case IMMEDIATE:
                    value = ArgFormat.parseImmediate(args[0]);
                    break;

                case SYMBOL:
                    if (program.hasSymbol(args[0])) {
                        value = program.getSymbolValue(args[0]);

                    } else {
                        throw new DataFormatException("No such symbol: "
                                + args[0]);
                    }
                    break;

                default:
                    throw new DataFormatException("Invalid argument to .EQU: "
                            + args[0]);
                }

                addSymbol(program, label, value);
            }
        } else if (opCode.equals(".FILL")) {
            if (args.length == 1) {
                size = 1;
            } else {
                wrongArgCount = true;
            }
        } else if (opCode.equals(".ORIG")) {
            // .ORIG must have a label
            if (label == null) {
                throw new DataFormatException(".ORIG requires label");
            }

            // .ORIG can have zero or one arguments. If zero, set program to
            // relocatable and start at zero.
            // If one, validate that address and set size equal to it.
            if (args.length == 0) {
                program.isRelocatable(true);
            } else if (args.length == 1) {
                size = ArgFormat.parseImmediate(args[0]);

                // Verify that the segment record is within memory
                if (size < 0 || 0xffff < size) {
                    throw new DataFormatException(
                            ".ORIG argument outside of system memory.");
                }
            } else if (args.length > 1) {
                wrongArgCount = true;
            }

            program.setFirstAddress(size);
            program.setSegmentName(label);
        } else if (opCode.equals(".ENT")) {

        } else if (opCode.equals(".EXT")) {

        }

        // If any of the above had too many arguments, error out.
        if (wrongArgCount) {
            throw new DataFormatException("Too many arguments for " + opCode);
        }

        return size;
    }

    private void addSymbol(Program program, String symbol, int symbolValue)
            throws DataFormatException {
        if (program.hasSymbol(symbol)) {
            throw new DataFormatException("Duplicate symbol: " + symbol);
        } else {
            program.addSymbol(symbol, symbolValue);
        }
    }

    /**
     * Take a line of validated input and extract the arguments from it.
     *
     * @param line
     *            The
     * @return An array of arguments as Strings.
     */
    private String[] parseArgs(String line) throws DataFormatException {
        String[] args = new String[0];

        if (line.length() >= 18) {
            // If the line starts with a double-quote, we'll parse it as a
            // string literal.
            // Otherwise, treat it as a list of comma-separated arguments with
            // NO whitespace allowed.
            if (line.charAt(17) == '"') {
                line = line.substring(17);
                if (line.matches("^\".*\".*")) {
                    args = new String[1];
                    args[0] = line.substring(0, line.indexOf('"', 1) + 1);
                } else {
                    throw new DataFormatException("Unterminated string literal");
                }
            } else {
                String argString = line.substring(17).split("[\\s;]")[0];

                args = argString.split(",");
            }
        }

        for (String arg : args) {
            if (ArgFormat.isValid(arg) == ArgType.BAD) {
                throw new DataFormatException("Malformed argument: \"" + arg
                        + "\"");
            }
        }

        return args;
    }

    /**
     * Extracts a label from a string.
     * <p>
     * <b>Requires:</b> {@code isValidInput(line) = true}
     *
     * @param line
     * @return The label if present, otherwise {@code null}.
     */
    private String parseLabel(String line) throws DataFormatException {
        String labelField = line.substring(0, 6);
        String label = null;

        // If the label is entirely whitespace, we want to return null to
        // indicate a lack of label.
        // Otherwise, verify it meets the requirements of a label:
        // * Starts with an alphabetic character that isn't 'R' or 'x'.
        // * The remaining characters are alphanumeric.
        // * All the characters after the last non-alphanumeric character is
        //   whitespace.
        if (labelField.trim().length() > 0) {
            if (labelField.matches("^[A-Za-z&&[^Rx]][A-Za-z0-9]{0,5} *")) {
                label = labelField.trim();
            } else {
                throw new DataFormatException("Invalid label");
            }
        }

        return label;
    }

    /**
     * <b>Requires</b>: {@code line} have a well-formed op code.
     *
     * @param line
     * @return
     */
    private String parseOpCode(String line) throws DataFormatException {
        // The given range for opcodes would be substring(9, 14).
        // If the line is shorter than that, adjust accordingly.
        int lastIndex = 14;
        if (line.length() < 14) {
            lastIndex = line.length();
        }

        String opCode = line.substring(9, lastIndex);
        opCode = opCode.trim();

        if (!MachineOpTable.hasOpCode(opCode)
                && !PseudoOpTable.hasOpCode(opCode)) {
            throw new DataFormatException("Invalid opcode");
        }

        return opCode;
    }

    /**
     * Returns true iff the input is a comment.
     * <p>
     * <b>Requires:</b> {@code |input| > 0}
     *
     * @param line
     *            The line to check.
     * @return {@code line[0] == ';'}
     */
    private boolean isComment(String line) {
        if (line.charAt(0) == ';') {
            return true;
        }
        return false;
    }

    /**
     * Reads in the next line of input and verifies that the input line is
     * well-formed.
     *
     * @param input
     *            The BufferedReader to read from.
     * @return
     */
    private String getNextLine(BufferedReader input) throws IOException,
            DataFormatException {
        String nextLine = null;

        try {
            nextLine = input.readLine();
        } catch (IOException e) {
            throw new IOException("Problem reading input file");
        }

        if (nextLine == null) {
            throw new DataFormatException("Unexpected end of input");
        } else if (!isValidInput(nextLine)) {
            throw new DataFormatException("Invalid input");
        }

        return nextLine;
    }

    /**
     * Returns true only if this line passes a basic format check. This check
     * ensures the line is non-empty and that it is either a comment or a
     * potentially valid record.
     * <p>
     * <b>Requires:</b> {@code line != null}
     *
     * @param line
     *            The String to check for validity.
     * @return True iff the basic format check outlined above is passed.
     */
    private boolean isValidInput(String line) {
        boolean isValid = false;

        // Valid lines are of non-empty
        if (line.length() > 0) {
            // If the line is a comment, it's valid.
            // Otherwise, make sure everything up to the
            if (isComment(line)) {
                isValid = true;
            } else if (line.matches("^.{6} {3}[\\.A-Z][A-Z][A-Z ]{1,3}.*")) {
                // TODO: Reduce the responsibility of the above regex.
                // Responsibility for validating labels and maybe even operands
                // can be handled elsewhere.
                isValid = true;
            }
        }

        return isValid;
    }

}
