package messages;
import dht.Host;
import merrimackutil.json.types.JSONArray;
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

        // Per instructions should be nodelist
        if (!type.equals("NODELIST")) {
            throw new IllegalArgumentException("Type field for Node message type should be 'NODELIST'");
        }

        this.hosts = hosts;
    }

   /**
     * Serializes the fields of the message into a JSON object
     * @return a JSON object containing the fields of this message
     */
    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("source-address", this.sourceAddress);
        obj.put("source-port", this.sourcePort);
        
        // Serialize hosts array, cant just add the array 
        JSONArray hostsArray = new JSONArray();
        if (this.hosts != null) {
            for (Host host : this.hosts) {
                hostsArray.add(host.toJSONType());
            }
        }
        obj.put("hosts", hostsArray);
        return obj.toJSON();
    }

    /**
     * returns the list of hosts for this message obj
     * @return ^
     */
    public Host[] getHosts() {
        return this.hosts;
    }
    
}
