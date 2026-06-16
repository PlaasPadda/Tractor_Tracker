package tractor_tracker.app;

import java.io.*;
import java.net.*;
public class Client_sim {
	
	public void connect(){

		String dataReceived;
		DataOutputStream outToServer;
		
		System.out.println("Client started");

		try {

			while (true) {
				//read input from console
				BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Port: ");
				String Port = inFromUser.readLine();
				System.out.println("Msg: ");
				String userString = inFromUser.readLine();
				System.out.println("Sending " + userString + " over port " + Port);
				
				//setup socket
				Socket socket = new Socket("localhost", Integer.parseInt(Port));
				
				//send message over socket
				outToServer = new DataOutputStream(socket.getOutputStream());
				byte[] outByteString = userString.getBytes("UTF-8");
				outToServer.write(outByteString);
				
				//read replied message from socket
				byte[] inByteString = new byte[500] ;
	            int numOfBytes = socket.getInputStream().read(inByteString);
	            dataReceived = new String(inByteString, 0, numOfBytes, "UTF-8");
				System.out.println("Received: " + dataReceived);

				//close connection
				socket.close();
				Thread.sleep(3000);
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
		
	public static void main(String[] args) {
		Client_sim client = new Client_sim();
		client.connect();
	}

}
