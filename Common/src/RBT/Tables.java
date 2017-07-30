package RBT;

import java.security.MessageDigest;

public class Tables {
  /**
   * Converts a byte array to hex for familiar looking SHA-1 hashes.<br>
   * Credit to: https://stackoverflow.com/a/311179/3846437
   * @param b byte[] of length 20
   * @return Hex string of 40-characters
   */
  protected static String byteArrayToHexString(byte[] b) {
    String result = "";
    for (int i = 0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }

  /**
   * Converts a hex string representing an SHA-1 hash to a <code>byte[]</code>.
   * This is compatible with what {@link java.security.MessageDigest#digest()} returns.
   * @see MessageDigest#digest()
   * @param s Hex string of 40-characters
   * @return byte[] of length 20
   */
  protected static byte[] hexStringToByteArray(String s) {
    byte[] result = new byte[(s.length()/2)];
    int front = 0; // Front position in String 's'
    int back = 2; // Rear position in String 's'
    // Step through String 's', 2 characters at a time
    for(int i = 0; i < (s.length()/2); i++) {
      String hex = s.substring(front, back); // Two characters from 's'
      result[i] = (byte)Integer.parseInt(hex, 16);
      front = back;
      back += 2;
    }

    return result;
  }

  /**
   * Creates an SHA-1 hash of supplied string in <code>byte[]</code> form.
   * Uses {@link java.security.MessageDigest#digest()}.
   * @see java.security.MessageDigest#digest()
   * @param plaintext String to hash
   * @return byte[] of length 20
   */
  protected static byte[] createShaHash(String plaintext) {
    MessageDigest shaHash = null;
    try {
      shaHash = MessageDigest.getInstance("SHA-1");
    } catch(Exception e) {
      System.exit(-1);
    }
    shaHash.update(plaintext.getBytes());

    return shaHash.digest();
  }

  /**
   * Reduce then hash, <code>n</code> times. Static version.<br>
   * This method takes a hash, treats it as if it were at some location on a chain, and then
   * runs it along the chain until the final position.<br>
   * So, consider steps ('n') to be counted <b><i>from the right side of the chain</i></b>.<p>
   * For example, with a chain of - K1:H1:K2:H2:K3:H3:...:K10:H10
   * n = 3 would be three in from the right.
   * This places 'plaintext' at the position of hashToKey(hash, 6, 5), or the 7th chain from
   * the left in a chain of length 10.
   * salt = 10 - 3 - 1 = 6</p>
   * @see Config
   * @param initialHash Starting hash value
   * @param n Number of times to hash, reduce
   * @param rbtcfg Rainbow table 'Config'
   * @return Hash in byte[] form that is 'n' steps from 'initialHash'
   */
  protected static byte[] hashToHashStep(byte[] initialHash, int n, Config rbtcfg) {
    // Being asked to make 0 steps, which is just 'initialHash'
    if(n == 0) {
      return initialHash;
    }
    // Prevent stepping off the end of the chain
    if (n > rbtcfg.getChainLen() - 1) {
      System.out.println("Trying to hash/reduce off chain");
      System.exit(-1);
    }
    int salt = rbtcfg.getChainLen() - n - 1; // Appropriate salt for present chain location
    byte[] hash = initialHash; // Holds hash that's ultimately returned
    // Reduce (hashToKey()) then hash (createShaHash()), 'n' times
    for (int i = 0; i < n; i++) {
      hash = createShaHash(hashToKey(hash, salt, rbtcfg));
      salt++;
    }

    return hash;
  }

  /**
   * Hash reduction algorithm. Produces different result for the same hash depending on supplied
   * <code>salt</code>. Static version.<br>
   * When considered against the *Step() methods, this could be considered a single step.
   * @param hash Hash in byte[] form
   * @param salt int that acts as modifier to method's output
   * @param rbtcfg Rainbow table 'Config'
   * @return Plain-text key
   */
  protected static String hashToKey(byte[] hash, int salt, Config rbtcfg) {
    String reducedKey = ""; // String we will produce from 'hash'

    // 'salt' will range from 0 to (chainLength-1). When ALLOWABLE_CHARS.length < chainLength
    // 'salt' can grow to be larger than the number of allowable characters. We want to avoid the
    // scenario where salt%ALLOWABLE_CHARS.length wraps around to a value that has already been
    // seen. Should this happen we'll only have ALLOWABLE_CHARS.length unique reduction functions,
    // rather than chainLength-1. Here I attempt to solve this.
    // TODO: Not convinced the order of operations is as I expect here.
    if(salt >= rbtcfg.ALLOWABLE_CHARS.length) {
      salt += salt%rbtcfg.ALLOWABLE_CHARS.length+1;
    }

    // TODO: Consider this (now quite outdated) different implementation.
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

    /* The length of the hash is divided by the plain-text key length, and stored in 'chunks'.
     * The remainder is stored in 'extra'.
     * The 'byte' elements of the hash are divided evenly among "'chunks' number"
     * of bins, with 1 character from 'extra' added to each bin until none remain.
     * Each byte in a bin is XOR'd with one another (intended to create a unique result), 0xff is
     * added (ensures a positive integer), 'salt' is added, then modular arithmetic used to take
     * the new integer and turn it into a character appropriate for the keyspace.
     */

    int chunks = hash.length / rbtcfg.getKeyLen(); // Integer division
    int extra = hash.length % chunks;

    int leftBound;
    int rightBound = 0;
    // The length of the plain-text key is used to divide the number of bytes in the hash
    // as evenly as possible.
    for (int i = 0; i < rbtcfg.getKeyLen(); i++) {
      leftBound = rightBound;
      if (extra > 0) {
        // Include one extra byte
        rightBound = leftBound + chunks + 1;
        extra--;
      } else {
        rightBound = leftBound + chunks;
      }
      byte subByte = 0;
      // Step through bytes for this group, XORing with 'subByte'
      for(int k = leftBound; k < rightBound; k++) {
        subByte ^= hash[k];
      }
      // TODO: Replace with StringBuilder, although I question the value of this currently
      reducedKey += rbtcfg.ALLOWABLE_CHARS[ ((subByte&0xff) + salt) % rbtcfg.ALLOWABLE_CHARS.length ];
    }

    return reducedKey;
  }

  /**
   * Performs basic checks that the supplied hash is valid.
   * @param hash Supplied hash as 40-character hex string
   * @param rbtcfg Rainbow table 'Config'
   * @return Validity of hash
   */
  protected static boolean isValidHexHash(String hash, Config rbtcfg) {
    // Is the length equal to 'HASHLEN'? Is it proper hexadecimal?
    return hash.matches("^[a-f0-9]{" + rbtcfg.HASHLEN + "}");
  }

  /**
   * Hash, reduce, n times, along a chain that starts with <code>initialKey</code>.
   * Static version.<br>
   * 'n' is a location on the chain, <b><i>when counting from left to right</i></b>, which should
   * point to the location of a target key (which isn't held in this method). The intent is that,
   * given the head of the chain, 'initialKey', 'n' steps in will be the particular key that the
   * caller is looking for.
   *
   * @param initialKey Initial plain-text key
   * @param n Number of times to hash, reduce
   * @param rbtcfg Rainbow table 'Config'
   * @return Result of 'n' hash, reduce steps
   */
  protected static String keyToKeyStep(String initialKey, int n, Config rbtcfg) {
    // Prevent stepping off the end of the chain
    if (n > rbtcfg.getChainLen() - 1) {
      System.out.println("Trying to reduce/hash off chain");
      System.exit(-1);
    }
    String key = initialKey; // Key that's ultimately returned
    // Hash (createShaHash) then reduce (hashToKey) 'n' times
    for (int i = 0; i < n; i++) {
      key = hashToKey(createShaHash(key), i, rbtcfg);
    }

    return key;
  }

}
