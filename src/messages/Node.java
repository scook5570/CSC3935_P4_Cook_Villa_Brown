package messages;
import dht.Host;
import merrimackutil.json.types.JSONObject;

/**
 * Class representign a Node message type
 */
public class Node extends Message {

    private Host[] hosts;

    /**
     * Builds a Node message type from the inputted fields
     * @param type message type
     * @param sourceAddr source IP address
     * @param sourcePrt source port
     * @param hosts array of hosts
     */
    public Node(String type, String sourceAddr, int sourcePrt, Host[] hosts) {
        super(type, sourceAddr, sourcePrt);

        if (type != "NODE") {
            throw new IllegalArgumentException("Type field for Node message type should be 'NODE'");
        }

        this.hosts = hosts;
    }

   /**
     * Serializes the fields of the message into a JSON object
     * @return a JSON object containing the fields of this message
     */
    @Override
    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("source-address", this.sourceAddress);
        obj.put("source-port", this.sourcePort);
        obj.put("hosts", this.hosts);
        return obj;
    }

    /**
     * returns the list of hosts for this message obj
     * @return ^
     */
    public Host[] getHosts() {
        return this.hosts;
    }
    
}
