package messageTypes;

/**
 * Tuple helper class inspired by Dr. Kissel aka Doc Z-Kizzy aka Da Oracle
 * (If you dont like the nicknames they're from Jelly (if you do they're from me))
 * 
 * Used in Store and Value classes
 */
public class Tuple {

    String key;
    String value;

    /**
     * Builds a Tuple from the two inputted values
     * @param ky key
     * @param val value
     */
    public Tuple(String ky, String val) {
        this.key = ky;
        this.value = val;
    }

    /**
     * Returns the key of this tuple
     * @return ^
     */
    public String getKey() {
        return this.key;
    }
    
    /**
     * Returns the values of this tuple
     * @return ^
     */
    public String getValue() {
        return this.value;
    }

    /**
     * prints out the contents of this Tuple
     */
    public void printKeyValuePair() {
        System.out.println("[" + this.key + ":" + this.value + "]");
    }
}
