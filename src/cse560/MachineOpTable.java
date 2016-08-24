package cse560;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the descriptions of each instruction for the MMXI architecture.
 * Clients can look up the pertinent information for each instruction by
 * querying this component with the operation's mnemonic string (e.g., "ADD" or
 * "DBUG").
 * <p>
 * Note that this class violates this project's typical rule to have an
 * interface or, if that's not possible, an abstract class for every component.
 * However, since every method for this class is a class method, implementing an
 * interface or abstract class would be meaningless.
 * <p>
 * <b>Model:</b> Since it's purpose is entirely as a reference table, this
 * component's model is simply a map from strings to the following structure:
 * <ul>
 * <li>{@code integer template} - The essential bit pattern for the operation.
 * See "Templating" below.</li>
 * <li>{@code integer numArgs} - The number of arguments this operation takes.</li>
 * <li>{@code array of (integer, integer) args} - The position and length of
 * each argument, stored in an array of two-tuples. The position indicates the
 * position of the least significant bit in the template.</li>
 * </ul>
 *
 * <h1>Templating</h1>
 *
 * The template provides the "skeleton" of the instruction. In the template, all
 * bits that must be on are on, all bits that must be off are off, and
 * everything else is set to zero. This allows the operands to be inserted into
 * the template using a bitwise-OR operation.
 *
 * <h2>Examples</h2>
 *
 * <h3>AND</h3>
 *
 * <ul>
 * <li>{@code template = 0101 0000 0000 0000}</li>
 * <li>{@code numArgs = 3}</li>
 * <li>{@code args = &lt;(9, 3), (6, 3), (0, 6)&gt;}</li>
 * </ul>
 * Note that ADD and AND are special since the last operand may be either a
 * five-bit immediate or a register. The programmer indicates which type of
 * argument she wants by setting the sixth bit to on in the former case and off
 * in the latter.
 * <p>
 * The {@code MachineOp} component handles this discrepancy by treating the
 * third argument as a six-bit field: one bit for the immediate flag and the
 * remaining five bits for either the register or the immediate. <b>Clients of
 * this component are expected to handle this as a special case, constructing a
 * full six-bit argument with the first bit set appropriately.</b>
 *
 * <h3>BRNZ</h3>
 * <ul>
 * <li>{@code template = 0000 1010 0000 0000}</li>
 * <li>{@code numArgs = 1}</li>
 * <li>{@code args = &lt;(0, 9)&gt;}</li>
 * </ul>
 *
 * @author Igor Tolkachev
 *
 */
public final class MachineOpTable {
    /**
     * Simple structure to hold a machine op's instruction format.
     *
     * @author Joel McCance
     */
    private static final class MachineOp {
        /** Template for this instruction. */
        public int template;

        /** Number of arguments this instruction takes. */
        public int numArgs;

        /** Array of (position, length) tuples. */
        public ArgFormat[] args = new ArgFormat[MachineOpTable.MAX_ARGS];

        /** True iff the last argument of this op can be a relative symbol. */
        public boolean allowRelative;

        /** Number of arguments added to the MachineOp so far. */
        private int argCount = 0;

        /**
         * Initializes a new {@code MachineOp} with the specified template and
         * number of arguments.
         *
         * @param template
         *            The template bit pattern as an integer.
         * @param numArgs
         *            The number of arguments this machine operation expects.
         */
        public MachineOp(int template, int numArgs, boolean allowRelative) {
            this.template = template;
            this.numArgs = numArgs;
            this.allowRelative = allowRelative;
        }

        /**
         * Adds an argument to the end of the argument list.
         *
         * @param arg
         *            The argument to add.
         */
        public void addArgument(ArgFormat arg) {
            // The number of added arguments should not exceed numArgs.
            assert argCount <= numArgs;

            args[argCount] = arg;
            ++argCount;
        }
    }

    /** Maximum number of arguments an instruction can take. */
    public static final int MAX_ARGS = 3;

