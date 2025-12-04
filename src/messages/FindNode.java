package messages;
import merrimackutil.json.types.JSONObject;

/**
 * Class representing a FindNode message type
 */
public class FindNode extends Message {

    private String targetUID;

    /**
     * Builds a FindNode message type
     * @param type message type of this object
     * @param srcAddr source IP address
     * @param srcPort source port
     * @param tUid target UID
     */
    public FindNode(String type, String srcAddr, int srcPort, String tUID) {

        super(type, srcAddr, srcPort);

        if (!type.equals("FINDNODE")) {
            throw new IllegalArgumentException("Type field must be FINDNODE for the FindNode message type");
        }

        if (!isBase64(tUID)) {
            String newUID = Message.toBase64(tUID);
            this.targetUID = newUID;
        } else if (!Message.checkString(tUID)) {
            throw new IllegalArgumentException("Target UID field is empty or null");
        }

        this.targetUID = tUID;
    }

    /**
     * Serializes the fields of the message into a JSON string
     * @return a JSON string containing the fields of this message
     */
    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("source-address", this.sourceAddress);
        obj.put("source-port", this.sourcePort);
        obj.put("target-uid", this.targetUID);
        return obj.toJSON();
    }

    /**
     * returns the target UID of this message
     * @return ^
     */
    public String getTargetUid() {
        return this.targetUID;
    }
    
}
