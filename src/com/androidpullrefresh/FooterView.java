package com.androidpullrefresh;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidpullrefresh.HeaderView.ScrollTask;
import com.example.androidpulltest.R;

/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */
public class FooterView extends LinearLayout {
	private int			curbottom;
	private ProgressBar	progressBar;
	private ImageView	arrows;
	private TextView	tvRefresh;
	private boolean		isArrowsUp	= true;
	private FooterView  footerView;
	//数据加载完成不再加载
	public boolean     isloadover=false;//数据是否加载完成啦
	public FooterView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FooterView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.pull_footer, this);
		footerView=this;
		init(context);
	}

	private void init(Context context) {
		progressBar = (ProgressBar) findViewById(R.id.pull_to_load_progress);
		arrows = (ImageView) findViewById(R.id.pull_to_load_image);
		tvRefresh = (TextView) findViewById(R.id.pull_to_load_text);
	}

	/**
	 * 上拉加载更多
	 */
	public int setStartLoad() {
		if(!isloadover){
		progressBar.setVisibility(View.GONE);
		arrows.setVisibility(View.VISIBLE);
		tvRefresh.setText("上拉加载更多");

		if (!isArrowsUp) {
			RotateAnimation mReverseFlipAnimation = new RotateAnimation(180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
			mReverseFlipAnimation.setDuration(250);
			mReverseFlipAnimation.setFillAfter(true);

			arrows.clearAnimation();
			arrows.startAnimation(mReverseFlipAnimation);
		}

		isArrowsUp = true;
		return PullScrollView.PULL_UP_STATE;
		}else{
			return PullScrollView.GONE;
		}
	}

	/**
	 * 松开手加载更多
	 */
	public int releaseLoad() {
		progressBar.setVisibility(View.GONE);
		arrows.setVisibility(View.VISIBLE);
		tvRefresh.setText("松开手加载更多");

		if (isArrowsUp) {
			RotateAnimation animationUp = new RotateAnimation(0, 180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			animationUp.setInterpolator(new LinearInterpolator());
			animationUp.setDuration(250);
			animationUp.setFillAfter(true);

			arrows.clearAnimation();
			arrows.setAnimation(animationUp);
		}

		isArrowsUp = false;
		return PullScrollView.RELEASE_TO_LOADING;
	}

	/**
	 * 正在加载更多
	 */
	public int setLoading() {
		arrows.clearAnimation();
		progressBar.setVisibility(View.VISIBLE);
		arrows.setVisibility(View.GONE);
		tvRefresh.setText("正在加载更多");
		return PullScrollView.LOADING;
	}

	/**
	 * 设置 View 高度
	 * 
	 * @param presetHeight
	 *            原始高度
	 * @param currentHeight
	 *            当前高度
	 */
	public int setPadding(int presetHeight, int paddingHeight) {
		//如果没有加载完成的话就加载
		if(!isloadover){
			//移动过程中设置底部
			this.setPadding(0, 0, 0, paddingHeight);
	/*		curbottom=footerView.getPaddingBottom();
			new ScrollTask().execute(paddingHeight);*/
			// 初始化箭头状态向上
			if (paddingHeight <= presetHeight/2) {
				return setStartLoad();
			} else { // 改变按钮状态向下
				return releaseLoad();
			}
		}else{
			return PullScrollView.GONE;
		}
	}

	/**
	 * 初始化 FootView PaddingButtom
	 */
	public void setPaddingButtom() {
		//this.setPadding(0, 0, 0, 0);
		curbottom=footerView.getPaddingBottom();
		new ScrollTask().execute(0);
		arrows.clearAnimation();
	}

	/**
	 * 显示加载更多按钮
	 */
	public void show() {
		this.setVisibility(View.VISIBLE);
	}
	/*
	 * 加载完成状态
	 * */
	public void setloadOverText(String str){
		isloadover=true;
		if(TextUtils.isEmpty(str))str="加载完毕";
		tvRefresh.setText(str);
		arrows.setVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
	}
	/**
	 *重新开始加载数据重置
	 ***/
	public void resetloadOver(){
		isloadover=false;//设置成数据没有加载完成重新加载
		tvRefresh.setText("上拉加载更多");
		arrows.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);		
	}
	/**
	 * 隐藏加载更多按钮
	 */
	public void hide() {
		this.setVisibility(View.GONE);
	}
	   class ScrollTask extends AsyncTask<Integer, Integer, Integer> {
		   
	        @Override
	        protected Integer doInBackground(Integer... speed) {
	            // 根据传入的速度来滚动界面，当滚动到达左边界或右边界时，跳出循环。
	        	int dbottom = speed[0];
	            while (true) {
	            	curbottom-=2;
	            	if(curbottom<=dbottom){
	            		break;
	            	}
	                // 为了要有滚动效果产生，每次循环使线程睡眠20毫秒，这样肉眼才能够看到滚动动画。
	            	publishProgress(curbottom);
	                sleep(5);
	            }
				return dbottom;
	        }
	 
	        @Override
	        protected void onProgressUpdate(Integer... ptop) {
	        	footerView.setPadding(0, 0, 0, ptop[0]);
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

		public void setBackgroundColor(String string) {
			// TODO Auto-generated method stub
			
		}

}
