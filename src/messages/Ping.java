package messages;

import merrimackutil.json.types.JSONObject;

public class Ping extends Message {

    /**
     * Builds a Ping message type
     * 
     * @param type    message type of this object
     * @param srcAddr source IP address
     * @param srcPort source port
     */
    public Ping(String type, String srcAddr, int srcPort) {
        super(type, srcAddr, srcPort);
        if (!"PING".equals(type)) {
            throw new IllegalArgumentException("Type must be PING for Ping message");
        }
    }

    /**
     * Serializes the fields of the message into a JSON object
     * 
     * @return a JSON object containing the fields of this message
     */
    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("source-address", this.sourceAddress);
        obj.put("source-port", this.sourcePort);
        return obj.toJSON();
    }
}
