package RBT;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/** Processes and holds rainbow table configuration data */
public class Config {
  // Configuration defaults
  public static final String DEFAULT_KEY_LEN = "5";
  public static final String DEFAULT_CHAIN_LEN = "10";
  public static final String DEFAULT_TBL_LEN = "10000";
  private Map<String,String> cmdArgs; // Map of CLI flags and corresponding arguments

  // Given only 'args', use a new 'Options' to retrieve flags and options
  public Config(String[] args) {
    cmdArgs = getOptionMap(new Options(), args);
  }

  // Use the provided 'Options' to retrieve flags/options.
  public Config(Options opt, String[] args) {
    cmdArgs = getOptionMap(opt, args);
  }

  // Adds the 'Option' arguments considered default to a supplied 'Options' object
  private static Options buildDefaultOptions(Options options) {
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
            .longOpt("table-length")
            .desc("Table length (default: " + DEFAULT_TBL_LEN + ")")
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
    options.addOption("h", "help", false, "Print this message.");

    return options;
  }

  // Parse CLI arguments
  private static CommandLine parseOptions(Options options, String[] args) {
    // TODO: printHelp() is giving 'RBT', but it may now be 'RainbowTools'

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch(Exception e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("RBT", options, true);
      System.exit(-1);
    }

    // Step in right away if help requested, then quit
    if(cmd.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("RBT", options, true);
      System.exit(-1);
    }

    return cmd;
  }

  // Creates hash map of CLI flags and corresponding arguments
  private static Map<String,String> getOptionMap(Options options, String[] args) {
    // Add the 'Option' objects considered default arguments
    options = buildDefaultOptions(options);
    CommandLine cmd = parseOptions(options, args);

    // Put command line arguments into a hash
    Map<String,String> cmdArgs = new HashMap<String,String>();
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
    if(!cmdArgs.containsKey("table-length")) {
      cmdArgs.put("table-length", DEFAULT_TBL_LEN);
    }

    return cmdArgs;
  }

  // Getters
  public int getKeyLen() {
    return Integer.decode(cmdArgs.get("key-length"));
  }
  public int getChainLen() {
    return Integer.decode(cmdArgs.get("chain-length"));
  }
  public long getTblLen() {
    return Long.decode(cmdArgs.get("table-length"));
  }
  public String getKey() {
    return cmdArgs.getOrDefault("key", null);
  }

  // A pass through of the Map method '.containsKey(key)'
  public boolean containsArg(String key) {
    return cmdArgs.containsKey(key);
  }

  // A pass through of the Map method '.get(key)'
  public String getArg(String key) {
    return cmdArgs.get(key);
  }
}
