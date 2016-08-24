package cse560;

/**
 * Specifies the type of operand. These are the types that can appear in
 * assembly code: immediates, literals, symbols, registers, and strings. Also
 * includes a type to indicate that an argument is in someway malformed.
 *
 * @author Igor Tolkachev
 */
public enum ArgType {
    /** Argument is a numerical immediate. */
    IMMEDIATE,

    /** Argument is a literal. */
    LITERAL,

    /** Argument is a symbol. */
    SYMBOL,

    /** Argument is a register. */
    REGISTER,

    /** Argument is a string. */
    STRING,

    /**
     * Argument is invalid. I.e., the argument does not conform to any valid
     * argument type.
     */
    BAD;
}
