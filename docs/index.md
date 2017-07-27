---
---
# Overview
This page describes a project I undertook while attending the [Recurse Center](https://www.recurse.com/) from May to August, 2017. I used Java to create an implementation of [rainbow tables](https://en.wikipedia.org/wiki/Rainbow_table) - a time/memory trade off that allows for the repeated searching of large password hash spaces, a task that would otherwise be intractable.  

In its present state, my application can retrieve plain-text passwords from SHA-1 hashes, operating with 90%+ accuracy in key spaces of over 1 billion. At the bottom of this page I list methods by which the manageable key space size could be increased to significantly larger numbers.  

## Challenge
 My goal in choosing this project was to become more familiar working with computationally large problems. I wished to develop a configurable application that allowed for the efficient, repeated searching of very large password key spaces - up to the limits of the host hardware. Program overhead had remain minimal, with most execution time spent on hash generation (rather than data structure operations).   

## Solution
Using Java, I have built a program that allows a user to generate and search rainbow tables. All parameters are configurable, allowing for coverage of arbitrarily large password key spaces and allowable character sets.  In addition, I have also written standalone tools that assist in the development, understanding, and use of the main program.  

## Conclusion
The unexpected pleasure from this project came from the concept of rainbow tables themselves. My initial implementation came together quickly, however it was in analyzing results and program performance that my depth of knowledge was repeatedly challenged. Several times I reread the reference paper as unsatisfactory results were encountered, and in doing so I would find small details that I had previously missed.  

While there is little practical use of this application, as a vehicle for working on a large computational problem, it was ideal. The problem size can be readily scaled up or down and with an unlimited potential for keys and hashes, it's natural to extend this application from a single host to a distributed system.  

**_--help_ output**
![--help output]({{ site.url }}/assets/RBT_Help.png)

**User hash entry**
![User hash entry]({{ site.url }}/assets/RBT_Example1.png)

# Programming overview
## API documentation
[Javadoc documentation]({{ site.url }}/docs/)

## Third-party libraries
**[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/)**  
CLI arguments are prompted for and processed using Apache Commons CLI. As implemented, it is also possible for other programs to easily extend the arguments expected and parsed.  

**[MessagePack](http://msgpack.org/)**  
Once generated, the TreeMap data structures used to represent the rainbow table are written to disk. Should the program receive parameters that match a table previously generated, the corresponding MessagePack is loaded from disk.  

## Structure
All code exists in the 'RBT' package. Inside IntelliJ the code is broken into 3 distinct modules:
1. **Common**  
    Code common to both the primary 'RainbowTable' program, as well as the utilities in 'Tools'.

2. **RainbowTable**  
    The program for generating and allowing the user to search rainbow tables.

3. **Tools**  
    Utilities related to rainbow tables, but unnecessary for the primary program. Examples include hashing each line of a text file, or generating a full-length rainbow table chain.

### Configuration
Many rainbow table operations cannot be done without first knowing the characteristics of the table to be operated on (key length, chain length, etc.). The 'Config' class allows for the creation of an object that provides this description of a table. Any class that needs to know the characteristics of a table before it can perform the desired function expects a 'Config' object to be provided. 'Config' parses CLI arguments, produces CLI _--help_ output, and allows the client programmer to query a 'Config' object for argument values.  

The 'Config' class has a small set of _required_ parameters, which are the minimum necessary to represent a rainbow table. However, beyond these defaults, it is also possible for client code to arbitrarily extend the CLI arguments accepted by 'Config' (as is done by programs under the _Tools_ module). They may specify CLI arguments in addition to the required defaults. 'Config' will generate an accurate _--help_ message, prompt the user for arguments marked as "required", and parse input. The client programmer can then query the 'Config' object for the input associated with their arguments.  

### Tools
Any code that could be useful outside of the primary program (RBT.Main) was put in the 'Common' module. The 'Config' class, discussed previously, exists in this module, as well as a 'Search' class for searching a passed table, and a 'Table' class for generating tables. By doing this I could create simple utility programs to provide useful table-related functionality, but that could exist outside the primary program.  

As an example, the _ChainTools_ CLI program is able to perform several helpful functions: when given a key it can print a full length length chain, or when provided with a hash it can apply the reduction function. These operations were useful for understanding and debugging rainbow tables, but don't fit well with the functionality of the primary program. 'ChainTools' is a relatively simple program thanks to this separation of 'Common' classes from more specific functionality.  

## Testing
My method for testing was to take the standard UNIX 'words' dictionary and group words of equal length into distinct files. I then create a companion file by hashing each word with SHA-1, and supplied that file to my program. Searching for each hash, the success rate is printed at the end.  
**Example test output**
![User hash entry]({{ site.url }}/assets/RBT_Example2.png)

# Problems and solutions
During development there were a couple issues whose solution I found particularly interesting. I discuss them below.  

## Collisions
A rainbow table looks something like this:
```
keyA:hashB
keyC:hashD
keyE:hashF
   ...
```
Only the heads and tails of what were once long chains of hash/reduce operations are saved. A collision is when, during the construction of a table, a key from one chain matches the key from another chain, at an identical point in the chain progression. For example:
<table>
    <tr>
        <th></th>
        <th colspan="2">Link 1</th>
        <th colspan="2">Link 2</th>
        <th colspan="2">Link 3</th>
        <th colspan="2">Link 4</th>
        <th colspan="2">Link 5</th>
    </tr>
    <tr>
        <th>Chain 1</th>
        <td>keyA</td><td>hash1</td>
        <td>key2</td><td>hash2</td>
        <td><i>key3</i></td><td>hash3</td>
        <td>key4</td><td>hash4</td>
        <td>key5</td><td>hashB</td>
    </tr>
    <tr>
        <th>Chain 2</th>
        <td>keyY</td><td>hash9</td>
        <td>key8</td><td>hash8</td>
        <td><i>key3</i></td><td>hash3</td>
        <td>key4</td><td>hash4</td>
        <td>key5</td><td>hashB</td>
    </tr>
</table>
  

Here are two different chains, starting with two different keys, _keyA_ and _keyY_. However, at link 3 two different hashes reduce to the same key. Because this happened in the same link, both chains will now match from link 3 on.

With tables of millions of rows, this kind of collision is not uncommon. Due to the way tables are searched, "chain 1" and "chain 2" above can't both be saved to the same table. The official solution ("Official," as it's in the academic paper) is to have more than one table.  

As I've implemented it, the number of tables is user configurable. The paper, referenced below, uses 5 distinct tables for their highest accuracy example. When saving chains I balance between all available tables in an attempt to reduce the number of collisions encountered, which would necessitate a retry with a different table. Currently all tables generated stay in memory, but should I move to disk-backed tables it's possible that constant table switching during searches will lead to repeatedly pushing tables out of cache, requiring a costly reload from disk for every search.  


## Profiling
Initially with Oracle's VisualVM, but more successfully with [JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html), I had success in significantly increasing the speed of table generation.  

**Expensive byte[] to hex conversion**  
The hash strings I'm familiar with - and initially worked with - were 40-character hexadecimal strings. Represented as String data types, this hash representation is perfect for use in TreeMap tables, as they are easily sorted. When watching the generation of rainbow tables with VisualVM, it was readily apparent that what worked for me wasn't working for Java. Java's 'MessageDigest' library generates SHA-1 hashes as byte[]'s, and for each hash generated I was converting to a hex string. Obvious in retrospect, but a simple 5 line method was accounting for over 90% of the execution time before I changed this approach.  

**MessageDigest::getInstance()**  
Found with JProfiler (after VisualVM started hanging on my increasingly large heap size), the method 'MessageDigest::getInstance()' was found to be accumulating a significant amount of processor time. I was calling 'getInstance()' for each hash performed. When I created a 'MessageDigest' instance just once for the entire object and continually reused it, CPU time for this method dropped off the screen. I would guess that if I were dealing with sensitive data this might not be advisable, but I can't see the drawback to this current approach for hashing many strings.  

## Indexing by byte[]
The expense of converting SHA-1 hashes from the byte[] provided by Java's 'MessageDigest' class to hexadecimal strings, I mentioned above. Switching from the String representation to byte[] wasn't a matter of just changing the data type in code. In order to search large tables in a reasonable amount of time the contents have to be sorted, but the byte[] data type does not offer any default methods for sorting. To get around this I created a byte[] '[Comparator](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html)', which established the rules for how one byte[] compares with another. This Comparator, when provided to the TreeMap constructor, allows byte[]'s to be used as keys.  

This change resulted in a significant increase in speed, as the byte[]-to-hex conversion is now only done when there is interaction with the user (who might prefer to see the string). All other times the native byte[] representation is used.

# Future revisions
Loosely sorted by what I'd consider a priority:
1. **Search for keys of variable length _simultaneously_**  
    Presently, you can't search for keys of length 9 _and under_ (for example), only of length 9. You would have to re-run the program for keys of length 8, and so on.  

2. **Make it faster**  
    I'd like to split the hash and search work in to threads based on the number of available processors. With multiple tables and a large number of hashes to generate, I feel there's a lot of room for speed increases.  

3. **Work with larger tables**  
    Currently, all tables have to fit in memory. Between 6 and 8GB of RAM is needed to generate and query ~20 million rows. Using [JavaDB](http://www.oracle.com/technetwork/java/javadb/overview/index.html) I'd like to see if it's feasible to grow table size without increasing the memory required. JavaDB would be my first choice simply because it's included in the JDK.  

4. **Make it distributed**  
    Could be done in a centralized way, with a head-node to provision tasks to worker nodes, store incoming data, and perform searches. This could also be done in a decentralized way, where each node would hold a known portion of the table and the client would have a method to know which node to query given the hash it is searching for.  

5. **Use more standard Java tools**  
   JUnit tests, proper logging, along with a build system.  

6. **Faster hashing**  
    There are options for utilizing OpenCL from within a Java program; this could be a feasible route for making my program faster. Alternately, a standalone C program using OpenCL could be integrated with the existing code.  

# References
**Making a Faster Cryptanalytic Time-Memory Trade-Off**, _Philippe Oechslin_  
I believe this paper is where rainbow tables originate.  

**Rainbow table**
_From Wikipedia: [https://en.wikipedia.org/wiki/Rainbow_table](https://en.wikipedia.org/wiki/Rainbow_table)_  
I didn't find this as complete as I needed, although it seems highly regarded.
