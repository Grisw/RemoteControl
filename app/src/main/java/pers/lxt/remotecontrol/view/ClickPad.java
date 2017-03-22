package pers.lxt.remotecontrol.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import pers.lxt.remotecontrol.util.Network;

public class ClickPad  extends SurfaceView implements SurfaceHolder.Callback{

    static {
        System.loadLibrary("native-lib");
    }

    private Point last=new Point();
    private SurfaceHolder holder;
    private int touchCount=0;
    private int movingSpeed = 5;

    public ClickPad(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder=getHolder();
        holder.addCallback(this);
        setOnTouchListener(new OnTouchListener() {

            private long time = 0;
            private Point begin = new Point();
            private boolean rFlag = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Point p = new Point((int) event.getX(), (int) event.getY());
                switch (event.getAction()&MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        last = new Point(p.x, p.y);
                        begin = new Point(p.x, p.y);
                        time = System.currentTimeMillis();
                        touchCount++;
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        touchCount++;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if(touchCount==1){
                            final Point vector = new Point(p.x - last.x, p.y - last.y);
                            if (vector.x != 0 && vector.y != 0) {
                                new Thread() {
                                    public void run() {
                                        Network.setCursorPos(vector.x / movingSpeed, vector.y / movingSpeed);
                                        Log.i("cursor", vector.x / movingSpeed + "," + vector.y / movingSpeed);
                                    }
                                }.start();
                            } else {
                                if (rFlag && System.currentTimeMillis() - time >= 800 && p.x - begin.x == 0 && p.y - begin.y == 0) {
                                    rFlag = false;
                                    new Thread() {
                                        public void run() {
                                            Network.cursorRightClick();
                                            Log.i("cursor", "rightclick");
                                        }
                                    }.start();
                                }
                            }
                            last = new Point(p.x, p.y);
                        }else if(touchCount==2){
                            final Point vector = new Point(p.x - last.x, p.y - last.y);
                            if (vector.x != 0 && vector.y != 0) {
                                if(vector.y>0){
                                    new Thread() {
                                        public void run() {
                                            Network.scrollDown();
                                            Log.i("cursor", "scrolldown");
                                        }
                                    }.start();
                                }else{
                                    new Thread() {
                                        public void run() {
                                            Network.scrollUp();
                                            Log.i("cursor", "scrollup");
                                        }
                                    }.start();
                                }
                            }
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        touchCount--;
                        break;

                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - time <= 100 && p.x - begin.x == 0 && p.y - begin.y == 0) {
                            new Thread() {
                                public void run() {
                                    Network.cursorLeftClick();
                                    Log.i("cursor", "leftclick");
                                }
                            }.start();
                        }
                        rFlag = true;
                        touchCount--;
                        break;
                }
                return true;
            }
        });
    }

    public void refresh(Bitmap bmp){
        compoundImage(bmp,1);
        Canvas canvas=null;
        try{
            canvas=holder.lockCanvas();
            canvas.drawBitmap(bmp, null, new Rect(0, 0, ClickPad.this.getWidth(), ClickPad.this.getHeight()), null);
        }catch(Exception e){
            Log.e("refresh",e.getMessage(),e);
        }finally {
            if(canvas!=null)
                holder.unlockCanvasAndPost(canvas);
            bmp.recycle();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Network.close();
    }

    public static native void compoundImage(Object bmp,long id);
}
