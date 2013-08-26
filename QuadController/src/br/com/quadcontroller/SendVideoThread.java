package br.com.quadcontroller;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * Video Sender
 * 
 * Constantly sends video frames to the controller. Uses a set packet size to calculate 
 * the amount of packets per frame and set the header.
 * 
 * @author walbao
 */
public class SendVideoThread implements Runnable {

	private final String TAG = "Video Sender";
	
	private final int DATAGRAM_MAX_SIZE = 491;
	private final int HEADER_SIZE = 5;
	private int frame_nb = 0;
	private InetAddress clientAddress;
	
	private final String CLIENT_IP;
	private final int VIDEO_PORT;
	private final DatagramSocket socket;

	public SendVideoThread() throws SocketException, UnknownHostException {
		
		VIDEO_PORT = 6775;
		CLIENT_IP = "192.168.43.1";
		
		this.socket = new DatagramSocket(VIDEO_PORT);
		clientAddress = InetAddress.getByName(CLIENT_IP);
	}
	
	public void run() {
		while (true) {
			try {
				byte[] imageBytes;
				if (Preview.currentFrame == 0) {
					imageBytes = Preview.frames[1];
				} else {
					imageBytes = Preview.frames[0];
				}

				if (imageBytes.length > 0) {
					frame_nb++;
					if (frame_nb > 10)
						frame_nb = 1;
					int nb_packets = (int) Math.ceil(imageBytes.length / (float) DATAGRAM_MAX_SIZE);
					int size = DATAGRAM_MAX_SIZE;

					// Loop through frame slices
					for (int i = 0; i < nb_packets; i++) {
						if (i > 0 && i == nb_packets - 1)
							size = imageBytes.length - i * DATAGRAM_MAX_SIZE;

						// Set additional header
						byte[] data2 = new byte[HEADER_SIZE + size];
						data2[0] = (byte) frame_nb;
						data2[1] = (byte) nb_packets;
						data2[2] = (byte) i;
						data2[3] = (byte) (size >> HEADER_SIZE);
						data2[4] = (byte) size;

						// Copy current slice to byte array
						if (i < nb_packets)
							System.arraycopy(imageBytes, i * DATAGRAM_MAX_SIZE, data2, HEADER_SIZE, size);
						else
							System.arraycopy(imageBytes, imageBytes.length - ((i - 1) * DATAGRAM_MAX_SIZE),	data2, HEADER_SIZE, size);

						int size_p = data2.length;
						DatagramPacket packet = new DatagramPacket(data2, size_p, clientAddress, VIDEO_PORT);
						Log.d("Server Packet Assembly", "Sending frame " + frame_nb + " packet " + (i + 1) + "/" + nb_packets);
						socket.send(packet);
						data2 = null;
					}
				}
				imageBytes = null;
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
