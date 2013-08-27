package br.com.quadremote;

import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class QuadRemote extends Activity {
	
	private SendCommandThread sendCommandThread;
	private ReceiveVideoThread receiveVideoThread;
	
	public static QuadRemote activity;
	
	private CharSequence[] supportedResolutions;
	private String currentResolution;
	
	//User interface variables
	private TextView mTextview;
	private ImageView iv;
	private Joystick joy1;
	private Joystick joy2;
	private Button picButton;
	private ToggleButton videoButton;
	private TextView videoTimer;
	
	//Image variable
	private Bitmap image;
	
	//Messaging variable
	private Handler handler = new Handler();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		activity = this;
		
		//Retrieve and store UI elements for later usage
		mTextview = (TextView) findViewById(R.id.textView1);
		iv = (ImageView) findViewById(R.id.imageView1);
		joy1 = (Joystick) findViewById(R.id.joystick1);
		joy2 = (Joystick) findViewById(R.id.joystick2);
		picButton = (Button) findViewById(R.id.picButton);
		videoButton = (ToggleButton) findViewById(R.id.videoButton);
		videoTimer = (TextView) findViewById(R.id.videoTimer);
		
		//Create listeners for the buttons
		picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//take picture command
				Log.d("Remote Main", "Sending command 4");
				sendCommand(4);
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
		
		try {
			//Run new thread to receive video frames via UDP socket
			receiveVideoThread = new ReceiveVideoThread(handler, iv);
			receiveVideoThread.start();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		try {
			sendCommandThread = new SendCommandThread(this);
			sendCommandThread.start();
			
			//Now that we've started the thread we ask the controller 
			//for the available resolutions
			sendCommand(4);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
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
	public synchronized void sendCommand(int command) {
		
		byte[] commandBytes = new byte[9];
		
		switch(command){
			case 1:
			case 2:
			case 3:
			case 4:
				commandBytes[0] = (byte) command;
				break;
			case 5:
				commandBytes[0] = (byte) command;
				byte[] currentResolutionBytes = currentResolution.getBytes();
				System.arraycopy(currentResolutionBytes, 0, commandBytes, 1, currentResolutionBytes.length);
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
		
		sendCommandThread.setCommandBytes(commandBytes);
	}
	
	public void updateTextView(final String newMessage){
		handler.post(new Runnable() {
			@Override
			public void run() {
				mTextview.setText(newMessage);
			}
		});
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.menu_settings:
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 1);
            break;
 
        }
 
        return true;
    }
    
    public void setSupportedResolutions(String resolutionsStr){
    	CharSequence[] resolutions = resolutionsStr.split(",");
    	this.supportedResolutions = resolutions;
    }

	/**
	 * @return the supportedResolutions
	 */
	public CharSequence[] getSupportedResolutions() {
		return supportedResolutions;
	}

	/**
	 * @param supportedResolutions the supportedResolutions to set
	 */
	public void setSupportedResolutions(CharSequence[] supportedResolutions) {
		this.supportedResolutions = supportedResolutions;
	}

	/**
	 * @return the currentResolution
	 */
	public String getCurrentResolution() {
		return currentResolution;
	}

	/**
	 * @param currentResolution the currentResolution to set
	 */
	public void setCurrentResolution(String currentResolution) {
		this.currentResolution = currentResolution;
	}
}