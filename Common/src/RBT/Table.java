package RBT;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

// Loads or generates a rainbow table. The user can subsequently search this table for hashes
public class Table {

  final static boolean DEBUG = true;

  // Rainbow table chains to be populated
  protected Map<String, byte[]> keyToHash = new TreeMap<>();
  protected Map<byte[], String> hashToKey = new TreeMap<>(new ByteArrayComparator());

//  public static final Character[] ALLOWABLE_CHARS = {
//      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
//      't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
//      'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
//  };
  public static final Character[] ALLOWABLE_CHARS = {
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
      't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
  };
//  public static final Character[] ALLOWABLE_CHARS = {
//      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
//      't', 'u', 'v', 'w', 'x', 'y', 'z'
//  };
  public static final int HASHLEN = 40;

  /* CONFIGURATION */
  public int keyLength;
  public int chainLength;
  public long tableLength;
  public long keySpace; // Calculated from above parameters
  public String fileName; // Hashtables, serialized

  Table(Config cfg) {
    keyLength = cfg.getKeyLen();
    chainLength = cfg.getChainLen();
    tableLength = cfg.getTblLen();
    keySpace = (long) Math.pow(ALLOWABLE_CHARS.length, keyLength);

    /*
     * Generate the file name a previously generated rainbow table would have
     * used, given the parameters set. May or may not exist.
     */
    // Create a string of configuration parameters, and hash that string for file name
    String cfgString =
        Arrays.deepToString(ALLOWABLE_CHARS)
            + keyLength
            + chainLength
            + tableLength;
    fileName = "RT_" + cfgString.hashCode() + ".ser";

    // If computed hash tables aren't on disk, compute them
    if (!loadHashMap()) {
      createTable(tableLength);
      writeHashMap();
    }
  }

  /* PUBLIC */
  public void printSummary() {
    System.out.println("Rainbow table loaded with the following parameters:");
    System.out.println("  Configured -");
    System.out.printf("    * %13s: %,d%n", "Table length", hashToKey.size());
    System.out.printf("    * %13s: %,d%n", "Chain length", chainLength);
    System.out.printf("    * %13s: %,d%n", "Key length", keyLength);
    System.out.println();
    System.out.println("  Static -");
    System.out.printf("    * %13s: %,d%n", "Key space size", keySpace);
    System.out.printf("    * %13s:%n", "Allowable character set");
    // TODO: I know this output looks bad, but it appears the effort to wrap the output is more
    // trouble than I think it's worth right now.
    System.out.println(Arrays.deepToString(ALLOWABLE_CHARS));
    System.out.println();
  }


