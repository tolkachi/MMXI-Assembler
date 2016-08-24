package cse560;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main program of the MMXI Assembler.  Provides a command-line user
 * interface, loads object files into the parser and gives a Program component and a BufferedWriter pointing to an output file to the assembler.
 * <p>
 * Command line arguments/options accepted by the MMXIAssembler are:
 * <ul>
 * <li>{@code -d} - Dumps the post-parse file to filename.dump.</li>
 * <li>{@code -M N} - Terminate execution after $N$ instructions. Default: 2000 steps.</li>
 * <li>{@code -f filename} - The name of the file to be executed. (Required)</li>
 * <li>{@code -s N} - Set the maxmium number of symbols to $N$.</li>
 * <li>{@code -L N} - Set the maximum numver of literals to $N$.</li>
 * </ul>
 * If a required option is missing, a nonexistent option is selected (e.g., "{@code -X}"), or a provided option is used incorrectly
 * (e.g., "{@code -M asdfj}"), the program will print a usage message and exit.
 * <p>
 * Command-line arguments are parsed using the JOpt Simple Library.  A link to the documentation for JOpt Simple is provided
 * in the introduction to the Programmer's Guide.
 *
 * @author Igor Tolkachev
 *
 */

public class MMXIAssembler {

	public static int MAX_SYMBOLS = 100;
	public static int MAX_LITERALS = 50;
	public static String fileName = null;

	/**
	 * @param args - Command line arguments passed in by the user.
	 */
	public static void main(String[] args) {

		Parser machine = new ParserImp2();
		Assembler assembler = new AssemblerImp();
		OptionParser optParser = new OptionParser("dM:s:L:f:");
		OptionSet options;
		Program program;

		//Set default assembler mode.
		String mode = "DEFAULT";

		//Variables for file I/O.
		BufferedReader input = null;
		File inputFile = null;
		BufferedWriter listing = null, output = null;

		int maxSteps = 2000;

		try{
			options = optParser.parse(args);

			//If -d is set, mark mode to DUMP to dump the post-parse file later.
			if (options.has("d")) {
				mode = "DUMP";
			}

			//If -M is set, set the maximum instructions to its argument.
			if (options.has("M")) {
				maxSteps = Integer.parseInt((String) options.valueOf("M"));
			}

			// If -s is set, set the maximum number of symbols to N.
			if (options.has("s")) {
				MAX_SYMBOLS = Integer.parseInt((String) options.valueOf("s"));
			}

			//If -L is set, set the maximum number of liteals to N.
			if (options.has("L")) {
				MAX_LITERALS = Integer.parseInt((String) options.valueOf("L"));
			}

			//If -f is not set, exit with a usage message.  Unable to execute instructions without an input file.
			if (!options.has("f") || options.valueOf("f") == null) {
				System.err.println("[ERROR 201] No input file specified.  Please specify an input file the -f option.");
				MMXIAssembler.printOptions();
				System.exit(0);
			}
			else {
				inputFile = new File((String) options.valueOf("f"));
				fileName = (String) options.valueOf("f");
			}

		} catch(OptionException e) {
			MMXIAssembler.printOptions();
			System.exit(1);
		}

		//Check to see if the specified input file exists.
		if (!inputFile.exists()) {
			ErrorHandler.fatalError("Input file does not exist", 202);
		}

		try {
			input = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			ErrorHandler.fatalError("Could not open input file", 203);
		}

		//Pass the parser the input file and the number of maximum instructions.
		program = machine.parse(input, maxSteps);

		//Dump the post parse file if the mode selected was -d
		if (mode == "DUMP") {
			MMXIAssembler.dumpParsed(program);
		}

		try {

			listing = new BufferedWriter(new FileWriter("listing.txt"));
		} catch (IOException e) {
			ErrorHandler.fatalError("Could not open listing file", 204);
		}

		try {
			output = new BufferedWriter(new FileWriter("output.txt"));
		} catch (IOException e) {
			ErrorHandler.fatalError("Could not open output file", 205);
		}

		//Pass the assembler the BufferedWriter and a program component.
		try {
			assembler.assemble(output, listing, program);
		} catch (IOException e) {

		}
	}


	/**
	 * Outputs the post-parse file to filename.dump.
	 */
	private static void dumpParsed(Program program) {
		String dumpName = fileName.concat(".dump");
		File dumpFile = new File(dumpName);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(dumpFile);
		} catch (FileNotFoundException e) {
			ErrorHandler.fatalError("Could not open dump file", 200);
		}

		program.writeStateTo(out);
	}

	/**
	 * Prints a usage message containing the format and options of the Assember.
	 */
	private static void printOptions() {
		System.out.println("Usage: java -jar \"MMXI Assembler.jar\" [options]");
		System.out.println("	-d				Dump the post-parse file o filename.dump");
		System.out.println("	-M N			Stop execution after N steps. (Default Value: 2000)");
		System.out.println("	-f filename		Execute the specified object file \"filename\"");
		System.out.println("	-s N			Set the maximum number of symbols to N.");
		System.out.println("	-L N			Set the maximum number of literals to N.");
	}
}
