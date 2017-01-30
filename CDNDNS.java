/*
CDNDNS.java

Please run this on rhea.cs.rit.edu
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class CDNDNS {

    ServerSocket cdnDNS;
    PrintWriter out;
    BufferedReader in;
    HashMap<String, String> cdnMap;
    HashMap<String, String> geoMap;
    String cdn1 = "129.21.34.80:40000";
    String cdn2 = "129.21.37.41:40000";
    String cdn3 = "129.21.30.37:40000";

    CDNDNS(int port) throws IOException {
        this.cdnDNS = new ServerSocket(port);
        this.cdnMap = new HashMap<>();
        this.geoMap = new HashMap<>();
        this.fillCDNMap();
        this.fillGeoMap();

    }

    /**
     * This method puts data to a cdnMap which hold info about which file is stored on which
     * cdn server.
     */
    private void fillCDNMap() {
        this.cdnMap.put("FS1E1", cdn1);
        this.cdnMap.put("FS2E1", cdn1);
        this.cdnMap.put("FS3E1", cdn1);
        this.cdnMap.put("FS4E1", cdn1);
        this.cdnMap.put("FS5E1", cdn2);
        this.cdnMap.put("FS6E1", cdn2);
        this.cdnMap.put("FS7E1", cdn2);
        this.cdnMap.put("FS8E1", cdn3);
        this.cdnMap.put("FS9E1", cdn3);
        this.cdnMap.put("FS10E1", cdn3);
    }

    /**
     * This method is used to add data in geoMap which holds information about
     * closest CDN servers to particular region. (Assuming region can be found from IP
     * it maps an IP to a CDN so if a client runs on some different server it will get data from different servers.)
     */
    private void fillGeoMap() {
        this.geoMap.put("129.21.22.196", cdn1);
        this.geoMap.put("129.21.37.41", cdn2);
        this.geoMap.put("129.21.30.37", cdn3);
    }

    /**
     * Using this function CDNDNS waits for a request from another server.
     * The request can be to find closest CDN for a client or from a CDN server to
     * find CDN server which holds a particular file.
     * The two requests are identified by prefixes (host and file).
      * @throws IOException
     */
    private void waitForReq() throws IOException {
        while(true) {
            Socket client = this.cdnDNS.accept();
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String input = in.readLine();
            if (input.startsWith("host")) {
                System.out.println("Searching: " + input);
                String url = input.split(":")[1];
                String loc = url.split("/")[1];
                String res = this.geoMap.get(loc);
                System.out.println("Found: " + res);
                out.println(res);
            } else if(input.startsWith("file")) {
                System.out.println("Searching file: " + input);
                String filename = input.split(":")[1];
                System.out.println("Found on: " + this.cdnMap.get(filename));
                out.println(this.cdnMap.get(filename));
            }
            out.close();
            in.close();
            client.close();
        }
    }

    /**
     * The main method.
     * @param args
     * @throws IOException
     */

    public static void main(String[] args) throws IOException {
        CDNDNS cdndns = new CDNDNS(35000);
        cdndns.waitForReq();
    }
}
