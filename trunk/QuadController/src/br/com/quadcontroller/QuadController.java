package br.com.quadcontroller;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.math.BigDecimal;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Main activity
 * 
 * @author walbao
 */
public class QuadController extends IOIOActivity {

	private final String TAG = "QuadController";
	
	// User Interface variables
	private TextView connectionStatus;
	//public static TextView accelField;
	//public static TextView gyroField;
	private TextView _varField;
	private Handler handler;

	// Video variables
	private Camera mCamera;
	private String supportedResolutions;
	private Preview preview;
	private int currentWidth = 320;
	private int currentHeight = 240;
	
	//Joystick variables
	private int x1;
	private int y1;
	private int x2;
	private int y2;
	
	//Directions:
	//0 - stop
	//1 - hover
	//2 - up
	//3 - down
	//4 - left
	//5 - right
	private int direction = 0;
	
	//Sensor variables
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor orientation;
	private float accelX;
	private float accelY;
	private float accelZ;
	private float orientationX;
	private float orientationY;
	private float orientationZ;
	
	//Threads
	private ReceiveCommandThread receiveCommandThread;
	private SendVideoThread sendVideoThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		handler = new Handler();
		
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
		orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		sensorManager.registerListener(new AccelerometerListener(this), accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(new OrientationListener(this), orientation, SensorManager.SENSOR_DELAY_NORMAL);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Initialize the camera, create and add its preview surface to our layout
		mCamera = Camera.open();
		
		//Set a list of supported resolutions so the remote can request it when needed
		List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
		StringBuilder sizes = new StringBuilder();
		for(Size size : previewSizes){
			sizes.append(size.width + "x" + size.height + ",");
		}
		
		supportedResolutions = sizes.substring(0, sizes.length()-1);
		
		preview = new Preview(this, mCamera);
		((LinearLayout) findViewById(R.id.linearLayout1)).addView(preview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		try {
			//Run new thread to send video frames via UDP socket
			sendVideoThread = new SendVideoThread(this);
			sendVideoThread.start();
		} catch (SocketException e1) {
			Log.e(TAG, "Error in send video thread: " + e1.getMessage());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		try {
			//Run new thread to receive commands via UDP socket
			receiveCommandThread = new ReceiveCommandThread(this);
			receiveCommandThread.start();
		} catch (SocketException e) {
			Log.e(TAG, "Error in receive command thread: " + e.getMessage());
		}
		
	}
	
	// This is the thread which has access to the IOIO's I/O pins (controls the motors)
	class Looper extends BaseIOIOLooper {

		// The on-board LED
		private PwmOutput _led;
		
		// The four motors :)
		private PwmOutput frontLeftMotor;
		private PwmOutput frontRightMotor;
		private PwmOutput backLeftMotor;
		private PwmOutput backRightMotor;
		
		private float frontLeftMult = 0; 
		private float frontRightMult = 0;
		private float backLeftMult = 0;
		private float backRightMult = 0;
		
		private int currentDirection = 0;
		
		@Override
		protected void setup() throws ConnectionLostException {
			_led = ioio_.openPwmOutput(0, 300);
			
			frontLeftMotor = ioio_.openPwmOutput(1, 50); // 20ms periods
			frontRightMotor = ioio_.openPwmOutput(3, 50); // 20ms periods
			backLeftMotor = ioio_.openPwmOutput(5, 50); // 20ms periods
			backRightMotor = ioio_.openPwmOutput(7, 50); // 20ms periods
			
			frontLeftMult = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
			frontRightMult = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
			backLeftMult = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
			backRightMult = round((-y1 + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
		}
		
		@Override
		public void loop() throws ConnectionLostException {
			
			if(currentDirection != direction){
				int add = -52;
				if(direction == 2)
					add = -59;
				else if(direction == 3)
					add = -45;
				
				frontLeftMult = round((add + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
				frontRightMult = round((add + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
				backLeftMult = round((add + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
				backRightMult = round((add + 120f) / 240f, 2, BigDecimal.ROUND_HALF_UP);
			}
			
			if(accelX < -10.6 || accelX > -10.4){
				if(accelY > 0.5){
					frontRightMult++;
					backRightMult++;
					
					frontLeftMult--;
					backLeftMult--;
				} else if(accelY < -1.5){
					frontLeftMult++;
					backLeftMult++;
					
					frontRightMult--;
					backRightMult--;
				}
				
				if(accelZ > 1.5){
					frontRightMult++;
					frontLeftMult++;
					
					backRightMult--;
					backLeftMult--;
				} else if(accelZ < -0.5){
					backRightMult++;
					backLeftMult++;
					
					frontRightMult--;
					frontLeftMult--;
				}
			}
			
			
			frontLeftMotor.setDutyCycle(0.05f + frontLeftMult * 0.05f);
			frontRightMotor.setDutyCycle(0.05f + frontRightMult * 0.05f);
			backLeftMotor.setDutyCycle(0.05f + backLeftMult * 0.05f);
			backRightMotor.setDutyCycle(0.05f + backRightMult * 0.05f);
			
			_led.setDutyCycle(1 - frontLeftMult);
			
			handler.post(new Runnable() {
				@Override
				public void run() {
					_varField.setText("y: " + y1);
				}
			});
		}
	}

	public void takePicture(){
		mCamera.takePicture(null, null, null, new PhotoHandler());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	public float round(float unrounded, int precision, int roundingMode)
	{
	    BigDecimal bd = new BigDecimal(unrounded);
	    BigDecimal rounded = bd.setScale(precision, roundingMode);
	    return rounded.floatValue();
	}
	
	public void changeResolution(int width, int height){
		this.currentWidth = width;
		this.currentHeight = height;
		
		Log.e(TAG, "Changing resolution to " + width + "x" + height);
		
		preview.changeResolution();
	}
	
	public void setJoystickPos(int x1, int y1, int x2, int y2){
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		
		if(y1 <= -53)
			//go up
			direction = 2;
		else if(y1 >= 51)
			//go down
			direction = 3;
		else
			//hover
			direction = 1;
	}
	
	public void setOriValues(float x, float y, float z){
		this.orientationX = x;
		this.orientationY = y;
		this.orientationZ = z;
	}
	
	public void setAccelValues(float x, float y, float z){
		this.accelX = x;
		this.accelY = y;
		this.accelZ = z;
	}

	/**
	 * @return the mCamera
	 */
	public Camera getmCamera() {
		return mCamera;
	}

	/**
	 * @return the supportedResolutions
	 */
	public String getSupportedResolutions() {
		return supportedResolutions;
	}

	/**
	 * @param supportedResolutions the supportedResolutions to set
	 */
	public void setSupportedResolutions(String supportedResolutions) {
		this.supportedResolutions = supportedResolutions;
	}

	/**
	 * @return the preview
	 */
	public Preview getPreview() {
		return preview;
	}

	/**
	 * @return the receiveCommandThread
	 */
	public ReceiveCommandThread getReceiveCommandThread() {
		return receiveCommandThread;
	}

	/**
	 * @return the sendVideoThread
	 */
	public SendVideoThread getSendVideoThread() {
		return sendVideoThread;
	}

	/**
	 * @return the currentWidth
	 */
	public int getCurrentWidth() {
		return currentWidth;
	}

	/**
	 * @param currentWidth the currentWidth to set
	 */
	public void setCurrentWidth(int currentWidth) {
		this.currentWidth = currentWidth;
	}

	/**
	 * @return the currentHeight
	 */
	public int getCurrentHeight() {
		return currentHeight;
	}

	/**
	 * @param currentHeight the currentHeight to set
	 */
	public void setCurrentHeight(int currentHeight) {
		this.currentHeight = currentHeight;
	}
}