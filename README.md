# Fresco Outsourcing
Tools to work with FRESCO in the outsourced MPC setting. I.e., the MPC setting where not all parties actually run the MPC protocol. Instead, only a limited number of parties, called *servers* will run the MPC protocol, while a larger number of parties called *clients* will supply the inputs and receive outputs from the computation performed by the servers. This should be done so that the in/outputs of the clients should be protected similarly to if they where directly participating in the MPC protocol. Such a setup is often more scalable than having all parties participate directly in the computation when a large number of parties are involved.

This repo aims to 

- Provide general reuseable tools for FRESCO based applications in this setting. 

- Provide a general framework for implementing the required in/output protocols and functionality, to facilitate experimentation with various methods.

## Protocols

Initially, here are two protocols for in/output between clients and servers that we want to implement and experiment with in this repo. 

The protocol of [Damgård et al](https://eprint.iacr.org/2015/1006), which we denote the *DDNNT* protocol, is rather simple, does not require the clients to hold state and does not require communication between the servers for input and output. The protocol does however require some coordination between the servers in order to synchronize the use of preprocessed material, which can be a bit tricky. The protocol also has a considerable communication overhead; in order to input a single field element we must communicate around three field elements per server. 

The protocol of [Jakobsen et al](https://eprint.iacr.org/2016/037), which we will denote the *JNO* protocol, is a little more complex, it does require the client to have some state and the servers need to do some (small) extra MPC work to validate the input from clients. However, the protocol is more general, and using pseudo random secret sharing it should be possible to make very communication efficient (for the client/server interaction), requiring essentially just a single field element communicated for each input element (independent of the number of servers).


## Current State

Currently, the repo only holds an implementation of the input protocol for DDNNT. But the goal would be to implement both protocols and experiment with the trade-offs in some realistic deployment (such as the Federated Learning setting explored in [fresco-ml](https://github.com/aicis/fresco-ml).
