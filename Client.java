/*
Client.java
*/

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket connectToServer;
    private PrintWriter serverOut;
    private BufferedReader serverIn;
    private Socket connectToLDNS;
    private PrintWriter ldnsOut;
    private BufferedReader ldnsIn;
    private Socket connectToCDN;
    private PrintWriter cdnOut;
    private BufferedReader cdnIn;

    private String[] videos;

    Client() {

    }

    /**
     * This function connects to Server of the Service Provider
     * named as ServiceServer.
     *
     * @param serverAddr    IP address
     * @param port          Port number
     * @return              true if connection is successful, false otherwise
     * @throws Exception
     */
    public boolean setConnectToServer(String serverAddr, int port) throws Exception {
        try {
            System.out.println("Connecting to server...");
            this.connectToServer = new Socket(InetAddress.getByName(serverAddr), port);
            this.serverIn = new BufferedReader(new InputStreamReader(this.connectToServer.getInputStream()));
            String newPort = serverIn.readLine();
            this.connectToServer.close();
            this.connectToServer = new Socket(InetAddress.getByName(serverAddr), Integer.parseInt(newPort));
            this.serverOut = new PrintWriter(this.connectToServer.getOutputStream(), true);
            this.serverIn = new BufferedReader(new InputStreamReader(this.connectToServer.getInputStream()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This function connects to Server of the Local DNS
     * named as LDNS.
     *
     * @param dnsAddr    IP address
     * @param port          Port number
     * @return              true if connection is successful, false otherwise
     * @throws Exception
     */
    public boolean setConnectToLDNS(String dnsAddr, int port) throws Exception {
        try {
            System.out.println("Connecting to local DNS...");
            this.connectToLDNS = new Socket(InetAddress.getByName(dnsAddr), port);
            this.ldnsIn = new BufferedReader(new InputStreamReader(this.connectToLDNS.getInputStream()));
            String newPort = ldnsIn.readLine();
            this.connectToLDNS.close();
            this.connectToLDNS = new Socket(InetAddress.getByName(dnsAddr), Integer.parseInt(newPort));
            this.ldnsOut = new PrintWriter(this.connectToLDNS.getOutputStream(), true);
            this.ldnsIn = new BufferedReader(new InputStreamReader(this.connectToLDNS.getInputStream()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This function connects to CDN server named as CDN.
     *
     * @param cdnAddr    IP address
     * @param port          Port number
     * @throws Exception
     */
    public void setConnectToCDN(String cdnAddr, int port) throws Exception {
        System.out.println("Connecting to CDN...");
        this.connectToCDN = new Socket(InetAddress.getByName(cdnAddr), port);
        this.cdnIn = new BufferedReader(new InputStreamReader(this.connectToCDN.getInputStream()));
        String newPort = cdnIn.readLine();
        this.connectToCDN.close();
        this.connectToCDN = new Socket(InetAddress.getByName(cdnAddr), Integer.parseInt(newPort));
        this.cdnOut = new PrintWriter(this.connectToCDN.getOutputStream(), true);
        this.cdnIn = new BufferedReader(new InputStreamReader(this.connectToCDN.getInputStream()));
    }

    /**
     * Requests a file from the CDN server and store it in Download folder.
     * @param fileToLookup    name of file to request to CDN
     * @return
     * @throws IOException
     */
    public String getData(String fileToLookup) throws IOException {

        this.cdnOut.println(fileToLookup);
        String response = this.cdnIn.readLine().trim();
        System.out.println(response);
        if (response.startsWith("start")) {
            File file = new File(System.getProperty("user.dir") + "/Download/" + fileToLookup + ".txt");
            file.setReadable(true);
            file.setWritable(true);
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            String str = this.cdnIn.readLine().trim();
            while(!str.contains("**end**")) {
                writer.println(str);
                str = this.cdnIn.readLine();
            }
            writer.close();
            this.connectToCDN.close();
            return "Data Received";
        } else {
            return "Data Not Found";
        }

    }

    /**
     * Using this method client requests video list to Server
     * and prints the list on stdout.
     * @throws IOException
     */
    public void getVideoList() throws IOException {
        System.out.println("Fetching video list...");
        String result = "";
        String vidRequest = "request:videos";
        this.serverOut.println(vidRequest);
        result = serverIn.readLine();
        this.videos = result.split(",");
        this.printList(this.videos);
    }

    /**
     * Using this method client requests a video from Server.
     * Server responds with a URL for the video. Client then queries DNS
     * and gets IP address of the CDN server from where it can download the
     * Video.
     * @param videoNum    index of the video to download
     * @return
     * @throws Exception
     */
    public String requestVideo(int videoNum) throws Exception {

        String vid = this.videos[videoNum];
        String tempString = "request:vid(";
        this.serverOut.println(tempString + vid + ")");
        String vidAddr = this.serverIn.readLine();
        String parameter = vidAddr.split("/")[1];
        System.out.println("Sending DNS query for " + vidAddr);
        this.ldnsOut.println("host:" + vidAddr);
        String cdnAddr = this.ldnsIn.readLine();
        this.setConnectToCDN(cdnAddr.split(":")[0], Integer.parseInt(cdnAddr.split(":")[1]));
        return this.getData(parameter);
    }

    /**
     * Prints the given array as index : value.
     * @param list    Array to print
     */
    private void printList(String[] list) {
        for (int i = 0; i < list.length; i++) {
            System.out.println(i + " : " + list[i]);
        }
    }

    /**
     * Main method.
     * In this client does following (in given sequence):
     * 1. Connect to Server
     * 2. Connect to LDNS
     * 3. Requests video list from server
     * 4. Requests URL for a specific video
     * 5. Query LDNS and get's IP address of the CDN server holding the video
     * 6. Download the file in Download folder
     *
     * @param args    Command line arguments (ignored)
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);
        String serverAddr, ldnsAddr;
        int serverPort, ldnsPort, vidNo;
        System.out.println("Enter server's address: ");
        serverAddr = input.next();
        System.out.println("Enter server's port: ");
        serverPort = input.nextInt();

        ldnsAddr = "129.21.37.49";
        ldnsPort = 33000;

        Client cl = new Client();

        boolean b1 = cl.setConnectToServer(serverAddr, serverPort);
        while(!b1) {
            b1 = cl.setConnectToServer(serverAddr, serverPort);
        }

        b1 = cl.setConnectToLDNS(ldnsAddr, ldnsPort);

        while(!b1) {
            b1 = cl.setConnectToLDNS(ldnsAddr, ldnsPort);
        }

        cl.getVideoList();

        System.out.println("Which video you want to watch today?");
        vidNo = input.nextInt();

        System.out.println(cl.requestVideo(vidNo));
    }


}
