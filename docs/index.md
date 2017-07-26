---
---
# Introduction
A rainbow table offers a method by which one can search a very large key space multiple times without the need to save or recompute the entire key space for each search. Wikipedia article [here](https://en.wikipedia.org/wiki/Rainbow_table).  

Using Java, I have a rainbow table implementation that allows the user to generate and search SHA-1 rainbow tables. Key length, character space, table and chain size, as well as number of tables is all configurable. In addition to the main implementation, there are also standalone tools for helpful rainbow table-related tasks.  

**_--help_ output**
![--help output]({{ site.url }}/assets/RBT_Help.png)

**User hash entry**
![User hash entry]({{ site.url }}/assets/RBT_Example1.png)

## 5-minute presentation
I created a 5-minute presentation for this program that I presented at the Recurse Center.

## Much longer presentation
This is an un-presented presentation, which has more details.

# Programming overview
## API documentation
[Javadoc documentation]({{ site.url }}/docs/)

## Third-party libraries used
**[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/)**  
Using Apache commons-cli the program accepts CLI arguments to override defaults. These arguments are used in the construction of the table.  

**[MessagePack](http://msgpack.org/)**  
What is being called a "rainbow table" consists of two TreeMap data structures, used to create a bidirectional map (key->hash and hash->key). After a table has been generated one of these TreeMap's is written to disk using MessagePack. When the program receives parameters that match a table previously generated, the corresponding MessagePack is loaded from disk.


## Structure
Inside IntelliJ the code is broken into 3 distinct modules:
1. **Common**  
    Code common to both the primary 'RainbowTable' program, as well as the utilities in 'Tools'.

2. **RainbowTable**  
    The program for generating and allowing the user to search rainbow tables.

3. **Tools**  
    Utilities related to rainbow tables, but unnecessary for the primary program. Examples include hashing each line of a text file, or generating a full-length rainbow table chain.

### Configuration
Many rainbow table operations cannot be done without first knowing the characteristics of the table to be operated on (key length, chain length, etc.). The 'Config' class allows for the creation of an object that provides this description of a table. Any class that needs to know the characteristics of a table before it can perform the desired function expects a 'Config' object to be passed. It parses CLI arguments, produces CLI _--help_ output, and allows the client programmer to query a 'Config' object for argument values.  

The 'Config' class has a small set of _required_ parameters, which are the minimum necessary to represent a rainbow table. However, beyond these defaults, it is also possible for client code to arbitrarily extend the CLI arguments accepted by 'Config' (as is done under the _Tools_ module). It is possible to specify CLI arguments in addition to the required defaults. 'Config' will generate an accurate _--help_ message, prompt the user for arguments marked as "required", and parse input. The client programmer can then query the 'Config' object for the input associated with their arguments.  

### Tools
Any code that could be useful outside of the primary program (RBT/Main) was put in the 'Common' module. The 'Config' class, discussed previously exists in this module, as well as a 'Search' class for searching a passed table, and a 'Table' class which generates tables. By doing this I could create simple utility programs which provided useful table-related functionality, but could be considered standalone.  

As an example, the 'ChainTools' CLI program is able to perform several helpful functions. When given a key, it will print a full length. It can also be provided with a hash, to which it will apply the reduction function. These operations were useful for understanding and debugging the primary application, but didn't fit well with the functionality of that program. 'ChainTools' is a relatively simple program, as most of the required functionality exists in the 'Common' classes.  

## Testing
There is no formal testing currently. My method for testing was to take the standard UNIX 'words' dictionary, and break it into files of identical length words. I then hashed each word with SHA-1, and supplied that file to my program. Searching for each hash, the success rate is printed at the end.  
**Example test output**
![User hash entry]({{ site.url }}/assets/RBT_Example2.png)

# Problems and solutions
## Collisions
A rainbow table looks something like this:
```
keyA:hashB
keyC:hashD
keyE:hashF
   ...
```
Only the heads and tails of what were long chains of hash/reduce operations are saved. A collision is when, during the construction of a table, a key from one chain matches the key from another chain, at an identical point in the chain. For example:
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
  

Here are two different chains, starting with two different keys, _keyA_ and _keyY_. However, at link 3 two different hashes reduce to the same key. Because they happened in the same link, both chains will now match from link 3 on.

With tables of millions of rows, this kind of collision is not uncommon. Due to the way tables are searched, "chain 1" and "chain 2" above couldn't both be saved to the same table. The official solution ("Official," as it's in the academic paper) is to have more than one table.  

As I've implemented it, the number of tables is user configurable. The paper, referenced below, uses 5 distinct tables, so that is what I've defaulted to. When saving chains I balance between all available tables in an attempt to reduce the number of collisions encountered, which would necessitate a retry with a different table. I foresee a drawback of this approach when it comes to searching. Currently all tables generated stay in memory, but should I move to disk-backed tables, when it comes time to searching the constant table switching could repeatedly push tables out of cache, requiring a costly reload from disk for every search.

## Profiling
Initially with Oracle's VisualVM, but more successfully with [JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html), I had success in significantly increasing the speed of table generation.  

**Expensive byte[] to hex conversion**  
The hash strings I'm familiar with, and initially worked with, were 40-character hexadecimal strings. Represented as String data types, this hash representation was perfect for use in my tables, as they were easily sorted. When watching the generation of my rainbow tables with VisualVM, it was readily apparent what worked for me wasn't working for Java. Java's MessageDigest library generates SHA-1 hashes as byte[]'s, and for each hash generated I was converting to a hex string. Obvious in retrospect, but a simple 5 line method was accounting for over 90% of the execution time before I changed my approach.  

**MessageDigest::getInstance()**  
Found with JProfiler (after VisualVM started hanging on my increasingly large heap size), the method MessageDigest::getInstance() was found to be a point of contention. I was calling getInstance() for each hash I did, and it's still not clear to me that this isn't the proper use for the method. When I created an instance just once for the entire object, and continually reused it, it dropped off the screen as a method of concern. I would guess that if I were dealing with sensitive data this might not be advisable, and that you would want a new instance of 'MessageDigest' each time.

## Indexing by byte[]
The expense of converting SHA-1 hashes from the byte[] provided by Java's MessageDigest class, to hexadecimal strings, I mentioned above. Switching from the String representation to byte[] wasn't a matter of just changing the data type in code. In order to search the tables in a reasonable amount of time the contents had to be sorted. The byte[] data type does not offer any default methods for sorting, and so it couldn't be readily used in the TreeMap container I was using. I ultimately created a byte[] Comparator, which established the rules for how one byte[] compares with another. This Comparator, when provided to the TreeMap constructor allows byte[]'s to be used as keys.  

This change resulted in a significant increase in speed, as the byte[]-to-hex conversion is now only done when there is interaction with the user (who might prefer to see the string). All other times the native byte[] representation is used for searching.

# Room for improvement
Loosely sorted by what I'd consider a priority:
1. **Make it more useful**  
    I may have glossed over the fact that you can only search for keys of a single length at a time. You can't search for keys of length 9 _and under_, only of length 9. You would have to re-run the program for keys of length 8, and so on.  

2. **Make it faster**  
    Without any work from me, it does seem to be able to utilize up to 5 processors (credit to the JVM, I think?). However, I'd like to formally split the work up in to threads based on the number of available processors. With multiple tables and a large number of hashes to generate, I feel there's a lot of room for speed increases.

3. **Work with larger tables**  
    Currently, my tables have to fit in memory. Using [JavaDB](http://www.oracle.com/technetwork/java/javadb/overview/index.html) I'd like to see if it's feasible to grow table size beyond the ~20 million rows I can reasonably work with now.

4. **Make it distributed**
    If you had X networked computers to work with, it'd be nice if you could split the problem in to X parts. Done in a centralized way, there would have to be a head-node with a data store that split up the work and allowed for searching. However, I can imagine this also done in a decentralized way, where each node would hold a known portion of the table, and the client would know which node to ask about regarding the hash being searched.

5. **Make it "proper"**  
   I'd like to have JUnit tests written, and proper logging.  

6. **Faster hashing**  
    Using some OpenCL password hashing utilities, I can see that I'm generating hashes at a snails pace. There are options for tying Java programs into OpenCL, which could be a feasible route for making my program faster.  

    A second method might be to create some interface between a C OpenCL program and my existing program, where the C program generated chains and the Java program just queried it. But at some point I would just have to admit the program should be in C if I cared so much about speed.

# References
**Making a Faster Cryptanalytic Time-Memory Trade-Off**, Philippe Oechslin  
I believe this paper is where rainbow tables originate.  

**Rainbow table**
From Wikipedia: [https://en.wikipedia.org/wiki/Rainbow_table](https://en.wikipedia.org/wiki/Rainbow_table)  
I didn't find this as complete as I needed, although it seems highly regarded.