/*
ServiceServer.java
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class ServiceServer extends Thread{

    private ServerSocket serviceSocket;
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    static int portNumber = 32001;
    static HashMap<String, String> vidMap = new HashMap<>();



    ServiceServer() {
    }

    ServiceServer(int port) throws IOException {
        this.serviceSocket = new ServerSocket(port);
    }

    /**
     * This method will data to vidMap which represents Service providers database
     * of videos it is providing to clients.
     */
    private void setVidMap() {
        vidMap.put("Friends: season 1 - Episod 1", "content.serviceserver.com/FS1E1");
        vidMap.put("Friends: season 2 - Episod 1", "content.serviceserver.com/FS2E1");
        vidMap.put("Friends: season 3 - Episod 1", "content.serviceserver.com/FS3E1");
        vidMap.put("Friends: season 4 - Episod 1", "content.serviceserver.com/FS4E1");
        vidMap.put("Friends: season 5 - Episod 1", "content.serviceserver.com/FS5E1");
        vidMap.put("Friends: season 6 - Episod 1", "content.serviceserver.com/FS6E1");
        vidMap.put("Friends: season 7 - Episod 1", "content.serviceserver.com/FS7E1");
        vidMap.put("Friends: season 8 - Episod 1", "content.serviceserver.com/FS8E1");
        vidMap.put("Friends: season 9 - Episod 1", "content.serviceserver.com/FS9E1");
        vidMap.put("Friends: season 10 - Episod 1", "content.serviceserver.com/FS10E1");
    }

    /**
     * Using this method server waits for a client to connect.
     * Once a client connects it send sa new port number and run a new thread at that
     * port dedicated for that client.
     * @throws IOException
     */
    private void waitForConn() throws IOException {
        Socket temp = this.serviceSocket.accept();
        new PrintWriter(temp.getOutputStream(), true).println("" + portNumber);
        temp.close();
        new ServiceServer(portNumber).start();
        portNumber++;
    }

    /**
     * Thsi method is used to send the video liist from vidMap.
     */
    private void sendVidList() {
        String output = "";
        for (String str: vidMap.keySet()) {
            output += str + ",";
        }
        output = output.substring(0, output.length() - 1);
        this.out.println(output);
    }

    /**
     * Run method.
     * In this server waits for a client to connect. Once it accepts a connection it
     * will wait to receive requests for video list and the and a video.
     */
    public void run() {
        try {
            this.client = this.serviceSocket.accept();
            System.out.println("Client connected: " + this.client.getInetAddress().getHostAddress());
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String input = "";
            while(true) {
                try {
                    input = this.in.readLine();
                    this.in = null;
                    this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    if (input.equals("request:videos")) {
                        this.sendVidList();
                        input = this.in.readLine();
                        if (input.startsWith("request:vid")) {
                            String reqVideo = input.substring(input.indexOf("(")+1, input.length() - 1);
                            System.out.println("Request received for: " + reqVideo);
                            this.out.println(vidMap.get(reqVideo));
                        }
                        this.in = null;
                        this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    } else {
                        this.out.println("error");
                    }
                } catch (Exception e) {
                    this.client.close();
                    this.serviceSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ServiceServer serviceServer = new ServiceServer(32000);
        System.out.println(InetAddress.getLocalHost().getHostAddress() + "/" + serviceServer.serviceSocket.getLocalPort());
        serviceServer.setVidMap();
        while(true) {
            serviceServer.waitForConn();
        }

    }

}
