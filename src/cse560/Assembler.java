package cse560;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Processes a valid {@code Program} object and produces an object file and
 * human-readable program listing.
 *
 * <h1>Object Files</h1>
 *
 * Object files take the same format as the "MMXI Simulator" application. Please
 * consult it's User's Guide for more information. However, this object does add
 * "relocation records" for relocatable programs. If the program is indicated to
 * be relocatable and a relative symbol is used, an "{@code M0}" or "{@code M1}"
 * will be added to the associated text record. This is to signal the
 * linker/loader that the last six or nine bits (respectively) will need to be
 * adjusted.
 *
 * <h1>Program Listing</h1>
 *
 * Listing output takes the following form:
 *
 * <blockquote>
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
 * <p>
 * If a column is invalid --- for example, a .EQU directive does not have any
 * associated memory contents --- the column will be omitted. In the case of
 * literals, which do not have an associated line number, the line-number column
 * will read "{@code ( lit)}".
 *
 * @author Igor Tolkachev
 *
 */
public interface Assembler {
    /**
     * Assembles the provided program and generates a listing.
     *
     * @param output
     *            The BufferedWriter to write the object file to.
     * @param listng
     *            The BufferedWriter to write the listing file to.
     * @param program
     *            A valid instance of {@code Program}.
     *
     */
    void assemble(BufferedWriter output, BufferedWriter listing, Program program)
            throws IOException;
}
