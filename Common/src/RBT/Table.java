package RBT;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * Loads or generates a rainbow table, given expected parameters in <code>Config</code> object.
 * Also contains static <code>Table</code> helper functions.
 *
 * @see Config
 * @see Search
 * @see Tables
 * @author Chris Cameron
 */
public class Table {
  /** Displays extra table generation data. */
  static final boolean DEBUG = true;

  /**
   * A running list of plain-text keys that have been used as chain-heads
   * @see java.util.TreeSet
   */
  Set<String> keysHashed = new TreeSet<>();
  /**
   * LinkedList of <code>TreeMap</code>'s that contain hashes in <code>byte[]</code> form to
   * plain-text keys.
   * @see java.util.TreeMap
   */
  List<TreeMap<byte[], String>> hashToKeyMap = new LinkedList<>();

  /** Simple name for a default parameter from <code>Config</code> object. */
  int allowableLength;
  /** Simple name for a default parameter from <code>Config</code> object. */
  int keyLength;
  /** Simple name for a default parameter from <code>Config</code> object. */
  int chainLength;
  /** Simple name for a default parameter from <code>Config</code> object. */
  long rowCount;
  /** Simple name for a default parameter from <code>Config</code> object. */
  int tableCount;
  /**
   * A count of all possible keys, given <code>KEYLENGTH</code> and <code>ALLOWABLE_CHARS</code>.
   * @see #keyLength
   * @see Config#ALLOWABLE_CHARS
   * */
  long keySpace;
  /** File location of the serialized <code>TreeMap</code> tables. */
  String fileName;
  /**
   * <code>Config</code> object that represents the table to be generated.
   * @see Config
   */
  Config cfg;

  /**
   * Constructs a rainbow table using parameters from <code>cfg</code>.
   * @param cfg Configuration parameters
   */
  Table(Config cfg) {
    this.cfg = cfg;

    // Set simple names for configuration options
    keyLength = cfg.KEYLENGTH;
    chainLength = cfg.getChainLen();
    rowCount = cfg.getRowCount();
    tableCount = cfg.getTblCount();
    allowableLength = cfg.ALLOWABLE_CHARS.length;
    keySpace = (long) Math.pow(allowableLength, keyLength);

    // Add the requested (tableCount) number of empty TreeMap's.
    // The TreeMap(s) are the "rainbow tables"
    for(int i = 0; i < tableCount; i++) {
      hashToKeyMap.add(new TreeMap<>(new ByteArrayComparator()));
    }

    /*
     * Generate the file name a previously generated rainbow table would have
     * used, given the parameters set. May or may not exist.
     */
    // Create a string of configuration parameters, and hash that string for file name
    String cfgString =
        "AC" + allowableLength +
        "KL" + keyLength +
        "CL" + chainLength +
        "RC" + rowCount +
        "TC" + tableCount;
    fileName = cfgString + ".ser";

    // Load the rainbow table represented by 'cfgString', if it exists, otherwise compute it.
    if (existsTableFile()) {
      readTableFile();
    } else {
      // Create and load table
      generateTable(rowCount);
      // Put new table on disk for next time
      writeTableFile();
    }
  }

  // PROTECTED, STATIC
  /**
   * Add a hash and key to the table.
   * @param b Hash
   * @param key Plain-text key
   * @return Success or failure
   */
  protected boolean add(byte[] b, String key) {
    // Choose a random starting table for attempted insert
    int tbl = ThreadLocalRandom.current().nextInt(0, tableCount);
    // Attempt insert on each table, if necessary
    for(int i = 0; i < tableCount; i++) {
      if(!hashToKeyMap.get(tbl).containsKey(b)) {
        hashToKeyMap.get(tbl).put(b, key);
        return true;
      }
    }

    return false;
  }

