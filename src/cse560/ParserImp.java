package cse560;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Provides an implementation of the Parser Interface
 * <p>
 * Implementation details: A {@code program} object is given to {@code Parser}.
 * This program object is used to represent the intermediate 'file' and contains
 * the symbol and literal tables, as well as data on all the non- comment
 * records.
 * <p>
 * Correspondence:
 *
 * @author Igor Tolkachev
 */
public final class ParserImp implements Parser {

    // ------------------------------------------------------------------------
    // PRIVATE VARIABLES
    // ------------------------------------------------------------------------

    /** instantiation of component to store records, symbols, literals */
    private final Program program = new ProgramImp();

    /** address of current instruction */
    private int locationCounter = -1;

    /** line number of current instruction */
    private int lineNumber;

    // ------------------------------------------------------------------------
    // PRIVATE METHODS
    // ------------------------------------------------------------------------

    /**
     * Validates all characters of a record up to the first comment
     *
     * @param record
     *            the record to be validated
     * @param label
     *            the label of the record
     * @param opName
     *            the instruction code in the record
     * @param ops
     *            the operands in the record
     *
     */
    void validateRecord(String record, String label, String opName, String[] ops) {
        if (!isWellFormed(record)) {
            ErrorHandler.fatalError("Invalid record", 100, lineNumber);
        }
        if (MachineOpTable.hasOpCode(opName)) {
            if (MachineOpTable.getNumArgs(opName) != ops.length) {
                ErrorHandler.fatalError("Invalid number of arguments", 130, lineNumber);
            }
            for (int i = 0; i < ops.length; i++) {
                if (!MachineOpTable.getArgFormat(opName, i).allows(
                        ArgFormat.getArgType(ops[i])))
                {
                    ErrorHandler.fatalError("Argument type not allowed", 133, lineNumber);
                }
            }
        } else if (PseudoOpTable.hasOpCode(opName)) {
            if (ops.length > 1
                    || (PseudoOpTable.mustHaveArgument(opName)
                    		&& ops.length == 0))
            {
                ErrorHandler.fatalError("Argument required", 131, lineNumber);
            }
            for (int i = 0; i < ops.length; i++) {
                if (!PseudoOpTable.isArgTypeAllowed(opName,
                        ArgFormat.getArgType(ops[i])))
                {
                    ErrorHandler.fatalError("Argument type not allowed", 133, lineNumber);
                }
            }
        } else {
            ErrorHandler.fatalError("Invalid operation", 101, lineNumber);
        }
    }

    /**
     * Checks a string to see if it could possibly be a well-formed record. A
     * well-formed record either starts with a ';' or meets the basic
     * character-by-character requirements for a record.
     *
     * @param record
     *            The record to check.
     * @return True iff the record is well-formed, as defined above.
     */
    boolean isWellFormed(String record) {
    	boolean result = false;
        
    	if (record.charAt(0) == ';') {
            result = true;
        } else if (record.matches("^[A-QS-Za-wyz ][0-9A-Za-z ]{5}   "
                + "[\\.A-Z][A-Z][A-Z ]{1,3}.*"))
        {
            result = true;
        }
        return result;
    }

    /**
     * Returns the label of a line of assembly code if present
     *
     * @param record
     *            The record whose label to return
     */
    String getLabel(String record) {
        String label = null;

        // if there is a label
        if (record.charAt(0) != ' ') {
            // check that it's a valid label
            label = record.substring(0, 6);
            if (label.trim().matches(".*\\s.*")) {
                ErrorHandler.fatalError("Unexpected whitespace in label", 134, lineNumber);
            }
            label = record.substring(0, record.indexOf(' '));
        } else if (getOpName(record).equals(".ORIG")
                || getOpName(record).equals(".EQU"))
        {
            ErrorHandler.fatalError("Expected label", 102, lineNumber);
        }
        return label;
    }

    /**
     * Returns the op name of a line of assembly code
     *
     * @param record
     *            The record whose op name to return
     */
    String getOpName(String record) {
        String opcode = null;

        if (record.charAt(0) == ';') {
            opcode = record;
        } else if (record.charAt(9) != ' ') {
            opcode = record.substring(9).split("[\\s;]")[0];
        } else {
            ErrorHandler.fatalError("Expected instruction", 103, lineNumber);
        }
        return opcode;
    }

