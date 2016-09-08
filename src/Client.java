import java.io.*;
import java.net.Socket;
import java.util.Scanner;

class Client {
    public static void main(String[] args) throws IOException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Socket client = new Socket(host, port);
        PrintWriter w = new PrintWriter(client.getOutputStream(), true);
        BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
        boolean isRunning = true;
        Scanner keyboard = new Scanner(System.in);
        String query;
        String[] splitQuery;

        while (isRunning) {
            System.out.print("myftp> ");

            // Send command
            query = keyboard.nextLine();
            splitQuery = query.split(" ");

            if (splitQuery[0].equals("quit")) {
                isRunning = false;
                // Send quit signal
                w.println(query);
            }
            else if (splitQuery[0].equals("terminate")) {
            	w.println(query);
            	String temporary = r.readLine();
            	System.out.println( temporary );
            }
            else if (splitQuery[0].equals("get")) {
                w.println(query);
                String response = r.readLine();

                // TODO this error checking is a little kludgy
                if (!response.contains("file does not exist")) {
                    long fileLength = Long.parseLong(response);
                    w.println();

                    File file = new File(splitQuery[1]);
                    InputStream in = client.getInputStream();
                    OutputStream out = new FileOutputStream(file);
                    byte[] buf = new byte[1024];  // USED TO BE 8192
                    boolean interrupt = false;
                    int len;
                    // int count = 2;
                    try {
	                    for (long curr = 0; curr < fileLength; curr += len) {
	                    	// count++;
	                        len = in.read(buf);
	                        
	                        if( len == 1 ) {
	                        	System.out.println("Recieved the right signal.");
	                        	interrupt = true;
	                        	break;
	                        }
	                        out.write(buf, 0, len);
	                    }
                    }
                    finally {
                    	System.out.println("Closing output streams.");
                    	out.flush();
                    	out.close();
                    	if(interrupt)
                    		file.delete();
                    }
                    
                    w.println();
                }
                else {
                    System.out.println(response);
                }
            }
            else if (splitQuery[0].equals("put")) {
                File file = new File(splitQuery[1]);
                if (file.exists()) {
                    w.println(query);

                    w.println(file.length());
                    r.readLine();

                    InputStream in = new FileInputStream(splitQuery[1]);
                    OutputStream out = client.getOutputStream();
                    byte[] buf = new byte[8192];
                    int count = 2;
                    int len;
                    boolean interrupt = false;
                    try {
	                    while ((len = in.read(buf)) != -1) {
	                        // count++;
	                    	// TODO remove this
	                        // try { Thread.sleep(10); } catch (Exception e) {}
	                    	if( len == 1) {
	                    		System.out.println("Recieved the signal.");
	                    		interrupt = true;
	                    		break;
	                    	}
	                    	
	                        out.write(buf, 0, len);
	                    }
                    }
                    finally {
                    	System.out.println("Closing input buffer.");
                    	
                    	in.close();
                    	if ( interrupt )
                    		file.delete();
                    }

                    // Synchronize with server
                    r.readLine();
                }
                else {
                    System.out.println(splitQuery[1] + ": file does not exist");
                }
            }
            else {
                // Send command
                w.println(query);

                // Receive response
                // TODO possibly better way to do this, fix double line output
                String output = r.readLine();
                if (!output.equals(""))
                    System.out.println(output);
            }
        }

        w.close();
        r.close();
        client.close();
    }
}
