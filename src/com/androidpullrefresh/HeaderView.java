package com.androidpullrefresh;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.androidpulltest.R;

/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */
public class HeaderView extends LinearLayout {
	private int			curtop;//
	private ProgressBar	progressBar;
	private ImageView	arrows;
	private TextView	tvRefresh;
	private TextView	tvDate;
	private boolean		isArrowsUp	= true;
	private HeaderView  headerView;

	public HeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}

	public HeaderView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.pull_header, this);
		headerView=this;
		init(context);
	}

	private void init(Context context) {
		progressBar = (ProgressBar) findViewById(R.id.pull_to_refresh_progress);
		arrows = (ImageView) findViewById(R.id.pull_to_refresh_image);
		tvRefresh = (TextView) findViewById(R.id.pull_to_refresh_text);
		tvDate = (TextView) findViewById(R.id.pull_to_refresh_updated_at);
	}

	/**
	 * 下拉刷新
	 */
	public int setStartRefresh() {
		arrows.setVisibility(View.VISIBLE);
		tvRefresh.setVisibility(View.VISIBLE);
		tvDate.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
		tvRefresh.setText("下拉刷新");

		if (!isArrowsUp) {
			RotateAnimation mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF,
					0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
			mReverseFlipAnimation.setDuration(250);
			mReverseFlipAnimation.setFillAfter(true);

			arrows.clearAnimation();
			arrows.setAnimation(mReverseFlipAnimation);
		}

		isArrowsUp = true;
		return PullScrollView.PULL_DOWN_STATE;
	}

	/**
	 * 松开手刷新
	 */
	public int releaseFreshing() {
		arrows.setVisibility(View.VISIBLE);
		tvRefresh.setVisibility(View.VISIBLE);
		tvDate.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
		tvRefresh.setText("松开手刷新");

		if (isArrowsUp) {
			RotateAnimation animationUp = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			animationUp.setInterpolator(new LinearInterpolator());
			animationUp.setDuration(250);
			animationUp.setFillAfter(true);

			arrows.clearAnimation();
			arrows.setAnimation(animationUp);
		}

		isArrowsUp = false;
		return PullScrollView.RELEASE_TO_REFRESH;
	}

	/**
	 * 正在刷新
	 */
	public int setRefreshing() {
		arrows.clearAnimation();
		arrows.setVisibility(View.GONE);
		tvRefresh.setVisibility(View.VISIBLE);
		tvDate.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);
		tvRefresh.setText("正在刷新...");
		return PullScrollView.REFRESHING;
	}

	/**
	 * 设置 View 高度
	 * 
	 * @param presetHeight
	 *            原始高度
	 * @param currentHeight
	 *            当前高度
	 */
	public int setPadding(int presetHeight, int currentHeight) {
		this.setPadding(0, currentHeight, 0, 0);
		curtop=headerView.getPaddingTop();
		new ScrollTask().execute(currentHeight);
		// 初始化箭头状态向下
		if (currentHeight <= presetHeight/2) {
			Log.v("TouthY","状态改变成下拉刷新");
			return setStartRefresh();
		} else { // 改变按钮状态向上
			Log.v("TouthY","状态改变成释放刷新");
			return releaseFreshing();
		}
	}

	/**
	 * 初始化 HeadView PaddingTop
	 */
	public void setPaddingTop(int paddingTop) {
		//this.setPadding(0, paddingTop, 0, 0);
		curtop=headerView.getPaddingTop();
		new ScrollTask().execute(paddingTop);
	}
	   class ScrollTask extends AsyncTask<Integer, Integer, Integer> {
		   
	        @Override
	        protected Integer doInBackground(Integer... speed) {
	            // 根据传入的速度来滚动界面，当滚动到达左边界或右边界时，跳出循环。
	        	int ptop = speed[0];
	            while (true) {
	            	curtop-=2;
	            	if(curtop<=ptop){
	            		break;
	            	}
	                // 为了要有滚动效果产生，每次循环使线程睡眠20毫秒，这样肉眼才能够看到滚动动画。
	            	publishProgress(curtop);
	                sleep(5);
	            }
				return ptop;
	        }
	 
	        @Override
	        protected void onProgressUpdate(Integer... ptop) {
	        	headerView.setPadding(0, ptop[0], 0, 0);
	        }
	 
	        @Override
	        protected void onPostExecute(Integer ptop) {

	        }
	    }
	 
	    /**
	     * 使当前线程睡眠指定的毫秒数。
	     * 
	     * @param millis
	     *            指定当前线程睡眠多久，以毫秒为单位
	     */
	    private void sleep(long millis) {
	        try {
	            Thread.sleep(millis);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
}
