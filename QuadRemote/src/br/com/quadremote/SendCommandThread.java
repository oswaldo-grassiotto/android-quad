package br.com.quadremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * Command Sender
 * 
 * Constantly tries to send a command trough the assigned server via UDP Socket
 */
public class SendCommandThread extends Thread {

	private final InetAddress SERVER_ADDRESS;
	private final int SERVER_PORT = 6774;
	
	private byte[] commandBytes;
	private DatagramSocket socket;
	
	public SendCommandThread() throws SocketException, UnknownHostException{
		this.SERVER_ADDRESS = InetAddress.getByName("192.168.43.201");
		this.socket = new DatagramSocket(SERVER_PORT);
	}
	
	public void setCommandBytes(byte[] commandBytes){
		this.commandBytes = commandBytes;
	}
	
	@Override
	public void run() {
		try{
			while(true){
				if(commandBytes != null){
					Log.d("Command Sender", "Sending command: "  + ((int) commandBytes[0]));
					DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length, SERVER_ADDRESS, SERVER_PORT);
					socket.send(packet);
					
					if(((int) commandBytes[0]) == 4){
						//if we've requested a list of available resolutions, read it
						byte[] resolutionsData = new byte[100];
						DatagramPacket resolutionsPacket = new DatagramPacket(resolutionsData, resolutionsData.length);
						socket.receive(resolutionsPacket);
						resolutionsData = resolutionsPacket.getData();
						String resolutionsStr = new String(resolutionsData, "UTF-8").trim();
						Log.d("Command Sender", "Data received: " + resolutionsStr);
						QuadRemote.setSupportedResolutions(resolutionsStr);
					}
					
					commandBytes = null;
				}
				
				
			}
		} catch (Exception e) {
			Log.e("Command Sender", e.getMessage());
		}
	}
}
