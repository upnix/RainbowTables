package RBT;

import java.util.Arrays;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ChainTools {
  private static Options buildOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder()
            .longOpt("key")
            .desc("Key")
            .hasArg()
            .argName("STRING")
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder()
            .longOpt("print-chain")
            .desc("Print the full, untruncated chain")
            .hasArg(false)
            .required(false)
            .build()
    );
    options.addOption(
        Option.builder()
            .longOpt("print-collisions")
            .desc("Print the collisions that occur during table generation")
            .hasArg(false)
            .required(false)
            .build()
    );
    return options;
  }

  static void createChainFromKey(String key, int chain_len, int key_len) {
    String hash = Table.createShaHash(key);
    System.out.println(key + ":" + hash);
    for(int i = 0; i < (chain_len-1); i++) {
      key = Table.hashReduce(hash, i, key_len);
      hash = Table.createShaHash(key);
      System.out.println(key + ":" + hash);
    }
  }

  static void printKeyCollisions(String key, Config config) {

  }



  public static void main(String[] args) {
    // For debugging
//    String[] debug_args = {"--key", "car",
//        "--key-length", "3",
//        "--chain-length", "10",
//        "--table-length", "14000",
//        "--print-chain"};
//    args = debug_args;
    // Create new 'Config' object
    System.out.println(Arrays.toString(args));
    Config toolConfig = new Config(buildOptions(), args);

    if(toolConfig.containsArg("print-chain")) {
      createChainFromKey(toolConfig.getKey(), toolConfig.getChainLen(), toolConfig.getKeyLen());
    }
//    } else if(toolConfig.containsArg("print-collisions")) {
//
//    }


  }
}