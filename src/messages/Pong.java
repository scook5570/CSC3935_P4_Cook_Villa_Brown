package messages;
import merrimackutil.json.types.JSONObject;

public class Pong extends Message {

    public Pong(String type, String srcAddr, int srcPort) {
        super(type, srcAddr, srcPort);
        if (!"PONG".equals(type)) {
            throw new IllegalArgumentException("Type must be PONG for Pong message");
        }
    }

    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("source-address", sourceAddress);
        obj.put("source-port", sourcePort);
        return obj.toJSON();
    }
}
