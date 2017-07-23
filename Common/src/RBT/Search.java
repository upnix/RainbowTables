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

  /**
   * Constructs a <code>Search</code> object that works against the provided rainbow table.
   * @see Table
   * @param rbt Rainbow table, as represented by 'Table' object
   */
  public Search(Table rbt) {
    this.rbt = rbt;
  }

  /**
   * Presents user with prompt that accepts hashes from user in 40-character hex form.
   * For each provided hash and attempt is made to find the corresponding plain-text key.
   */
  public void searchUserInterface() {
    String inputHash; // Hold user input hash
    Scanner sc = new Scanner(System.in); // Take user input

    // Prompt for a hash until one provided passes 'isValidHexHash()'
    do {
      System.out.print("Enter a hash to find: ");
      inputHash = sc.nextLine();
      inputHash = inputHash.toLowerCase().trim();

      if(Table.isValidHexHash(inputHash)) {
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
    byte[] searchHash_bytes = Table.hexStringToByteArray(searchHash);
    // CHRIS
    for(int i = 0; i < rbt.keyToHashMap.size(); i++) {
      for (int j = 0; j < rbt.chainLength; j++) {
        curHash = rbt.hashToHashStep(searchHash_bytes, j);
        if (rbt.hashToKeyMap.get(i).containsKey(curHash)) {
          // Double check head of the chain leads to the desired hash
          String chainHeadKey = rbt.hashToKeyMap.get(i).get(curHash);
          String targetKey = rbt.keyToKeyStep(chainHeadKey, (rbt.chainLength - j - 1));
          if (Arrays.equals(Table.createShaHash(targetKey), searchHash_bytes)) {
            System.out.println("Key found in table: " + i);
            return targetKey;
          }
        }
      }
    }


    // Not found
    return "";
  }
}