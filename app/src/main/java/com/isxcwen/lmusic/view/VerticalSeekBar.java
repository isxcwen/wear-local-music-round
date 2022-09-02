package com.isxcwen.lmusic.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.isxcwen.lmusic.R;

/**
 * 垂直拽托进度条
 */
public class VerticalSeekBar extends View {
    /**
     * View默认最小宽度
     */
    private int mDefaultWidth;
    /**
     * View默认最小高度
     */
    private int mDefaultHeight;
    /**
     * 控件宽
     */
    private int mViewWidth;
    /**
     * 控件高
     */
    private int mViewHeight;
    /**
     * 背景颜色
     */
    private int mBgColor;
    /**
     * 进度背景颜色
     */
    private int mProgressBgColor;
    /**
     * 进度条的圆角半径
     */
    private int mBgRadius;
    /**
     * 当前进度
     */
    private float mProgress;
    /**
     * 最小进度值
     */
    private float mMin;
    /**
     * 最大进度值
     */
    private float mMax;
    /**
     * 背景画笔
     */
    private Paint mBgPaint;
    /**
     * 进度画笔
     */
    private Paint mProgressPaint;
    /**
     * 进度更新监听
     */
    private VerticalSeekBar.OnProgressUpdateListener mOnProgressUpdateListener;

    public VerticalSeekBar(Context context) {
        this(context, null);
    }