    /**
     * Returns the operands in a String array
     *
     * @param record
     *            The record whose operands to return
     */
    String[] getOperands(String record) {
        String[] operands = null;

        if (record.length() < 18) {
            operands = new String[0];
        } else if (!getOpName(record).equals(".STRZ")) {
            if (record.charAt(17) != ' ') {
                // Takes everything from pos 18 on and breaks it at the first
                // semi-colon.
                String ops = record.substring(17).split("[\n;]")[0].trim();

                operands = ops.split(",");
                if (operands.length > MachineOpTable.MAX_ARGS) {
                    ErrorHandler.fatalError("Too many arguments", 104, lineNumber);
                }
            }
        } else { // .STRZ
            int endQuote = record.lastIndexOf('"');
            /*
             * if (record.length() > endQuote + 1) {
             * error("ERROR 105: Invalid argument for .STRZ at line " +
             * lineNumber, true); }
             */
            if (record.charAt(17) == '"' && endQuote > 17) {
                operands = new String[1];
                operands[0] = record.substring(17, endQuote + 1);
            } else {
            	ErrorHandler.fatalError("Invalid argument for .STRZ", 105, lineNumber);
            }
        }
        // Verify each argument is well-formed
        for (String op : operands) {
            if (ArgFormat.isValid(op) == ArgType.BAD) {
            	ErrorHandler.fatalError("Malformed argument", 132, lineNumber);
            }
        }
        return operands;
    }

    /**
     * Initializes and populates a SourceRecord and adds it to program
     *
     * @param label
     *            the label of the record
     * @param opCode
     *            the op code of the record
     * @param operands
     *            the array of operands of the instruction
     */
    void addRecord(String label, String opCode, String[] operands,
            int lineNumber, int location) {
        SourceRecord record = new SourceRecordImp();

        record.setLabel(label);
        record.setOpCode(opCode);
        record.setLineNumber(lineNumber);
        if (opCode.equals(".STRZ")) {
            record.setLocation(location - (operands[0].length() - 2));
        } else {
            record.setLocation(location);
        }
        // if there are any operands, add each
        for (int i = 0; i < operands.length; i++) {
            record.addArg(operands[i]);
        }
        program.addRecord(record);
    }

