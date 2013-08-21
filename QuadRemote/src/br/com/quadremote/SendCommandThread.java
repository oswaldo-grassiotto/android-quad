package br.com.quadremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;

/**
 * Command Sender
 * 
 * Constantly tries to send a command trough the assigned server via UDP Socket
 */
public class SendCommandThread extends Thread {

	private final InetAddress SERVER_ADDRESS;
	private final int SERVER_PORT;
	
	private byte[] commandBytes;
	private DatagramSocket socket;
	
	public SendCommandThread(InetAddress serverAddress, int port, DatagramSocket socket){
		this.SERVER_ADDRESS = serverAddress;
		this.SERVER_PORT = port;
		this.socket = socket;
	}
	
	public void setCommandBytes(byte[] commandBytes){
		this.commandBytes = commandBytes;
	}
	
	@Override
	public void run() {
		try{
			while(true){
				if(commandBytes != null){
					DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length, SERVER_ADDRESS, SERVER_PORT);
					socket.send(packet);
					commandBytes = null;
				}
			}
		} catch (Exception e) {
			Log.e("Command Sender", e.getMessage());
		}
	}
}
