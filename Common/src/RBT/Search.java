package RBT;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Allows for searching of rainbow tables created by <code>Table</code>.
 *
 * @see Config
 * @see Table
 * @author Chris Cameron
 */
public class Search {
  /**
   * <code>Table</code> object. This is the "rainbow table".
   * @see Table
   */
  private Table rbt;

  private Config cfg;

  /**
   * Constructs a <code>Search</code> object that works against the provided rainbow table.
   * @see Table
   * @param rbt Rainbow table, as represented by 'Table' object
   * @param cfg Rainbow table 'Config' object
   */
  public Search(Table rbt, Config cfg) {
    this.rbt = rbt;
    this.cfg = cfg;
  }

  /**
   * Presents user with prompt that accepts hashes in 40-character hex form.
   * For each provided hash an attempt is made to find the corresponding plain-text key.
   */
  public void searchUserInterface() {
    String inputHash; // Hold user input hash
    Scanner sc = new Scanner(System.in); // Take user input

    // Prompt for a hash until one provided passes 'isValidHexHash()'
    do {
      System.out.print("Enter a hash to find: ");
      inputHash = sc.nextLine();
      inputHash = inputHash.toLowerCase().trim();

      if(Tables.isValidHexHash(inputHash, cfg)) {
        long searchTime = System.currentTimeMillis();
        System.out.println(keyFromHash(inputHash));
        System.out.println((System.currentTimeMillis()-searchTime) + " milliseconds to complete.");
      } else if (!inputHash.equals("q")) {
        System.out.println("Inappropriate hash. Try again.\n");
      }
    } while (!inputHash.equals("q"));
  }

  /**
   * Attempt to find plain-text key that corresponds to hash provided by user.<br>
   * Starting from the far right (end) chain, we work our way towards the front of the chain, one
   * step at a time, running each location in the chain through <code>hashToHashStep()</code>, and
   * check the resulting hash for a match against <code>searchHash</code>.
   * @param searchHash 40-character hex-form hash
   * @return The plain-text key, or blank if not found
   */
  protected String keyFromHash(String searchHash) {
    // Loop through each position in the chain
    byte[] curHash = null; // Hash being examined
    byte[] searchHash_bytes = Tables.hexStringToByteArray(searchHash);

    // We can either step through the chain, and search each rainbow table, or search each
    // rainbow table, stepping through the chain for each. Stepping through the chain only once
    // has the advantage of only having to reduce/hash chainLength times. It's possible though
    // that by searching each table for each link in the chain, we continually are pushing
    // tables out of the cache (if that's even an issue).
    // Pick a table...
    for(int i = 0; i < rbt.tableCount; i++) {
      // Run through the chain...
      for (int j = 0; j < rbt.chainLength; j++) {
        curHash = Tables.hashToHashStep(searchHash_bytes, j, cfg);
        if(rbt.containsHash(curHash)) {
          String chainHeadKey = rbt.getHeadKey(curHash);
          String targetKey = Tables.keyToKeyStep(chainHeadKey, (rbt.chainLength - j - 1), cfg);
          if (Arrays.equals(Tables.createShaHash(targetKey), searchHash_bytes)) {
            return targetKey;
          }
        }
      }
    }

    // Not found
    return "";
  }
}