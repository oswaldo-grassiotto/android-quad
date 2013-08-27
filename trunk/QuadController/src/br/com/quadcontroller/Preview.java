package br.com.quadcontroller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Camera preview surface
 * 
 * Captures preview frames, converts them to jpeg format and 
 * stores them in a buffer to be sent to the remote.
 * 
 * @author walbao
 */
class Preview extends SurfaceView implements SurfaceHolder.Callback {
	SurfaceHolder mHolder;
	
	private int currentFrame;
	private byte[][] frames = new byte[2][0];
	private int jpegQuality = 50;
	
	private final QuadController MAIN_ACTIVITY;

	public Preview(Context context){
		super(context);
		this.MAIN_ACTIVITY = null;
	}
	
	public Preview(QuadController mainActivity, Camera mCamera) {
		super(mainActivity);
		this.MAIN_ACTIVITY = mainActivity;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {

		try {
			MAIN_ACTIVITY.getmCamera().setPreviewDisplay(holder);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create callback that is executed at every new preview frame
		// created
		MAIN_ACTIVITY.getmCamera().setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, MAIN_ACTIVITY.getCurrentWidth(), 
						MAIN_ACTIVITY.getCurrentHeight(), null);
				yuvImage.compressToJpeg(new Rect(0, 0, MAIN_ACTIVITY.getCurrentWidth(), 
						MAIN_ACTIVITY.getCurrentHeight()), jpegQuality, out);

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
	
	public void changeResolution(){
		
		MAIN_ACTIVITY.getmCamera().stopPreview();
		MAIN_ACTIVITY.getmCamera().setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, MAIN_ACTIVITY.getCurrentWidth(), 
						MAIN_ACTIVITY.getCurrentHeight(), null);
				yuvImage.compressToJpeg(new Rect(0, 0, MAIN_ACTIVITY.getCurrentWidth(), 
						MAIN_ACTIVITY.getCurrentHeight()), jpegQuality, out);

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
		

		Parameters parameters = MAIN_ACTIVITY.getmCamera().getParameters();
		parameters.setPreviewSize(MAIN_ACTIVITY.getCurrentWidth(), MAIN_ACTIVITY.getCurrentHeight());
		MAIN_ACTIVITY.getmCamera().setParameters(parameters);
		MAIN_ACTIVITY.getmCamera().startPreview();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's
		// very important to release it when the activity is paused.
		MAIN_ACTIVITY.getmCamera().stopPreview();
		//QuadController.getmCamera() = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w,	int h) {
		// Now that the size is known, set up the camera parameters and
		// begin the preview.
		Parameters parameters = MAIN_ACTIVITY.getmCamera().getParameters();
		
		parameters.setPreviewSize(MAIN_ACTIVITY.getCurrentWidth(), MAIN_ACTIVITY.getCurrentHeight());
		
		MAIN_ACTIVITY.getmCamera().setParameters(parameters);
		MAIN_ACTIVITY.getmCamera().startPreview();
	}

	public int getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(int jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	/**
	 * @return the currentFrame
	 */
	public int getCurrentFrame() {
		return currentFrame;
	}

	/**
	 * @return the frames
	 */
	public byte[][] getFrames() {
		return frames;
	}
}
