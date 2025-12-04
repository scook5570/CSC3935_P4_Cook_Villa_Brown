package dht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.net.Socket;
// pinger moved to DistributedHashTable; keep ServiceThread inbound-only
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import merrimackutil.json.JsonIO;
import messages.*;
import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

/**
 * ServiceThread: listens for incoming TCP connections, parses JSON messages,
 * updates the routing table with the sender's host info, and responds to
 * protocol messages (FINDNODE, FINDVALUE, STORE, NODE, VALUE).
 */
public class ServiceThread implements Runnable {

    private String address;
    private int port;
    private String uid; // local node UID (base64 encoded SHA-1)
    private RoutingTable rt;
    private KeyValueStore kvs;

    /**
     * Constructor for ServiceThread.
     * 
     * @param address the local address to bind the server socket
     * @param port    the local port to bind the server socket
     * @param uid     the local node UID (base64 encoded SHA-1)
     * @param rt      the routing table
     * @param kvs     the key-value store
     */
    public ServiceThread(String address, int port, String uid, RoutingTable rt, KeyValueStore kvs) {
        this.address = address;
        this.port = port;
        this.uid = uid;
        this.rt = rt;
        this.kvs = kvs;
    }

    // ServiceThread no longer runs the periodic pinger; DistributedHashTable handles outgoing pings

    /**
     * Run the service thread: listen for incoming connections and handle them.
     */
    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (true) {
                Socket sock = server.accept();
                try {
                    handleConnection(sock);
                } catch (Exception e) {
                    System.err.println("ServiceThread: error handling connection: " + e);
                } finally {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        System.err.println("ServiceThread: error closing socket: " + e); // maybe ignore? closes anyway
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ServiceThread: failed to open server socket on port " + port + ": " + e);
        }
    }

    /**
     * Handle an incoming connection: read JSON message, update routing table,
     * respond as needed.
     * 
     * @param sock the socket representing the incoming connection
     * @throws IOException if an I/O error occurs when reading from the socket
     */
    private void handleConnection(Socket sock) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        String payload = sb.toString().trim();
        if (payload.isEmpty())
            return;

        // parse the incoming JSON using JsonIO
        JSONObject obj = null;
        try {
            obj = JsonIO.readObject(payload);
        } catch (InvalidJSONException e) {
            System.err.println("ServiceThread: invalid JSON payload: " + e.getMessage());
            return;
        }

        // build a Message from the JSON
        Message m = null;
        try {
            m = Message.buildMessage(obj);
        } catch (InvalidObjectException | IllegalArgumentException e) {
            System.err.println("ServiceThread: failed to build message from JSON: " + e.getMessage());
            return;
        }

        // update routing table with sender info: compute peer UID from addr+port
        // (SHA-1, Base64)
        String srcAddr = m.getSourceAddress();
        int srcPrt = m.getSourcePort();
        String peerUid = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(srcAddr.getBytes(StandardCharsets.UTF_8));
            md.update(ByteBuffer.allocate(4).putInt(srcPrt).array());
            peerUid = Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("ServiceThread: SHA-1 not available: " + e);
        }

        Host peer = null;
        if (peerUid != null) {
            peer = new Host(srcAddr, srcPrt, peerUid);
            rt.addHost(uid, peer);
        }

        String responseStr = null;

        // dispatch based on actual message type
        if (m instanceof FindNode) {
            FindNode fn = (FindNode) m;
            String target = fn.getTargetUid();
            ArrayList<Host> closest = rt.getKClosestPeers(target, rt.getK());
            Node nodeMsg = new Node("NODELIST", address, port, closest.toArray(new Host[0]));
            responseStr = nodeMsg.serialize().toJSON();

        } else if (m instanceof FindValue) {
            FindValue fv = (FindValue) m;
            String target = fv.getTargetUid();
            String val = null;

            if (kvs != null) {
                val = kvs.getValue(target);
            }

            if (val != null) {
                Value vmsg = new Value("VALUE", address, port, target, val);
                responseStr = vmsg.serialize().toJSON();
            } else {
                ArrayList<Host> closest = rt.getKClosestPeers(target, rt.getK());
                Node nodeMsg = new Node("NODELIST", address, port, closest.toArray(new Host[0]));
                responseStr = nodeMsg.serialize().toJSON();
            }

        } else if (m instanceof Store) {
            Store st = (Store) m;
            String key = st.getKey();
            String value = st.getValue();

            if (kvs != null) {
                kvs.put(key, value);
            }

        } else if (m instanceof Node) {
            Node nm = (Node) m;
            Host[] hosts = nm.getHosts();
            if (hosts != null && hosts.length > 0) {
                rt.addHosts(uid, hosts);
            }

        } else if (m instanceof Ping) {
            // reply with PONG
            Pong pong = new Pong("PONG", address, port);
            responseStr = pong.serialize().toJSON();

        } else if (m instanceof Value) {
            Value vm = (Value) m;
            String key = vm.getKey();
            String value = vm.getValue();

            if (kvs != null) {
                kvs.put(key, value);
            }
        }

        // end of handleConnection dispatch

        if (responseStr != null) {
            try (BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8))) {
                out.write(responseStr);
                out.write("\n");
                out.flush();
            }
            // Close output stream to signal EOF to the client
            try {
                sock.shutdownOutput();
            } catch (IOException e) {
                // Ignore, socket may already be closed
            }
        }
    }
}
