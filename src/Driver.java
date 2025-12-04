/*
 * Copyright (C) 2023 - 2024  Zachary A. Kissel 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Scanner;

import dht.DistributedHashTable;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.JsonIO;
import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.Tuple;
import model.Configuration;


/**
 * This is the main service. Its job is to run a DHT node. 
 */
public class Driver
{
    public static boolean doHelp = false;               // True if help option present.
    private static Configuration conf = null;           // The configuration information.
    private static String configFile = "config.json";   // Default configuration file.
    

    /**
     * Prints the usage to the screen and exits.
     */
    public static void usage() {
        System.out.println("usage:");
        System.out.println("  dhtnode --config <config>");
        System.out.println("  dhtnode --help");
        System.out.println("options:");
        System.out.println("  -c, --config\t\tConfig file to use.");
        System.out.println("  -h, --help\t\tDisplay the help.");
        System.exit(1);
    }

    /**
     * Processes the command line arugments.
     * @param args the command line arguments.
     */
    public static void processArgs(String[] args)
    {
        OptionParser parser;
        boolean doHelp = false;
        boolean doConfig = false;

        LongOption[] opts = new LongOption[2];
        opts[0] = new LongOption("help", false, 'h');
        opts[1] = new LongOption("config", true, 'c');
        
        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("hc:");


        while (parser.getOptIdx() != args.length)
        {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst())
            {
                case 'h':
                    doHelp = true;
                break;
                case 'c':
                    doConfig = true;
                    configFile = currOpt.getSecond();
                break;
                case '?':
                    System.out.println("Unknown option: " + currOpt.getSecond());
                    usage();
                break;
            }
        }

        // Verify that that this options are not conflicting.
        if ((doConfig && doHelp))
            usage();
        
        if (doHelp)
            usage();
        
        try 
        {
            loadConfig();
        } 
        catch (FileNotFoundException e) 
        {
            System.err.println("dhtnode: " + e);
            System.exit(1);
        }
    }

    /**
     * Loads the configuration file.
     * @throws FileNotFoundException if the configuration file could not be found.
     */
    public static void loadConfig() throws FileNotFoundException
    {
        try
        { 
            JSONObject obj = JsonIO.readObject(new File(configFile));
            conf = new Configuration(obj);
        }
        catch(InvalidJSONException ex)
        {
            System.err.println("dhtnode: invalid JSON in configuration file.");
            System.out.println(ex);
            System.exit(1);
        }
        catch(InvalidObjectException ex)
        {
            System.err.println("dhtnode: invalid configuration file.");
            System.out.println(ex);
            System.exit(1);
        }
    }

    /**
     * Runs the command line interface portion of the dhtnode application.
     * @throws InterruptedException a donwload thread was interrupted.
     * @throws IOException data could not be accessed.
     */
    public static void doCLI() throws InterruptedException, IOException
    {
        String command;
        boolean done = false;

        // Construct the distributed hash table (dht). This is our abstraction 
        // of the network service.
        DistributedHashTable dht = new DistributedHashTable(conf.getPort(), conf.getAddress(), conf.getUID(), 
            conf.getBootstrapAddr(), conf.getBootstrapPort());

        System.out.println("Please type .help for help or .quit to exit the application.");

        try (Scanner scan = new Scanner(System.in))
        {
            while (!done)
            {
                // Read a command.
                do 
                {   
                    System.out.print("> ");
                    command = scan.nextLine();
                    command = command.strip();
                } while (command.equals(""));

                // Exit the application.
                if (command.equalsIgnoreCase(".quit"))
                    done = true;

            // Put a new key value pair into the DHT.
            else if (command.equalsIgnoreCase(".put"))
            {
                // Get the key.
                System.out.print("Enter the key: ");
                String key = scan.nextLine();

                // Get the value.
                System.out.print("Enter the value: ");
                String val = scan.nextLine();

                // Store the value in the DHT.
                System.out.print("Adding key-value pair . . . ");
                dht.put(key, val);
                System.out.println("[ OK ]");
            }

            // Lookup a value in the DHT.
            else if (command.equalsIgnoreCase(".lookup"))
            {
                // Get the key to search for.
                System.out.print("Enter the key: ");
                String key = scan.nextLine();

                // Try to lookup the value in the hash table.
                String res = dht.get(key);
                if (res == null)
                    System.out.println("No such key.");
                else 
                    System.out.println("Value: " + res);
            }

            // Let's allow the user to view the current routing table. 
            else if (command.equalsIgnoreCase(".showroutes"))
            {
                System.out.println();
                System.out.println("Routing Table");
                System.out.println("-------------");
                System.out.println(dht.getRoutes());
            }

            // Lets allow the user to view their node id.
            else if (command.equalsIgnoreCase(".showuid"))
                System.out.println(conf.getUID());

            // Display the local kvstore.
            else if (command.equalsIgnoreCase(".kvstore"))
                System.out.println(dht.getKVStore());

            // Let's display a help menu.
            else if (command.equalsIgnoreCase(".help"))
            {
                System.out.println();
                System.out.println(".help\t\tdisplay this message.");
                System.out.println(".quit\t\texit the application.");
                System.out.println(".put\t\tadd a key-value pair to the DHT.");
                System.out.println(".lookup\t\tget value associated with a key in the DHT.");
                System.out.println(".showroutes\tdisplays the routing table.");
                System.out.println(".showuid\tdisplays this nodes UID.");
                System.out.println(".kvstore\tdisplay the contents of the local kv-store.");
            }

            // We don't understand the given command.
            else
            {
                System.out.println("Error: \"" + command + "\" unknown.");
            }

        }
        }
    }

    /**
     * The entry point
     * @param args the command line arguments.
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException, IOException
    {
    
        if (args.length > 2)
            usage();

        processArgs(args); 

        // Interact with the DHT.
        doCLI();

        // We don't care about our threads, just crudely shutdown.
        System.exit(0);
    }
}