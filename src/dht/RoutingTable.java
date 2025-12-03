package dht;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;

/**
 * RoutingTable class representing a Kademlia routing table with k-buckets.
 */
public class RoutingTable {
    private ArrayList<Bucket> buckets;
    private int k; // Max records per bucket

    /**
     * Constructor for RoutingTable.
     * 
     * @param k maximum number of hosts per bucket
     */
    public RoutingTable(int k) {
        this.k = k;
        buckets = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            buckets.add(new Bucket(k));
        }
    }

    /**
     * Add a single host to the routing table for a node with id `localUid`.
     * Public methods are synchronized per spec.
     * 
     * @param localUid the local node ID
     * @param host     the host to add
     */
    public synchronized void addHost(String localUid, Host host) {
        int idx = getBucketIndex(localUid, host.getTargetUid());
        // If getBucketIndex returned -1 (identical/invalid IDs), just ignore.
        if (idx == -1)
            return;
        // Any other out-of-range index indicates a programming/data error — fail fast.
        if (idx < 0 || idx >= buckets.size())
            throw new IndexOutOfBoundsException("Invalid bucket index: " + idx);
        Bucket bucket = buckets.get(idx);

        // If host already present, update it
        for (int i = 0; i < bucket.hosts.size(); i++) {
            Host h = bucket.hosts.get(i);
            if (h.getTargetUid().equals(host.getTargetUid())) {
                bucket.hosts.set(i, host);
                return;
            }
        }

        if (bucket.hosts.size() < k) {
            bucket.hosts.add(host);
        } else {
            // Evict oldest (index 0) and append new host
            bucket.hosts.remove(0);
            bucket.hosts.add(host);
        }
    }

    /**
     * Add an array of hosts to the routing table.
     * 
     * @param localUid the local node ID
     * @param hosts    array of hosts to add
     */
    public synchronized void addHosts(String localUid, Host[] hosts) {
        if (hosts == null)
            return;
        for (Host h : hosts)
            addHost(localUid, h);
    }

    /**
     * Get the k closest peers to the given target UID from the entire routing
     * table.
     *
     * @param targetUid the target UID to find closest peers to
     * @param num       the number of closest peers to return
     * @return up to `num` hosts ordered by increasing XOR distance.
     */
    public synchronized ArrayList<Host> getKClosestPeers(String targetUid, int num) {
        ArrayList<Host> all = new ArrayList<>();
        for (Bucket b : buckets)
            all.addAll(b.hosts);

        final BigInteger target = decodeAsBigInt(targetUid);
        all.sort(Comparator.comparing(h -> xorDistanceBigInt(decodeAsBigInt(h.getTargetUid()), target)));

        ArrayList<Host> res = new ArrayList<>();
        for (int i = 0; i < Math.min(num, all.size()); i++)
            res.add(all.get(i));
        return res;
    }

    /**
     * Get a string representation of all routes in the routing table.
     * 
     * @return String representation of all buckets and their hosts.
     */
    public synchronized String getAllRoutes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buckets.size(); i++) {
            Bucket bucket = buckets.get(i);
            sb.append("Bucket ").append(i).append(":\n");
            for (Host host : bucket.hosts) {
                sb.append("  ID: ").append(host.getTargetUid())
                        .append(", IP: ").append(host.getAddress())
                        .append(", Port: ").append(host.getPort()).append("\n");
            }
        }
        return sb.toString();
    }

    /* Basic getters - synchronized */
    public synchronized int getK() {
        return k;
    }

    public synchronized ArrayList<Bucket> getBuckets() {
        return buckets;
    }

    public synchronized Bucket getBucket(int index) {
        return buckets.get(index);
    }

    public synchronized ArrayList<Host> getKClosestRecords(int bucketIndex) {
        if (bucketIndex < 0 || bucketIndex >= buckets.size())
            throw new IndexOutOfBoundsException("Invalid bucket index");
        return new ArrayList<>(buckets.get(bucketIndex).hosts);
    }

    /**
     * Compute bucket index as the number of leading shared prefix bits between
     * local and peer.
     *
     * @param localUid the local node ID
     * @param peerUid  the peer node ID
     * @return -1 if invalid or identical IDs.
     */
    public synchronized int getBucketIndex(String localUid, String peerUid) {
        if (localUid == null || peerUid == null)
            return -1;
        byte[] a = decodeId(localUid);
        byte[] b = decodeId(peerUid);
        if (a == null || b == null || a.length != b.length)
            return -1;
        int shared = sharedPrefixBits(a, b);
        if (shared == a.length * 8)
            return -1; // identical
        return shared;
    }

    /**
     * Decodes a Base64-encoded ID.
     *
     * @param b64 the Base64-encoded ID
     * @return the decoded ID as a byte array, or null if invalid
     */
    private static byte[] decodeId(String b64) {
        try {
            return Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Decodes a Base64-encoded ID into a BigInteger.
     *
     * @param b64 the Base64-encoded ID
     * @return the decoded ID as a BigInteger, or BigInteger.ZERO if invalid
     */
    private static BigInteger decodeAsBigInt(String b64) {
        byte[] bs = decodeId(b64);
        if (bs == null)
            return BigInteger.ZERO;
        return new BigInteger(1, bs);
    }

    /**
     * XOR distance between two BigIntegers.
     * 
     * @param a the first BigInteger
     * @param b the second BigInteger
     * @return the XOR distance as a BigInteger
     */
    private static BigInteger xorDistanceBigInt(BigInteger a, BigInteger b) {
        return a.xor(b);
    }

    /**
     * Calculate the number of leading shared prefix bits between two byte arrays.
     *
     * @param a the first byte array
     * @param b the second byte array
     * @return the number of leading shared prefix bits
     */
    private static int sharedPrefixBits(byte[] a, byte[] b) {
        int shared = 0;
        for (int i = 0; i < a.length; i++) {
            int xor = (a[i] ^ b[i]) & 0xFF;
            if (xor == 0) {
                shared += 8;
                continue;
            }
            int lz = Integer.numberOfLeadingZeros(xor) - 24; // leading zeros in byte
            shared += lz;
            break;
        }
        return shared;
    }

    /**
     * Bucket class representing a k-bucket in the routing table.
     */
    public class Bucket {
        private ArrayList<Host> hosts;

        public Bucket(int k) {
            hosts = new ArrayList<>(k);
        }

        public ArrayList<Host> getHosts() {
            return hosts;
        }

        public Host getHost(int index) {
            return hosts.get(index);
        }

        public void setHost(int index, Host host) {
            this.hosts.set(index, host);
        }

        public void addHost(Host host) {
            this.hosts.add(host);
        }
    }
}
