# MPC programs for "Attribute-based Single Sign-On: Secure, Private, and Efficient"
This repository is a fork of FRESCO-outsourcing, a general MPC framework for outsourced secure computation, I.e., the MPC setting where not all parties actually run the MPC protocol. Instead, only a limited number of parties, called *servers* will run the MPC protocol, while a larger number of parties called *clients* will supply the inputs and receive outputs from the computation performed by the servers. This should be done so that the in/outputs of the clients should be protected similarly to if they where directly participating in the MPC protocol. Such a setup is often more scalable than having all parties participate directly in the computation when a large number of parties are involved. 
This specific branch contains the test applications used to benchmark the paper "Attribute-based Single Sign-On: Secure, Private, and Efficient".

## Environment
The code is exclusively written in Java and requires a JDK with support for Java 11 and Maven to build.

## Building
Building and testing is done by running `mvn package`.

## Running a benchmark
To run a server or client, simply run `java -jar demo/target/demo.jar <type> <ID> <IP1> ... <IPn>` after building.
Type is either "c" or "s" depending on a client or serve role, ID is the ID of the party, which MUST start with 1 and monotonically increase.  <IP1> is the IP address of the server with ID 1 and IPn is the ID of the nth server.
To run the benchmark you need 1 client and at LEAST 2 servers. E.g. running the following, with each command in a different command line, in case of a local test:
- For the client `java -jar target/demo.jar c 1 localhost localhost`
- For server 1 `java -jar target/demo.jar c 1 localhost localhost`
- For server 2 `java -jar target/demo.jar s 2 localhost localhost`

### Results
Test results will be stored as an CSV in `demo/jmh-reports/<type>/<id>` with the format of <type of test>, <time in milliseconds>, <standard deviation> 

## Docker build
To make a Docker container run `docker image build -t docker-demo-jar:latest .` and to run the app through Docker run `docker run docker-java-jar:latest demo/target/demo.jar <type> <ID> <IP1> ... <IPn>`

# License
MIT License
Copyright (c) 2023 Security Lab // Alexandra Institute

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
