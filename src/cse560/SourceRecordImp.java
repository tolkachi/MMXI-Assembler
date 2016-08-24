package cse560;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SourceRecordImp implements SourceRecord {
    /** The line number in the source where this record would be found. */
    private int lineNumber = -1;

    /** The location in memory where this record will be stored. */
    private int location = -1;

    /** The label of this source record. Null if none. */
    private String label;

    /** The op-code --- machine or pseudo- --- of this source record. */
    private String opCode;

    /** Operands to the op-code, if any. */
    private final String[] args = new String[MachineOpTable.MAX_ARGS];

    /** Number of arguments added so far. */
    private int argCount = 0;

    @Override
    public void addArg(String operand) {
        assert this.argCount < MachineOpTable.MAX_ARGS;

        this.args[argCount] = operand;
        ++this.argCount;
    }

    @Override
    public String getArgAt(int index) {
        return this.args[index];
    }

    @Override
    public int getArgCount() {
        return this.argCount;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public int getLocation() {
        return this.location;
    }

    @Override
    public String getOpCode() {
        return this.opCode;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void setLocation(int location) {
        this.location = location;
    }

    @Override
    public void setOpCode(String opCode) {
        this.opCode = opCode;

        assert opCode.length() > 0;
    }

    @Override
    public String toString() {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);

        writer.printf("%4d (0x%08x): %-6s %-5s ", this.lineNumber,
                this.location, this.label, this.opCode);

        for (int i = 0; i < this.argCount; ++i) {
            writer.printf("%s", this.args[i]);

            if (i < this.argCount - 1) {
                writer.print(", ");
            }
        }

        writer.close();

        return output.toString();
    }
}
