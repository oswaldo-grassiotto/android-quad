package br.com.quadcontroller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class RotationListener implements SensorEventListener {
	
	private final QuadController MAIN_ACTIVITY;
	
	public RotationListener(QuadController mainActivity){
		this.MAIN_ACTIVITY = mainActivity;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		MAIN_ACTIVITY.setRotation(event.values);
	}
}