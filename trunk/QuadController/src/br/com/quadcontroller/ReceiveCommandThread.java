package br.com.quadcontroller;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.util.Log;

/**
 * Command Receiver
 * 
 * Constantly attempts to receive commands from the remote. When a command
 * is received we take appropriate action.
 * 
 * @author walbao
 */
class ReceiveCommandThread implements Runnable {

	private final int SERVER_PORT;
	private final DatagramSocket socket;
	
	public ReceiveCommandThread() throws SocketException {
		SERVER_PORT = 6774;
		this.socket = new DatagramSocket(SERVER_PORT);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				byte[] command = new byte[5];
				DatagramPacket packet = new DatagramPacket(command,	command.length);
				socket.receive(packet);

				byte[] data = packet.getData();

				int commandType = (int) data[0];
				
				Log.d("Command Receiver", "Received command: " + commandType);
				
				if (commandType == 0) {
					//We received a joystick (move) command
					QuadController.x1 = (int) data[1];
					QuadController.y1 = (int) data[2];
					QuadController.x2 = (int) data[3];
					QuadController.y2 = (int) data[4];

				} else if (commandType == 1) {
					//We received a take picture command
					QuadController.takePicture();
				} else if(commandType == 2){
					//We received a begin recording video command
					
				} else if(commandType == 3){
					//We received a stop recording video command
					
				} else if(commandType == 4){
					//We received a send available resolutions command
					byte[] resolutionsData = QuadController.supportedResolutions.getBytes();
					
					DatagramPacket resolutionsPacket = new DatagramPacket(resolutionsData, resolutionsData.length, 
							InetAddress.getByName("192.168.43.1"), 6774);
					Log.d("Command Receiver", "Sending list: " + new String(resolutionsData, "UTF-8"));
					socket.send(resolutionsPacket);
				}
			} catch (Exception e) {
				Log.e("Command Receiver Body", e.getMessage());
			}
		}
	}
}