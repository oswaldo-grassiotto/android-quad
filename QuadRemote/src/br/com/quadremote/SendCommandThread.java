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
	
	private byte[] commandBytes;
	
	private final DatagramSocket SOCKET;
	private final InetAddress SERVER_ADDRESS;
	private final int SERVER_PORT = 6774;
	private final QuadRemote MAIN_ACTIVITY;
	
	private boolean run = true;
	
	public SendCommandThread(QuadRemote mainActivity) throws SocketException, UnknownHostException{
		this.MAIN_ACTIVITY = mainActivity;
		
		//this.SERVER_ADDRESS = InetAddress.getByName("192.168.43.201");
		this.SERVER_ADDRESS = InetAddress.getByName("192.168.3.159");
		this.SOCKET = new DatagramSocket(SERVER_PORT);
		SOCKET.setSoTimeout(4000);
	}
	
	public void setCommandBytes(byte[] commandBytes){
		this.commandBytes = commandBytes;
	}
	
	@Override
	public void run() {
		run = true;
		
		while(run){
			try{
				if(commandBytes != null){
					Log.d("Command Sender", "Sending command: "  + ((int) commandBytes[0]));
					DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length, SERVER_ADDRESS, SERVER_PORT);
					SOCKET.send(packet);
					
					if(((int) commandBytes[0]) == 4){
						//if we've requested a list of available resolutions, read it
						byte[] resolutionsData = new byte[100];
						DatagramPacket resolutionsPacket = new DatagramPacket(resolutionsData, resolutionsData.length);
						SOCKET.receive(resolutionsPacket);
						resolutionsData = resolutionsPacket.getData();
						String resolutionsStr = new String(resolutionsData, "UTF-8").trim();
						Log.d("Command Sender", "Data received: " + resolutionsStr);
						MAIN_ACTIVITY.setSupportedResolutions(resolutionsStr);
					}
					
					commandBytes = null;
				}
			} catch (Exception e) {
				Log.e("Command Sender", "Error in command " + commandBytes[0], e);
				commandBytes = null;
			}
			
		}
	}
	
	public void stopThread(){
		run = false;
		this.SOCKET.close();
	}

	public boolean getRun() {
		return run;
	}
}
