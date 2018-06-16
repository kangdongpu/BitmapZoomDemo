package com.kdp.bitmapzoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/**
 * Created by kangdongpu on 2017/6/30.
 */

public class MatrixView extends android.support.v7.widget.AppCompatImageView implements View.OnTouchListener {

    private static final String TAG = MatrixView.class.getSimpleName();
    private static final int STATUS_INIT = 0; //默认状态
    private static final int STATUS_ZOOM = 1; //缩放
    private static final int STATUS_TRANSLATE = 2; //平移
    private static final int STATUS_ROTATE = 3; //旋转
    //    private static final int STATUS_ZOOM_IN = 2;  //缩小
    private int operation_type; //当前图片的操作状态
    private WindowManager wm;

    private float mDownFingersDistance; //按下时两指间距离
    private float mMoveFingersDistance; //滑动时,两指间的距离
    private float mSingleDownX, mSingleDownY;
    private float mSingleSlideDistanceX, mSingleSlideDistanceY;
    private float lastXMove = -1;
    private float lastYMove = -1;

    int touchSlop; //用于判断时单击操作还是滑动操作，在被判定为滚动之前手指可以移动的最大距离

    private float centerX, centerY; //图片中心点坐标

    //原图片宽高
    int mBitmapWidth;
    int mBitmapHeight;
    //图片放大缩小的宽高
    int mScaleWidth;
    int mScaleHeight;
    //屏幕宽高
    int mScreenWidth;
    int mScreenHeight;

    float totalScale = 1; //总的缩放倍数
    float scale; //当前缩放倍数
    float rotate;  //当前总旋转角度
    float mTotalTranslateX, mTotalTranslateY; //图片初始化位置

    BitmapDrawable drawable;
    Bitmap bitmap;

    private android.graphics.Matrix matrix;

    public MatrixView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        drawable = (BitmapDrawable) getDrawable();
        if (drawable != null) {
            bitmap = drawable.getBitmap();
            mBitmapWidth = bitmap.getWidth();
            mBitmapHeight = bitmap.getHeight();
            mScaleWidth = mBitmapWidth;
            mScaleHeight = mBitmapHeight;
            //初始化矩阵
            initMatrix();
        }

        setOnTouchListener(this);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    }

    private void initMatrix() {
        //初始化操作状态
        operation_type = STATUS_INIT;
        matrix = new android.graphics.Matrix();
        centerX = mScreenWidth / 2;
        centerY = mScreenHeight / 2;
        mTotalTranslateX = centerX - mBitmapWidth / 2;
        mTotalTranslateY = centerY - mBitmapHeight / 2;
        matrix.setTranslate(mTotalTranslateX, mTotalTranslateY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(mScreenWidth, mScreenHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) {
            return;
        }
        switch (operation_type) {
            case STATUS_INIT:
                canvas.drawBitmap(bitmap, matrix, null);
                break;
            case STATUS_ZOOM:
                zoom(canvas);
                break;
            case STATUS_TRANSLATE:
                translate(canvas);
                break;
            case STATUS_ROTATE:
                rotate(canvas);
                break;
        }

    }

    /**
     * 双指旋转
     * @param canvas
     */
    private void rotate(Canvas canvas) {

    }

    /**
     * 平移
     *
     * @param canvas
     */
    private void translate(Canvas canvas) {
        matrix.reset();
        matrix.postScale(totalScale, totalScale);

        mTotalTranslateX = mTotalTranslateX + mSingleSlideDistanceX;
        mTotalTranslateY = mTotalTranslateY + mSingleSlideDistanceY;
        centerX = mTotalTranslateX + mScaleWidth / 2;
        centerY = mTotalTranslateY + mScaleHeight / 2;

        matrix.postTranslate(mTotalTranslateX, mTotalTranslateY);
        canvas.drawBitmap(bitmap, matrix, null);

    }

    /**
     * 缩放
     *
     * @param canvas
     */
    private void zoom(Canvas canvas) {
        matrix.reset();
        //先将图片按总比例缩放
        matrix.postScale(totalScale, totalScale);
        //计算缩放后图片的宽高
        mScaleWidth = (int) (mBitmapWidth * totalScale);
        mScaleHeight = (int) (mBitmapHeight * totalScale);
        float translateX = centerX - mScaleWidth / 2;
        float translateY = centerY - mScaleHeight / 2;

        matrix.postTranslate(translateX, translateY);
        mTotalTranslateX = translateX;
        mTotalTranslateY = translateY;
        canvas.drawBitmap(bitmap, matrix, null);

    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                //当两只手同时按下时,计算两指之间的距离
                if (event.getPointerCount() == 2) {
                    mDownFingersDistance = dispatchDistanceDistance(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    float mMoveX = event.getX();
                    float mMoveY = event.getY();
                    if (lastXMove == -1 && lastYMove == -1) {
                        lastXMove = mMoveX;
                        lastYMove = mMoveY;
                    }
                    mSingleSlideDistanceX = mMoveX - lastXMove;
                    mSingleSlideDistanceY = mMoveY - lastYMove;

                    Log.d(TAG, "onTouch: mSingleSlideDistanceX: " + mSingleSlideDistanceX);
                    Log.d(TAG, "onTouch: mSingleSlideDistanceY: " + mSingleSlideDistanceY);

                    operation_type = STATUS_TRANSLATE;
                    invalidate();
                    lastXMove = mMoveX;
                    lastYMove = mMoveY;

                } else if (event.getPointerCount() == 2) {
                    mMoveFingersDistance = dispatchDistanceDistance(event);
                    if (Math.abs(mMoveFingersDistance - mDownFingersDistance) > touchSlop) {
                        operation_type = STATUS_ZOOM;

                        scale = mMoveFingersDistance / mDownFingersDistance;
                        totalScale = totalScale * scale;

                        if (totalScale > 4) {
                            totalScale = 4;
                        } else if (totalScale < 1) {
                            totalScale = 1;
                        }
                        invalidate();
                        mDownFingersDistance = mMoveFingersDistance;
                    }
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                lastXMove = -1;
                lastYMove = -1;
                break;
            case MotionEvent.ACTION_UP:
                lastXMove = -1;
                lastYMove = -1;
                Log.e(TAG, "onTouch: ACTION_UP");

                break;
        }
        return true;
    }

    private float dispatchDistanceDistance(MotionEvent event) {
        float distanceX = event.getX(0) - event.getX(1);
        float distanceY = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }
}