    /**
     * Handles the first pass of the assembly process
     *
     * @param label
     *            the label of the record
     * @param opCode
     *            the op code of the record
     * @param operands
     *            the array of operands of the instruction
     */
    void processRecord(String label, String opName, String[] ops) {
        // if not a comment line
        if (label != null) {
            // if there is a label, which is not already in symbol table
            if (label != null && !program.hasSymbol(label)) {
                // obey the MAX_SYMBOLS restriction
                if (program.numberOfSymbols() < MMXIAssembler.MAX_SYMBOLS) {
                    // check for duplicate opname
                    if (opName.equals(".ORIG")) {
                    	ErrorHandler.fatalError("ERROR: Extra .ORIG record", 135);
                    }
                    // check for relative symbol
                    if (program.isRelocatable() && !opName.equals(".EQU")) {
                        program.addSymbol(label, locationCounter + 1, true);
                    } else if (opName.equals(".EQU")) {
                        if (ops.length == 1) {
                            if (ops[0].startsWith("#")
                                    || ops[0].startsWith("x"))
                            { // operand is an immediate
                                if (ArgFormat.isValid(ops[0]) == ArgType.IMMEDIATE) {
                                    program.addSymbol(label,
                                            ArgFormat.parseImmediate(ops[0]));
                                } else {
                                	ErrorHandler.fatalError("Invalid argument of .EQU", 106, lineNumber);
                                }
                            } else { // operand is a symbol
                                if (program.hasSymbol(ops[0])) {
                                    program.addSymbol(label,
                                            program.getSymbolValue(ops[0]),
                                            true);
                                } else {
                                	ErrorHandler.fatalError("Undefined symbol", 107, lineNumber);
                                }
                            }
                        } else {
                        	ErrorHandler.fatalError("Too many operands for .EQU", 108, lineNumber);
                        }
                    } else {
                        program.addSymbol(label, locationCounter + 1);
                    }
                } else {
                    ErrorHandler.warning("Maximum number of symbols reached.", 109);
                }
            } else {
            	ErrorHandler.fatalError("Symbol " + label + " already defined.", 110);
            }
        }

        // if instruction is a valid machine or pseudo op
        if (MachineOpTable.hasOpCode(opName) || PseudoOpTable.hasOpCode(opName)) {
            // if locationCounter needs to be updated
            if (!opName.equals(".END") && !opName.equals(".EQU")) {
                if (opName.equals(".STRZ")) {
                    if (ops.length == 1 && ops[0].startsWith("\"")
                            && ops[0].endsWith("\""))
                    {
                        // allocate storage for the string
                        locationCounter += ops[0].length() - 1;
                    } else {
                    	ErrorHandler.fatalError("Invalid .STRZ operand", 111, lineNumber);
                    }
                } else if (opName.equals(".BLKW")) {
                    if (ops.length == 1) {
                        if (ArgFormat.isValid(ops[0]) == ArgType.IMMEDIATE) {
                            int words = ArgFormat.parseImmediate(ops[0]);
                            if (words > 0) {
                                // skip the specified number of words
                                locationCounter += words;
                            } else {
                            	ErrorHandler.fatalError("Expected positive .BLKW operand", 112, lineNumber);
                            }
                        } else if (ArgFormat.isValid(ops[0]) == ArgType.SYMBOL) {
                            if (program.hasSymbol(ops[0])
                                    && !program.isRelative(ops[0])
                                    && program.getSymbolValue(ops[0]) > 0)
                            {
                                locationCounter +=
                                        program.getSymbolValue(ops[0]);
                            } else {
                            	ErrorHandler.fatalError("Expected positive previously defined absolute symbol after .BLKW", 113, lineNumber);
                            }
                        } else {
                        	ErrorHandler.fatalError("Invalid .BLKW argument", 114, lineNumber);
                        }
                    } else {
                    	ErrorHandler.fatalError("Too many operands for .BLKW", 115, lineNumber);
                    }
                } else {
                    ++locationCounter;
                }
            }
            // create SourceRecord and add it to program
            addRecord(label, opName, ops, lineNumber, locationCounter);
        } else {
        	ErrorHandler.fatalError("Invalid operation", 116, lineNumber);
        }

        // check for literals, give error when it's not the last operand
        // of an LD instruction
        int i = 0;
        while (i < ops.length) {
            if (ArgFormat.getArgType(ops[i]) == ArgType.LITERAL) {
                if (i != 1 || !opName.equals("LD")) {
                	ErrorHandler.fatalError("Unexpected literal", 117, lineNumber);
                } else {
                    if (ArgFormat.isValid(ops[i]) == ArgType.LITERAL) {
                        int literal =
                                ArgFormat.parseImmediate(ops[i].substring(1));
                        // add only if not already defined
                        if (!program.hasLiteral(literal)) {
                            if (program.numberOfLiterals() < MMXIAssembler.MAX_LITERALS) {
                                program.addLiteral(literal);
                            } else {
                                ErrorHandler.warning("Maximum number of literals reached", 118);
                            }
                        }
                    } else {
                    	ErrorHandler.fatalError("Invalid literal", 119, lineNumber);
                    }
                }
            }
            ++i;
        }
    }

    // ------------------------------------------------------------------------
    // PUBLIC METHODS
    // ------------------------------------------------------------------------

