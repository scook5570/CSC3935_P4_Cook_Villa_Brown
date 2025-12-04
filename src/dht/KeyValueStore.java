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

    // Inner class to store both the original key and value
    private static class KeyValueEntry {
        final String originalKey;
        final String value;

        /**
         * Constructor for KeyValueEntry
         * 
         * @param originalKey the original key string
         * @param value       the value string
         */
        KeyValueEntry(String originalKey, String value) {
            this.originalKey = originalKey;
            this.value = value;
        }
    }

    // Create a map for the store which maps identifier (UID) -> KeyValueEntry
    private final Map<String, KeyValueEntry> store;

    /**
     * Default constructor for the KeyValueStore
     */
    public KeyValueStore() {
        this.store = new HashMap<>();
    }

    /**
     * Constructor for the KeyValueStore
     * 
     * @param obj JSON object to intialize KeyValueStore
     * @throws InvalidObjectException
     */
    public KeyValueStore(JSONType obj) throws InvalidObjectException {
        this.store = new HashMap<>();
        deserialize(obj);
    }

    /**
     * Stores a value for a given identifier and original key
     * 
     * @param identifier  the UID identifier to store the value to
     * @param originalKey the original key string
     * @param value       the value to store
     */
    public synchronized void put(String identifier, String originalKey, String value) {
        store.put(identifier, new KeyValueEntry(originalKey, value));
    }

    /**
     * Stores a value for a given identifier (backward compatibility - no original
     * key)
     * 
     * @param identifier the identifier to store the value to
     * @param value      the value to store to the identifier
     */
    public synchronized void put(String identifier, String value) {
        store.put(identifier, new KeyValueEntry(null, value));
    }

    /**
     * Retrieves a value from a given identifier
     * 
     * @param identifier the identifier to retrieve the value from
     * @return the value from the identifier
     */
    public synchronized String getValue(String identifier) {
        KeyValueEntry entry = store.get(identifier);
        return entry != null ? entry.value : null;
    }

    /**
     * Retrieves the original key from a given identifier
     * 
     * @param identifier the identifier to retrieve the original key from
     * @return the original key, or null if not found or not stored
     */
    public synchronized String getOriginalKey(String identifier) {
        KeyValueEntry entry = store.get(identifier);
        return entry != null ? entry.originalKey : null;
    }

    /**
     * Gets all stored entries as a map of identifier -> [originalKey, value]
     * 
     * @return map containing all entries
     */
    public synchronized Map<String, String[]> getAllEntries() {
        Map<String, String[]> result = new HashMap<>();
        store.forEach((identifier, entry) -> {
            result.put(identifier, new String[] { entry.originalKey, entry.value });
        });
        return result;
    }

    /**
     * Checks whether an identifier is contained in the store
     * 
     * @param identifier the identifier to check for
     * @return true if the identifier is contained or false if it is not
     */
    public synchronized boolean containsKey(String identifier) {
        return store.containsKey(identifier);
    }

    /**
     * Populate the KeyStoreValue from the JSONObj
     * 
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

            // Store the key-value pair (use key as both identifier and originalKey for
            // deserialization)
            String key = pair.getString("key");
            String value = pair.getString("value");
            store.put(key, new KeyValueEntry(key, value));
        }
    }

    /**
     * Method to pretty print the contents of the keyvalue store
     * 
     * @return string representation of the keyvalue store
     */
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("KevValueStore {\n");
        store.forEach((identifier, entry) -> sb.append("  ").append(identifier).append(" : ").append(entry.value)
                .append("\n"));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes the KeyValueStore into a JSONObj which is an arrya of key-value
     * pairs
     * 
     * @return the store as a JSONObj
     */
    @Override
    public synchronized JSONType toJSONType() {
        JSONArray arr = new JSONArray();
        store.forEach((identifier, entry) -> {
            JSONObject obj = new JSONObject();
            obj.put("key", identifier);
            obj.put("value", entry.value);
            arr.add(obj);
        });

        JSONObject storeObj = new JSONObject();
        storeObj.put("data", arr);
        return storeObj;
    }
}
