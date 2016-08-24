package cse560;

/**
 * Represents a single line of assembly code from an MMXI source file.
 * <p>
 * <b>Model:</b>
 * <ul>
 * <li>{@code integer lineNumber} - The number of the line of the source file
 * from which this record was generated.</li>
 * <li>{@code integer location} - The location in memory where this value will
 * be stored. Determined by {@link Parser}.</li>
 * <li>{@code string label} - The label of this record, if any.
 * <li>{@code string opCode} - The assembly op-code for this line. Will either
 * be a machine-op mnemonic ("ADD", "BRNZ", etc.) or a pseudo-op (".FILL",
 * ".EQU", etc.).</li>
 * <li>{@code string of strings operands} - The arguments to this operation. For
 * example, if the line were {@code ADD R0, R1, #5}, the operands would be
 * {@code <"R0", "R1", "#5">}</li>
 * </ul>
 *
 * @author Igor Tolkachev
 *
 */
public interface SourceRecord {
    /**
     * Add an operand to this record.
     * <p>
     * <b>Requires:</b> {@code 0 < |operand|}
     * <p>
     * <b>Ensures:</b> {@code operands = #operands * &lt;operand&gt;}
     *
     * @param operand
     *            A string-based operand for this record.
     */
    void addArg(String operand);

    /**
     * Returns the index-th operand.
     *
     * @param index
     *            The index of the desired operand.
     * @return {@code operands[index]}
     */
    String getArgAt(int index);

    /**
     * Return the number of operands this record has.
     *
     * @return {@code |this.operands|}
     */
    int getArgCount();

    /**
     * Return the label for this record.
     *
     * @return {@code this.label}, which is null if there is no label.
     */
    String getLabel();

    /**
     * Return the line number of this record.
     *
     * @return {@code this.lineNumber}
     */
    int getLineNumber();

    /**
     * Return the location in memory where this record will be stored.
     *
     * @return {@code this.location}
     */
    int getLocation();

    /**
     * Return the op-code for this record.
     *
     * @return {@code this.opCode}
     */
    String getOpCode();

    /**
     * Set a label for this record.
     * <p>
     * <b>Requires:</b>
     * <ul>
     * <li>{@code 0 < |label| <= 6}</li>
     * <li>{@code label} only contain alphanumeric characters.</li>
     * <li>{@code label[0]} not be 'R' or 'x'.</li>
     * </ul>
     * <p>
     * <b>Ensures:</b> {@code this.label = label}
     *
     * @param label
     *            The label of this record.
     */
    void setLabel(String label);

    /**
     * Set the line number of this {@code SourceRecord}.
     * <p>
     * <b>Requires:</b> {@code 0 <= lineNumber}
     * <p>
     * <b>Ensures:</b> {@code this.lineNumber = lineNumber}
     *
     * @param lineNumber
     *            The line number of this record.
     */
    void setLineNumber(int lineNumber);

    /**
     * Sets the location in memory where this record will be stored.
     * <p>
     * <b>Requires:</b> {@code 0 <= location < 0xffff}
     * <p>
     * <b>Ensures:</b> {@code this.location = location}
     *
     * @param location The address in memory to store this record at.
     */
    void setLocation(int location);

    /**
     * Set the op-code for this record.
     * <p>
     * <b>Requires:</b> {@code opCode} is a valid pseudo-op or machine op.
     * <p>
     * <b>Ensures:</b> {@code this.opCode = opCode}
     *
     * @param opCode
     *            The op-code for this record.
     */
    void setOpCode(String opCode);

    /**
     * Provides a human-readable form of this object. Output takes the following format:
     * <p>
     * <blockquote>"  11(0x0000): Label  OP    Arg1, Arg2, Arg3"</blockquote>
     * <p>
     * Where the columns are line number, location, label, op, and arguments.
     *
     * @return A human-readable representation of the object.
     */
    @Override
    String toString();
}
