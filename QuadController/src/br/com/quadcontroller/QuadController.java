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
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;

/**
 * Main activity
 * 
 * @author walbao
 */
public class QuadController extends IOIOActivity {

	private final String TAG = "QuadController";
	
	// User Interface variables
	public static TextView connectionStatus;
	public static TextView _varField;
	//private Handler handler;

	// Video variables
	private Camera mCamera;
	private String supportedResolutions;
	private Preview preview;
	private int currentWidth = 320;
	private int currentHeight = 240;
	
	//Joystick variables
	public static int x1 = 50;
	public static int y1 = 50;
	public static int x2 = 50;
	public static int y2 = 50;
	
	//Sensor variables
	private SensorManager sensorManager;
	private Sensor rotationSensor;
	
	//Position variables
	private float[] rMatrix = new float[9];
	private float[] tempRMatrix = new float[9];
	private float[] quadRotation = new float[3];
	
	//Threads
	private ReceiveCommandThread receiveCommandThread;
	private SendVideoThread sendVideoThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//handler = new Handler();
		
		// Retrieve and store UI elements for later usage
		connectionStatus = (TextView) findViewById(R.id.connection_status_textview);
		_varField = (TextView) findViewById(R.id.textView1);
		
		//get the sensor service
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		//get the rotation vector sensor
		rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		sensorManager.registerListener(new RotationListener(this), rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Initialize the camera, create and add its preview surface to our layout
		Log.d("keke", "keke");
		mCamera = Camera.open();
		
		//Set a list of supported resolutions so the remote can request it when needed
		List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
		StringBuilder sizes = new StringBuilder();
		for(Size size : previewSizes){
			sizes.append(size.width + "x" + size.height + ",");
		}
		
		supportedResolutions = sizes.substring(0, sizes.length()-1);
		
		//preview = new Preview(this, mCamera);
		//((LinearLayout) findViewById(R.id.linearLayout1)).addView(preview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		try {
			//Run new thread to send video frames via UDP socket
			sendVideoThread = new SendVideoThread(this);
			//sendVideoThread.start();
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
		//private DigitalOutput _led;
		//private PwmOutput _pin1;
		//private PwmOutput _pin3;
		
		// The four motors :)
		private PwmOutput frontLeftMotor;
		private PwmOutput frontRightMotor;
		private PwmOutput rearLeftMotor;
		private PwmOutput rearRightMotor;
		
		private float frontLeftMult = 1; 
		private float frontRightMult = 1;
		private float rearLeftMult = 1;
		private float rearRightMult = 1;
		
		private int deadZoneHigh = 53;
		private int deadZoneLow = 47;
		
		@Override
		protected void setup() throws ConnectionLostException {
			frontLeftMotor = ioio_.openPwmOutput(1, 50); // 20ms periods
			frontRightMotor = ioio_.openPwmOutput(3, 50); // 20ms periods
			rearLeftMotor = ioio_.openPwmOutput(5, 50); // 20ms periods
			rearRightMotor = ioio_.openPwmOutput(7, 50); // 20ms periods
		}
		
		@Override
		public void loop() throws ConnectionLostException {
			
			frontLeftMult = 0;
			frontRightMult = 0;
			rearLeftMult = 0;
			rearRightMult = 0;
			
											 //Centered values below assume the phone is sideways with the back camera facing forward
			float roll = quadRotation[1];    //Centered at 0, roll to the left/counterclockwise to get positive values
			float pitch = quadRotation[2];   //Centered at 90, pitch down to increase values
			
			float rollMod = 0;
			
			//Joystick outside roll deadzone, read the joystick's x value and apply it
			if(x1 > deadZoneHigh || x1 < deadZoneLow){
				rollMod = (x1 - 50)/10;
			}
			
			rollMod += roll/100f;
			
			frontLeftMult += rollMod;
			rearLeftMult += rollMod;
			
			frontRightMult += -rollMod;
			rearRightMult += -rollMod;
						
			
			float pitchMod = 0; 
			
			//Joystick outside pitch deadzone, read the joystick's y value and apply it
			if(y1 > deadZoneHigh || y1 < deadZoneLow){
				pitchMod = (y1 - 50)/10;
			}
			
			pitchMod += (pitch-90)/100f;
			
			frontLeftMult += pitchMod;
			frontRightMult += pitchMod;
			
			rearLeftMult += -pitchMod;
			rearRightMult += -pitchMod;
			
			//We don't compensate for yaw so just move when we have to
			float yawMod = (x2-50)/100f;
			
			frontLeftMult += yawMod;
			rearRightMult += yawMod;
			
			rearLeftMult += -yawMod;
			frontRightMult += -yawMod;
			
			//We're using manual hover controls for now 
			float hoverMod = y2/100f;
			
			frontLeftMult += hoverMod;
			frontRightMult += hoverMod;
			rearLeftMult += hoverMod;
			rearRightMult += hoverMod;
			
			//The maths are very much experimental at this point, clamping guarantees the duty cycle remains within the expected range
			frontLeftMult = clamp(frontLeftMult);
			frontRightMult = clamp(frontRightMult);
			rearLeftMult = clamp(rearLeftMult);
			rearRightMult = clamp(rearRightMult);
			
			frontLeftMotor.setDutyCycle(0.05f + frontLeftMult * 0.05f);
			frontRightMotor.setDutyCycle(0.05f + frontRightMult * 0.05f);
			rearLeftMotor.setDutyCycle(0.05f + rearLeftMult * 0.05f);
			rearRightMotor.setDutyCycle(0.05f + rearRightMult * 0.05f);

		}
		
		/**
		 * Clamps any value outside the 0.1 - 1 range
		 * 
		 * @param value the value to be checked
		 * @return the value itself if it's within the accepted range or whichever extreme the value exceeded
		 */
		private float clamp(float value){
			if(value > 1)
				return 1;
			else if(value < 0.1f)
				return 0.1f;
			else
				return value;
		}
	}
	
