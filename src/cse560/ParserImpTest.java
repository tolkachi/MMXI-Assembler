package cse560;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import org.junit.Test;

public class ParserImpTest {
	/** The instance of an InterpreterImp object to test. */
	private final ParserImp p = new ParserImp();

    @Test
    public void testValidateRecord(){
    	String record = "Lab2EG   .ORIG   x30B0";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "Lab2EG   .ORIG   x30B0; ;asdfasdfasd afaws";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "count    .FILL   #4";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "begni    LD      ACC,count    ;a;lewkjfadsl;ka";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	          
    	record = "         LEA     R7,msg";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "loop     TRAP    x22       ;asfl;kasd";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         ADD     ACC,ACC,#-1\t     ;asdfw";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         BRP     loop";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         JMP     Next";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "msg      .STRZ   \"hi! \"";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "Next     AND     R0,R0,x0       ;areuipq38e";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         NOT     R0,R0       ;q238ehrapidn";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         ST      R0,Array        ;fq38e9aindjz";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         LEA     R5,Array";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         LD      R6,=#100    ;4fq3";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    
    	record = "         STR     R0,R5,#1       ;eaviuq248";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         TRAP    x25";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "ACC      .EQU    #1";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "Array    .BLKW   #3";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         .FILL   x10";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    	
    	record = "         .END    begin";
    	p.validateRecord(record, p.getLabel(record), p.getOpName(record), p.getOperands(record));
    }
}
