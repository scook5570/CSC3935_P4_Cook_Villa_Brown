package messages;
import merrimackutil.json.types.JSONObject;

/**
 * class representing a FindValue message type
 */
public class FindValue extends Message {

    private String targetUID;

    /**
     * Builds a FindValue message type
     * @param type message type of this object
     * @param srcAddr source IP address
     * @param srcPort source port
     * @param tUid target UID
     */
    public FindValue(String type, String srcAddr, int srcPort, String tUID) {

        super(type, srcAddr, srcPort);

        if (!type.equals("FINDVALUE")) {
            throw new IllegalArgumentException("Type field must be FindValue for the FindValue message type");
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
     * Serializes the fields of the message into a JSON object
     * @return a JSON object containing the fields of this message
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