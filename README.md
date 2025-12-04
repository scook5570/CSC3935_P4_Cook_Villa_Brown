# CSC3935_P4_Cook_Villa_Brown

## Overview

This project is a **Java-based Distributed Hash Table (DHT)** implementation that facilitates distributed storage and retrieval of key-value pairs across a network of nodes. The DHT is based on Kademlia-like principles, using a routing table with k-buckets to efficiently locate and store data across the network. Each node can join the network via a bootstrap node, maintain a routing table of peers, and participate in distributed storage operations.

## Features

- **Distributed Storage**: Store and retrieve key-value pairs across a network of DHT nodes
- **Kademlia-based Routing**: Uses k-bucket routing tables (k=3) to efficiently locate peers and data
- **Bootstrap Support**: Nodes can join the network by connecting to a bootstrap node
- **Automatic Replication**: Key-value pairs are automatically replicated to the k closest peers
- **Routing Table Management**: Maintains and updates routing tables as nodes join and leave the network
- **Interactive CLI**: Command-line interface for interacting with the DHT node
- **Node Discovery**: Automatically discovers and learns about other nodes in the network
- **Ping Pong**: Ping operation that will periodically (every 20 seconds) check liveness of peers in the routing table and remove a node if a peer is not found

## Project Structure

```
src/
    dht/                          # DHT implementation
        DistributedHashTable.java # Main DHT class managing routing and storage
        Host.java                 # Represents a peer node in the network
        KeyValueStore.java        # Local key-value storage
        RoutingTable.java         # Kademlia routing table with k-buckets
        ServiceThread.java        # Handles incoming network connections
    messages/                     # DHT message types
        FindNode.java            # Message to find nodes closest to a UID
        FindValue.java           # Message to find a value by key
        Message.java             # Base message class
        Node.java                # Response containing a list of nodes
        Store.java               # Message to store a key-value pair
        Tuple.java               # Key-value tuple representation
        Value.java               # Response containing a value
    model/
        Configuration.java       # Configuration file parser
    Driver.java                  # Main entry point for the DHT node
lib/
    merrimackutil.jar            # Utility library for option parsing and JSON
build/                           # Compiled classes
dist/                            # JAR distribution
example-configs/                 # Example configuration files
    config.json                  # Bootstrap node configuration
    config2.json                 # Node connecting to bootstrap
```

## Getting Started

### Prerequisites

- Java JDK 8 or higher
- Apache Ant (for building)

### Build Instructions

Open a terminal in the project root.

Run:

```bash
ant clean
ant dist
```

Compiled JAR will be in `dist/dhtnode.jar`.

## Running a DHT Node

Run a DHT node with:

```bash
java -jar dist/dhtnode.jar --config <config_file>
```

Or with a default config file:

```bash
java -jar dist/dhtnode.jar --config config.json
```

For help:

```bash
java -jar dist/dhtnode.jar --help
```

## Configuration

Configuration files are JSON format and specify the node's network settings.

Example `config.json` (Bootstrap Node):

```json
{
    "addr": "127.0.0.1",
    "port": 5000,
    "boot-addr": "",
    "boot-port": 0
}
```

Example `config2.json` (Node connecting to bootstrap):

```json
{
    "addr": "127.0.0.1",
    "port": 5001,
    "boot-addr": "127.0.0.1",
    "boot-port": 5000
}
```

### Configuration Fields

- **addr**: The IP address this node will listen on
- **port**: The port this node will listen on
- **boot-addr**: The IP address of the bootstrap node (empty string for bootstrap node)
- **boot-port**: The port of the bootstrap node (0 for bootstrap node)

**Note**: Each node's UID is automatically computed as a SHA-1 hash of its address and port combination.

## Interactive Commands

Once a node is running, you can use the following commands:

- **`.help`** - Display the help menu
- **`.put`** - Store a key-value pair in the DHT
- **`.lookup`** - Retrieve a value by key from the DHT
- **`.showroutes`** - Display the routing table
- **`.showuid`** - Display this node's UID
- **`.kvstore`** - Display the contents of the local key-value store
- **`.quit`** - Exit the application

## Testing

### Setting Up Multiple Nodes

To test the DHT, you'll need to run multiple nodes. Open separate terminal windows for each node:

#### Terminal 1 - Bootstrap Node
```bash
java -jar dist/dhtnode.jar --config example-configs/config.json
```

#### Terminal 2 - Node 2
```bash
java -jar dist/dhtnode.jar --config example-configs/config2.json
```

### Test Scenarios

#### Test storing a key-value pair

On Node 2:
```
> .put
Enter the key: hello
Enter the value: world
Adding key-value pair . . . [ OK ]
```

#### Test retrieving a value

On Node 3:
```
> .lookup
Enter the key: hello
Value: world
```

#### Test viewing routing table

On any node:
```
> .showroutes
```

#### Test viewing local storage

On any node:
```
> .kvstore
```

## Authors

[Samantha Cook](https://github.com/scook5570), [Nayeli Villa](https://github.com/nayeliMC26), [Mike Brown](https://github.com/MKVBII)
