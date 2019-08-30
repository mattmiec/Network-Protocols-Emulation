#Network Protocols Emulation
* mtm2163
* Matt Miecnikowski
* CSEE S4119 Computer Networks
* PA2 - Network Protocols Emulation

##Overview
* This is a simple implementation of two nodes implementing the Go-Back-N network protocol
* Built on top of UDP packets/sockets, with a single byte of data transmitted per packet
* Nodes can be configured to drop packets either deterministically or probabilistically

##How to Build (on Ubuntu 14.04.6 LTS)
* Install JDK: "sudo apt-get install default-jdk"
* Compile: "cd src; javac gbnnode/&ast.java"

##How to Run
* Execute "java gbnnode/GbnNode <self-port> <peer-port> <window-size> [ -d <value-of-n> | -p <value-of-p>]"
    * The argument -d <value-of-n> configures the node to drop every nth packet
    * The argument -p <value-of-p> configures the node to drop each packet with probability p
* Send data with the command "send <string-of-chars>"
* See test.txt for an example
