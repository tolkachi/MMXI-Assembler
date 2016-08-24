package cse560;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import org.junit.Test;

public class AssemblerImpTest {

    @Test
    public void test() throws IOException {
        int location = 0;
        int length = 0;

        Program prog = new ProgramImp();
        prog.setSegmentName("Lab2");
        prog.setExecAddress(0);
        prog.setFirstAddress(0);
        prog.addSymbol("Foo", 10, true);
        prog.isRelocatable(true);
        prog.addLiteral(25);

        SourceRecord record = new SourceRecordImp();
        record.setOpCode(".ORIG");
        record.setLineNumber(1);
        record.addArg("x0000");	

        prog.addRecord(record);

        record = new SourceRecordImp();
        record.setOpCode("JMP");
        record.setLineNumber(2);
        record.addArg("Foo");
        record.setLocation(location);

        prog.addRecord(record);
        ++location;

        record = new SourceRecordImp();
        record.setOpCode(".STRZ");
        record.setLineNumber(3);
        record.addArg("\"hi! \"");
        record.setLocation(location);

        prog.addRecord(record);
        location += 5;

        record = new SourceRecordImp();
        record.setOpCode(".FILL");
        record.setLineNumber(4);
        record.addArg("#16");
        record.setLocation(location);

        prog.addRecord(record);
        ++location;

        record = new SourceRecordImp();
        record.setOpCode("LD");
        record.setLineNumber(5);
        record.addArg("R3");
        record.addArg("=#25");
        record.setLocation(location);

        prog.addRecord(record);
        ++location;

        record = new SourceRecordImp();
        record.setLabel("Fish");
        record.setOpCode(".EQU");
        record.setLineNumber(6);
        record.addArg("#16");

        prog.addRecord(record);

        record = new SourceRecordImp();
        record.setOpCode(".END");
        record.setLineNumber(7);
        record.addArg("x0000");

        prog.addRecord(record);

        prog.setLength(10);
        prog.startLiteralsAt(location);

        prog.writeStateTo(System.err);

        Assembler assembler = new AssemblerImp();

        StringWriter objFile = new StringWriter();
        assembler.assemble(new BufferedWriter(objFile), new BufferedWriter(
                new OutputStreamWriter(System.out)), prog);

        System.out.println(objFile);
    }
}
