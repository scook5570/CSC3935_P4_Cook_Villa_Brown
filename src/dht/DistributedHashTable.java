package dht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;

import messages.FindNode;
import messages.FindValue;
import messages.Message;
import messages.Node;
import messages.Store;
import messages.Value;
import messages.Ping;
import messages.Pong;
import merrimackutil.json.JsonIO;
import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.types.JSONObject;

/**
 * Represents the DistributedHashTable class
 */
public class DistributedHashTable {
    private String address;
    private int port;
    private String uid;
    private RoutingTable routingTable;
    private KeyValueStore kvStore;
    private ServiceThread serviceThread;
    private Thread serviceThreadRunner;
    private static final int K = 3; // Number of closest peers to contact
    // Ping timeouts (increased to avoid false positives on localhost under load)
    private static final int PING_CONNECT_TIMEOUT_MS = 10000;
    private static final int PING_READ_TIMEOUT_MS = 10000;
    private Timer pinger = null;
    private Timer storeReplicator = null;

    /**
     * Builds a DistributedHashTable
     * @param port          the port to listen on
     * @param address       the address to listen on
     * @param uid           the node's UID
     * @param bootstrapAddr the bootstrap node's address
     * @param bootstrapPort the bootstrap node's port
     */
    public DistributedHashTable(int port, String address, String uid, String bootstrapAddr, int bootstrapPort) {
        this.port = port;
        this.address = address;
        this.uid = uid;

        this.routingTable = new RoutingTable(K);
        this.kvStore = new KeyValueStore();

        this.serviceThread = new ServiceThread(address, port, uid, routingTable, kvStore);
        this.serviceThreadRunner = new Thread(serviceThread);
        this.serviceThreadRunner.start();

        // Add the bootstrap to the routing table
        if (bootstrapAddr != null && bootstrapPort > 0) {
            try {
                // Compute the bootstrap node's UID
                ByteBuffer buff = ByteBuffer.allocate(4);
                buff.putInt(bootstrapPort);
                MessageDigest hash = MessageDigest.getInstance("SHA-1");
                hash.update(bootstrapAddr.getBytes(StandardCharsets.UTF_8));
                hash.update(buff.array());
                String bootstrapUid = Base64.getEncoder().encodeToString(hash.digest());

                // Connect to the bootstrap node and send a FINDNODE request to discover other peers
                try (Socket sock = new Socket(bootstrapAddr, bootstrapPort)) {
                    Host bootstrapHost = new Host(bootstrapAddr, bootstrapPort, bootstrapUid);
                    routingTable.addHost(uid, bootstrapHost);
                    // Create a FINDNODE message requesting peers closest to this node's UID
                    FindNode findNode = new FindNode("FINDNODE", address, port, uid);
                    // windering if refactor for MSG types is better to not have to do a toJSON but to be honest its style choice
                    String requestJson = findNode.serialize();

                    // Send the findnode request to the bootstrap node
                    BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                    out.write(requestJson);
                    out.flush();
                    sock.shutdownOutput();

                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(
                            new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    // Process response then update the routing table with discovered peers
                    if (response.length() > 0) {
                        JSONObject obj = JsonIO.readObject(response.toString());
                        Message msg = Message.buildMessage(obj);

                        // If the response is a NODELIST message, get and add hosts to the routing table
                        if (msg instanceof Node) {
                            Node nodeMSG = (Node) msg;
                            Host[] hosts = nodeMSG.getHosts();
                            if (hosts != null && hosts.length > 0) {
                                routingTable.addHosts(uid, hosts);
                            }
                        }
                    }
                } catch (IOException | InvalidJSONException | IllegalArgumentException e) {
                    System.err.println("DHT: Error sending FINDNODE to " + bootstrapAddr + ":" + bootstrapPort + ": " + e);
                }
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Internal DHT Error: SHA1 hash not supported.");
            } catch (Exception e) {
                System.err.println("DHT: Error during bootstrap: " + e);
            }
        }

        // start periodic pinger
        startPinger();

        // start periodic store replicator
        startStoreReplicator();
    }

    private void startPinger() {
        if (pinger != null) return;
        pinger = new Timer("DHT-Pinger", true);
        // Schedule first ping in 20 seconds
        schedulePingTask();
    }

    private void schedulePingTask() {
        if (pinger == null) return;
        pinger.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    pingAllPeers();
                } catch (Exception e) {
                    System.err.println("DistributedHashTable pinger error: " + e);
                } finally {
                    // Schedule the next ping
                    schedulePingTask();
                }
            }
        }, 20000);
    }

    private void pingAllPeers() {
        List<Host> hosts = routingTable.getAllHosts();
        HashSet<String> seen = new HashSet<>();
        for (Host h : hosts) {
            if (h == null) continue;
            String peerUid = h.getTargetUid();
            if (peerUid == null || seen.contains(peerUid)) continue;
            seen.add(peerUid);
            boolean ok = false;
            try {
                ok = sendPing(h);
            } catch (Exception ex) {
                ok = false;
            }
            if (!ok) {
                routingTable.removeHost(uid, peerUid);
                System.err.println("DistributedHashTable: removed unreachable peer " + h.getAddress() + ":" + h.getPort());
            }
        }
    }

    private boolean sendPing(Host peer) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(peer.getAddress(), peer.getPort()), PING_CONNECT_TIMEOUT_MS);
            s.setSoTimeout(PING_READ_TIMEOUT_MS);

            Ping p = new Ping("PING", address, port);
            String requestJson = p.serialize();

            // Send PING message
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            out.write(requestJson);
            out.write("\n");
            out.flush();
            s.shutdownOutput();  // Signal EOF to server without closing input stream
            
            // Read PONG response from the same socket
            StringBuilder response = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            if (response.length() == 0) {
                return false;
            }
            JSONObject obj = JsonIO.readObject(response.toString());
            Message msg = Message.buildMessage(obj);
            return (msg instanceof Pong);
        } catch (java.net.SocketTimeoutException e) {
            return false;
        } catch (java.net.ConnectException e) {
            return false;
        } catch (IOException | InvalidJSONException | IllegalArgumentException e) {
            return false;
        }
    }

    private void startStoreReplicator() {
        if (storeReplicator != null) return;
        storeReplicator = new Timer("DHT-StoreReplicator", true);
        // Schedule first replication in 60 seconds
        scheduleReplicationTask();
    }

    private void scheduleReplicationTask() {
        if (storeReplicator == null) return;
        storeReplicator.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    replicateAllEntries();
                } catch (Exception e) {
                    System.err.println("DistributedHashTable store replicator error: " + e);
                } finally {
                    // Schedule the next replication
                    scheduleReplicationTask();
                }
            }
        }, 60000);
    }

    private void replicateAllEntries() {
        // Get all entries from the key-value store
        Map<String, String[]> allEntries = kvStore.getAllEntries();
        
        if (allEntries.isEmpty()) {
            return;
        }

        // For each entry, send STORE messages to the k closest peers
        for (Map.Entry<String, String[]> entry : allEntries.entrySet()) {
            String identifier = entry.getKey();
            String[] keyValue = entry.getValue();
            String value = keyValue[1]; // keyValue[0] is originalKey, keyValue[1] is value

            // Find k closest peers to this identifier
            ArrayList<Host> closestPeers = routingTable.getKClosestPeers(identifier, K);

            // Send STORE message to each of the closest peers
            for (Host peer : closestPeers) {
                try (Socket sock = new Socket(peer.getAddress(), peer.getPort())) {
                    Store store = new Store("STORE", address, port, identifier, value);
                    String requestJson = store.serialize();

                    BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                    out.write(requestJson);
                    out.flush();
                } catch (IOException e) {
                    // Silently ignore errors during replication to avoid spam
                    // The periodic pinger will handle removing dead peers
                }
            }
        }
    }

    /**
     * A put operation that takes a key and value, stores this key value pair in the local key value store, and issues a STORE message to the k closest peers.
     * @param key   the key to store
     * @param value the value to store
     */
    public void put(String key, String value) {
        // Check for null first
        if (key == null || value == null) {
            System.err.println("DHT: null key or value to put()");
            return;
        }

        // Compute key UID
        String keyUid = null;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.update(key.getBytes(StandardCharsets.UTF_8));
            keyUid = Base64.getEncoder().encodeToString(hash.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Internal DHT Error: SHA1 hash not supported.");
            return;
        }

        // Store in the key value Store locally using the key UID and original key
        kvStore.put(keyUid, key, value);

        // Find k = 3 closest peers to the key UID
        ArrayList<Host> closestPeers = routingTable.getKClosestPeers(keyUid, K);

        // Send the STORE message to each of the peers
        for (Host peer : closestPeers) {
            try (Socket sock = new Socket(peer.getAddress(), peer.getPort())) {
                Store store = new Store("STORE", address, port, keyUid, value);
                String requestJson = store.serialize();

                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                out.write(requestJson);
                out.flush();
            } catch (IOException e) {
                System.err.println("DHT: Error sending STORE to " + peer.getAddress() + ":" + peer.getPort() + ": " + e);
            }
        }
    }

    /**
     * A get operation that takes a key (a String), checks the local key value store, if found return that entry
     * @param key the key to look up
     * @return the value associated with the key
     */
    public String get(String key) {
        // CHeck for null first
        if (key == null) {
            System.err.println("DHT: null key to get()");
            return null;
        }
        String keyUid = null;
        try {
            // Compute key UID
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.update(key.getBytes(StandardCharsets.UTF_8));
            keyUid = Base64.getEncoder().encodeToString(hash.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Internal DHT Error: SHA1 hash not supported.");
            return null;
        }

        // Check local keyvalue store first and return value if it is not null
        String value = kvStore.getValue(keyUid);
        if (value != null) {
            return value;
        }

        // If not found in local, check the k = 3 closest neighbors
        ArrayList<Host> closestPeers = routingTable.getKClosestPeers(keyUid, K);

        // Ask each of the closest peers to see if they have the value
        for (Host peer : closestPeers) {
            try (Socket sock = new Socket(peer.getAddress(), peer.getPort())) {
                // Create a FINDVALUE message requesting the value for the key UID
                FindValue findValue = new FindValue("FINDVALUE", address, port, keyUid);
                String requestJson = findValue.serialize();

                // Send the FINDVALUE request to the peer
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                out.write(requestJson);
                out.flush();
                sock.shutdownOutput();

                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                if (response.length() > 0) {
                    JSONObject obj = JsonIO.readObject(response.toString());
                    Message msg = Message.buildMessage(obj);

                    // Return the value if found 
                    if (msg instanceof Value) {
                        Value valueMSG = (Value) msg;
                        String foundValue = valueMSG.getValue();
                        // Only store and return if the value is not null
                        if (foundValue != null) {
                            kvStore.put(keyUid, foundValue);
                            return foundValue;
                        }
                    }

                    // Return nodelist if not found
                    else if (msg instanceof Node) {
                        Node nodeMSG = (Node) msg;
                        Host[] hosts = nodeMSG.getHosts();
                        if (hosts != null && hosts.length > 0) {
                            // Update routing table with the closer peers for future queries
                            routingTable.addHosts(uid, hosts);
                        }
                    }
                }
            } catch (IOException | InvalidJSONException | IllegalArgumentException e) {
                System.err.println("DHT: Error asking " + peer.getAddress() + ":" + peer.getPort() + ": " + e);
            }
        }

        // Value not found then return null
        return null;
    }

    /**
     * A getKVStore operation that returns the contents of the local key value store
     * as a string
     * 
     * @return the key value store as a string
     */
    public String getKVStore() {
        return kvStore.toString();
    }

    /**
     * A getRoutes operation that returns the routing table as a string
     * 
     * @return the routing table as a string
     */
    public String getRoutes() {
        return routingTable.getAllRoutes();
    }
}


