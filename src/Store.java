import merrimackutil.json.types.JSONObject;

public class Store extends Message {

    private String key;
    private String value;
    private Tuple keyValuePair;

    /**
     * Builds a Store message type from the inputted values
     * @param type message type of this object
     * @param srcAddr source IP address
     * @param srcPort source port
     * @param ky key
     * @param val value
     */
    public Store(String type, String srcAddr, int srcPort, String ky, String val) {
        
        super(type, srcAddr, srcPort);

        if (type != "STORE") {
            throw new IllegalArgumentException("Type field should be 'STORE' for Store message type");
        }

        if (!Message.checkString(val)) {
            throw new IllegalArgumentException("Value input is null or empty");
        }

        if (!Message.checkString(ky)) {
            throw new IllegalArgumentException("Key input is null or empty");
        }

        this.key = ky;
        this.value = val;
        this.keyValuePair = new Tuple(ky, val);

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
        obj.put("key", this.key);
        obj.put("value", this.value);
        return obj;
    }
    
    /**
     * Returns the key value as a pair
     * @return
     */
    public Tuple getKeyValuePair() {
        return this.keyValuePair;
    }

    // or in case it's more useful to get them individually . . . 

    /**
     * Returns the key field of this message
     * @return ^ 
     */
    public String getKey() {
        return this.keyValuePair.getKey();
    }

    /**
     * Returns the value field of this message
     * @return ^
     */
    public String getValue() {
        return this.keyValuePair.getValue();
    }

}
