package br.com.quadremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class QuadRemote extends Activity {

	//Networking variables
	public static final int SERVERPORT = 6775;
	public static final String SERVERIP = "192.168.43.201";
	public static final String CLIENTIP = "192.168.43.1";
	public static DatagramSocket socket;
	public InetAddress serverAddress;
	static SendCommandThread sendCommand;
	
	//User interface variables
	private TextView mTextview;
	private ImageView iv;
	static Joystick joy1;
	static Joystick joy2;
	Button picButton;
	ToggleButton videoButton;
	TextView videoTimer;
	
	//Image variable
	Bitmap image;
	
	//Messaging variable
	private Handler handler = new Handler();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//Retrieve and store UI elements for later usage
		mTextview = (TextView) findViewById(R.id.textView1);
		iv = (ImageView) findViewById(R.id.imageView1);
		joy1 = (Joystick) findViewById(R.id.joystick1);
		joy2 = (Joystick) findViewById(R.id.joystick2);
		picButton = (Button) findViewById(R.id.picButton);
		videoButton = (ToggleButton) findViewById(R.id.videoButton);
		videoTimer = (TextView) findViewById(R.id.videoTimer);
		
		try {
			serverAddress = InetAddress.getByName(SERVERIP);
		} catch (UnknownHostException e1) {
			Log.e("Client Startup", e1.getMessage());
		}
		
		//Create listeners for the buttons
		picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//take picture command
				sendCommand(1);
			}
		});

		videoButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					//begin recording command
					sendCommand(2);
				}
				else {
					//stop recording command
					sendCommand(3);
				}
			}
		});
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		try {
			createSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//Run new thread to receive video frames via UDP socket
		Thread receiveVideo = new Thread(new ReceiveVideoThread());
		receiveVideo.start();
		
		sendCommand = new SendCommandThread(serverAddress, SERVERPORT, socket);
		sendCommand.start();
	}
	
	/**
	 * Creates a new UDP socket connection if one doesn't already exist
	 * 
	 * @throws SocketException
	 */
	private synchronized void createSocket() throws SocketException{
		if(socket == null)
			socket = new DatagramSocket(SERVERPORT);
	}

	/**
	 * Build a byte array containing to command code and data (when necessary) and
	 * send it through the socket.
	 * 
	 * @param command: the code of the command to be sent
	 * 
	 * 0: move command, reads joystick data and sends it along with the command code
	 * 1: take picture command
	 * 2: begin recording video command
	 * 3: stop recording video command
	 */
	public static synchronized void sendCommand(int command) {
		
		byte[] commandBytes = new byte[5];
		
		switch(command){
			case 1:
			case 2:
			case 3:
				commandBytes[0] = (byte) command;
				break;
			default:
				int x1 = joy1.getxAxis();
				int y1 = joy1.getyAxis();
				int x2 = joy2.getxAxis();
				int y2 = joy2.getyAxis();
				
				commandBytes[1] = (byte) x1;
				commandBytes[2] = (byte) y1;
				commandBytes[3] = (byte) x2;
				commandBytes[4] = (byte) y2;
		}
		
		sendCommand.setCommandBytes(commandBytes);
	}
	
	/**
	 * Video Receiver Thread
	 * 
	 * Constantly reads data from the socket. Reads the header and attempts
	 * to assemble a frame from multiple packets then display it.
	 */
	class ReceiveVideoThread implements Runnable {

		private int current_frame = 0;
		private int slicesStored = 0;
		private final int DATA_MAX_SIZE = 491;
		private final int HEADER_SIZE = 5;
		private byte[] imageData;
		
		public void run() {
			
			try {
				createSocket();
			} catch (SocketException e1) {
				Log.e("Video Receiver Connection", e1.getMessage());
			}
			
			while (true) {	
				handler.post(new Runnable() {
					@Override
					public void run() {
						mTextview.setText("IP: " + CLIENTIP);
					}
				});
					
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
		            	image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
						handler.post(new Runnable() {
							@Override
							public void run() {
								try{
									iv.setImageBitmap(image);
								}catch(Exception e){
									Log.e("Video Receiver Display", e.getMessage());
								}
							}
						});
		            }
	            } catch (Exception e) {
	                Log.e("Video Receiver", e.getMessage());
	            }               
			}
		}
	}
}