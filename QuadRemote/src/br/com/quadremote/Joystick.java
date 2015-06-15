package br.com.quadremote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
  
public class Joystick extends View {  
    
	private Point currentPoint = null; 
	private Point midPoint = null;  
    private Paint paint;  
    private int width = getWidth();  
    private int height = getHeight();
    
    private float positionScale = 3.575f; //Used to set the joystick axis range to [0-100]
    private float midPointScale = positionScale * 2; //Used to get the midpoint of the axis in the [0-100] range
    
    private boolean[] holdMode = {false, false}; //If this is true the value of the specified axis (x,y) won't return to the center when the pad is released
    
    private final QuadRemote MAIN_ACTIVITY;
  
    public Joystick(Context context, AttributeSet attrs)  
    {  
        super(context, attrs);  
        
        this.MAIN_ACTIVITY = (QuadRemote) context;
        		
        currentPoint = new Point(width/2, height/2);
        
        initializePaint();
        setListener();
    }  
      
    public void initializePaint(){  
        paint = new Paint();  
        paint.setStrokeWidth(2);
    }  
      
    @SuppressLint("DrawAllocation")  
    @Override  
    protected void onDraw(Canvas canvas)  
    {  
        if(midPoint == null){  
            width = getWidth();  
            height = getHeight();  
          
            midPoint = new Point((int)(width/midPointScale), (int)(height/midPointScale));
        }  
        
        super.onDraw(canvas);  
    }  
    
    /**
     * Creates listeners to read and store the positions of the x and y axis.
     */
    public void setListener(){  
        setOnTouchListener(new View.OnTouchListener() {  
              
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                	
                } else if(event.getAction() == MotionEvent.ACTION_MOVE){
                	float eventX = event.getX();
                	float eventY = event.getY();
                	
                	if( eventX >= 0 && eventX <= width)
                		currentPoint.x = ((int)(eventX/3.575));
                    
                	if( eventY >= 0 && eventY <= height){
                		currentPoint.y = ((int)(eventY/3.575));
                		currentPoint.y = (currentPoint.y * -1) - 100;    //Invert y values so 0 is the bottom of the pad and 100 is the top
                	}
                	
                	MAIN_ACTIVITY.mTextview2.setText("y:" + currentPoint.y);
            		MAIN_ACTIVITY.mTextview.setText("x:" + currentPoint.x);
                    
                    invalidate();
                    
					MAIN_ACTIVITY.sendCommand(0);
                    
                } else if(event.getAction() == MotionEvent.ACTION_UP){  
                	currentPoint.set(midPoint.x, midPoint.y);
                	
                	if(!holdMode[0])
                		currentPoint.x = midPoint.x;
                	
                	if(!holdMode[1])
                		currentPoint.y = midPoint.y;
                	
                	MAIN_ACTIVITY.mTextview.setText("x:" + currentPoint.x);
                	MAIN_ACTIVITY.mTextview2.setText("y:" + currentPoint.y);
                	
                    invalidate();
                    
                    MAIN_ACTIVITY.sendCommand(0);
                }  
                return true;  
            }  
        });  
    }  
      
    /**
     * Returns the position of the X axis. 0 means left and 100 means right
     * 
     * @return an integer number in the range [0-100]
     */
    public int getxAxis(){  
        return currentPoint.x;  
    }  
      
    /**
     * Returns the position of the Y axis. 0 means up and 100 means down
     * 
     * @return an integer number in the range [0-100]
     */
    public int getyAxis(){  
        return currentPoint.y;  
    }  
} 