  /* PROTECTED */
  // Converts a byte array to hex for familiar looking SHA hashes
  // Credit to: https://stackoverflow.com/a/311179/3846437
  protected static String byteArrayToHexString(byte[] b) {
    String result = "";
    for (int i = 0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }

  protected static byte[] hexStringToByteArray(String s) {
    byte[] result = new byte[(s.length()/2)];
    int front = 0;
    int back = 2;
    for(int i = 0; i < (s.length()/2); i++) {
      String hex = s.substring(front, back);
      result[i] = (byte)Integer.parseInt(hex, 16);
      front = back;
      back += 2;
    }

    return result;
  }

  protected static byte[] createShaHash(String plaintext) {
    MessageDigest shaHash = null;
    try {
      shaHash = MessageDigest.getInstance("SHA-1");
    } catch (Exception e) {
      System.out.println(e);
      System.exit(-1);
    }
    shaHash.update(plaintext.getBytes());

    return shaHash.digest();

//    return byteArrayToHexString(shaHash.digest());
  }

  // Create a rainbow table of length 'num'
  protected void createTable(long num) {
    System.out.format("Generating table of size %,d%n", num);
    System.out.format("%s\t%s\t%s\t%s\t%s%n",
        "Elapsed", "Rows remaining", "Rows complete/time", "Collisions", "Successful H/s");
    long startTime = System.currentTimeMillis() / 1000l;
    long execTime = System.currentTimeMillis() / 1000l;
    long printTime = 30; // Print every 30 seconds
    int collisions = 0; // Number of key collisions
    int prevCollisions = 0;
    long prevNum = num; // The value of 'num' at the previous time of reporting.
    while (num > 0) {
      if( ((System.currentTimeMillis() / 1000l)-execTime) >= printTime && DEBUG) {
        System.out.format("%d\t%d\t%d\t%d\t%d%n",
            System.currentTimeMillis()/1000l - startTime,
            num,
            prevNum-num,
            collisions-prevCollisions,
            (prevNum-num)*chainLength/(System.currentTimeMillis()/1000l - execTime));
        execTime = System.currentTimeMillis()/1000l;
        prevCollisions = collisions;
        prevNum = num;
      }
      String key = generateKey(); // Starting chain key
      byte[] hash = hashReduceStep(createShaHash(key), (chainLength - 1));

      // If the hash already exists in hashToKey, generate a new chain
      // I believe we should only collide here if two different
      // starting keys end up generating the same end hash.
      if(hashToKey.containsKey(hash)) {
//        System.out.println("Collided - " + hashToKey.get(hash) + ":" + hash +
//            " and " + key + ":" + hash);
        collisions++;
      } else {
        keyToHash.put(key, hash);
        hashToKey.put(hash, key);
        num--;
      }
    }

    System.out.println("Collisions: " + collisions);
  }

  // Reduce then hash, n times. Returns hash
  protected byte[] hashReduceStep(byte[] initialHash, int n) {
    return hashReduceStep(initialHash, n, chainLength, keyLength);
  }
  protected static byte[] hashReduceStep(byte[] initialHash, int n, int cl, int kl) {
    if (n > cl - 1) {
      System.out.println("Trying to hash/reduce off chain");
      System.exit(-1);
    }
    int chainIndex = cl - n - 1; // Current place in the chain
    byte[] hash = initialHash; // Holds hash that's ultimately returned
    for (int i = 0; i < n; i++) {
      hash = createShaHash(Table.hashReduce(hash, chainIndex, kl));
      chainIndex++;
    }

    return hash;
  }

  // Randomly generates keys according to user-set configuration parameters
  protected String generateKey() {
    String builtString; // Holds the key as it's built
    boolean success = false;

    // Loop on the key building process until a key in generated that doesn't
    // match an existing key.
    do {
      builtString = "";
      for (int i = 0; i < keyLength; i++) {
        builtString +=
            ALLOWABLE_CHARS[ThreadLocalRandom.current().nextInt(0, ALLOWABLE_CHARS.length)];
      }

    } while (keyToHash.containsKey(builtString));

    return builtString;
  }

  // Hash reduction algorithm, produces different result depending on 'salt'
  protected String hashReduce(byte[] hash, int salt) {
    return Table.hashReduce(hash, salt, keyLength);
  }
  protected static String hashReduce(byte[] hash, int salt, int kl) {
    String reducedKey = ""; // String we produce from hash

    // So a salt of 26 doesn't have two sets of hashReduce functions on an allowable
    // character set of 52.
    if(salt >= ALLOWABLE_CHARS.length) {
      salt += salt%ALLOWABLE_CHARS.length+1;
    }

    // TODO: Different implementation.
    // Slower than the one below, doesn't seem to generate as many matching
    // keys somehow? I like it because it seems cleaner. Will come back to.
    // BigInteger hashAsInt = new BigInteger(hash, 16);
    // BigInteger keySpaceBI = new BigInteger(Long.toString(keySpace));
    // long hashMod = hashAsInt.mod(keySpaceBI).longValue();

    // for(int i = 0; i < kl; i++) {
    // 	long divisor = (long)Math.pow(ALLOWABLE_CHARS.length, i);
    // 	long modDividend = (long)(hashMod/divisor)+salt;
    // 	reducedKey += ALLOWABLE_CHARS[(int)(modDividend%ALLOWABLE_CHARS.length)];
    // }
    // System.out.println(reducedKey);

    /* The length of the hash is divided by 'kl', and stored in 'chunks'.
     * The remainder is stored in 'extra'.
     * The characters of the hash are divided evenly among "'chunks' number"
     * of bins, with 1 character from 'extra' added to each bin until none remain.
     * Each bin is converted to integers from their hex values, 'salt' is added,
     * then modular arithmetic used to take the new integer and turn it into a
     * character appropriate for the keyspace.
     */

//    int chunks = hash.length() / kl; // Integer division
    int chunks = hash.length / kl;
    int extra = hash.length % chunks;
//    int extra = hash.length() % chunks; // Amount left over from above division

    // If the key length is too short, the chunk sizes will be very large,
    // and the conversion process of hex -> dec will overflow.
    try {
      // Loop through the number of spots in a key
      int strStart = 0; // Start bound of hash substring
      int strEnd = 0; // End bound of hash substring
      for (int i = 0; i < kl; i++) {
        strStart = strEnd;
        if (extra > 0) {
          // Include one extra character
          strEnd = strStart + chunks + 1;
          extra--;
        } else {
          strEnd = strStart + chunks;
        }
        // Add one character to 'reducedKey'
//        reducedKey += ALLOWABLE_CHARS[
//            (int)
//                ((Long.parseLong(hash.substring(strStart, strEnd), 16) + salt)
//                    % ALLOWABLE_CHARS.length)];
        byte subByte = 0;
        for(int k = strStart; k < strEnd; k++) {
          subByte ^= hash[k];
        }
        reducedKey += ALLOWABLE_CHARS[((subByte&0xff) + salt) % ALLOWABLE_CHARS.length];
      }
    } catch (NumberFormatException e) {
      System.out.println("Choose a longer key length.");
      System.exit(-1);
    }

    return reducedKey;
  }

  // Basic checks that a hash is valid
  protected static boolean isValidHash(String hash) {
    // Is the length equal to 'HASHLEN'? Is it proper hexadecimal?
    // TODO: Is inserting HASHLEN this way cool?
    return hash.matches("^[a-f0-9]{" + HASHLEN + "}");
  }

  // Hash then reduce, n times, along a chain that starts with 'initialKey'. Returns key
  protected String keyHashStep(String initialKey, int n) {
    return Table.keyHashStep(initialKey, n, chainLength, keyLength);
  }
  protected static String keyHashStep(String initialKey, int n, int cl, int kl) {
    if (n > cl - 1) {
      System.out.println("Trying to reduce/hash off chain");
      System.exit(-1);
    }
    String key = initialKey; // Key that's ultimately returned
    for (int i = 0; i < n; i++) {
      key = Table.hashReduce(createShaHash(key), i, kl);
    }

    return key;
  }

  // Read in a previously computed hash maps, if they exist.
  private boolean loadHashMap() {
    try {
      MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new FileInputStream(fileName));
      // Load 'keyToHash'
      int mapLength = unpacker.unpackMapHeader();
      for (int i = 0; i < mapLength; i++) {
        keyToHash.put(unpacker.unpackString(), unpacker.readPayload(unpacker.unpackBinaryHeader()));
      }
      // Load 'hashToKey'
      mapLength = unpacker.unpackMapHeader();
      for (int i = 0; i < mapLength; i++) {
        hashToKey.put(unpacker.readPayload(unpacker.unpackBinaryHeader()), unpacker.unpackString());
      }
    } catch (IOException e) {
      // The file doesn't exist
      return false;
    }
    return true;
  }

