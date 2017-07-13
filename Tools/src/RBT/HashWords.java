package RBT;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;

public class HashWords {

  private static CommandLine parseArguments(String[] args) {
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

  private static void safeBufferedWrite(BufferedWriter bw, String s) {
    try {
      bw.write(s);
    } catch(Exception e) {
      System.exit(-1);
    }
  }

  public static void main(String[] args) throws IOException {

    CommandLine cmd = parseArguments(args);

    // If a string was given on CLI, hash, print, exit
    if(cmd.getArgs().length > 0) {
      String strToHash = Arrays.stream(cmd.getArgs()).collect(Collectors.joining(" "));
      System.out.println("Input: " + strToHash);
      System.out.println("Output: " + Table.createShaHash(strToHash));
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
            map(Table::createShaHash).
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
          map(Table::createShaHash).
          forEach(System.out::println);
    }


    // Open the output file
//    Writer writer = null;
//    if(outputFile != null) {
//       writer = new BufferedWriter(new OutputStreamWriter(
//          new FileOutputStream(outputFile), "utf-8"));
////      Writer writer = new BufferedWriter(new FileWriter(outputFile));
//    }



    // Take a line from 'inputFile', hash, and print output





//    // Open the input file, and write
//    try (BufferedReader br = new BufferedReader(new FileReader(input_file))) {
//      String line;
//      while((line = br.readLine()) != null) {
//        writer.write(Table.createShaHash(line) + "\n");
//      }
//      writer.close();
//    }
  }
}
