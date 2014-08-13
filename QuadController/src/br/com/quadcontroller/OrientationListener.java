package br.com.quadcontroller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class OrientationListener implements SensorEventListener {
	
	private final QuadController MAIN_ACTIVITY;
	
	public OrientationListener(QuadController mainActivity){
		this.MAIN_ACTIVITY = mainActivity;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		
		MAIN_ACTIVITY.setOriValues(x, y, z);
	}
}
