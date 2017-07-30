package RBT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * CLI program that accepts plain-text strings and outputs hashes as 40-character hex strings.
 *
 * @see Config
 * @see Search
 * @see Table
 * @author Chris Cameron
 */
public class HashWords {
  /**
   * Generates a <code>CommandLine</code> object, given CLI arguments passed by user.
   * @see CommandLine
   * @param args CLI arguments from main(String[]) method
   * @return Parsed CLI arguments in the form of a 'CommandLine' object
   */
  protected static CommandLine parseArguments(String[] args) {
    Options options = new Options();

    options.addOption(
        Option.builder("i")
            .longOpt("input")
            .desc("Input file")
            .hasArg()
            .argName("file")
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("Output file")
            .hasArg()
            .argName("file")
            .required(false)
            .build()
    );

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch(Exception e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("HashWords [<word>]", options, true);
      System.exit(-1);
    }

    return cmd;
  }

  /**
   * A <code>BufferedWriter</code> encapsulated in a <code>try{}</code> block.
   * This is done to allow the use of <code>BufferedWriter.write(String)</code> in a
   * lambda expression.
   * @see BufferedWriter#write(String)
   * @param bw BufferedWriter
   * @param s Hash in 40-character hex form to be written
   */
  protected static void safeBufferedWrite(BufferedWriter bw, String s) {
    try {
      bw.write(s);
    } catch(Exception e) {
      System.exit(-1);
    }
  }

  /**
   * Main method which reads input, generates necessary hashes, and writes output to screen or file.
   * @param args CLI arguments from main(String[]) method
   * @throws IOException Unable to create supplied output file
   */
  public static void main(String[] args) throws IOException {
    CommandLine cmd = parseArguments(args);

    // If a string was given on CLI, hash, print, exit
    if(cmd.getArgs().length > 0) {
      String strToHash = Arrays.stream(cmd.getArgs()).
          collect(Collectors.joining(" "));
      System.out.println("Input: " + strToHash);
      System.out.println("Output: " + Tables.byteArrayToHexString(Tables.createShaHash(strToHash)));
      System.exit(0);
    }

    // If input file is given, check it can be read
    File inputFile = null;
    if(cmd.hasOption("input")) {
      inputFile = new File(cmd.getOptionValue("input"));

      if(!inputFile.canRead()) {
        System.out.println("Cannot read file: " + cmd.getOptionValue("input"));
        System.exit(-1);
      }
    } else {
      // At some point I would read from STDIN if there wasn't an input file.
      // At this point, I'll just exit.
      System.exit(0);
    }

    // If output file is given, obtain a BufferedWriter
    File outputFile = null;
    if(cmd.hasOption("output")) {
      try {
        final BufferedWriter outputWriter = Files.newBufferedWriter(
            Paths.get(cmd.getOptionValue("output")), StandardCharsets.UTF_8);
        // Write hashes to file
        Files.lines(inputFile.toPath()).
            map(Tables::createShaHash).
            map(Tables::byteArrayToHexString).
            forEach(h -> safeBufferedWrite(outputWriter, h + "\n"));
        outputWriter.close();
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Cannot create specified output file: " +
            cmd.getOptionValue("output"));
        System.exit(-1);
      }
    } else {
      // Write to stdout
      Files.lines(inputFile.toPath()).
          map(Tables::createShaHash).
          map(Tables::byteArrayToHexString).
          forEach(System.out::println);
    }
  }
}