    @Override
    public Program parse(BufferedReader input, int maxRecords) {
        try {
            String inputLine = input.readLine();
            lineNumber = 1;

            // get to first non-comment line
            while (inputLine != null && inputLine.charAt(0) == ';') {
                inputLine = input.readLine();
                ++lineNumber;
            }

            if (!isWellFormed(inputLine)) {
            	ErrorHandler.fatalError("Malformed record", 119, lineNumber);
            }
            String opName = getOpName(inputLine);
            String label = getLabel(inputLine);
            String[] ops = getOperands(inputLine);

            validateRecord(inputLine, label, opName, ops);

            // check that the opfield contains .ORIG
            if (!opName.equals(".ORIG")) {
            	ErrorHandler.fatalError("Expected .ORIG", 120, lineNumber);
            }

            // set segment info
            program.setSegmentName(label);

            if (ops.length == 0) {
                program.isRelocatable(true);
            } else {
                if (ops.length == 1) {
                    if (ops[0].length() >= 2 && ops[0].length() <= 5
                            && ops[0].charAt(0) == 'x'
                            && ArgFormat.isValid(ops[0]) == ArgType.IMMEDIATE)
                    {
                        int arg = ArgFormat.parseImmediate(ops[0]);
                        program.setFirstAddress(arg);
                        locationCounter = arg - 1;
                    } else {
                    	ErrorHandler.fatalError("Invalid start address.", 121);
                    }
                } else {
                	ErrorHandler.fatalError("Too many operands in .ORIG record.", 122);
                }
            }

            // add .ORIG record to program
            addRecord(label, opName, ops, lineNumber, locationCounter);

            // Loop over the records until we either hit the maximum or run out.
            while (program.numberOfRecords() < maxRecords
                    && (inputLine = input.readLine()) != null)
            {
                // Verify that the formatting is correct
                if (!isWellFormed(inputLine)) {
                	ErrorHandler.fatalError("Malformed record", 132, lineNumber);
                }

                // If not a comment, process line
                if (inputLine.charAt(0) != ';') {
                    label = getLabel(inputLine);
                    opName = getOpName(inputLine);
                    ops = getOperands(inputLine);

                    // Make sure that the inputLine is not an .END record.
                    if (opName.equals(".END")) {
                        break;
                    }

                    validateRecord(inputLine, label, opName, ops);
                    processRecord(label, opName, ops);
                }
                ++lineNumber;
            }

            if (program.numberOfRecords() == maxRecords) {
                ErrorHandler.warning("Maximum number of records reached.", 123);
            }

            // check if last line before stopping was a .END record
            if (getOpName(inputLine).equals(".END")) {
                if (label != null) {
                	ErrorHandler.fatalError("Encountered label at .END record.", 124);
                }
                ops = getOperands(inputLine);

                // see if an execution address is specified, and if so make
                // sure it's valid
                if (ops == null) {
                    // no execution address specified, set it to start address
                    if (program.isRelocatable()) {
                        program.setExecAddress(0);
                    } else {
                        program.setExecAddress(program.getFirstAddress());
                    }
                } else {
                    // set execution address to the one specified
                    if (ops.length <= 1) {
                        int execAddress = program.getFirstAddress();
                        if (ops.length > 0
                                && ops[0].length() <= 5
                                && ops[0].charAt(0) == 'x'
                                && ArgFormat.isValid(ops[0]) == ArgType.IMMEDIATE)
                        {
                            execAddress = ArgFormat.parseImmediate(ops[0]);
                        } else if (ops.length > 0
                                && ArgFormat.isValid(ops[0]) == ArgType.SYMBOL
                                && program.hasSymbol(ops[0]))
                        {
                            program.setExecAddress(program
                                    .getSymbolValue(ops[0]));
                        }
                        else {
                        	ErrorHandler.fatalError("Invalid start execution address.", 125);
                        }
                        program.setExecAddress(execAddress);
                    } else {
                        System.out.println(ops.length);
                        ErrorHandler.fatalError("Too many operands in .END record.", 126);
                    }
                }
                addRecord((String) null, ".END", ops, lineNumber,
                        locationCounter);
                // replace addresses of literals with their correct values
                program.startLiteralsAt(locationCounter + 1);
                program.setLength(locationCounter + program.numberOfLiterals()
                        - program.getFirstAddress() + 1);
            } else {
            	ErrorHandler.fatalError("No .END record.", 1);
            }
        } catch (IOException e) {
            ErrorHandler.warning("Could not read from input file: " + e, 128);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
            	ErrorHandler.fatalError("Problem closing input file: " + e, 129);
            }
        }
        return program;
    }
}