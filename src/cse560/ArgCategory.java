package cse560;

/**
 * Defines categories of arguments for machine and pseudo-operations. An
 * argument category is a
 *
 * @author Igor Tolkachev
 *
 */
public enum ArgCategory {
    /** An MMXI address. */
    ADDRESS,

    /** A register. */
    REGISTER,

    /** An immediate. */
    IMMEDIATE,

    /** An index. */
    INDEX,

    /** A string literal. */
    STRING,

    /** A trap-vector. */
    TRAPVECT;
}
