package cse560;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores the descriptions of each pseudo-op in MMXI assembly language.
 * Clients can look up the pertinent information for each instruction by
 * querying this component with the operation's mnemonic string (e.g. ".FILL")
 * <p>
 * TODO: finish description
 *
 * @author Igor Tolkachev
 *
 */
public final class PseudoOpTable {
    /**
     * Simple structure to hold a pseudo-op's information.
     *
     * @author Boyan Alexandrov
     */
    private static final class PseudoOp {

        /** True if the pseudo-op must have an argument */
        public boolean mustHaveArgument;

        /** True iff the argument of this pseudo-op can be a relative symbol */
        public boolean allowRelative;

        /** Set of allowed types of argument for this pseudo-op */
        private final Set<ArgType> allowedTypes = new HashSet<ArgType>();

        /**
         * Initializes a new {@code PseudoOp} with the parameters.
         *
         * @param mustHaveArg
         * @param allowRelative
         * @param types
         */
        public PseudoOp(boolean mustHaveArg, boolean allowRelative, ArgType... types) {
            this.mustHaveArgument = mustHaveArg;
            this.allowRelative = allowRelative;

            for (ArgType type : types) {
                this.allowedTypes.add(type);
            }
        }
    }

    /** Mapping of machine-op mnemonics to their format. */
    private static Map<String, PseudoOp> opTable =
            new HashMap<String, PseudoOp>();

    static {
        // fill opTable with data for all the pseudo-ops
        opTable.put(".ORIG", new PseudoOp(false, false, ArgType.IMMEDIATE));
        opTable.put(".END", new PseudoOp(false, true, ArgType.IMMEDIATE, ArgType.SYMBOL));
        opTable.put(".EQU", new PseudoOp(true, false, ArgType.IMMEDIATE, ArgType.SYMBOL));
        opTable.put(".FILL", new PseudoOp(true, true, ArgType.IMMEDIATE, ArgType.SYMBOL));
        opTable.put(".STRZ", new PseudoOp(true, false, ArgType.STRING));
        opTable.put(".BLKW", new PseudoOp(true, false, ArgType.IMMEDIATE, ArgType.SYMBOL));
    }

    /**
     * Returns true if the opcode allows relative symbols.
     *
     * @param opCode The opcode to check.
     *
     * @return {@code this[opCode].allowRelative}
     */
    public static boolean allowsRelative(String opCode) {
        return opTable.get(opCode).allowRelative;
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
     * Returns whether {@code type} is allowed for the pseudo-op {@code opCode}
     *
     * @param opCode
     *            The opcode string to look up.
     * @param type
     *
     * @return True iff {@code opcode} is a key for {@code this.opTable}.
     */
    public static boolean isArgTypeAllowed(String opCode, ArgType type) {
        return opTable.get(opCode).allowedTypes.contains(type);
    }

    /**
     * Return the number of args taken by {@code opCode}.
     *
     * @param opCode
     *            The human-readable mnemonic for the desired instruction.
     * @return {@code this[opCode].numArgs}
     */
    public static boolean mustHaveArgument(String opCode) {
        return opTable.get(opCode).mustHaveArgument;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PseudoOpTable() {
        throw new UnsupportedOperationException();
    }
}