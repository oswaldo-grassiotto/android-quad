package br.com.quadcontroller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
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
	
	public static int currentFrame;
	public static byte[][] frames = new byte[2][0];
	
	private final int STANDARD_WIDTH = 320;
	private final int STANDARD_HEIGHT = 240;
	
	private Size currentSize;
	private int jpegQuality = 50;
	

	public Preview(Context context){
		super(context);
	}
	
	public Preview(Context context, Camera mCamera) {
		super(context);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		Parameters parameters = QuadController.getmCamera().getParameters();
		List<Size> previewSizes = parameters.getSupportedPreviewSizes();
		
		for(Size size : previewSizes){
			if(size.width == STANDARD_WIDTH && size.height == STANDARD_HEIGHT){
				this.currentSize = size;
				break;
			}
		}
		
		//if the standard resolution is not supported pick the smallest one available
		if(this.currentSize == null){
			this.currentSize = previewSizes.get(previewSizes.size() - 1);
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {

		try {
			QuadController.getmCamera().setPreviewDisplay(holder);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create callback that is executed at every new preview frame
		// created
		QuadController.getmCamera().setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, currentSize.width, currentSize.height, null);
				yuvImage.compressToJpeg(new Rect(0, 0, currentSize.width, currentSize.height), jpegQuality, out);

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
		QuadController.getmCamera().stopPreview();
		//QuadController.getmCamera() = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w,	int h) {
		// Now that the size is known, set up the camera parameters and
		// begin the preview.
		Parameters parameters = QuadController.getmCamera().getParameters();
		
		parameters.setPreviewSize(currentSize.width, currentSize.height);
		
		QuadController.getmCamera().setParameters(parameters);
		QuadController.getmCamera().startPreview();
	}

	public int getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(int jpegQuality) {
		this.jpegQuality = jpegQuality;
	}
}
