/*
LDNS.java

Please run this on rhea.cs.rit.edu
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class LDNS extends Thread{

    static int portNumber = 33001;
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    private ServerSocket dnsServer;
    private HashMap<String, String> dnsLookupTable = new HashMap<>();

    LDNS() {
    }

    LDNS(int port) throws Exception{
        this.dnsServer = new ServerSocket(port);
        this.dnsLookupTable.put("serviceserver", "129.21.37.49/34000");
        this.dnsLookupTable.put("cdnservice", "129.21.37.49/35000");
    }

    /**
     * Returns domain from the URL
     * @param addr    URL from which domain is to be found
     * @return    Domain in the URL passed
     */
    private String getDomain(String addr) {
        String parts[] = addr.split("\\.");
        return parts[1];
    }

    /**
     * Using this method DNS server waits for someone to connect to
     * it. Once it accepts a connection it return a new port number and
     * start a thread at that port number which will be dedicated to that socket.
     * @throws Exception
     */
    private void waitForConn() throws Exception {
        Socket temp = this.dnsServer.accept();
        new PrintWriter(temp.getOutputStream(), true).println("" + portNumber);
        new LDNS(portNumber).start();
        temp.close();
        portNumber++;
    }

    /**
     * Run method.
     * In this DNS server connects to another server and accepts requests as DNS query.
     * A DNS query is formed as "host:<host address>".
     * Once IP for that host is found that is returned to requesting server.
     */
    public void run() {
        try{
            this.client = this.dnsServer.accept();
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String input = in.readLine();
            String parameter = input.split("/")[1];
            String source = this.client.getInetAddress().getHostAddress();
            input = input.split("/")[0];
            while(input.startsWith("host")) {
                System.out.println("Searching : " + input);
                String host = this.getDomain(input.split(":")[1]);
                Socket hostSocket = new Socket(
                        this.dnsLookupTable.get(host).split("/")[0],
                        Integer.parseInt(this.dnsLookupTable.get(host).split("/")[1]));
                PrintWriter tempWriter = new PrintWriter(hostSocket.getOutputStream(), true);
                BufferedReader tempReader = new BufferedReader(new InputStreamReader(hostSocket.getInputStream()));
                tempWriter.println(input + "/" + source);
                input = tempReader.readLine();
                tempWriter.close();
                tempReader.close();
                hostSocket.close();
            }
            System.out.println("Found: " + input);
            this.out.println(input);

        } catch(Exception e) {

        }

    }

    /**
     * The main method.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int serverPort = 33000;
        Scanner input = new Scanner(System.in);

        LDNS ldns = new LDNS(serverPort);
        System.out.println(InetAddress.getLocalHost().getHostAddress() + "/" + ldns.dnsServer.getLocalPort());

        while(true) {
            ldns.waitForConn();
        }
    }
}
