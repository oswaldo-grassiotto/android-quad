package br.com.quadcontroller;

import java.math.BigDecimal;
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
class ReceiveCommandThread extends Thread {

	private final int SERVER_PORT;
	private final DatagramSocket socket;
	
	private final QuadController MAIN_ACTIVITY;
	
	public ReceiveCommandThread(QuadController mainActivity) throws SocketException {
		this.MAIN_ACTIVITY = mainActivity;
		
		SERVER_PORT = 6774;
		this.socket = new DatagramSocket(SERVER_PORT);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				byte[] commandBytes = new byte[9];
				DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length);
				socket.receive(packet);

				byte[] data = packet.getData();

				int command = (int) data[0];
				
				Log.d("Command Receiver", "Received command: " + command);
				
				switch(command){
					case 0:
						//We received a joystick (move) command
						MAIN_ACTIVITY.setJoystickPos((int) data[1], (int) data[2], (int) data[3], (int) data[4]);
						Log.d("Command Receiver", "Joystick command received: " + "x1:" + ((int) data[1]) + " y1:" + ((int) data[2]) + " x2:" + ((int) data[3]) + "y2:" + ((int) data[4]));
						break;
					case 1:
						//We received a take picture command
						MAIN_ACTIVITY.takePicture();
						break;
					case 2:
						//We received a begin recording video command
						break;
					case 3:
						//We received a stop recording video command
						break;
					case 4:
						//We received a send available resolutions command
						byte[] resolutionsData = MAIN_ACTIVITY.getSupportedResolutions().getBytes();
						
						DatagramPacket resolutionsPacket = new DatagramPacket(resolutionsData, resolutionsData.length, 
								InetAddress.getByName("192.168.43.1"), 6774);
						Log.d("Command Receiver", "Sending list: " + new String(resolutionsData, "UTF-8"));
						socket.send(resolutionsPacket);
						break;
					case 5:
						//we received a change resolution command
						byte stringBytes[] = new byte[8];
						System.arraycopy(data, 1, stringBytes, 0, data.length-1);
						String resolution = new String (stringBytes, "UTF-8").trim();
						String[] resolutionArray = resolution.split("x");
						int width = Integer.parseInt(resolutionArray[0]);
						int height = Integer.parseInt(resolutionArray[1]);
						
						MAIN_ACTIVITY.changeResolution(width, height);
						break;
				}
				
				
			} catch (Exception e) {
				Log.e("Command Receiver Body", e.getMessage());
			}
		}
	}
}