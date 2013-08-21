package br.com.quadcontroller;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuadController extends IOIOActivity {

	// User Interface variables
	protected SurfaceView mView;
	protected SurfaceHolder mHolder;
	private TextView connectionStatus;
	//public static TextView accelField;
	//public static TextView gyroField;
	private TextView _varField;

	// Video variables
	private Camera mCamera;
	byte[][] frames;
	private final int JPEG_QUALITY = 30;
	private final int VIDEO_WIDTH = 352;
	private final int VIDEO_HEIGHT = 288;
	public static int currentFrame = 0;
	// Supported resolutions (Xperia P)
	// 1280 x 720 1.778
	// 768 x 432 1.778
	// 640 x 480 1.333
	// 576 x 432 1.333
	// 384 x 288 1.333
	// 352 x 288 1.222
	// 320 x 240 1.333
	// 176 x 144 1.222

	// Networking variables
	public static final String SERVERIP = Utils.getIPAddress(true);
	public static final int VIDEOPORT = 6775;
	public static final int COMMANDPORT = 6776;
	public static final String CLIENTIP = "192.168.43.1";
	private Handler handler = new Handler();
	private DatagramSocket socket;

	// IOIO variables
	private volatile float _varValue;
	
	//Joystick variables
	public int x1;
	public float y1;
	public int x2;
	public int y2;
	
	//Sensor variables
	SensorManager sensorManager;
	Sensor accelerometer;
	Sensor gyroscope;
	public static float accelX;
	public static float accelY;
	public static float accelZ;
	public static float gyroX;
	public static float gyroY;
	public static float gyroZ;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Retrieve and store UI elements for later usage
		connectionStatus = (TextView) findViewById(R.id.connection_status_textview);
		_varField = (TextView) findViewById(R.id.textView1);
		//accelField = (TextView) findViewById(R.id.textView2);
		//gyroField = (TextView) findViewById(R.id.textView3);
		
		
		
		
		//get the sensor service
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		//get the accelerometer sensor
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//get the gyroscope sensor
		gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		sensorManager.registerListener(new AccelerometerListener(), accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(new GyroscopeListener(), gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Prepare the frame queue to store frames which are ready to be sent
		frames = new byte[2][0];

		// Initialize the camera, create and add its preview surface to our layout
		mCamera = Camera.open();
		Preview prev = new Preview(this);
		((LinearLayout) findViewById(R.id.linearLayout1)).addView(prev,	new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		// Run new thread to send video frames via UDP socket
		Thread sendVideo = new Thread(new SendVideoThread());
		sendVideo.start();

		// Run new thread to receive commands via UDP socket
		Thread receiveCommand = new Thread(new ReceiveCommandThread());
		receiveCommand.start();
	}

	
	
	// This is the thread which sends commands to the IOIO (controls the motors)
	@SuppressWarnings("unused")
	class Looper extends BaseIOIOLooper {

		// The on-board LED
		private PwmOutput _led;
		// a attached servo
		
		private PwmOutput frontLeftMotor;
		private PwmOutput frontRightMotor;
		private PwmOutput backLeftMotor;
		private PwmOutput backRightMotor;
		
		@Override
		protected void setup() throws ConnectionLostException {
			_led = ioio_.openPwmOutput(0, 300);
			
			//frontLeftMotor = ioio_.openPwmOutput(1, 50); // 20ms periods
			//frontRightMotor = ioio_.openPwmOutput(3, 50); // 20ms periods
			backLeftMotor = ioio_.openPwmOutput(5, 50); // 20ms periods
			//backRightMotor = ioio_.openPwmOutput(7, 50); // 20ms periods
		}
		
		@Override
		public void loop() throws ConnectionLostException {
			final float m1 = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
			//final float m1 = y1;
			//frontLeftMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			//frontRightMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			backLeftMotor.setDutyCycle(0.05f + m1 * 0.05f);
			//backRightMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			
			_led.setDutyCycle(1 - m1);
			
			handler.post(new Runnable() {
				@Override
				public void run() {
					//_varField.setText("y1: " + m1);
				}
			});
			
			
		}
	}

	class Preview extends SurfaceView implements SurfaceHolder.Callback {
		SurfaceHolder mHolder;

		Preview(Context context) {
			super(context);

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		public void surfaceCreated(SurfaceHolder holder) {

			try {
				mCamera.setPreviewDisplay(holder);

			} catch (IOException e) {
				e.printStackTrace();
			}

			// Create callback that is executed at every new preview frame
			// created
			mCamera.setPreviewCallback(new PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, VIDEO_WIDTH, VIDEO_HEIGHT, null);
					yuvImage.compressToJpeg(new Rect(0, 0, VIDEO_WIDTH,	VIDEO_HEIGHT), JPEG_QUALITY, out);

					if (currentFrame == 0) {
						frames[0] = out.toByteArray();
						currentFrame = 1;
					} else {
						frames[1] = out.toByteArray();
						currentFrame = 0;
					}

					yuvImage = null;
					out = null;
				}
			});
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// Surface will be destroyed when we return, so stop the preview.
			// Because the CameraDevice object is not a shared resource, it's
			// very important to release it when the activity is paused.
			mCamera.stopPreview();
			mCamera = null;
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w,	int h) {
			// Now that the size is known, set up the camera parameters and
			// begin the preview.
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
			mCamera.setParameters(parameters);
			mCamera.startPreview();
		}
	}

	/**
	 * Command Receiver
	 * 
	 * Constantly attempts to receive commands from the controller. When a command is
	 * received we take appropriate action.
	 */
	class ReceiveCommandThread implements Runnable {

		@Override
		public void run() {

			try {
				createSocket();
			} catch (IOException e1) {
				Log.e("Command Receiver Connection", e1.getMessage());
			}
			
			while (true) {
				try {
					byte[] command = new byte[5];
					DatagramPacket packet = new DatagramPacket(command, command.length);
					socket.receive(packet);
					
					byte[] data = packet.getData();
					
					
					int commandType = (int) data[0];
					
					if(commandType == 0){
						//If we received a joystick (move) command
						x1 = (int) data[1];
						y1 = (int) data[2];
						x2 = (int) data[3];
						y2 = (int) data[4];
						
						handler.post(new Runnable() {
							@Override
							public void run() {
								//x1 = (-x1 + 120f) / 300f;
								//y1 = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
								//x2 = (-x2 + 120f) / 300f;
								//y2 = (-y2 + 120f) / 300f;
								
								//round to 2 decimal places
								//x1 = round(x1, 2, BigDecimal.ROUND_HALF_UP);
								//y1 = round(y1, 2, BigDecimal.ROUND_HALF_UP);
								//x2 = round(x2, 2, BigDecimal.ROUND_HALF_UP);
								//y2 = round(y2, 2, BigDecimal.ROUND_HALF_UP);
								
								_varValue = y1;
								_varField.setText("y: " + y1);
								//_varField.setText("x1: " + x1 + " / y1: " + y1 + " x2: " + x2 + " / y2: " + y2);
							}
						});
						
					} else if(commandType == 1){
						//If we received a take picture command
						mCamera.takePicture(null, null, null, new PhotoHandler());
					}
				} catch (Exception e) {
					Log.e("Command Receiver Body", e.getMessage());
				}
			}
		}
	}

	/**
	 * Video Sender
	 * 
	 * Constantly sends video frames to the controller. Uses a set packet size to calculate 
	 * the amount of packets per frame and set the header.
	 */
	public class SendVideoThread implements Runnable {

		private final int DATAGRAM_MAX_SIZE = 491;
		private final int HEADER_SIZE = 5;
		private int frame_nb = 0;
		private InetAddress clientAddress;

		public void run() {

			// If we managed to read our IP start sending video frames
			if (SERVERIP != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						connectionStatus.setText("Aguardando conexão: "	+ SERVERIP + ":" + VIDEOPORT);
					}
				});

				try {
					createSocket();
					clientAddress = InetAddress.getByName(CLIENTIP);
				} catch (Exception e) {
					Log.e("Video Sender Connection", e.getMessage());
				}
				while (true) {
					try {
						byte[] imageBytes;
						if (currentFrame == 0) {
							imageBytes = frames[1];
							frames[1] = new byte[0];
						} else {
							imageBytes = frames[0];
							frames[0] = new byte[0];
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
								DatagramPacket packet = new DatagramPacket(data2, size_p, clientAddress, VIDEOPORT);
								// Log.d("Server Packet Assembly", "Sending frame " + frame_nb + " packet " + (i + 1) + "/" + nb_packets);
								socket.send(packet);
								data2 = null;
							}
						}
						imageBytes = null;
					} catch (Exception e) {
						Log.e("Video Sender Body", e.getMessage());
						e.printStackTrace();
					}
				}
			} else {
				handler.post(new Runnable() {

					@Override
					public void run() {
						connectionStatus
								.setText("Não foi possível obter endereço IP.");
					}
				});
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	private synchronized void createSocket() throws SocketException{
		if(socket == null)
			socket = new DatagramSocket(VIDEOPORT);
	}
	
	public static float round(float unrounded, int precision, int roundingMode)
	{
	    BigDecimal bd = new BigDecimal(unrounded);
	    BigDecimal rounded = bd.setScale(precision, roundingMode);
	    return rounded.floatValue();
	}
}