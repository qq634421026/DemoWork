package com.aroen.threedscrollview;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

public class My3DScrollView extends ViewGroup {

	private int maxChildWidth = 0;
	private int maxChildHeight = 0;

	private static final float MINSCALE = 0.5f;
	private static final float MINALPHA = 0.0f;

	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;

	// 单位y轴滑动转动的角度
	private float angelPerY = 0.5f;
	/**
	 * 滚动到下一张图片的速度
	 */
	private static final int SNAP_VELOCITY = 600;
	/**
	 * 记录当前的触摸状态
	 */
	private int mTouchState = TOUCH_STATE_REST;

	/**
	 * 记录上次触摸的横坐标值
	 */
	private float mLastMotionY;

	/**
	 * 记录被判定为滚动运动的最小滚动值
	 */
	private int mTouchSlop;

	// 控件数量
	private int mCount;

	// 转过角度
	private int curAngel = 0;
	// 平均角度
	private int aveAngel;
	// 半径
	private int redius;

	/**
	 * 表示滚动到下一张图片这个动作
	 */
	private static final int SCROLL_NEXT = 0;
	/**
	 * 表示滚动到上一张图片这个动作
	 */
	private static final int SCROLL_PREVIOUS = 1;
	/**
	 * 表示滚动回原图片这个动作
	 */
	private static final int SCROLL_BACK = 2;
	private static Handler handler;
	/**
	 * 是否强制重新布局
	 */
	private boolean forceToRelayout;
	private int[] mItems;
	private int mWidth;
	private int mHeight;
	private int mCurrentMiddle;
	private Timer timer;
	private TimerTask a;

