package model;
/*
 * Copyright (C) 2022 -- 2023 Zachary A. Kissel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Configuration implements JSONSerializable
{
    private int port;              // The port to listen on.
    private String addr;           // The address to listen on. 
    private int bootPort;          // The bootstrap port. 
    private String bootAddr;       // The bootstrap address.
    private String uid;            // The nodes UID. 

    /**
     * Builds a configuration from a JSON object.
     * @throws InvalidObjectException if the configuration object is invalid.
     */
    public Configuration(JSONObject obj) throws InvalidObjectException
    {
        deserialize(obj);
    }

    /**
     * Gets the port for the node.
     * @return the server port.
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Get the address the node will use.
     * @return the file directory as a string.
     */
    public String getAddress()
    {
        return addr;
    }

    
    /**
     * Get the bootstrap port. 
     * @return the bootstrap node's port.
     */
    public int getBootstrapPort() 
    {
        return bootPort;
    }

    /**
     * Get the bootstrap address.
     * @return the address for the bootstrap node.
     */
    public String getBootstrapAddr() 
    {
        return bootAddr;
    }

    /**
     * Get the UID of this node. 
     * @return the UID as a base64 string. 
     */
    public String getUID()
    {
        return this.uid;
    }

    /**
     * Deserialize the object into a configuration.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException 
    {
        JSONObject config = null;

        if (!(obj instanceof JSONObject))
            throw new InvalidObjectException("Expected configuration object.");
        
        config = (JSONObject) obj;
        
        // Try to read the port number.
        if (config.containsKey("port"))
            this.port = config.getInt("port");
        else 
            throw new InvalidObjectException("Configuration missing port.");

        // Try to read the location of the file data directory.
        if (config.containsKey("addr"))
            this.addr = config.getString("addr");
        else 
            throw new InvalidObjectException("Configuration missing address.");

        // Try to read the seed file location.
        if (config.containsKey("boot-addr"))
            this.bootAddr = config.getString("boot-addr");
        else 
            throw new InvalidObjectException("Configuration missing bootstrap address.");

        // Try to get the size of the pool, this is optional.
        if (config.containsKey("boot-port")) 
            this.bootPort = config.getInt("boot-port");
        else 
            throw new InvalidObjectException("Configuration missing bootstrap port.");

        if (config.size() > 4)
            throw new InvalidObjectException("Invalid configuration -- superflouous fields.");


        // At this point we have read all of the configuration file. Using this configuration information, 
        // we will generate the uid for this node.
        createUID();
    }

    /**
     * Serialize the object into a JSON string.
     */
    public String serialize() 
    {
        return toJSONType().toJSON();
    }

    /**
     * Constructs a JSON object representing the configuration.
     */
    public JSONType toJSONType() 
    {
        JSONObject obj = new JSONObject();
        obj.put("addr", addr);
        obj.put("port", port);
        obj.put("boot-addr", bootAddr);
        obj.put("boot-port", bootPort);
        return obj;
        
    }

    /**
     * Compute the nodes ID by taking the SHA-1 hash of the 
     * IP address and port pair to determine the UID of this 
     * node.
     */
    private void createUID()
    {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt(this.port);
        try 
        {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.update(this.addr.getBytes());
            hash.update(buff.array());
            this.uid = Base64.getEncoder().encodeToString(hash.digest());
        } 
        catch (NoSuchAlgorithmException e) 
        {
            System.err.println("Internal Error: SHA1 hash not supported.");
        }
    }
}