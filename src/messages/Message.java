package messages;
import java.io.InvalidObjectException;
import java.util.Base64;

import dht.Host;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

/**
 * abstract super class representing an overarching message type
 */
public abstract class Message {
    
    protected String type;
    protected String sourceAddress;
    protected int sourcePort;

   

    /**
     * Builds a Message with the given fields
     * @param tp message type
     * @param srcAddr source address (IP address of the sender)
     * @param srcPort soure port (port number of the sender)
     */
    public Message(String tp, String srcAddr, int srcPort) {

        if (!this.checkType(tp)) {
            throw new IllegalArgumentException("Invalid type input");
        } else if (!Message.checkString(tp)) {
            throw new IllegalArgumentException("Empty or null type input");
        }

        if (!Message.checkString(srcAddr)) {
            throw new IllegalArgumentException("Empty or null source address input");
        }

        if (srcPort < 0) {
            throw new IllegalArgumentException("Invalid source port input");
        }

        this.type = tp;
        this.sourceAddress = srcAddr;
        this.sourcePort = srcPort;
    }


    //-Getters------------------------||
    public String getType() {
        return this.type;
    }

    public String getSourceAddress() {
        return this.sourceAddress;
    }

    public int getSourcePort() {
        return this.sourcePort;
    }
    //--------------------------------||

    /**
     * Static method to build any of the messages with a JSONType
     * @param obj the JSON object
     * @return a Message of whatever type is in the JSONType
     * @throws InvalidObjectException
     */
    public static Message buildMessage(JSONType arg) throws InvalidObjectException {

        if (arg == null || !arg.isObject()) {
            throw new InvalidObjectException("Invalid JSON object");
        }

        JSONObject obj = (JSONObject) arg;

        if (obj.containsKey("type")) {

            String type;
            String sourceAddr;
            int sourcePrt;
            String targetUID;
            String key;
            String value;
            String[] findNode_and_findValue_keys = {"type", "source-address", "source-port", "target-uid"};
            String[] nodeKeys = {"type", "source-address", "source-port", "hosts"};
            String[] store_and_value_keys = {"type", "source-address", "source-port", "key", "value"};


            switch (obj.getString("type")) {

                case ("FINDNODE"):
                    obj.checkValidity(findNode_and_findValue_keys);
                    type = obj.getString("type");
                    sourceAddr = obj.getString("source-address");
                    sourcePrt = obj.getInt("source-port");
                    targetUID = obj.getString("target-uid");
                    return new FindNode(type, sourceAddr, sourcePrt, targetUID);

                case ("FINDVALUE"):
                    obj.checkValidity(findNode_and_findValue_keys);
                    type = obj.getString("type");
                    sourceAddr = obj.getString("source-address");
                    sourcePrt = obj.getInt("source-port");
                    targetUID = obj.getString("target-uid");
                    return new FindValue(type, sourceAddr, sourcePrt, targetUID);
                    // Per the instructions this shuld be nodelist
                case ("NODELIST"):
                    obj.checkValidity(nodeKeys);
                    type = obj.getString("type");
                    sourceAddr = obj.getString("source-address");
                    sourcePrt = obj.getInt("source-port");
                    
                    // Deserialize hosts array
                    JSONArray hostsArray = obj.getArray("hosts");
                    Host[] hosts = new Host[hostsArray.size()];
                    for (int i = 0; i < hostsArray.size(); i++) {
                        try {
                            JSONObject hostObj = hostsArray.getObject(i);
                            hosts[i] = new Host(hostObj);
                        } catch (InvalidObjectException e) {
                            throw new IllegalArgumentException("Failed to deserialize host at index " + i + ": " + e.getMessage());
                        }
                    }
                    return new Node(type, sourceAddr, sourcePrt, hosts);
                   
                case ("STORE"):
                    obj.checkValidity(store_and_value_keys);
                    type = obj.getString("type");
                    sourceAddr = obj.getString("source-address");
                    sourcePrt = obj.getInt("source-port");
                    key = obj.getString("key");
                    value = obj.getString("value");
                    return new Store(type, sourceAddr, sourcePrt, key, value);

                case ("VALUE"):
                    obj.checkValidity(store_and_value_keys);
                    type = obj.getString("type");
                    sourceAddr = obj.getString("source-address");
                    sourcePrt = obj.getInt("source-port");
                    key = obj.getString("key");
                    value = obj.getString("value");
                    return new Value(type, sourceAddr, sourcePrt, key, value);
                
                default:
                    throw new IllegalArgumentException("Could not convert object into a Message object");
            }

        } else {
            throw new IllegalArgumentException("Inputted JSON object doesn't contain the 'type' key required for this method");
        }
    }

    /**
     * Serializes the fields of the message into a JSON object
     * @return a JSON object containing the fields of this message
     */
    public abstract JSONObject serialize();

    /**
     * Helper method for if theres ever a need to check the type of a message object
     * @return the value associated with the message type
     */
    public String getMessageType() {
        switch (this.type) {
            case "FINDVALUE":
                return "FINDVALUE";
            case "FINDNODE":
                return "FINDNODE";
            case "STORE":
                return "STORE";
            case "NODELIST":
                return "NODELIST";
            case "VALUE":
                return "VALUE";
            case null:
            case "":
                return "Type field is empty";
            default:
                return "Invalid type field";
        }
    }

    /**
     * Returns true if the inputted string is a valid message type, false otherwise
     * @return ^
     */
    public boolean checkType(String tp) {
        switch (tp) {
            case "FINDVALUE":
            case "FINDNODE":
            case "STORE":
            case "NODE":
            case "NODELIST":
            case "VALUE":
                return true;
            default:
                return false;
        }
    }

    /**
     * Helper method for whenever a string needs to be encoded into Base64
     * (can be removed if not as useful as I thought)
     * @param str to be encoded string
     * @return a base 64 encoded version of the inputted string
     */
    public static String toBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

    /**
     * helper method for the constructor
     * checks if the inputted string is Base64
     * @param str inputted string
     * @return true if the string is Base64, false otherwise
     */
    public boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Helper class to do basic string checks (made static in case it helps outside of expected classes)
     * @param str inputted string
     * @return true if the string isn't empty or null, false otherwise
     */
    public static boolean checkString(String str) {
        if (str.isEmpty() || str.isBlank() || str == null) {
            return false;
        }

        return true;
    }
}
