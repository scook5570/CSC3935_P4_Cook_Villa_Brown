import java.io.InvalidObjectException;
import java.util.Base64;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

/**
 * class representing a routing record
 */
public class Host implements JSONSerializable {

    String address;
    int port;
    String UID;

    /**
     * Builds a Host object from the inputted fields
     * @param addr IP address
     * @param prt port number
     * @param uid host UID
     */
    public Host(String addr, int prt, String uid) {

        if (!Message.checkString(addr)) {
            throw new IllegalArgumentException("Address input is null or empty");
        }

        if (!Message.checkString(uid)) {
            throw new IllegalArgumentException("UID input is null or empty");
        } 
        
        if (prt < 0) {
            throw new IllegalArgumentException("Port number can't be less than 0");
        }

        this.address = addr;
        this.port = prt;

        // if the inputted UID isn't Base64, make it so
        String temp = isBase64(uid) ? uid : Message.toBase64(uid);

        this.UID = temp;
    }

    /**
     * Builds a Host object from the inputted fields
     * @param arg JSONType containing the fields of this Host
     */
    public Host(JSONType arg) throws InvalidObjectException {
       deserialize(arg);
    }

    /**
     * Serializes the fields of the message into a JSON object
     * @return a JSON object containing the fields of this message
     */
    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        
        JSONObject obj;
        if (!arg0.isObject()) {
            throw new InvalidObjectException("Inputted JSON type is an invalid object");
        }

        obj = (JSONObject) arg0;

        String[] keys = {"address", "port", "uid"};
        obj.checkValidity(keys);

        String addr = obj.getString("address");
        int prt = obj.getInt("port");
        String uid = obj.getString("uid");

        if (!Message.checkString(addr)) {
            throw new IllegalArgumentException("Address input is null or empty");
        }

        if (!Message.checkString(uid)) {
            throw new IllegalArgumentException("UID input is null or empty");
        } 
        
        if (prt < 0) {
            throw new IllegalArgumentException("Port number can't be less than 0");
        }

        this.address = addr;
        this.port = prt;
        this.UID = isBase64(obj.getString("uid")) ? obj.getString("uid") : Message.toBase64(obj.getString("uid"));
    }

    /**
     * Deserializes the fields of a JSON object into a message type
     * @return a Message object with it's fields populated by those in the packet
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("address", this.address);
        obj.put("port", this.port);
        obj.put("uid", this.UID);
        return obj;
    }

    /**
     * helper method for the constructor
     * checks if the inputted string is Base64
     * @param str inputted string
     * @return true if the string is Base64, false otherwise
     */
    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void printAttributes() {
        System.out.println("address: " + this.address + "\nport: " + this.port + "\nuid: " + this.UID);
    }

    /**
     * returns the target UID of this message
     * @return ^
     */
    public String getTargetUid() {
        return this.UID;
    }
    
}
