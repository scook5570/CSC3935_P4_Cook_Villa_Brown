package messages;

import merrimackutil.json.types.JSONObject;

public class Pong extends Message {

    /**
     * Builds a Pong message type
     * 
     * @param type    message type of this object
     * @param srcAddr source IP address
     * @param srcPort source port
     */
    public Pong(String type, String srcAddr, int srcPort) {
        super(type, srcAddr, srcPort);
        if (!"PONG".equals(type)) {
            throw new IllegalArgumentException("Type must be PONG for Pong message");
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
        obj.put("type", type);
        obj.put("source-address", sourceAddress);
        obj.put("source-port", sourcePort);
        return obj.toJSON();
    }
}
