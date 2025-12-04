package messages;
import merrimackutil.json.types.JSONObject;

public class Ping extends Message {

    public Ping(String type, String srcAddr, int srcPort) {
        super(type, srcAddr, srcPort);
        if (!"PING".equals(type)) {
            throw new IllegalArgumentException("Type must be PING for Ping message");
        }
    }

    @Override
    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("source-address", this.sourceAddress);
        obj.put("source-port", this.sourcePort);
        return obj;
    }
}
