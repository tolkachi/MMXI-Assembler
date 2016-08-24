package cse560;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores the position and length of an argument as well as which argument types
 * are valid.
 *
 * @author Igor Tolkachev
 */
public final class ArgFormat {
    /**
     * Determines what type of argument {@code arg} is. Assumes that {@code arg}
     * has already been determined to be a valid argument. Use this method when
     * you know your argument is good and just want to know what kind it is. Use
     * {@code isValid} when you have a string and do not yet know whether it's a
     * well-formed argument or not.
     * <p>
     * <b>Requires:</b> {@code isValid(arg) != ArgType.BAD}
     *
     * @param arg
     *            The string to determine the type of.
     *
     * @return The {@code ArgType} of {@code arg}.
     */
    public static ArgType getArgType(String arg) {
        switch (arg.charAt(0)) {
        case 'R':
            return ArgType.REGISTER;

        case '=':
            return ArgType.LITERAL;

        case 'x':
        case '#':
            return ArgType.IMMEDIATE;

        case '"':
            return ArgType.STRING;

        default:
            return ArgType.SYMBOL;
        }
    }

    /**
     * Detects roughly what type of argument {@code arg} is and returns the
     * appropriate {@link ArgType}.
     * <p>
     * WARNING: This method currently has some pretty massive caveats in the
     * name of efficiency. Specifically, it does not check if any type is
     * well-formed beyond examining the first character.
     * <p>
     * Examples:
     * <ul>
     * <li>"Radical" would return ArgType.REGISTER.
     * <li>"=fish" would return ArgType.LITERAL
     * <li>"xylophone" would return ArgType.IMMEDIATE
     * </ul>
     * ...and so on.
     * <p>
     * <b>Requires:</b> {@code |arg| > 0}
     *
     * @param arg
     * @return
     */
    public static ArgType isValid(String arg) {
        // Null or zero-length arguments are bad arguments.
        if (arg == null || arg.length() == 0) {
            return ArgType.BAD;
        }

        switch (arg.charAt(0)) {
        // Detect registers
        case 'R':
            if (arg.matches("^R[0-7]$")) {
                return ArgType.REGISTER;
            }
            break;

        // Detect literals
        case '=':
            // A literal is an '=' followed by an immediate
            if (ArgFormat.isValid(arg.substring(1)) == ArgType.IMMEDIATE) {
                return ArgType.LITERAL;
            }
            break;

        // Detect hex immediates
        case 'x':
            if (arg.matches("^x[0-9a-fA-F]{1,4}$")) {
                return ArgType.IMMEDIATE;
            }
            break;

        // Detect decimal immediates
        case '#':
            if (arg.matches("^#-?\\d{1,5}$")) {
                return ArgType.IMMEDIATE;
            }
            break;

        // Detect strings
        case '"':
            if (arg.matches("^\".*\"$")) {
                return ArgType.STRING;
            }
            break;

        // Detect symbols
        default:
            if (arg.matches("^[A-Za-z][A-Za-z0-9]{0,6}$")) {
                return ArgType.SYMBOL;
            }
        }

        return ArgType.BAD;
    }

    /**
     * Parses an MMXI immediate. Can be used to parse an MMXI immediate by
     * passing in all but the '='.
     * <p>
     * <b>Requires:</b> {@code num} be a well-formed MMXI immediate.
     *
     * @param imm
     *            An MMXI immediate as a string with base-indicating lead
     *            character.
     *
     * @return The integer represented by {@code imm}.
     */
    public static int parseImmediate(String imm) {
        if (imm.charAt(0) == 'x') {
            return Integer.parseInt(imm.substring(1), 16);
        } else {
            return Integer.parseInt(imm.substring(1));
        }
    }

    /** Position in template of the rightmost bit of the argument. */
    private final int position;

    /** Length of argument in bits. */
    private final int length;

    /** Set of allowed categories for this argument. */
    private final Set<ArgCategory> allowedCategories = new HashSet<ArgCategory>();

    /** Mapping of ArgCategories to allowed ArgTypes. */
    private static Map<ArgType, Set<ArgCategory>> typeRules =
            new HashMap<ArgType, Set<ArgCategory>>();

    static {
        // Initialize the type rules

        typeRules.put(ArgType.IMMEDIATE, new HashSet<ArgCategory>());
        typeRules.get(ArgType.IMMEDIATE).add(ArgCategory.INDEX);
        typeRules.get(ArgType.IMMEDIATE).add(ArgCategory.TRAPVECT);
        typeRules.get(ArgType.IMMEDIATE).add(ArgCategory.ADDRESS);
        typeRules.get(ArgType.IMMEDIATE).add(ArgCategory.IMMEDIATE);

        typeRules.put(ArgType.LITERAL, new HashSet<ArgCategory>());
        typeRules.get(ArgType.LITERAL).add(ArgCategory.ADDRESS);

        typeRules.put(ArgType.REGISTER, new HashSet<ArgCategory>());
        typeRules.get(ArgType.REGISTER).add(ArgCategory.REGISTER);

        typeRules.put(ArgType.SYMBOL, new HashSet<ArgCategory>());
        typeRules.get(ArgType.SYMBOL).add(ArgCategory.REGISTER);
        typeRules.get(ArgType.SYMBOL).add(ArgCategory.INDEX);
        typeRules.get(ArgType.SYMBOL).add(ArgCategory.TRAPVECT);
        typeRules.get(ArgType.SYMBOL).add(ArgCategory.ADDRESS);
        typeRules.get(ArgType.SYMBOL).add(ArgCategory.IMMEDIATE);

        typeRules.put(ArgType.STRING, new HashSet<ArgCategory>());
        typeRules.get(ArgType.STRING).add(ArgCategory.STRING);

        typeRules.put(ArgType.BAD, new HashSet<ArgCategory>());
    }

    /**
     * Initializes a new argument with the specified state.
     *
     * @param position
     *            The rightmost bit of the argument.
     * @param length
     *            The length of the argument in bits.
     * @param types
     *            One or more argument types that this argument may be.
     */
    public ArgFormat(int position, int length, ArgCategory... types) {
        this.position = position;
        this.length = length;

        for (ArgCategory type : types) {
            this.allowedCategories.add(type);
        }
    }

    /**
     * Returns true if {@code type} is allowed for this argument.
     *
     * @param type The {@link ArgType} to check.
     * @return True iff {@code this.allowedTypes} contains {@code type}.
     */
    public boolean allows(ArgType type) {
        boolean allowed = false;

        // For each allowed category, check to see if the type rules allow
        // this type to be used in that category.
        for (ArgCategory category : this.allowedCategories) {
            if (ArgFormat.typeRules.get(type).contains(category)) {
                allowed = true;
            }
        }

        return allowed;
    }

    /**
     * Gets the length of the argument in bits.
     *
     * @return {@code this.length}
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the rightmost bit of the argument.
     *
     * @return {@code this.position}
     */
    public int getPosition() {
        return position;
    }
}