	public My3DScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		timer = new Timer();
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				handlewhat(msg.what, 2);
			}
		};
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		// mScroller = new Scroller(context);
		setChildrenDrawingOrderEnabled(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Log.i("tag", "onmeasure");
		/**
		 * 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
		 */
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
		int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

		// Log.i("tag", (heightMode == MeasureSpec.UNSPECIFIED) + "," +
		// sizeHeight
		// + "," + getLayoutParams().height);
		// 计算出所有的childView的宽和高
		measureChildren(widthMeasureSpec, heightMeasureSpec);
		/**
		 * 记录如果是wrap_content是设置的宽和高
		 */
		int width = 400;
		int height = 400;
		mCount = getChildCount();
		/**
		 * 根据childView计算的出的宽和高，以及设置的margin计算容器的宽和高，主要用于容器是warp_content时
		 */
		for (int i = 0; i < mCount; i++) {
			View childView = getChildAt(i);

			int cWidth = childView.getMeasuredWidth();
			int cHeight = childView.getMeasuredHeight();
			// Log.i("tag", "cWidth:" + cWidth + ",cHeight:" + cHeight);
			if (maxChildHeight < cHeight) {
				maxChildHeight = cHeight;
			}
			if (maxChildWidth < cWidth) {
				maxChildWidth = cWidth;
			}

		}

		/**
		 * 如果是wrap_content设置为我们计算的值 否则：直接设置为父容器计算的值
		 */
		setMeasuredDimension((widthMode == MeasureSpec.EXACTLY) ? sizeWidth
				: width, (heightMode == MeasureSpec.EXACTLY) ? sizeHeight
				: height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Log.i("tag", "onlayout");
		mWidth = getWidth();
		mHeight = getHeight();
		redius = mHeight / 2 - maxChildHeight;
		aveAngel = 360 / mCount;
		setScrollX(0);
		float remain = curAngel % 360;
		int x = 360 / (2 * mCount);
		for (int i = 0; i <= mCount; i++) {
			if (remain >= 0) {
				if (remain <= aveAngel * i + x && remain > aveAngel * i - x) {
					mCurrentMiddle = (mCount - i) % mCount;
					break;
				}
			} else if (remain < 0) {
				if (remain > -aveAngel * i - x && remain <= -aveAngel * i + x) {
					mCurrentMiddle = i % mCount;
					break;
				}
			}
		}
		// Log.i("tag", "mCurrentMiddle::" + mCurrentMiddle);
		for (int i = 0; i < mCount; i++) {
			View child = getChildAt(i);

			int cHeight = child.getMeasuredHeight();
			child.layout(
					l, t + mHeight / 2 - (int) (redius * Math
							.sin((curAngel + aveAngel * i) * Math.PI / 180)) - cHeight / 2, r, t + mHeight
							/ 2 - (int) (redius * Math.sin((curAngel + aveAngel * i) * Math.PI
											/ 180)) + cHeight / 2);
		}
		refreshScaleAndAlpha();
	}

	private void refreshScaleAndAlpha() {
		for (int i = 0; i < getChildCount(); i++) {
			View childView = getChildAt(i);
			if (i != mCurrentMiddle) {
				childView.setFocusableInTouchMode(false);
				childView.setFocusable(false);
				childView.setEnabled(false);
				childView.setClickable(false);
				childView.setActivated(false);
				childView.clearFocus();
			} else {
				childView.setFocusableInTouchMode(true);
				childView.setFocusable(true);
				childView.setEnabled(true);
				childView.setClickable(true);
				childView.setActivated(false);
			}
			childView.setScaleX(0.5f + ((float) (Math.cos((curAngel + aveAngel
					* i)
					* Math.PI / 180) + 1) / 4));
			;
			childView.setScaleY(0.5f + ((float) (Math.cos((curAngel + aveAngel
					* i)
					* Math.PI / 180) + 1) / 4));
			;
			if (mCurrentMiddle != i) {
				childView.setEnabled(false);
				childView
						.setAlpha(0.5f + ((float) (Math
								.cos((curAngel + aveAngel * i) * Math.PI / 180) + 1) / 4));
			} else {
				childView.setEnabled(true);
				childView.setAlpha(1);
			}
			childView.invalidate();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float y = event.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			curAngel += (mLastMotionY - y) * angelPerY;
			curAngel %= 360;
			Log.i("tag", "curAngel:" + curAngel);
			requestLayout();
			invalidate();
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
			float remain = curAngel % 360;
			int x = 360 / (2 * mCount);
			for (int i = 0; i <= mCount; i++) {
				if (remain >= 0) {
					if (remain <= aveAngel * i + x && remain > aveAngel * i - x) {
						runUIchange(aveAngel * i);
						break;
					}
				} else if (remain < 0) {
					if (remain > -aveAngel * i - x
							&& remain <= -aveAngel * i + x) {
						runUIchange(-aveAngel * i);
						break;
					}
				}
			}
			break;
		default:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return true;
	}

	private void runUIchange(final int what) {
		if (a != null) {
			a.cancel();
			a = null;
		}
		a = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(what);
			}
		};
		int tempangel = curAngel;
		timer.schedule(a, 0, 15);
	}

	/**
	 * 根据当前的触摸状态来决定是否屏蔽子控件的交互能力。
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}
		float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionY = y;
			mTouchState = TOUCH_STATE_REST;
			break;
		case MotionEvent.ACTION_MOVE:
			int yDiff = (int) Math.abs(mLastMotionY - y);
			if (yDiff > mTouchSlop) {
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			break;
		case MotionEvent.ACTION_UP:
		default:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return mTouchState != TOUCH_STATE_REST;
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		if (i < mCurrentMiddle) {
			return i;
		} else if (i >= mCurrentMiddle) {
			return childCount - 1 - i + mCurrentMiddle;
		} else {
			return childCount;
		}

	}

	@Override
	public void computeScroll() {
//		 Log.i("tag", "computescroll");
//		 先判断mScroller滚动是否完成
//		 if (mScroller.computeScrollOffset()) {
//		 Log.i("tag", "mScroller.getCurrX(), mScroller.getCurrY()::"
//		 + mScroller.getCurrX() + "," + mScroller.getCurrY());
//		 // 这里调用View的scrollTo()完成实际的滚动
//		 scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
//
//		 // 必须调用该方法，否则不一定能看到滚动效果
//		 postInvalidate();
//		 }
		super.computeScroll();
	}

	public void handlewhat(int angel, int step) {
		if (angel - curAngel > 0) {
			curAngel += step;
			if (curAngel > angel) {
				curAngel = angel;
				a.cancel();
			}
		} else {
			curAngel -= step;
			if (curAngel <= angel) {
				curAngel = angel;
				a.cancel();
			}
		}
		requestLayout();
		invalidate();
	}
}
