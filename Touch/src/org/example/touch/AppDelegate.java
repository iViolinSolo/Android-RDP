package org.example.touch;

import android.app.Application;
import android.util.Log;

import java.io.IOException;
import java.net.*;

public class AppDelegate extends Application {
	
	private ClientThread client;
	public int mouse_sensitivity = 1;
	public boolean connected = false;
	public boolean network_reachable = true;
	
	public void onCreate(){
		super.onCreate();
	}
	
	public void createClientThread(String ipAddress, int port){
		client = new ClientThread(ipAddress, port);
		
		Thread cThread = new Thread(client);
	    cThread.start();
	}
	
	public void sendMessage(String message){
		client.sendMessage(message);
	}
	
	public void sendMsgTransImg(String message) {
		client.sendMsgTransImg(message);
	}
	
	public void stopServer(){
		if(connected){
			client.closeSocket();
		}
	}
	
	// ClientThread Class implementation
     public class ClientThread implements Runnable {
    	
    	private InetAddress serverAddr;
    	private int serverPort;
		private final int imgTransPort = 6473;
    	private DatagramSocket socket,imgRecSocket;
    	byte[] buf = new byte[1000];
    	byte[] imgBuf = new byte[8192];
    	
    	public ClientThread(String ip, int port){
    		try{
    			serverAddr = InetAddress.getByName(ip);
    		}
    		catch (Exception e){
    			Log.e("ClientActivity", "C: Error", e);
    		}
    		serverPort = port;
    	}
    		
    	//Opens the socket and output buffer to the remote server
        public void run() {
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(1000);
                imgRecSocket = new DatagramSocket();//receive image socket
//                imgRecSocket.setSoTimeout(1000);
                connected = testConnection();
                if(connected)
                	surveyConnection();
            }
            catch (Exception e) {
                Log.e("ClientActivity", "Client Connection Error", e);
            }
        }
        
        public void sendMsgTransImg(String message) {//same as the following method
			try {
				imgBuf = message.getBytes();
//				DatagramPacket out = new DatagramPacket(imgBuf, imgBuf.length, serverAddr, imgTransPort);
				DatagramPacket out = new DatagramPacket(imgBuf, imgBuf.length, serverAddr, serverPort);
				imgRecSocket.send(out);
                Log.d("ClientActivity", "Sent." + message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
        public void sendMessage(String message){
    		try {
                buf = message.getBytes();
                DatagramPacket out = new DatagramPacket(buf, buf.length, serverAddr, serverPort);
                socket.send(out);
                Log.d("ClientActivity", "Sent." + message);
                network_reachable = true;
            }
    		catch (Exception e){ 
    			Log.e("ClientActivity", "Client Send Error:");
    			if(e.getMessage().equals("Network unreachable")){
    				Log.e("ClientActivity", "Netork UNREACHABLE!!!!:");
    				network_reachable = false;
    			}
    			closeSocketNoMessge();
    		}
        }
        
        public void closeSocketNoMessge(){
        	socket.close();
        	imgRecSocket.close();
        	connected = false;
        }
        
        public void closeSocket(){
        	sendMessage(new String("Close"));
        	socket.close();
        	imgRecSocket.close();
        	connected = false;
        }
        
        private boolean testConnection(){
	        	try {
		        	 Log.d("Testing", "Sending");
		        	 
		        	 if(!connected) buf = new String("Connectivity").getBytes();
		        	 else buf = new String("Connected").getBytes();
		        	 
		             DatagramPacket out = new DatagramPacket(buf, buf.length, serverAddr, serverPort);
		             socket.send(out);
		             Log.d("Testing", "Sent");
		        	}
	        	catch(Exception e){return false;}
	        	
	        	try{
	        		Log.d("Testing", "Receiving");
	        		DatagramPacket in = new DatagramPacket(buf, buf.length);
	        		socket.receive(in);
	        		Log.d("Testing", "Received");
	        		return true;
	        	}
	        	catch(Exception e){return false;}
        }
        
        private void surveyConnection(){
        	int count = 0;
        	while(connected){
        		try{Thread.sleep(1000);}
	        	catch(Exception e){}
	        	
        		if(!testConnection())
        			count++;
        		else
        			count = 0;
        		
        		if(count == 3){
        			closeSocket();
        			return;
        		}
        	}
        }
         
    }
}