  // Serialize 'hashToKey' and 'keyToHash', write to disk
  private void writeHashMap() {
    try {
      MessagePacker packer = MessagePack.newDefaultPacker(new FileOutputStream(fileName));

      // keyToHash
      packer.packMapHeader(keyToHash.size());
      for (Map.Entry<String, byte[]> entry : keyToHash.entrySet()) {
        packer.packString(entry.getKey());
        packer.packBinaryHeader(entry.getValue().length);
        packer.writePayload(entry.getValue());
      }
      // hashToKey
      packer.packMapHeader(hashToKey.size());
      for (Map.Entry<byte[], String> entry : hashToKey.entrySet()) {
        packer.packBinaryHeader(entry.getKey().length);
        packer.writePayload(entry.getKey());
        packer.packString(entry.getValue());
      }
      packer.close();
    } catch (Exception e) {
      // We can continue, but their table is lost after program termination
      System.out.println("Error writing to disk.");
      e.printStackTrace();
    }
  }
}

// Credit to: https://stackoverflow.com/a/38552674/3846437
class ByteArrayComparator implements Comparator<byte[]> {
  @Override
  public int compare(byte[] o1, byte[] o2) {
    int result = 0;
    int maxLength = Math.max(o1.length, o2.length);
    for (int index = 0; index < maxLength; index++) {
      byte o1Value = index < o1.length ? o1[index] : 0;
      byte o2Value = index < o2.length ? o2[index] : 0;
      int cmp     = Byte.compare(o1Value, o2Value);
      if (cmp != 0) {
        result = cmp;
        break;
      }
    }
    return result;
  }
}