package RBT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * CLI program to allow for the creation and searching of rainbow tables.
 *
 * @see Config
 * @see Search
 * @see Table
 * @see Tables
 * @author Chris Cameron
 */
public class Main {
  /**
   * Allows for successful execution without CLI arguments. "DEBUG" status does not extend
   * beyond <code>Main</code>
   * */
  static final boolean DEBUG = false;

  /** Used for clearing the terminal screen. */
  private static final String ANSI_CLS = "\u001b[2J";
  /** Used for clearing the terminal screen. */
  private static final String ANSI_HOME = "\u001b[H";

  /**
   * Creates an <code>Options</code> object with the options necessary for this program.
   * These options are in addition to the default ones added in
   * {@link Config#buildDefaultOptions(Options)}.
   * @see Options
   * @return 'Options' object with desired options
   */
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

  /**
   * Main method which kicks off the necessary steps to generate a table and allow searching.
   * @param args User supplied CLI arguments
   */
  public static void main(String[] args) {
    // For debugging
    String[] debug_args = {
        "--key-length", "5",
        "--chain-length", "50",
        "--row-count", "1000000",
        "--table-count", "5"};
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
    Search rbt_search = new Search(rbt, cfg);

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
      rbt_search.searchUserInterface();
    }
  }
}