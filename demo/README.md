# SPDZ outsourced computation demo

A demo that runs an outsourced computation with two SPDZ servers and a single input client.

## Building a jar
Update `fresco-outsourcing/demo/pom.xml` to make a jar of the desired java class with a main method. I.e. the element `<mainClass>` in `<build><plugins><plugin><configuration><manifest>` should be updated to point at one of the files:
* `demo/Demo.java`, the most basic demo. Only for localhost
* `benchmark/Main.java`, outsourced computation demo doing hashed-based equality test. 
* `benchmark/ClassicMain.java`, plain Fresco dummy computation. Currently outputting the static value 42.
Then run `mvn package -DskipTests` from the directory `fresco-outsourcing/demo` to build the jar `demo.jar` in `fresco-outsourcing/demo/target`. 

## Simple, local demo
The most plain demo is specified in the class Demo, which shows the computation of the sum of different client's inputs.
It is run through the main function `Demo.main` with the following arguments:
```
fresco-outsourcing/demo$ <role> <id>
```
Where `role` is either `c` (indicating client) or `s` (indicating server) and `id` is the ID of the client/server. 
This is an integer start from 1. 
Thus to run a simple demo, open terminals on the same machine and run, from the directory `fresco-outsourcing/demo`:
```
fresco-outsourcing/demo$ java -jar target/demo.jar s 1
fresco-outsourcing/demo$ java -jar target/demo.jar s 2
fresco-outsourcing/demo$ java -jar target/demo.jar c 1
fresco-outsourcing/demo$ java -jar target/demo.jar c 2
fresco-outsourcing/demo$ java -jar target/demo.jar c 3 
...
```

## Outsourced benchmark
An outsourced benchmark using `benchmark/Main.java` runs a hashed-based equality test using Pesto++, i.e. with a single client and any number of servers.
The command line arguments are as follows:
```
fresco-outsourcing/demo$ <role> <id> <IPs>
```
Where `role` is either `c` (indicating client) or `s` (indicating server) and `id` is the ID of the client/server.
This is an integer start from 1. Finally `IPs` is a list of space-separated IPs of all servers. The first one is associated to the server with id 1, the second one to the server with id 2 and so on.
Thus to run a simple demo, open terminals on different machines run one of the lines below, from the directory `fresco-outsourcing/demo`:
```
fresco-outsourcing/demo$ java -jar target/demo.jar s 1 1.1.1.1 2.2.2.2 3.3.3.3
fresco-outsourcing/demo$ java -jar target/demo.jar s 2 1.1.1.1 2.2.2.2 3.3.3.3
fresco-outsourcing/demo$ java -jar target/demo.jar s 3 1.1.1.1 2.2.2.2 3.3.3.3
fresco-outsourcing/demo$ java -jar target/demo.jar c 1 1.1.1.1 2.2.2.2 3.3.3.3 
...
```
This will result in a benchmark run, where the program is executed repeatedly. The results are summerized in the file `fresco-outsourcing/jmh-reports/<role>/<id>/benchmark.csv`.
The file contains the name of the class being benchmarked followed by the mean execution time and then the standard deviation, as a CSV.

## Classic Fresco benchmark
A classic Fresco benchmark using `benchmark/Main.java` runs a dummy program outputting 42, with any number of servers.
The command line arguments are as follows:
```
fresco-outsourcing/demo$ <bitlength> <id> <IPs>
```
Where `bitlength` is the bitlength of the modulus to use in Fresco,  `id` identifies the current server. This is an integer, monotonically increasing from 1. `IPs` is a list of space-separated IPs of all servers. 
The first one is associated to the server with id 1, the second one to the server with id 2 and so on.
Thus to run a simple demo, open terminals on different machines run one of the lines below, from the directory `fresco-outsourcing/demo`:
```
fresco-outsourcing/demo$ java -jar target/demo.jar 40 1 1.1.1.1 2.2.2.2 3.3.3.3
fresco-outsourcing/demo$ java -jar target/demo.jar 40 2 1.1.1.1 2.2.2.2 3.3.3.3
fresco-outsourcing/demo$ java -jar target/demo.jar 40 3 1.1.1.1 2.2.2.2 3.3.3.3
...
```
This will result in a benchmark run, where the program is executed repeatedly. The results are summerized in the file `fresco-outsourcing/jmh-reports/<role>/<id>/benchmark.csv`.
The file contains the name of the class being benchmarked followed by the mean execution time and then the standard deviation, as a CSV.