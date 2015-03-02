package com.terrakok.terraslider;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by terrakok on 02.02.15.
 */
public class Slider extends ScrollView {
    //высота, после которой сработает автоскролл
    private int MAX_OFFSET_FOR_AUTOSCROLL = 150;

    //id "тела" слайдера
    private int mBodyId = -1;

    //высота отступа над "телом"
    private int mHeaderSize = 0;

    //флаг, показывающий, что слайдер свернут
    private boolean isClosed = true;

    //флаг, показывающий, что слайдер захвачен при перетаскивании
    private boolean isTouched = false;

    //флаг, показывающий, что слайдер развернут и скроллится выше
    private boolean isOverHeader = false;

    //скроллер для расчета анимаций
    private Scroller mScroller = new Scroller(getContext());

    //аниматор для отображения анимаций
    private ValueAnimator mAnimator = ValueAnimator.ofFloat(0, 1f);

    //обработчик вызовов аниматора (двигает вьюху)
    private final ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mScroller.computeScrollOffset()) {
                int newY = mScroller.getCurrY();
                setScrollY(newY);

                for (Callbacks c : mCallbacks) {
                    c.onScrollChanged(-(newY - mHeaderSize));
                }
            } else {
                mAnimator.cancel();
            }
        }
    };

    private ArrayList<Callbacks> mCallbacks = new ArrayList<Callbacks>();

    public Slider(Context context) {
        super(context);
        init(context);
    }

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Slider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Slider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        MAX_OFFSET_FOR_AUTOSCROLL = context.getResources().getDimensionPixelSize(R.dimen.max_offset);
    }

    /**
     * Set slider body ID.
     * If not call this method, then will be as simple ScrollView
     *
     * @param bodyId: generated R.id value
     */
    public void setBodyId(int bodyId) {
        mBodyId = bodyId;
    }

    @Override
    public void draw(Canvas canvas) {
        initHeaderSizeIfNeeded();
        super.draw(canvas);
    }

    //один раз при первой отрисовке запоминаем высоту хэдера
    private void initHeaderSizeIfNeeded() {
        if (mHeaderSize == 0 && mBodyId != -1) {
            mHeaderSize = findViewById(mBodyId).getTop();
        }
    }

    /**
     * This method close slider if it was open
     *
     * @return true - if slider was opened, false - if slider was closed
     */
    public boolean closeSlider() {
        if (!isClosed) {
            mScroller.forceFinished(true);

            //эмуляция касания, чтобы остановить супер скроллер
            MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
            super.onTouchEvent(motionEvent);

            //чтобы не останавливать, как после флинга
            isOverHeader = false;

            //закрываем слайдер
            scrollTo(0, 0);
            isClosed = true;
            return true;
        }
        return false;
    }

    /**
     * This method toggle slider
     */
    public void toggleSlider() {
        mScroller.forceFinished(true);

        //эмуляция касания, чтобы остановить супер скроллер
        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
        super.onTouchEvent(motionEvent);

        //чтобы не останавливать, как после флинга
        isOverHeader = false;

        if (!isClosed) {
            //закрываем слайдер
            scrollTo(0, 0);
            isClosed = true;
        } else {
            //открываем слайдер
            scrollTo(0, mHeaderSize);
            isClosed = false;
        }
    }

    //автоматически довести до края (открыть/закрыть)
    private void doAutoScrolling() {
        if (isClosed) {
            if (getScrollY() > MAX_OFFSET_FOR_AUTOSCROLL) {
                autoScrollUp();
                isClosed = false;
            } else {
                autoScrollDown();
            }
        } else {
            if (getScrollY() < mHeaderSize - MAX_OFFSET_FOR_AUTOSCROLL) {
                autoScrollDown();
                isClosed = true;
            } else {
                autoScrollUp();
            }
        }
    }

    private void autoScrollUp() {
        if (mScroller.isFinished()) {
            mScroller.startScroll(0, getScrollY(), 0, mHeaderSize - getScrollY());
            mAnimator.setDuration(mScroller.getDuration());
            mAnimator.start();
        }
    }

    private void autoScrollDown() {
        if (mScroller.isFinished()) {
            mScroller.startScroll(0, getScrollY(), 0, 0 - getScrollY());
            mAnimator.setDuration(mScroller.getDuration());
            mAnimator.start();
        }
    }

    @Override
    protected void onScrollChanged(int l, int currentY, int oldl, int oldt) {
        super.onScrollChanged(l, currentY, oldl, oldt);
        //если при флинге из зоны isOverHeader = true попали ниже хэдера, то останавливаем движение
        if (isOverHeader && currentY < mHeaderSize) {
            setScrollY(mHeaderSize);

            for (Callbacks c : mCallbacks) {
                c.onScrollChanged(0);
            }
        } else {
            for (Callbacks c : mCallbacks) {
                c.onScrollChanged(-(currentY - mHeaderSize));
            }
        }
    }

    //обработка касаний
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getScrollY() <= mHeaderSize) {
            isOverHeader = false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mScroller.forceFinished(true);
                    super.onTouchEvent(event); //чтобы остановить скроллер суперкласса
                    return true;
                case MotionEvent.ACTION_UP:
                    //автоскроллим
                    doAutoScrolling();
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        } else {
            isClosed = false;
            isOverHeader = true;
            return super.onTouchEvent(event);
        }
    }

    //захват касаний, если попали не в хэдер
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isTouched) {
            //проверяем, что попали не в хэдер и забираем все жесты
            if (event.getY() > mHeaderSize - getScrollY()) {
                isTouched = true;
                return super.dispatchTouchEvent(event);
            }
            return false;
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                isTouched = false;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    public void addCallbacksListener(Callbacks listener) {
        if (!mCallbacks.contains(listener)) {
            mCallbacks.add(listener);
        }
    }

    public static interface Callbacks {
        //уведомляет о положении верха "тела"
        public void onScrollChanged(int currentBodyY);
    }
}