  /**
   * Checks available tables for the passed hash.
   * @param b Hash to search tables for
   * @return Success or failure
   */
  protected boolean containsHash(byte[] b) {
    // TODO: Value in randomizing?
    for(int i = 0; i < tableCount; i++) {
      if(hashToKeyMap.get(i).containsKey(b)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the key, that is paired with the passed hash, from the head of the chain.
   * @param b Hash corresponding the key
   * @return Head key, or blank if existent
   */
  protected String getHeadKey(byte[] b) {
    // TODO: Benefit from randomizing?
    for(int i = 0; i < tableCount; i++) {
      if(hashToKeyMap.get(i).containsKey(b)) {
        return hashToKeyMap.get(i).get(b);
      }
    }

    return "";
  }

  // PUBLIC
  /** Prints table information summary. */
  public void printSummary() {
    System.out.println("Rainbow table loaded with the following parameters:");
    System.out.println("  Configured -");
    System.out.printf("    * %20s: %,d%n", "Total rows", rowCount);
    System.out.printf("    * %20s: %,d%n", "Table count", tableCount);
    System.out.printf("    * %20s: %,d%n", "Chain length", chainLength);
    System.out.printf("    * %20s: %,d%n", "Key length", keyLength);
    System.out.println();
    System.out.println("  Static -");
    System.out.printf("    * %20s: %,d%n", "Average table size", (rowCount/tableCount));
    System.out.printf("    * %20s: %,d%n", "Key space", keySpace);
    System.out.printf("    * %20s:%n", "Character set");
    // TODO: I know this output looks bad, but it appears the effort to wrap the output is more
    // trouble than it's worth right now.
    System.out.println(Arrays.deepToString(cfg.ALLOWABLE_CHARS));
    System.out.println();
  }

  // PROTECTED
  /**
   * Create a rainbow table of length <code>num</code>.
   * @param num Length of table generated
   */
  protected void generateTable(long num) {
    // Mostly for debugging
    long startTime = currentTimeSeconds();
    long curTime = startTime; // Time since current round was started
    long printTime = 15; // Print every X seconds
    int totalCollisions = 0;
    int prevCollisions = 0; // Key collisions from the previous round
    long prevNum = num; // 'num' from previous round

    if(DEBUG) {
      System.out.format("Generating table of size %,d%n", num);
      System.out.format("%s\t%s\t%s\t%s\t%s%n",
          "Elapsed", "Rows remaining", "Rows complete/time", "Collisions", "Successful H/s");
    }

    while (num > 0) {

      /*
       * START DEBUGGING - DEBUG is set and time since last round started is >= printTime
       */
      if(DEBUG && ((currentTimeSeconds())-curTime) >= printTime) {
        // "Elapsed", "Rows remaining", "Rows complete/time", "Collisions", "Successful H/s"
        System.out.format("%d\t%d\t%d\t%d\t%d%n",
            currentTimeSeconds() - startTime,
            num,
            prevNum-num,
            totalCollisions-prevCollisions,
            (prevNum-num)*chainLength/(currentTimeSeconds() - curTime));
        curTime = currentTimeSeconds();
        prevCollisions = totalCollisions;
        prevNum = num;
      }
      /*
       * END DEBUGGING
       */

      // Generate key
      String key = generateKey(); // Starting chain key
      // Produce hash from the end of a chain of length 'chainLength' that starts with 'key'
      byte[] hash = Tables.hashToHashStep(
          Tables.createShaHash(key, cfg),
          (chainLength - 1),
          cfg);
      // Try to place the new 'key' and 'hash' in one of the available tables.
      if(add(hash, key)) {
        num--;
      } else {
        if(DEBUG) {
          totalCollisions++;
        }
      }
    }
    if(DEBUG) {
      System.out.println("Collisions: " + totalCollisions);
    }
  }

  /**
   * Returns the current system time in seconds.
   * @see System#currentTimeMillis()
   * @return Current time in seconds
   */
  protected long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000L;
  }

  /**
   * Randomly generates one key according to parameters in <code>Config</code> object,
   * provided at construction.
   * @see Config
   * @return String adhering to key space constraints
   */
  protected String generateKey() {
    String builtString; // Holds the key as it's built

    // Loop on the key building process until a key in generated that doesn't
    // match an existing key.
    do {
      builtString = "";
      for (int i = 0; i < keyLength; i++) {
        builtString +=
            cfg.ALLOWABLE_CHARS[ThreadLocalRandom.current().nextInt(0, allowableLength)];
      }

    } while (keysHashed.contains(builtString));

    return builtString;
  }

  // PRIVATE

  /**
   * Check if table file(s) exist.
   * @return Success or failure
   */
  private boolean existsTableFile() {
    return Files.exists(Paths.get(fileName));
  }

  /**
   * Read in previously computed tables that match supplied <code>Config</code> object,
   * if it exists. <code>hashToKeyMap</code> is read from file.
   * @see MessageUnpacker
   * @return Success or failure
   */
  private boolean readTableFile() {
    try {
      MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new FileInputStream(fileName));
      // Load 'hashToKeyMap'
      int rbTables = unpacker.unpackArrayHeader(); // # of rainbow tables held
      for(int i = 0; i < rbTables; i++) {
        // Load the particular table
        int mapLength = unpacker.unpackMapHeader();
        for (int j = 0; j < mapLength; j++) {
          byte[] hash = unpacker.readPayload(unpacker.unpackBinaryHeader());
          String key = unpacker.unpackString();
          hashToKeyMap.get(i).put(hash, key);
        }
      }
      unpacker.close();
    } catch (IOException e) {
      // The file doesn't exist
      return false;
    }
    return true;
  }

  /**
   * Serialize rainbow table(s) contained in <code>hashToKeyMap</code>, write to disk.
   * @see MessagePack
   * @see MessagePacker
   */
  private void writeTableFile() {
    try {
      MessagePacker packer = MessagePack.newDefaultPacker(new FileOutputStream(fileName));

      // Write hashToKeyMap
      packer.packArrayHeader(hashToKeyMap.size());
      // For each rainbow table held...
      for(int i = 0; i < hashToKeyMap.size(); i++) {
        // Store this particular table
        packer.packMapHeader(hashToKeyMap.get(i).size());
        for (Map.Entry<byte[], String> entry : hashToKeyMap.get(i).entrySet()) {
          packer.packBinaryHeader(entry.getKey().length);
          packer.writePayload(entry.getKey());
          packer.packString(entry.getValue());
        }
      }
      packer.close();
    } catch (Exception e) {
      // We can continue, but their table is lost after program termination
      System.out.println("Error writing to disk.");
      e.printStackTrace();
    }
  }

  /**
   * <code>Comparator</code> for <code>byte[]</code>. This allows hashes in the form of
   * <code>byte[]</code> to be used as <code>TreeMap</code> keys.<br>
   * Credit to: https://stackoverflow.com/a/38552674/3846437
   * @see Comparator
   */
  protected static class ByteArrayComparator implements Comparator<byte[]> {
    @Override
    public int compare(byte[] o1, byte[] o2) {
      int result = 0;
      int maxLength = Math.max(o1.length, o2.length);
      for (int index = 0; index < maxLength; index++) {
        byte o1Value = index < o1.length ? o1[index] : 0;
        byte o2Value = index < o2.length ? o2[index] : 0;
        int cmp = Byte.compare(o1Value, o2Value);
        if (cmp != 0) {
          result = cmp;
          break;
        }
      }
      return result;
    }
  }
}