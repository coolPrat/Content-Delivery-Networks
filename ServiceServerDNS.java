/*
ServiceServerDNS.java

please run this on rhea.cs.rit.edu
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class ServiceServerDNS {

    ServerSocket serviceDNS;
    String contentCDN = "www.cdnservice.com";
    PrintWriter out;
    BufferedReader in;


    ServiceServerDNS(int port) throws IOException {
        this.serviceDNS = new ServerSocket(port);
    }

    /**
     * This method accepts a connection and responds to DNS query
     * fired by the server connected.
     * @throws IOException
     */
    private void waitForReq() throws IOException {
        while(true) {
            Socket client = this.serviceDNS.accept();
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String input = in.readLine();
            if (input.startsWith("host")) {
                System.out.println("searching: " + input);
                String url = input.split(":")[1];
                if (url.startsWith("content")) {
                    System.out.println("Sending: " + contentCDN);
                    this.out.println("host:" + contentCDN);
                }
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
        int serverPort = 34000;
        ServiceServerDNS serviceDNS = new ServiceServerDNS(serverPort);
        serviceDNS.waitForReq();
    }
}
