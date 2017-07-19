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
  Search(Table rbt) {
    this.rbt = rbt;
  }

  /**
   * Presents user with prompt that accepts hashes from user in 40-character hex form.
   * For each provided hash and attempt is made to find the corresponding plain-text key.
   */
  public void hashUserInterface() {
    String inputHash; // Hold user input hash
    Scanner sc = new Scanner(System.in); // Take user input

    // Prompt for a hash until one provided passes 'isValidHash()'
    do {
      System.out.print("Enter a hash to find: ");
      inputHash = sc.nextLine();
      inputHash = inputHash.toLowerCase().trim();

      if(Table.isValidHash(inputHash)) {
        System.out.println(keyFromHash(inputHash));
      } else if (!inputHash.equals("q")) {
        System.out.println("Inappropriate hash. Try again.\n");
      }
    } while (!inputHash.equals("q"));
  }

  /**
   * Attempt to find plain-text key that corresponds to hash provided by user.
   * @param searchHash 40-character hex-form hash
   * @return The plain-text key, or blank if not found
   */
  protected String keyFromHash(String searchHash) {
    byte[] curHash = null; // Hash being examined

    // Loop through each position in the chain
    byte[] searchHash_bytes = Table.hexStringToByteArray(searchHash);
    for (int i = 0; i < rbt.chainLength; i++) {
      curHash = rbt.hashReduceStep(searchHash_bytes, i);
      if (rbt.hashToKey.containsKey(curHash)) {
        // Double check head of the chain leads to the desired hash
        String chainHeadKey = rbt.hashToKey.get(curHash);
        String targetKey = rbt.keyHashStep(chainHeadKey, (rbt.chainLength - i - 1));
        if (Arrays.equals(Table.createShaHash(targetKey), searchHash_bytes)) {
          return targetKey;
        }
      }
    }

    // Not found
    return "";
  }
}