    public VerticalSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        initAttr(context, attrs, defStyleAttr);
        //取消硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        //背景画笔
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setColor(mBgColor);
        mBgPaint.setStyle(Paint.Style.FILL);
        //进度画笔
        mProgressPaint = new Paint();
        mProgressPaint.setColor(mProgressBgColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.FILL);
        //计算默认宽、高
        mDefaultWidth = dip2px(context, 36f);
        mDefaultHeight = dip2px(context, 114f);
    }

    private void initAttr(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        int defaultBgColor = Color.parseColor("#EDF0FA");
        int defaultProgressBgColor = Color.parseColor("#6D79FE");
        int defaultBgRadius = dip2px(context, 8f);
        float defaultProgress = 0;
        float defaultMinProgress = 0;
        int defaultMaxProgress = 100;
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar, defStyleAttr, 0);
            //进度背景颜色
            mBgColor = array.getColor(R.styleable.VerticalSeekBar_vsb_bg, defaultBgColor);
            //已有进度的背景颜色
            mProgressBgColor = array.getColor(R.styleable.VerticalSeekBar_vsb_progress_bg, defaultProgressBgColor);
            //进度条的圆角
            mBgRadius = array.getDimensionPixelSize(R.styleable.VerticalSeekBar_vsb_bg_radius, defaultBgRadius);
            //当前进度值
            mProgress = array.getFloat(R.styleable.VerticalSeekBar_vsb_progress, defaultProgress);
            //最小进度值
            mMin = array.getFloat(R.styleable.VerticalSeekBar_vsb_min_progress, defaultMinProgress);
            //最大进度值
            mMax = array.getFloat(R.styleable.VerticalSeekBar_vsb_max_progress, defaultMaxProgress);
            array.recycle();
        } else {
            mBgColor = defaultBgColor;
            mProgressBgColor = defaultProgressBgColor;
            mProgress = defaultProgress;
            mMin = defaultMinProgress;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //裁切圆角
        clipRound(canvas);
        //画背景
        drawBg(canvas);
        //画进度条
        drawProgress(canvas);
    }

    //------------ getFrameXxx()方法都是处理padding ------------

    private float getFrameLeft() {
        return getPaddingStart();
    }

    private float getFrameRight() {
        return mViewWidth - getPaddingEnd();
    }

    private float getFrameTop() {
        return getPaddingTop();
    }

    private float getFrameBottom() {
        return mViewHeight - getPaddingBottom();
    }

    //------------ getFrameXxx()方法都是处理padding ------------

    /**
     * 裁剪圆角
     */
    private void clipRound(Canvas canvas) {
        Path path = new Path();
        RectF roundRect = new RectF(getFrameLeft(), getFrameTop(), getFrameRight(), getFrameBottom());
        path.addRoundRect(roundRect, mBgRadius, mBgRadius, Path.Direction.CW);
        canvas.clipPath(path);
    }

    /**
     * 画背景
     */
    private void drawBg(Canvas canvas) {
        canvas.drawRect(new RectF(getFrameLeft(), getFrameTop(),
                        getFrameRight(), getFrameBottom()),
                mBgPaint);
    }

    /**
     * 画进度
     */
    private void drawProgress(Canvas canvas) {
        float contentHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
        //计算出当前进度应该有个top值，因为进度是从小往上，所以百分比要被1减去
        float progressRatio = getProgressRatio();
        float top;
        if (progressRatio == 0) {
            top = contentHeight;
        } else {
            top = contentHeight * (1 - progressRatio);
        }
        //画进度矩形
        RectF rect = new RectF(getFrameLeft(),
                top,
                getFrameRight(),
                getFrameBottom());
        canvas.drawRect(rect, mProgressPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(handleMeasure(widthMeasureSpec, true),
                handleMeasure(heightMeasureSpec, false));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //拦截Down事件，然后让父类不进行拦截
        if (action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (mOnProgressUpdateListener != null) {
                mOnProgressUpdateListener.onStartTrackingTouch(this);
            }
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int contentHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
        if (action == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            //Move或Up的时候，计算拽托进度
            float endY = event.getY();
            //限制拉到顶
            if (endY < 0) {
                endY = 0;
            }
            //限制拉到底
            if (endY > contentHeight) {
                endY = contentHeight;
            }
            //计算触摸点和高度的差值
            float distanceY = Math.abs(contentHeight - endY);
            float ratio = distanceY / contentHeight;
            //计算百分比应该有的进度：进度 = 总进度 * 进度百分比值
            float progress = mMax * ratio;
            setProgress(progress, true);
            if (action == MotionEvent.ACTION_UP) {
                if (mOnProgressUpdateListener != null) {
                    mOnProgressUpdateListener.onStopTrackingTouch(this);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 处理MeasureSpec
     */
    private int handleMeasure(int measureSpec, boolean isWidth) {
        int result;
        if (isWidth) {
            result = mDefaultWidth;
        } else {
            result = mDefaultHeight;
        }
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            //处理wrap_content的情况
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * 设置进度背景颜色
     */
    public void setBgColor(int bgColor) {
        //没有变化，不重绘
        if (bgColor == mBgColor) {
            return;
        }
        this.mBgColor = bgColor;
        mBgPaint.setColor(bgColor);
        invalidate();
    }

    /**
     * 设置已有进度的背景颜色
     */
    public void setProgressBgColor(int progressBgColor) {
        //没有变化，不重绘
        if (progressBgColor == mProgressBgColor) {
            return;
        }
        this.mProgressBgColor = progressBgColor;
        mProgressPaint.setColor(progressBgColor);
        invalidate();
    }

    /**
     * 设置进度
     */
    public void setProgress(float progress) {
        setProgress(progress, false);
    }

    /**
     * 设置进度
     *
     * @param fromUser 是否是用户触摸发生的改变
     */
    public void setProgress(float progress, boolean fromUser) {
        //忽略相同进度的设置
        if (mProgress == progress) {
            return;
        }
//        if (progress > mMin && progress < mMax) {
//        }
        this.mProgress = progress;
        invalidate();
        if (mOnProgressUpdateListener != null) {
            mOnProgressUpdateListener.onProgressUpdate(this, progress, fromUser);
        }
    }

    /**
     * 获取当前进度
     */
    public float getProgress() {
        return mProgress;
    }

    /**
     * 设置进度最小值
     */
    public void setMin(float min) {
        this.mMin = min;
        invalidate();
    }

    /**
     * 获取最小进度
     */
    public float getMin() {
        return mMin;
    }

    /**
     * 设置进度最大值
     */
    public void setMax(float max) {
        this.mMax = max;
        invalidate();
    }

    /**
     * 获取最大进度
     */
    public float getMax() {
        return mMax;
    }

    public interface OnProgressUpdateListener {
        /**
         * 按下时回调
         */
        void onStartTrackingTouch(VerticalSeekBar seekBar);

        /**
         * 进度更新时回调
         *
         * @param progress 当前进度
         * @param fromUser 是否是用户改变的
         */
        void onProgressUpdate(VerticalSeekBar seekBar, float progress, boolean fromUser);

        /**
         * 松手时回调
         */
        void onStopTrackingTouch(VerticalSeekBar seekBar);
    }

    public static class SimpleProgressUpdateListener implements OnProgressUpdateListener {
        @Override
        public void onStartTrackingTouch(VerticalSeekBar seekBar) {
        }

        @Override
        public void onProgressUpdate(VerticalSeekBar seekBar, float progress, boolean fromUser) {
        }

        @Override
        public void onStopTrackingTouch(VerticalSeekBar seekBar) {
        }
    }

    public void setOnProgressUpdateListener(
            VerticalSeekBar.OnProgressUpdateListener onProgressUpdateListener) {
        mOnProgressUpdateListener = onProgressUpdateListener;
    }

    /**
     * 获取当前进度值比值
     */
    public float getProgressRatio() {
        return mProgress / (mMax * 1.0f);
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
