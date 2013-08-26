package br.com.quadremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * Video Receiver Thread
 * 
 * Constantly reads data from the socket. Reads the header and attempts
 * to assemble a frame from multiple packets then display it.
 */
class ReceiveVideoThread extends Thread {

	private final String TAG = "Video Receiver";
	private final int DATA_MAX_SIZE = 491;
	private final int HEADER_SIZE = 5;
	private final int SERVER_PORT = 6775;
	
	private int current_frame = 0;
	private int slicesStored = 0;
	private byte[] imageData;
	private DatagramSocket socket;
	
	private Handler handler;
	private final ImageView iv;
	
	public ReceiveVideoThread(Handler handler, ImageView iv) throws SocketException {
		this.handler = handler;
		this.iv = iv;
		
		this.socket = new DatagramSocket(SERVER_PORT);
	}
	
	public void run() {
		
		while (true) {	
			
			/*try {
				String ip = InetAddress.getLocalHost().getHostAddress();
				QuadRemote.updateTextView("IP: " + ip);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			
			try {		
				//Attempt to receive video packet
				DatagramPacket packet = null;
				byte[] receivedData = new byte[496];
				packet = new DatagramPacket(receivedData, 496);
				socket.receive(packet);
				
				byte[] data = packet.getData();         
	            
				//Read header data
				int frame_nb = (int)data[0];
	            int nb_packets = (int)data[1];
	            int packet_nb = (int)data[2];
	            int size_packet = (int) ((data[3] & 0xff) << HEADER_SIZE | (data[4] & 0xff));
	            
	            //If we're starting to receive a new frame
	            if((packet_nb==0) && (current_frame != frame_nb)) {
	                
	            	current_frame = frame_nb;
	                slicesStored = 0;
	                //Log.d("Client Frame Assembly", "Creating new buffer for frame " + frame_nb + ", size is " + (nb_packets * DATA_MAX_SIZE) + " bytes");
	                imageData = new byte[nb_packets * DATA_MAX_SIZE];
	                
	            } 
	            
	            if(frame_nb == current_frame) {
	            	//Else we got another piece of the frame we're currently assembling
	            	
	            	System.arraycopy(data, HEADER_SIZE, imageData, (packet_nb * DATA_MAX_SIZE), size_packet);
	            	
	            	slicesStored++;             
	            }

	            /* If the frame is complete display it */
	            if (slicesStored == nb_packets) {
	            	final Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
	            	handler.post(new Runnable() {
						@Override
						public void run() {
							iv.setImageBitmap(image);
						}
					});
	            }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }               
		}
	}
}