    /** Mapping of machine-op mnemonics to their format. */
    private static Map<String, MachineOp> opTable =
            new HashMap<String, MachineOp>();

    static {
        // Each argument is commented to provide a first line of defense against
        // errors in the template or argument format. If the initialization
        // doesn't match the comments, the initialization is probably wrong. If
        // the comments don't match the MMXI manual, be sure to fix the
        // comments!
        //
        // The comments below take the following format:
        // OPNAME
        // Template: [Essential bit pattern of the instruction]
        // Args:     Argument format. Dashes are bits that don't contain
        //              arguments. Bits with a number N in them contain the Nth
        //              argument.

        // ADD
        // Template: 0001 AAAB BBCC CCCC
        // Args:     ---- 1112 2233 3333
        opTable.put("ADD", new MachineOp(0x1000, 3, false));
        opTable.get("ADD").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("ADD").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("ADD")
                .addArgument(
                        new ArgFormat(0, 6, ArgCategory.REGISTER,
                                ArgCategory.IMMEDIATE));

        // AND
        // Template: 0101 0000 0000 0000
        // Args:     ---- 1112 2233 3333
        opTable.put("AND", new MachineOp(0x5000, 3, false));
        opTable.get("AND").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("AND").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("AND")
                .addArgument(
                        new ArgFormat(0, 6, ArgCategory.REGISTER,
                                ArgCategory.IMMEDIATE));

        // BRN
        // Template: 0000 1000 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRN", new MachineOp(0x0800, 1, true));
        opTable.get("BRN")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRZ
        // Template: 0000 0100 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRZ", new MachineOp(0x0400, 1, true));
        opTable.get("BRZ")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRP
        // Template: 0000 0010 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRP", new MachineOp(0x0200, 1, true));
        opTable.get("BRP")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRNZ
        // Template: 0000 1100 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRNZ", new MachineOp(0x0c00, 1, true));
        opTable.get("BRNZ").addArgument(
                new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRNP
        // Template: 0000 1010 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRNP", new MachineOp(0x0a00, 1, true));
        opTable.get("BRNP").addArgument(
                new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRZP
        // Template: 0000 0110 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRZP", new MachineOp(0x0600, 1, true));
        opTable.get("BRZP").addArgument(
                new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // BRNZP
        // Template: 0000 1110 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("BRNZP", new MachineOp(0x0e00, 1, true));
        opTable.get("BRNZP").addArgument(
                new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // DBUG
        // Template: 1000 0000 0000 0000
        opTable.put("DBUG", new MachineOp(0x8000, 0, false));

        // JMP
        // Template: 0100 0000 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("JMP", new MachineOp(0x4000, 1, true));
        opTable.get("JMP")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // JMPR
        // Template: 1100 0000 0000 0000
        // Args:     ---- ---1 1122 2222
        opTable.put("JMPR", new MachineOp(0xc000, 2, false));
        opTable.get("JMPR").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("JMPR").addArgument(new ArgFormat(0, 6, ArgCategory.INDEX));

        // JSR
        // Template: 0100 1000 0000 0000
        // Args:     ---- ---1 1111 1111
        opTable.put("JSR", new MachineOp(0x4800, 1, true));
        opTable.get("JSR")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // JSRR
        // Template: 1100 0000 0000 0000
        // Args:     ---- ---1 1122 2222
        opTable.put("JSRR", new MachineOp(0xc800, 2, false));
        opTable.get("JSRR").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("JSRR").addArgument(new ArgFormat(0, 6, ArgCategory.INDEX));

        // LD
        // Template: 0010 0000 0000 0000
        // Args:     ---- 1112 2222 2222
        opTable.put("LD", new MachineOp(0x2000, 2, true));
        opTable.get("LD")
                .addArgument(new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("LD").addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // LEA
        // Template: 1110 0000 0000 0000
        // Args:     ---- 1112 2222 2222
        opTable.put("LEA", new MachineOp(0xe000, 2, true));
        opTable.get("LEA").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("LEA")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // LDI
        // Template: 1010 0000 0000 0000
        // Args:     ---- 1112 2222 2222
        opTable.put("LDI", new MachineOp(0xa000, 2, true));
        opTable.get("LDI").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("LDI")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // LDR
        // Template: 0110 0000 0000 0000
        // Args:     ---- 1112 2233 3333
        opTable.put("LDR", new MachineOp(0x6000, 3, false));
        opTable.get("LDR").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("LDR").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("LDR").addArgument(new ArgFormat(0, 6, ArgCategory.INDEX));

        // NOT
        // Template: 1001 0000 0000 0000
        // Args:     ---- 1112 22-- ----
        opTable.put("NOT", new MachineOp(0x9000, 2, false));
        opTable.get("NOT").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("NOT").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));

        // RET
        // Template: 1101 0000 0000 0000
        opTable.put("RET", new MachineOp(0xd000, 0, false));

        // ST
        // Template: 0011 0000 0000 0000
        // Args:     ---- 1112 2222 2222
        opTable.put("ST", new MachineOp(0x3000, 2, true));
        opTable.get("ST")
                .addArgument(new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("ST").addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // STI
        // Template: 1011 0000 0000 0000
        // Args:     ---- 1112 2222 2222
        opTable.put("STI", new MachineOp(0xb000, 2, true));
        opTable.get("STI").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("STI")
                .addArgument(new ArgFormat(0, 9, ArgCategory.ADDRESS));

        // STR
        // Template: 0111 0000 0000 0000
        // Args:     ---- 1112 2233 3333
        opTable.put("STR", new MachineOp(0x7000, 3, false));
        opTable.get("STR").addArgument(
                new ArgFormat(9, 3, ArgCategory.REGISTER));
        opTable.get("STR").addArgument(
                new ArgFormat(6, 3, ArgCategory.REGISTER));
        opTable.get("STR").addArgument(new ArgFormat(0, 6, ArgCategory.INDEX));

        // TRAP
        // Template: 1111 0000 0000 0000
        // Args:     ---- ---- 1111 1111
        opTable.put("TRAP", new MachineOp(0xf000, 1, false));
        opTable.get("TRAP").addArgument(
                new ArgFormat(0, 8, ArgCategory.TRAPVECT));
    }

    /**
     * Return the length of the {@code index}-th argument of {@code opCode}.
     *
     * @param opCode
     *            The human-readable mnemonic for the desired instruction.
     * @param index
     *            The index of the argument whose length is desired.
     * @return {@code this[opCode][index].length}
     */
    public static ArgFormat getArgFormat(String opCode, int index) {
        return opTable.get(opCode).args[index];
    }

    /**
     * Returns true if the opcode allows relative symbols.
     *
     * @param opCode
     *            The opcode to check.
     *
     * @return {@code this[opCode].allowRelative}
     */
    public static boolean allowsRelative(String opCode) {
        return opTable.get(opCode).allowRelative;
    }

    /**
     * Return the number of args taken by {@code opCode}.
     *
     * @param opCode
     *            The human-readable mnemonic for the desired instruction.
     * @return {@code this[opCode].numArgs}
     */
    public static int getNumArgs(String opCode) {
        return opTable.get(opCode).numArgs;
    }

    /**
     * Return the template for {@code opCode}.
     *
     * @param opCode
     *            The human-readable mnemonic for the desired instruction.
     * @return {@code this[opCode].template}
     */
    public static int getTemplate(String opCode) {
        return opTable.get(opCode).template;
    }

    /**
     * Returns whether the provided opcode exists in the machine opcode table.
     *
     * @param opCode
     *            The opcode string to look up.
     * @return True iff {@code opcode} is a key for {@code this.opTable}.
     */
    public static boolean hasOpCode(String opCode) {
        return opTable.containsKey(opCode);
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MachineOpTable() {
        throw new UnsupportedOperationException();
    }
}
