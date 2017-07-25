# RainbowTable
An application that creates a rainbow table with given parameters, as well as some tools specific to rainbow tables.  

## Website
[Rainbow tables](https://upnix.github.io/RainbowTables/)

## API documentation
[Javadocs](https://upnix.github.io/RainbowTables/docs/)

## Running
### Requirements
* Java JDK 8
* commons-cli (included in this repository)
* msgpack-core (included in this repository)

### Steps
1. Clone repository  
`git clone https://github.com/upnix/RainbowTables.git`
2. Set your `CLASSPATH`  
`export CLASSPATH=.:./lib/commons-cli-1.4.jar:./lib/msgpack-core-0.8.13.jar`
3. From the root of the 'RainbowTable' directory, compile with:  
`javac RainbowTable/src/RBT/Main.java Common/src/RBT/Config.java Common/src/RBT/Search.java Common/src/RBT/Table.java -d .`
4. Execute program:  
`java RBT/Main --help`

### Searching for hashes
#### Just one hash
* Generate a hash:
```
$ echo -n 'hello' | shasum
aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d
```
* Given the key length (5 for 'hello') and allowable character set (a-z, 26 characters), determine the size of the key space that needs to be searched.  
26<sup>5</sup> = 11,881,376
* Determine the number of rows, chain length, and tables you'd like to generate:  
`--row-count 2000000 --chain-length 10 --key-length 5`
  * Key space covered: 2,000,000*0 = 20,000,000 / 11,881,376 = 168% of key space. 
* Generate the table:  
`$ java RBT/Main --key-length 5 --row-count 2000000 --chain-length 10 --table-count 5`
* Enter your hash:
```
Rainbow table loaded with the following parameters:
  Configured -
    *           Total rows: 2,000,000
    *          Table count: 5
    *         Chain length: 10
    *           Key length: 5

  Static -
    *   Average table size: 400,000
    *            Key space: 11,881,376
    *        Character set:
[a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z]

Enter a hash to find: aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d
hello
1 milliseconds to complete.
Enter a hash to find:
```
#### Many hashes
You can supply a text file with a hash per line to the `Main` program. For example:  
`java RBT/Main --key-length 5 --row-count 2000000 --chain-length 10 --table-count 5 --search-file dict_5word_lc.sha1`

## Present shortcomings 
* Only generates and searches keys of a single length (doesn't find keys of length 5 _or less_)
* I should be using a build system

