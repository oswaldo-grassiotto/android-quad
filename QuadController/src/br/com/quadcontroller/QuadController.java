package br.com.quadcontroller;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
	
	//Sensor variables
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor gyroscope;
	private float accelX;
	private float accelY;
	private float accelZ;
	private float gyroX;
	private float gyroY;
	private float gyroZ;
	
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
		gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		sensorManager.registerListener(new AccelerometerListener(this), accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(new GyroscopeListener(this), gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		
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
	
	// This is the thread which sends commands to the IOIO (controls the motors)
	@SuppressWarnings("unused")
	class Looper extends BaseIOIOLooper {

		// The on-board LED
		private PwmOutput _led;
		
		// The four motors :)
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
			
			//frontLeftMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			//frontRightMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			backLeftMotor.setDutyCycle(0.05f + m1 * 0.05f);
			//backRightMotor.setDutyCycle(0.05f + _varValue * 0.05f);
			
			_led.setDutyCycle(1 - m1);
			
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
	}
	
	public void setGyroValues(float x, float y, float z){
		this.gyroX = x;
		this.gyroY = y;
		this.gyroZ = z;
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