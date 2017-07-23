package RBT;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/**
 * Processes and holds rainbow table configuration data.
 *
 * @see Search
 * @see Table
 * @see org.apache.commons.cli
 * @author Chris Cameron
 */
public class Config {
  // Configuration defaults
  /** Default key length */
  public static final String DEFAULT_KEY_LEN = "5";
  /** Default chain length */
  public static final String DEFAULT_CHAIN_LEN = "10";
  /** Default total number of rows to compute */
  public static final String DEFAULT_ROW_COUNT = "10000";
  /** Default number of tables */
  public static final String DEFAULT_TBL_COUNT = "1";

  /** <code>Map</code> of CLI flags and corresponding arguments. */
  private final Map<String,String> cmdArgs;

  /**
   * Constructs a <code>Config</code> object using CLI flags from <code>args</code>.
   * A default set of <code>Option</code>'s are generated.
   * @see Options
   * @param args CLI arguments from main(String[]) method
   */
  public Config(String[] args) {
    cmdArgs = getOptionMap(new Options(), args);
  }

  /**
   * Constructs a <code>Config</code> object using CLI flags from <code>args</code> and
   * the passed <code>Options</code> object.
   * @see Options
   * @param opt Options object with non-default Option values
   * @param args CLI arguments from main(String[]) method
   */
  public Config(Options opt, String[] args) {
    cmdArgs = getOptionMap(opt, args);
  }

  // PUBLIC
  /**
   * A pass through of the method <code>Map#containsKey(Object)</code>.
   * @see Map#containsKey(Object)
   * @param key CLI argument name.
   * @return True if key passed on CLI
   */
  public boolean containsArg(String key) {
    return cmdArgs.containsKey(key);
  }

  /**
   * A pass through of the method <code>Map#get(Object)</code>.
   * @see Map#get(Object)
   * @param key CLI argument name.
   * @return CLI argument passed to key
   */
  public String getArg(String key) {
    return cmdArgs.get(key);
  }

  /**
   * Return length of chains to be contained in table.
   * @return Chain length
   */
  public int getChainLen() {
    return Integer.decode(cmdArgs.get("chain-length"));
  }

  /**
   * Return the plain-text passed via the <code>--key</code> flag, if any.
   * @return Key
   */
  public String getKey() {
    return cmdArgs.getOrDefault("key", null);
  }

  /**
   * Return length of plain-text keys.
   * @return Key length
   */
  public int getKeyLen() {
    return Integer.decode(cmdArgs.get("key-length"));
  }

  /**
   * Return the number of total rows to generate.
   * @return Total rows to generate
   */
  public long getRowCount() {
    return Long.decode(cmdArgs.get("row-count"));
  }

  /**
   * Return the number of tables to generate.
   * @return Number of tables
   */
  public int getTblCount() { return Integer.decode(cmdArgs.get("table-count")); }


  // PROTECTED
  /**
   * Adds the <code>Option</code> arguments considered default to a supplied
   * <code>Options</code> object.
   * @see Option
   * @see Options
   * @param options Options object that default Option objects will be added to
   * @return Options object with default Option objects appended
   */
  protected Options buildDefaultOptions(Options options) {
    options.addOption(
        Option.builder()
            .longOpt("key-length")
            .desc("Key length (default: " + DEFAULT_KEY_LEN + ")")
            .hasArg()
            .argName("NUM")
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder()
            .longOpt("row-count")
            .desc("Total rows to generate (default: " + DEFAULT_ROW_COUNT + ")")
            .hasArg()
            .argName("NUM")
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder()
            .longOpt("chain-length")
            .desc("Chain length (default: " + DEFAULT_CHAIN_LEN + ")")
            .hasArg()
            .argName("NUM")
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder()
            .longOpt("table-count")
            .desc("Number of rainbow tables generated (default: " + DEFAULT_TBL_COUNT + ")")
            .hasArg()
            .argName("NUM")
            .required(false)
            .build()
    );

    options.addOption("h", "help", false, "Print this message.");

    return options;
  }

  /**
   * Creates <code>Map</code> of CLI flags and corresponding arguments.
   * @param options 'Options' object
   * @param args CLI arguments from main(String[]) method
   * @return 'Map' of CLI key:value pairs
   */
  protected Map<String,String> getOptionMap(Options options, String[] args) {
    // TODO: printHelp() is giving 'RBT', but it may now be 'RainbowTools'
    // Append options considered default by 'Config'
    options = buildDefaultOptions(options);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch(Exception e) {
      printHelpAndExit(options);
    }

    // Step in right away if help requested, then quit
    if(cmd.hasOption("h")) {
      printHelpAndExit(options);
    }

    // Put command line arguments into a Map
    Map<String,String> cmdArgs = new HashMap<>();
    for(Option opt : cmd.getOptions()) {
      cmdArgs.put(opt.getLongOpt(), opt.getValue());
    }

    // If not provided we insert the default values
    if(!cmdArgs.containsKey("key-length")) {
      cmdArgs.put("key-length", DEFAULT_KEY_LEN);
    }
    if(!cmdArgs.containsKey("chain-length")) {
      cmdArgs.put("chain-length", DEFAULT_CHAIN_LEN);
    }
    if(!cmdArgs.containsKey("row-count")) {
      cmdArgs.put("row-count", DEFAULT_ROW_COUNT);
    }
    if(!cmdArgs.containsKey("table-count")) {
      cmdArgs.put("table-count", DEFAULT_TBL_COUNT);
    }

    return cmdArgs;
  }

  /**
   * Prints the Apache commons-cli help for the passed 'options', then exits.
   * @see HelpFormatter
   * @param options Generated 'Options' object
   */
  protected void printHelpAndExit(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Main", options, true);
    System.exit(-1);
  }
}