	/**
	 * Debug method used to display the phones orientation and duty cycle used for each motor
	 */
	public void printStabilizationData(){
		float frontLeftMult = 0;
		float frontRightMult = 0;
		float rearLeftMult = 0;
		float rearRightMult = 0;
		
		//Values assume the phone is sideways
		float roll = quadRotation[1];    //Centered at 0, roll to the left/counterclockwise to get positive values
		float pitch = quadRotation[2];   //Centered at 90, pitch down to increase values
		
		float rollMod = 0;
		
		//Joystick outside roll deadzone, read the joystick's x value and apply it
		if(x1 > 53 || x1 < 47){
			rollMod = (x1 - 50)/10;
		}
		
		rollMod += roll/100f;
		
		frontLeftMult += rollMod;
		rearLeftMult += rollMod;
		
		frontRightMult += -rollMod;
		rearRightMult += -rollMod;
					
		
		float pitchMod = 0; 
		
		//Joystick outside pitch deadzone, read the joystick's y value and apply it
		if(y1 > 53 || y1 < 47){
			pitchMod = (y1 - 50)/10;
		}
		
		pitchMod += (pitch-90)/100f;
		
		frontLeftMult += pitchMod;
		frontRightMult += pitchMod;
		
		rearLeftMult += -pitchMod;
		rearRightMult += -pitchMod;
		
		//We don't compensate for yaw so just move when we have to
		float yawMod = (x2-50)/100f;
		
		frontLeftMult += yawMod;
		rearRightMult += yawMod;
		
		rearLeftMult += -yawMod;
		frontRightMult += -yawMod;
		
		//We're using manual hover controls for now 
		float hoverMod = y2/100f;
		
		frontLeftMult += hoverMod;
		frontRightMult += hoverMod;
		rearLeftMult += hoverMod;
		rearRightMult += hoverMod;
		
		//The maths are very much experimental at this point, clamping guarantees the duty cycle remains within the expected range
		frontLeftMult = clamp(frontLeftMult);
		frontRightMult = clamp(frontRightMult);
		rearLeftMult = clamp(rearLeftMult);
		rearRightMult = clamp(rearRightMult);
		
		final float flm = frontLeftMult;
		final float frm = frontRightMult;
		final float rlm = rearLeftMult;
		final float rrm = rearRightMult;
		
		QuadController._varField.post(new Runnable() {
            public void run() {
            	
            	float fl = (0.05f + flm * 0.05f);
        		float fr = (0.05f + frm * 0.05f);
        		float rl = (0.05f + rlm * 0.05f);
        		float rr = (0.05f + rrm * 0.05f);
        		
        		_varField.setText("fl: " + fl + " | rl: " + rl + " | fr: " + fr + " | rr: " + rr);
            }
        });
		
		final float[] rot = quadRotation;
		QuadController.connectionStatus.post(new Runnable() {
            public void run() {
        		connectionStatus.setText("yaw: " + rot[0] + " | roll: " + rot[1] + " | pitch: " + rot[2]);
            }
        });
	}
	
	private float clamp(float value){
		if(value > 1)
			return 1;
		else if(value < 0.1f)
			return 0.1f;
		else
			return value;
	}

	public void takePicture(){
		mCamera.takePicture(null, null, null, new PhotoHandler());
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
		QuadController.x1 = x1;
		QuadController.y1 = y1;
		QuadController.x2 = x2;
		QuadController.y2 = y2;
	}
	
	public void setRotation(float[] rotationVector){
		calculateAngles(quadRotation, rotationVector);
		printStabilizationData();
	}
	
	/**
	 * Credit to maxlukichev from http://androbotus.wordpress.com/
	 * 
	 * @param result the array of Euler angles in the order: yaw, roll, pitch
	 * @param rVector the rotation vector
	 */
	public void calculateAngles(float[] result, float[] rVector){
	    //caculate rotation matrix from rotation vector first
	    SensorManager.getRotationMatrixFromVector(tempRMatrix, rVector);
	    
	    //translate rotation matrix according to the new orientation of the device
	    SensorManager.remapCoordinateSystem(tempRMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_Y, rMatrix);
	 
	    //calculate Euler angles now
	    SensorManager.getOrientation(rMatrix, result);
	 
	    //The results are in radians, need to convert it to degrees
	    convertToDegrees(result);
	}
	 
	private void convertToDegrees(float[] vector){
	    for (int i = 0; i < vector.length; i++){
	        vector[i] = Math.round(Math.toDegrees(vector[i]));
	    }
	}
	
	@Override
    protected void onStop() {
        super.onStop();
    }
	
	@Override
    protected void onDestroy() {
        super.onStop();
        mCamera.release();
        Log.d("ControllerDestroy", "INSIDE: onDestroy");
        receiveCommandThread.stopThread();
        sendVideoThread.stopThread();
    }
	
	@Override
    protected void onResume() {
        super.onResume();
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