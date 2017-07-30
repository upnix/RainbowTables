package RBT;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads or generates a rainbow table, given expected parameters in <code>Config</code> object.
 * Also contains static <code>Table</code> helper functions.
 *
 * @see Config
 * @see Search
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

  AtomicInteger tblN = new AtomicInteger(0);

  /**
   * A count of all possible keys, given <code>keyLength</code> and <code>ALLOWABLE_CHARS</code>.
   * @see #keyLength
   * @see Config#ALLOWABLE_CHARS
   * */
  long keySpace;

  Config tblcfg;

  /** Database connection */
  private Connection conn = null;


  /**
   * Constructs a rainbow table using parameters from <code>cfg</code>.
   * @param cfg Configuration parameters
   */
  Table(Config cfg) {
    tblcfg = cfg;

    // Set simple names for configuration options
    keyLength = tblcfg.getKeyLen();
    chainLength = tblcfg.getChainLen();
    rowCount = tblcfg.getRowCount();
    tableCount = tblcfg.getTblCount();
    allowableLength = tblcfg.ALLOWABLE_CHARS.length;
    keySpace = (long) Math.pow(allowableLength, keyLength);

    // Connect to the database
    try {
      establishDBConnection();
    } catch(Exception e) {
      System.out.println("Unable to create a database!");
      e.printStackTrace();
      System.exit(-1);
    }

    // Check if the rainbow table we've been asked to create already exists. If not, create it.
    if (!existsRBTables()) {
      generateTables(rowCount);
      // TODO: Better SQLException's
      try {
        conn.commit();
      } catch(SQLException se) {
        se.printStackTrace();
      }
    }
  }

  // PROTECTED, STATIC
  /**
   * Add a hash and key to the database.
   * @param b Hash
   * @param key Plain-text key
   * @return Success or failure
   */
  protected boolean add(byte[] b, String key) {
    // Choose a random starting table for attempted insert
    int tblNum = ThreadLocalRandom.current().nextInt(0, tableCount);
    // Attempt insert on each table, if necessary
    for(int i = 0; i < tableCount; i++) {
      String add_q = "INSERT INTO KL" + key.length() + "_" + tblNum + " VALUES(?, ?)";
      try {
        PreparedStatement add_ps = conn.prepareStatement(add_q);
        add_ps.setBytes(1, b);
        add_ps.setString(2, key);
        add_ps.execute();
        return true;
      } catch(SQLException se) {
        // Assuming duplicate key, try next table.
        tblNum = (tblNum+i+1)%tableCount;
      }
    }

    return false;
  }

  protected boolean add(byte[] b, String key, int tblnum) {
    // Choose a random starting table for attempted insert
//    int tblNum = ThreadLocalRandom.current().nextInt(0, tableCount);
    // Attempt insert on each table, if necessary
//    for(int i = 0; i < tableCount; i++) {
      String add_q = "INSERT INTO KL" + key.length() + "_" + tblnum + " VALUES(?, ?)";
      try {
        PreparedStatement add_ps = conn.prepareStatement(add_q);
        add_ps.setBytes(1, b);
        add_ps.setString(2, key);
        add_ps.execute();
        return true;
      } catch(SQLException se) {
        // Assuming duplicate key, try next table.
//        tblNum = (tblNum+i+1)%tableCount;
//      }
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
      String chk_q = "SELECT 1 FROM KL" + keyLength + "_" + i + " " +
          "WHERE hashVal = ?";
      // TODO: Better SQLException's
      try {
        PreparedStatement chk_ps = conn.prepareStatement(chk_q);
        chk_ps.setBytes(1, b);
        ResultSet chk_rs = chk_ps.executeQuery();
        if(chk_rs.next()) {
          return true;
        }
      } catch(SQLException se) {
        se.printStackTrace();
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
    // TODO: Benefit from radomization?
    for(int i = 0; i < tableCount; i++) {
      String getKey_q = "SELECT hashKey FROM KL" + keyLength + "_" + i + " " +
          "WHERE hashVal = ?";
      // TODO: Better SQLException's
      try {
        PreparedStatement getKey_ps = conn.prepareStatement(getKey_q);
        getKey_ps.setBytes(1, b);
        ResultSet getKey_rs = getKey_ps.executeQuery();
        if(getKey_rs.next()) {
          return getKey_rs.getString("hashKey");
        }
      } catch(SQLException se) {
        se.printStackTrace();
      }
    }

    return "";
  }

  /**
   * Establish database connection, creating the DB if necessary.
   * @throws SQLException Failed to connect and/or create
   */
  protected void establishDBConnection() throws SQLException {
    // Create a string of configuration parameters, and hash that string for file name
    String dbName = "AC" + allowableLength +
        "KL" + keyLength +
        "CL" + chainLength +
        "RC" + rowCount +
        "TC" + tableCount;

    String connectionURL = "jdbc:derby:" + dbName + ";create=true";
    conn = DriverManager.getConnection(connectionURL);
    conn.setAutoCommit(false);
  }

  // PUBLIC
  public void close() {
    boolean gotSQLExc = false;
    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException se)  {
      if ( se.getSQLState().equals("XJ015") ) {
        gotSQLExc = true;
      }
    }
    if (!gotSQLExc) {
      System.out.println("Database did not shut down normally");
    }  else  {
      System.out.println("Database shut down normally");
    }
  }
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
    System.out.println(Arrays.deepToString(tblcfg.ALLOWABLE_CHARS));
    System.out.println();
  }

  // PROTECTED
  /**
   * Create a rainbow table of length <code>num</code>.
   */
  protected void generateTables(long num) {
    // Create the configured number ('tableCount') of DB tables
    for(int i = 0; i < tableCount; i++) {
      // Of the form: KLX_Y, where 'X' is the key length, 'Y' is the table number
      String createTbl_q = "CREATE TABLE KL" + keyLength + "_" + i + " (" +
          "hashVal CHAR(20) FOR BIT DATA PRIMARY KEY, " +
          "hashKey VARCHAR(10) NOT NULL)";
      // TODO: Fix SQLException's
      try {
        conn.createStatement().execute(createTbl_q);
        conn.commit();
      } catch(SQLException se) {
        se.printStackTrace();
      }
    }

    Runnable table_gen = () -> {
      long n = rowCount/tableCount;
      int local_tbl = tblN.getAndIncrement();
      while(n > 0) {
        String key = generateKey(); // Starting chain key
        // Produce hash from the end of a chain of length 'chainLength' that starts with 'key'
        byte[] hash = Tables.hashToHashStep(Tables.createShaHash(key), (chainLength - 1), tblcfg);
        // Try to place the new 'key' and 'hash' in one of the available tables.
        if(add(hash, key)) {
          n--;
        }
      }
    };

    Thread[] table_generators = new Thread[tableCount];
    for(int i = 0; i < tableCount; i++) {
      table_generators[i] = new Thread(table_gen);
      table_generators[i].start();
    }

    try {
      for(Thread thread : table_generators) {
        thread.join();
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
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
   * Reduce then hash, <code>n</code> times.
   * @param initialHash Starting hash value
   * @param n Number of times to hash, reduce
   * @return Hash in byte[] form that is 'n' steps from 'initialHash'
   */
//  protected byte[] hashToHashStep(byte[] initialHash, int n) {
//    return hashToHashStep(initialHash, n, chainLength, keyLength);
//  }

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
            tblcfg.ALLOWABLE_CHARS[ThreadLocalRandom.current().nextInt(0, allowableLength)];
      }

    } while (keysHashed.contains(builtString));

    return builtString;
  }


  // PRIVATE
  /**
   * Rudimentary check for all expected tables. Tables could still exist but be incomplete.
   * @return True if expected tables exist, false if not
   */
  private boolean existsRBTables() {
    // How many tables for the target key length exist?
    String tblExists_q = "SELECT COUNT(*) AS rbtables FROM " +
        "SYS.SYSTABLES WHERE TABLENAME LIKE 'KL" + keyLength + "_%'";
    // TODO: Something better needs to happen with these exceptions
    try {
      ResultSet tblExists_rs = conn.createStatement().executeQuery(tblExists_q);
      tblExists_rs.next();
      // Should have equal tables to what we excepted ('tableCount')
      if(tblExists_rs.getInt("rbtables") == tableCount) {
        return true;
      }
    } catch(SQLException se) {
      se.printStackTrace();
    }

    return false;
  }
}