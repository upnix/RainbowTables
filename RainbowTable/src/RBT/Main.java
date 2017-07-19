package RBT;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


/** CLI program to allow the creation and searching of rainbow tables. */
public class Main {

  final static boolean DEBUG = false;

  // Used for clearing the terminal screen
  private static final String ANSI_CLS = "\u001b[2J";
  private static final String ANSI_HOME = "\u001b[H";

  /** Creates an 'Options' object with the options necessary for this program. */
  private static Options buildOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder()
            .longOpt("search-file")
            .desc("File with hashes to search")
            .hasArg()
            .argName("file")
            .required(false)
            .build()
    );

    return options;
  }

  /** Main method which kicks off the necessary steps to generate a table and allow searching */
  public static void main(String[] args) {
    // For debugging
    String[] debug_args = {
        "--key-length", "3",
        "--chain-length", "10",
        "--table-length", "4600"};
    if(DEBUG) {
      args = debug_args;
    }

    // Create new 'Config' object
    Config cfg = new Config(buildOptions(), args);

    // Take 'cfg' and generate a table
    Table rbt = new Table(cfg);

    // Clear screen
    System.out.print(ANSI_CLS + ANSI_HOME);
    System.out.flush();
    // Show summary of table
    rbt.printSummary();

    // Create a new 'Search' object
    Search rbt_search = new Search(rbt);

    // Have we been asked to search a supplied file?
    if(cfg.containsArg("search-file")) {
      List<String> hashes = null;
      try {
        hashes = Files.readAllLines(Paths.get(cfg.getArg("search-file")));
      } catch(Exception e) {
        System.out.println("Probably couldn't open your hash file.");
        System.exit(-1);
      }
      int found = 0;
      System.out.println("Searching for " + hashes.size() + " hashes");
      for(String hash : hashes) {
        String key = rbt_search.keyFromHash(hash);
        if(!key.isEmpty()) {
          System.out.println(hash + ":" + key);
          found++;
        }
      }
      System.out.println("\n-- SEARCH STATS --");
      System.out.format("Hashes found: %d/%d = %f%%%n",
          found, hashes.size(), ((float)found/hashes.size())*100);
    } else {
      // Allow user to search
      rbt_search.hashUserInterface();
    }
  }
}