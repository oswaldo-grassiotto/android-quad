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
    
	private Point pontoFinal, pontoMeio = null;  
    private Paint paint;  
    private int larg = getWidth();  
    private int alt = getHeight();
    
    private final QuadRemote MAIN_ACTIVITY;
  
    public Joystick(Context context, AttributeSet attrs)  
    {  
        super(context, attrs);  
        
        this.MAIN_ACTIVITY = (QuadRemote) context;
        		
        pontoFinal = new Point(larg/2, alt/2);
        
        iniciarPaint();
        setarListener();
    }  
      
    public void iniciarPaint(){  
        paint = new Paint();  
        paint.setStrokeWidth(2); //Seta a "grossura" do Paint  
    }  
      
    @SuppressLint("DrawAllocation")  
    @Override  
    protected void onDraw(Canvas canvas)  
    {  
        if(pontoMeio == null){  
            larg = getWidth();  
            alt = getHeight();  
          
            pontoMeio = new Point(larg/2, alt/2);  
        }  
        
        super.onDraw(canvas);  
    }  
      
    public void setarListener(){  
        setOnTouchListener(new View.OnTouchListener() {  
              
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                	
                } else if(event.getAction() == MotionEvent.ACTION_MOVE){
                	float eventX = event.getX();
                	float eventY = event.getY();
                	
                	if( eventX >= 0 && eventX <= larg)
                		pontoFinal.x = (int)eventX;
                    
                	if( eventY >= 0 && eventY <= alt){
                		pontoFinal.y = (int)eventY;
                		MAIN_ACTIVITY.mTextview2.setText("y:" + event.getY());
                	}
                	
                    
                    invalidate();
                    
					MAIN_ACTIVITY.sendCommand(0);
                    
                } else if(event.getAction() == MotionEvent.ACTION_UP){  
                    pontoFinal.set(pontoMeio.x, pontoMeio.y);  
                    invalidate();
                    
                    MAIN_ACTIVITY.sendCommand(0);
                }  
                return true;  
            }  
        });  
    }  
      
    /**
     * Retorna o valor do eixo X.
     * 
     * valores negativos significam que a posicao atual esta na parte
     * esquerda do joystick.
     * 
     * @return
     */
    public int getxAxis(){  
        int ret = 0;  
          
        if(pontoFinal.x > 0 && pontoFinal.x < larg){  
            ret = pontoFinal.x - pontoMeio.x;  
        }  
          
        if(pontoFinal.x > larg){  
            ret = larg/2;  
        }  
          
        if(pontoFinal.x < 0){  
            ret = 0-(larg/2);  
        }  
          
        return ret;  
    }  
      
    /**
     * Retorna o valor da posicao do eixo Y.
     * 
     * valores negativos significam que a posicao atual esta na parte
     * superior do joystick.
     * 
     * @return
     */
    public int getyAxis(){  
        int ret = 0;  
          
        if(pontoFinal.y > 0 && pontoFinal.y < alt){  
            ret = pontoFinal.y - pontoMeio.y;  
        }  
          
        if(pontoFinal.y > alt){  
            ret = alt/2;  
        }  
          
        if(pontoFinal.y < 0){  
            ret = 0-(alt/2);  
        }  
          
        return ret;  
    }  
} 