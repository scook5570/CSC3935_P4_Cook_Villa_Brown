package dht;

import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

/**
 * Class representing thread-safe KeyValueStore for DHT node
 */
public class KeyValueStore implements JSONSerializable {

    // Create a map for the store which will be initialized as a HashMap
    private final Map<String, String> store;

    /**
     * Default constructor for the KeyValueStore
     */
    public KeyValueStore() {
        this.store = new HashMap<>();
    }

    /**
     * Constructor for the KeyValueStore
     * @param obj JSON object to intialize KeyValueStore
     * @throws InvalidObjectException
     */
    public KeyValueStore(JSONType obj) throws InvalidObjectException {
        this.store = new HashMap<>();
        deserialize(obj);
    }

    /**
     * Stores a value for a given key
     * @param key the key to store the value to
     * @param value the value to store to the key
     */
    public synchronized void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Retrieves a value from a given key
     * @param key the key to retrieve the value from
     * @return the value from the key
     */
    public synchronized String getValue(String key) {
        return store.get(key);
    }

    /**
     * Checks whether a key is contained in the store
     * @param key the key to check for
     * @return true if the key is contained or false if it is not
     */
    public synchronized boolean containsKey(String key) {
        return store.containsKey(key);
    }

    /**
     * Populate the KeyStoreValue from the JSONObj
     * @param obj the JSON object to deserialize
     * @throws InvalidObjectException if the JSONObj is in an incorrect format
     */
    @Override
    public synchronized void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Expected KeyValueStore object.");
        }

        JSONObject storeObj = (JSONObject) obj;

        String[] requiredKeys = { "data" };
        storeObj.checkValidity(requiredKeys);

        JSONArray dataArray = storeObj.getArray("data");

        store.clear();

        // Iterate through each key-value pair object in the array
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject pair = dataArray.getObject(i);

            // Check that each pair contains both "key" and "value"
            String[] pairKeys = { "key", "value" };
            pair.checkValidity(pairKeys);

            // Store the key-value pair
            store.put(pair.getString("key"), pair.getString("value"));
        }
    }

    /**
     * Method to pretty print the contents of the keyvalue store
     */
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("KevValueStore {\n");
        store.forEach((k, v) -> sb.append("  ").append(k).append(" : ").append(v).append("\n"));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes the KeyValueStore into a JSONObj which is an arrya of key-value
     * pairs
     * @return the store as a JSONObj
     */
    @Override
    public synchronized JSONType toJSONType() {
        JSONArray arr = new JSONArray();
        store.forEach((k, v) -> {
            JSONObject obj = new JSONObject();
            obj.put("key", k);
            obj.put("value", v);
            arr.add(obj);
        });

        JSONObject storeObj = new JSONObject();
        storeObj.put("data", arr);
        return storeObj;
    }
}
