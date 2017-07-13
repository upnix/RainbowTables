package RBT;

import java.util.Scanner;

public class Search {
  private Table rbt;

  Search(Table rbt) {
    this.rbt = rbt;
  }

  // Take hash from user, search the table
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

  // Attempt to find hash provided from user
  protected String keyFromHash(String searchHash) {
    String curHash = ""; // Hash being examined

    // Loop through each position in the chain
    for (int i = 0; i < rbt.chainLength; i++) {
      curHash = rbt.hashReduceStep(searchHash, i);
      if (rbt.hashToKey.containsKey(curHash)) {
        // Double check head of the chain leads to the desired hash
        String chainHeadKey = rbt.hashToKey.get(curHash);
        String targetKey = rbt.keyHashStep(chainHeadKey, (rbt.chainLength - i - 1));
        if (Table.createShaHash(targetKey).equals(searchHash)) {
          return targetKey;
        }
      }
    }

    // Not found
    return "";
  }
}