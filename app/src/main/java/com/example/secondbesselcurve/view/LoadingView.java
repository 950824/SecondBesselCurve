package com.example.secondbesselcurve.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.example.secondbesselcurve.R;

public class LoadingView extends SurfaceView implements SurfaceHolder.Callback,Runnable {






    private enum LoadingState{
        DOWN,UP,FREE
    }

    private LoadingState loadingState = LoadingState.DOWN;

    private int ballColor;//小球颜色
    private int ballRaius;//小球半径
    private int lineColor;//连线颜色
    private int lineWidth;//连线长度
    private int strokeWidh;//绘制线宽
    private float downDistance;//水平位置下降距离
    private float maxDownDistance;//水平位置下降距离最低点
    private float upDistance;//从底部上弹的距离
    private float freeDownDistance;//自由落体的距离
    private float maxFreeDownDistance;//自由落体的距离最高点
    private ValueAnimator downControl;
    private ValueAnimator upControl;
    private ValueAnimator freeDownControl;
    private AnimatorSet animatorSet;
    private boolean isAnimationShowing;
    private SurfaceHolder holder;
    private Canvas canvas;
    private Paint paint;
    private Path path;
    private boolean isRunning;

    public LoadingView(Context context) {
        this(context,null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //自定义属性获取
        initAttr(context,attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidh);
        path = new Path();
        holder = getHolder();
        //getSurfaceHolder添加回调
        holder.addCallback(this);
        //初始化动画
        initContorl();
    }

    private void initContorl() {
        downControl = ValueAnimator.ofFloat(0,maxDownDistance);
        downControl .setDuration(500);
        downControl.setInterpolator(new DecelerateInterpolator());
        downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                downDistance = (float) animation.getAnimatedValue();
            }
        });
        downControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.DOWN;
                isAnimationShowing = true;
            }
        });

        upControl = ValueAnimator.ofFloat(0,maxDownDistance);
        upControl .setDuration(500);
        upControl.setInterpolator(new ShockInterpolator());
        upControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                upDistance = (float) animation.getAnimatedValue();
                if (upDistance>=maxDownDistance&&freeDownControl!=null&&!freeDownControl.isRunning()&&!freeDownControl.isStarted()){
                    freeDownControl.start();
                }
            }
        });
        upControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.UP;
                isAnimationShowing = true;
            }
        });

        freeDownControl = ValueAnimator.ofFloat(0, (float) (2*Math.sqrt(maxFreeDownDistance/5)));
        freeDownControl .setDuration(500);
        freeDownControl.setInterpolator(new AccelerateInterpolator());
        freeDownControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();
                //v0t - 1/2gt^2
                //v0 = 10*Math.sqrt(maxFreeDownDistance/5)
                freeDownDistance = (float) (10*Math.sqrt(maxFreeDownDistance/5)*t-5*t*t);
            }
        });
        freeDownControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationShowing = false;
                //重新开启动画
                startAllAnimator();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.FREE;
                isAnimationShowing = true;
            }
        });

        animatorSet = new AnimatorSet();
        animatorSet.play(downControl).before(upControl);
    }

    public void startAllAnimator() {
        if (isAnimationShowing){
            return;
        }
        if (animatorSet.isRunning()){
            animatorSet.end();
            animatorSet.cancel();
        }
        loadingState = LoadingState.DOWN;
        new Thread(this).start();//绘制线程开启
        //动画开启
        animatorSet.start();
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);
        ballColor = typedArray.getColor(R.styleable.LoadingView_ball_color, Color.BLUE);
        lineColor = typedArray.getColor(R.styleable.LoadingView_line_color, Color.BLUE);
        lineWidth = typedArray.getDimensionPixelOffset(R.styleable.LoadingView_line_width,200);
        strokeWidh = typedArray.getDimensionPixelOffset(R.styleable.LoadingView_stroke_width,4);
        maxDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadingView_max_down,50);
        maxFreeDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadingView_max_up,50);
        ballRaius = typedArray.getDimensionPixelSize(R.styleable.LoadingView_ball_radius,10);
        typedArray.recycle();//用完回收


    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //创建时回调
        isRunning = true;
        drawView();//绘制
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //销毁 资源进行回收
    }

    @Override
    public void run() {
        //绘制动画（死循环）
        while (isRunning){
            drawView();
            try {
                Thread.sleep(16);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void drawView() {

        try {
            if (holder!=null){
                canvas = holder.lockCanvas();
//                canvas.drawColor(0, PorterDuff.Mode.CLEAR);//清空屏幕canvas.drawColor(*****);值为你的背景色也可以达到效果
                canvas.drawColor(Color.WHITE);
                paint.setColor(lineColor);
                path.reset();
                path.moveTo(getWidth()/2f-lineWidth/2f,getHeight()/2);
                if (loadingState == LoadingState.DOWN){
                    //小球在绳子上下降
                    /**
                     *    B(t)=(1-t)^2P0 + 2t(1-t)P1 + t^2P2 (求贝塞尔曲线上任意一个点) 二阶贝塞尔曲线控制点简介.png
                     *    r=0.5
                     *    cp[1].x = (cp[0].x+cp[2].x)/2  连线中点
                     *    float c0 = (1-t)*(1-t)  0.25
                     *    float c1 = 2*t*(1-t);   0.5
                     *    float c2 = t*t         0.25
                     *    growX = c0 * cp[0].x + c1 * cp[1].x + c2 * cp[2].x;
                     *    growY = c0 * cp[0].y + c1 * cp[1].y + c2 * cp[2].y;
                     *    cp[1].y = (growy - 0.5cp[0].y)*2  （这里采用的是相对距离rQuadTo所以）growY 是下垂直线距离 cp[0].y cp[2].y都是0
                     */
                    path.rQuadTo(lineWidth/2f,2*downDistance,lineWidth,0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path,paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(getWidth()/2f,(float) (getHeight()/2+downDistance-ballRaius-strokeWidh/2),ballRaius,paint);

                }else {
                    //上升 或 自用落体过程
                    path.rQuadTo(lineWidth/2f,2*(maxDownDistance-upDistance),lineWidth,0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path,paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    if (loadingState==LoadingState.FREE){
                        //自由落体
                        canvas.drawCircle(getWidth()/2f,getHeight()/2f-freeDownDistance-ballRaius-strokeWidh/2f,ballRaius,paint);
                    }else {
                        //上升
                        canvas.drawCircle(getWidth()/2f,getHeight()/2f+(maxDownDistance-upDistance)-ballRaius-strokeWidh/2f,ballRaius,paint);
                    }
                }

                paint.setColor(ballColor);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(getWidth()/2f-lineWidth/2f,getHeight()/2f,ballRaius,paint);
                canvas.drawCircle(getWidth()/2f+lineWidth/2f,getHeight()/2f,ballRaius,paint);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (canvas!=null){
                holder.unlockCanvasAndPost(canvas);
            }
        }

    }


    class ShockInterpolator implements Interpolator{

        @Override
        public float getInterpolation(float input) {
            float value = (float) (1-Math.exp(-3*input)*Math.cos(10*input));
            return value;
        }
    }
}
