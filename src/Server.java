import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/*
Sources:
http://www.tutorialspoint.com/javaexamples/net_multisoc.htm
http://stackoverflow.com/questions/11543967/how-to-lock-a-file
 */

public class Server implements Runnable {
    Socket client;

    final static Object lock = new Object();
    static List<Integer> commandList = new ArrayList<Integer>();
    
    Server(Socket client) {
        this.client = client;
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        ServerSocket server = new ServerSocket(port);
        while (true) {
            Socket client = server.accept();
            new Thread(new Server(client)).start();
        }
    }

    public void run() {
        try {
            PrintWriter w = new PrintWriter(client.getOutputStream(), true);
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String query;
            
            while (true) {
                query = r.readLine();
                String[] splitQuery = query.split(" ");

                
                if (splitQuery[0].equals("get")) {
                    synchronized (lock) {
                        String pwd = System.getProperty("user.dir");
                        File file = new File(pwd + "/" + splitQuery[1]);
                        if (file.exists()) {
                            w.println(file.length());
                            r.readLine();

                            InputStream in = new FileInputStream(splitQuery[1]);
                            OutputStream out = client.getOutputStream();
                            byte[] buf = new byte[1024];      // USED TO BE SAME AS CLIENT
                            int len, commandID;
                            
                            Random rand = new Random();
                            while( true ) {
                            	commandID = rand.nextInt(1000);
                            	if( !commandList.contains(commandID)) {
                            		commandList.add(commandID);
                            		break;
                            	}
                            }
                            
                            boolean interrupted = false;
                            // int count = 2;
                            try {
	                            while ((len = in.read(buf)) != -1) {
	                            	System.out.println( "Entered the loop " );
	                                // count++;
	                                if( !commandList.contains( commandID ) ) {
	                                	System.out.println("Entered the if condition.");
	                                	interrupted = true;
	                                	System.out.println("Sending term signal to client.");
	                                	out.write(buf, 0, 1);
	                                	System.out.println("Trying to terminate get.");
	                                	break;
	                                }
	                            	out.write(buf, 0, len);
	                            }
	                        }
                            finally {
                            	System.out.println("Closing input stream.");
                            	
                            	if( interrupted ) {
                            		file.delete();
                            		out.flush();
                            	}
                            	commandList.remove(commandID);
                            	in.close();
                            }

                            // Synchronize with client
                            r.readLine();
                        }
                        else {
                            w.println(splitQuery[1] + ": file does not exist");
                        }
                    }
                }
                else if (splitQuery[0].equals("put")) {
                    synchronized (lock) {
                        long fileLength = Long.parseLong(r.readLine());
                        w.println();

                        String pwd = System.getProperty("user.dir");
                        File file = new File(pwd + "/" + splitQuery[1]);
                        InputStream in = client.getInputStream();
                       
                        OutputStream out = new FileOutputStream(file);

                        byte[] buf = new byte[8192];
                        int len, commandID;
                        boolean isInterrupted = false;
                        
                        Random rand = new Random();
                        while( true ) {
                        	commandID = rand.nextInt(1000);
                        	if( !commandList.contains(commandID)) {
                        		commandList.add(commandID);
                        		break;
                        	}
                        }
                        
                        int count = 3;
                        try {
	                        for (long curr = 0; curr < fileLength; curr += len) {
	                            len = in.read(buf);
	                            count++;
	                            if( !commandList.contains(commandID) || count >= 5 ) {
	                            	System.out.println("Command ID is no longer in the list.");
	                            	isInterrupted = true;
	                            	System.out.println("Sending the signal to reciever.");
	                            	out.write(buf, 0 , 1);
	                            	break;
	                            }
	                            
	                            out.write(buf, 0, len);
	                        }
                        }
                        finally {
                        	System.out.println("Closing the output stream.");
                        	commandList.remove(commandID);
                        	if (isInterrupted ) {
                        		file.delete();
                        	}
                        	out.flush();
                        	out.close();
                        	
                        	
                        }
                        // Synchronize with client
                        w.println();
                    }
                }
                else if (splitQuery[0].equals("delete")) {
                    File deleteFile = new File(splitQuery[1]);
                    Files.deleteIfExists(Paths.get(deleteFile.getPath()));
                    w.println();
                }
                else if (splitQuery[0].equals("ls")) {
                    File pwd = new File(System.getProperty("user.dir"));
                    File[] files = pwd.listFiles();
                    String ls = "";

                    // TODO delineate by newlines, rather than spaces
                    for (File f : files)
                        ls += f.getName() + "  ";

                    w.println(ls);
                }
                else if (splitQuery[0].equals("terminate")) {
                	int command = Integer.parseInt(splitQuery[1]);
                	String tempStr;
                	
                	if( commandList.contains(command) ) {
                		tempStr = "Command found and removed."; 
                        commandList.remove(command);
                	}
                	else {
                		tempStr = "Command is not in execution.";
                	}
                	
                	w.println(tempStr);
                }
                else if (splitQuery[0].equals("cd")) {
                    File pwd = new File(System.getProperty("user.dir"));
                    if (splitQuery[1].equals("..")) {
                        // Check if we're at root
                        if (pwd.getParentFile() == null) {
                            w.println();
                        }
                        else {
                            System.setProperty("user.dir", pwd.getParentFile().getAbsolutePath());
                            w.println();
                        }
                    }
                    else {
                        File newDir = new File(System.getProperty("user.dir") + "/" + splitQuery[1]);
                        if (newDir.exists() && newDir.isDirectory()) {
                            System.setProperty("user.dir", newDir.getAbsolutePath());
                            w.println();
                        }
                        else {
                            printError(splitQuery[0], splitQuery[1], w);
                        }
                    }
                }
                else if (splitQuery[0].equals("mkdir")) {
                    File dir = new File(System.getProperty("user.dir") + "/" + splitQuery[1]);
                    if (dir.mkdir()) {
                        w.println();
                    }
                    else {
                        w.println("Error creating " + splitQuery[1]);
                    }
                }
                else if (splitQuery[0].equals("pwd")) {
                    String pwd = new File(".").getAbsolutePath();
                    w.println(pwd);
                }
                else if (splitQuery[0].equals("quit")) {
                    return;
                }
                else {
                    // Do nothing
                    w.println();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printError(String command, String file, PrintWriter w) {
        w.println(command + ": " + file + ": No such file or directory");
    }
}