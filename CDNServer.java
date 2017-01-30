/*
CDNServer.java

please run this server on one of the following servers: comet, elvis, queeg
 */

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class CDNServer extends Thread{
    static int portnumber = 40005;
    Socket server2, dnsServer;
    int numberOfFiles = 0;
    int thresholdFiles = 5;
    static ServerSocket s;
    int id, dnsPort = 35000;
    HashMap<String, String> list, secList;
    static HashMap<String, String> cdnServers;
    ServerSocket ss;
    String dnsAddr = "129.21.37.49", name, sourceFilePath;
    PrintWriter pwDns;
    BufferedReader brDns;


    public CDNServer(String name, int id, ServerSocket ss) {
        this.id = id;
        this.ss = ss;
        this.name = name;
        this.sourceFilePath = System.getProperty("user.dir") + "/" + this.name;
        list = new HashMap<>();
        secList = new HashMap<>();
        this.fillList();
    }

    private void makeFolders() {
        File root = new File(this.sourceFilePath);
        if (!root.exists()) {
            root.mkdir();
        }
        File primary = new File(this.sourceFilePath + "/Files");
        if(!primary.exists()) {
            primary.mkdir();
        }
        File sec = new File(this.sourceFilePath + "/Sec_Files");
        if(!sec.exists()) {
            sec.mkdir();
        }
    }

    public static void main(String args[]) throws IOException {
        new Thread(new CDNServer(args[0], 0, new ServerSocket(40000))).start();
    }

    /**
     * This method adds data to the list which holds list of files this server is having.
     * We have 2 different folders named Files and Sec_Files.
     * Sec_Files holds copy of files downloaded from another servers and Files will have
     * the files which are supposed to be on the current server.
     *
     * If it finds that number of files available is greater than threshold it will delete
     * all files in Sec_Files folder.
     */
    private void fillList() {
        int num1 = 0, num2 = 0;
        File folder = new File(this.sourceFilePath + "/Files");
        File[] fileList = folder.listFiles();
        for(File f: fileList) {
            if (f.isFile() && !f.isDirectory()) {
                num1++;
                list.put(f.getName().split("\\.")[0], f.getPath());
            }
        }

        folder = new File(this.sourceFilePath + "/Sec_Files");
        fileList = folder.listFiles();
        for(File f: fileList) {
            if (f.isFile() && !f.isDirectory()) {
                num2++;
                secList.put(f.getName().split("\\.")[0], f.getPath());
            }
        }

        if (num1+num2 > this.thresholdFiles) {
            System.out.println("Storage full... Deleting secondary Files");
            this.secList = new HashMap<>();
            this.numberOfFiles = num1;
            num2 = 0;
        }
        this.numberOfFiles = num1+num2;
    }

    /**
     * Connects to client requesting data
     * @param addr    Address of client
     * @param port    port number of client
     * @throws IOException
     */
    private void connectToServer(String addr, int port) throws IOException {
        this.server2 = new Socket(InetAddress.getByName(addr), port);
        BufferedReader br = new BufferedReader(new InputStreamReader(this.server2.getInputStream()));
        String newPort = br.readLine();
        this.server2.close();
        this.server2 = new Socket(InetAddress.getByName(addr), Integer.parseInt(newPort));
    }

    /**
     * This method is used to connect to CDNDNS.
     * This connection is used to get IP of another CDN server holding specific information
     * @throws IOException
     */
    private void connectToDNS() throws IOException {
        System.out.println("connecting");
        this.dnsServer = new Socket(InetAddress.getByName(this.dnsAddr), this.dnsPort);
        this.pwDns = new PrintWriter(this.dnsServer.getOutputStream(), true);
        this.brDns = new BufferedReader(new InputStreamReader(this.dnsServer.getInputStream()));
    }

    /**
     * This method is used to get address of CDN holding file passed as argument.
     * @param file    File to look up
     * @return        Address of the server
     * @throws IOException
     */
    private String getServerAddr(String file) throws IOException {
        this.pwDns.println("file:"+file);
        String serverAddr = this.brDns.readLine();
        return serverAddr;
    }

    /**
     * Downloads file from another CDN to Sec_Files folder.
     * Before downloading it will check if we will go beyond threshold
     * if yes it will first delete all files in Sec_Files and then download requested file.
     * If the file is not found anywhere it will return false
     *
     * @param fileName    Files to download
     * @return
     * @throws IOException
     */
    private boolean getFile(String fileName) throws IOException {

        try {
            System.out.println("Not found on this server... Searching file on other servers...");
            this.connectToDNS();
            String addr = this.getServerAddr(fileName);
            System.out.println("Found on: " + addr.split(":")[0]);
            if (this.numberOfFiles + 1 > this.thresholdFiles) {
                System.out.println("Low Storage space... deleting secondary videos");
                this.numberOfFiles = this.numberOfFiles - this.secList.size();
                this.secList = new HashMap<>();
                File folder = new File(this.sourceFilePath + "/Sec_Files");
                File[] fileList = folder.listFiles();
                for(File f: fileList) {
                    if (f.isFile() && !f.isDirectory()) {
                        f.delete();
                    }
                }
                File f = new File(this.sourceFilePath + "/Sec_Files");
                boolean b = f.mkdir();
            }
            this.connectToServer(addr.split(":")[0], Integer.parseInt(addr.split(":")[1]));

            PrintWriter pw = new PrintWriter(this.server2.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(this.server2.getInputStream()));

            pw.println(fileName);

            String response = br.readLine().trim();
            if (response.startsWith("start")) {
                File file = new File(this.sourceFilePath + "/Sec_Files/" + fileName + ".srt");
                file.setReadable(true);
                file.setWritable(true);
                PrintWriter writer = new PrintWriter(file, "UTF-8");
                String str = br.readLine().trim();
                while (!str.contains("**end**")) {
                    writer.println(str);
                    str = br.readLine();
                }
                writer.close();
                server2.close();
                secList.put(fileName, this.sourceFilePath + "/Sec_Files/" + fileName + ".srt");
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The Run method.
     * This will receive clients request for a file.
     * If file is found on the server it will return the file, else it will
     * look up another server from where it can download file. If o server is found it will return error
     * message to client.
     */
    public void run() {

        if (this.id == 0) {
            while (true) {
                Socket server = null;
                try {
                    byte b[] = new byte[64000];
                    server = this.ss.accept();
                    DataInputStream in = new DataInputStream(server.getInputStream());
                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    out.write(("" + portnumber).getBytes());
                    new Thread(new CDNServer(this.name, 1, new ServerSocket(portnumber))).start();
                    portnumber += 10;
                    server.close();
                } catch (Exception e) {
                    try {
                        server.close();
                    } catch (IOException e1) {

                    }
                }
            }
        } else if (this.id == 1) {
            boolean f2=true;
            DataInputStream in2 = null ;
            DataOutputStream out2 = null ;
            Socket server2;

            while (f2) {
                try {
                    byte b[] = new byte[64000];
                    byte b2[] = new byte[64000];
                    server2 = this.ss.accept();
                    in2 = new DataInputStream(server2.getInputStream());
                    out2 = new DataOutputStream(server2.getOutputStream());
                    boolean flag = true;
                        while (flag) {
                            in2.read(b2);
                            String lookup = new String(b2).trim();
                            FileInputStream reader = null;
                            File f = null;
                            System.out.println("Request for: " + lookup);
                            if (list.containsKey(lookup)) {
                                reader = new FileInputStream(list.get(lookup));
                                f = new File(list.get(lookup));
                            } else if(secList.containsKey(lookup)) {
                                reader = new FileInputStream(secList.get(lookup));
                                f = new File(secList.get(lookup));
                            } else if(getFile(lookup)) {
                                reader = new FileInputStream(secList.get(lookup));
                                f = new File(secList.get(lookup));
                            } else {
                                out2.write("Wrong Filename:".getBytes());
                                server2.close();
                                System.exit(0);
                            }
                            System.out.println("Sending File...");
                                String temp = "start:" + f.getName();
                                out2.write(temp.getBytes());
                                int n;
                                b2 = new byte[64000];
                                while ((n = reader.read(b2)) != -1) {
                                    out2.write(b2);
                                }
                                out2.write("**end**".getBytes());
                                out2.close();
                        }
                    server2.close();
                } catch (Exception e) {

                    try {
                        in2.close();
                        out2.close();
                    } catch (IOException e2) {
                        // TODO Auto-generated catch block
                        e2.printStackTrace();
                    }
//                    System.out.println("error received" + e.getLocalizedMessage());
                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    f2=false;
                }
            }
        }
    }
}
