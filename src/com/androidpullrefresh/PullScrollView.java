package com.androidpullrefresh;

import java.text.SimpleDateFormat;

import com.example.androidpulltest.R;



import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */
public class PullScrollView extends ScrollView {
	private AlphaAnimation mHideAnimation= null;//渐隐

	private AlphaAnimation mShowAnimation= null;//渐显

	private static final String	TAG					= "PullScrollView";

	// pull state 上拉开始加载更多
	public static final int		PULL_UP_STATE		= 0;

	// pull state 下拉开始刷新
	public static final int		PULL_DOWN_STATE		= 1;

	// release states 释放 去刷新
	public static final int		RELEASE_TO_REFRESH	= 3;

	// release states 释放 去加载更多
	public static final int		RELEASE_TO_LOADING	= 4;

	// 正在刷新
	public static final int		REFRESHING			= 5;

	// 正在加载更多
	public static final int		LOADING				= 6;

	// 没做任何操作
	public static final int	DONE				= 7;
	// 设置滚动条到上面
	public static final int	SET_SCROLL_UP		= 8;
	// 设置滚动条到下面
	public static final int	SET_SCROLL_DOWN		= 9;
	// 实际的padding的距离与界面上偏移距离的比例
	private final static int	RATIO				= 4;
	private LinearLayout		innerLayout;
	private LinearLayout		bodyLayout;

	private HeaderView			headerView;
	private FooterView			footerView;
	private PullScrollView		pullscrollView;
	private int					headContentHeight;
	private int					footContentHeight;
	// Down 初始化 Y
	private int					startY;//手按下时的Y轴坐标
//	private int					historyY;//手移动过程中记录的前一个Y轴坐标
	private int					scrollY				= -1;

	private Context				mContext;

	// 上下拉状态
	private int					pullState;

	// 刷新、加载更多 接口
	private OnPullListener		onPullListener;
	
	//是否要底部加载更多
	private boolean 			isfooter=true;
	//是否在线程中设置滚动条位置
	private boolean 			isSetScrolling=false;
	
/*	Handler mHandler=new Handler();
	Runnable scrollViewRunable = new Runnable() {  
	    @Override  
	    public void run() {  
	    	//pullscrollView.scrollTo(10, 10) ;
			pullscrollView.fullScroll(ScrollView.FOCUS_UP);
	    }  
	  };*/
	public void setFooterShow(boolean show){
		this.isfooter=show;
		innerLayout.removeViewAt(2);
	}
	public PullScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PullScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PullScrollView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		this.mContext = context;
		// ScrollView 可以滑动必须有且只有一个子View - ScrollView 内装的View都将放在 innerLayout 里面
		// ScrollView 设置为上下滚动 LinearLayout.VERTICAL
		innerLayout = new LinearLayout(context);
		innerLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		innerLayout.setOrientation(LinearLayout.VERTICAL);

		addheaderView();

		// 设置 bodyLayout 区域
		bodyLayout = new LinearLayout(context);
		bodyLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		bodyLayout.setOrientation(LinearLayout.VERTICAL);
		innerLayout.addView(bodyLayout);

		pullscrollView=this;

		// 初始化刷新、加载状态
		pullState = DONE;
	}

	/**
	 * 添加 headerView
	 */
	private void addheaderView() {
		headerView = new HeaderView(mContext);
		measureView(headerView);

		headContentHeight = headerView.getMeasuredHeight();
		Log.v("TouthY","headerview高度"+headContentHeight);
		// 初始化 headerView 位置（不可见）
		headerView.setPadding(0,-1 * headContentHeight,0,0);
		headerView.invalidate();

		innerLayout.addView(headerView);
		addView(innerLayout);
	}

	/**
	 * 添加 footerView
	 */
	private void addfooterView() {
		footerView = new FooterView(mContext);
		measureView(footerView);
		footContentHeight = footerView.getMeasuredHeight();
		footerView.setPaddingButtom();
		footerView.invalidate();
		//先把页脚隐藏
	    mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
	    mHideAnimation.setDuration(0);
	    mHideAnimation.setFillAfter( true );
	    footerView.startAnimation( mHideAnimation );
	    
		innerLayout.addView(footerView);
	}

	/**
	 * 添加 BodyView : 滑动内容区域
	 */
	private void addBodyView(View view) {
		bodyLayout.addView(view);
	}
	/**
	 * 添加布局文件 : 滑动内容区域
	 * 返回添加成功的view
	 */	
	public View addBodyLayoutFile(Context context,int res){
		LayoutInflater inflater=LayoutInflater.from(context);
		View view=(LinearLayout)inflater.inflate(res, null);
		addBodyView(view);
		return view;
	}
	/**
	 * footer view 在此添加保证添加到 innerLayout 中的最后
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		if(isfooter)addfooterView();
	}

	/**
	 * 滑动时，首先会触发 onInterceptTouchEvent事件，然后触发 onTouchEvent 事件时
	 * 
	 * onInterceptTouchEvent 总是将 onTouchEvent 事件中的 ACTION_DOWN 事件拦截
	 * 
	 * 所以在此做监听，以防万一
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// 首先拦截down事件,记录y坐标
				scrollY = getScrollY();
				Log.i(TAG, "在down时候记录当前位置scrollY[onInterceptTouchEvent]" + scrollY);
				startY = (int) event.getY();
				Log.i(TAG, "在down时候记录当前位置startY[onInterceptTouchEvent]" + startY);
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
		}
		onTouchEvent(event);
		return super.onInterceptTouchEvent(event);
	}

	/**
	 * 监听上下拉首饰操作
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (isLoadable()) {
			switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					scrollY = getScrollY();
					startY = (int) event.getY();
					break;

				case MotionEvent.ACTION_MOVE:

					int tempY = (int) event.getY() - startY;
					
					// 如果 ScrollViwe 滑到最顶端，且有下拉刷新手势，则激活下拉刷新动作
					if (tempY > 0 && scrollY == 0) {
						//historyY=tempY;
						changeheaderViewHeight(tempY);
					}
					
					// 如果 ScrollViwe 滑倒最底端，且有上拉刷加载更多手势，则激活上拉加载更多动作
					else if (tempY < 0 && isfooter) { // 上拉加载更多
						changefooterViewHeight(tempY);
					}
					break;

				case MotionEvent.ACTION_UP:
					// 重置 headerView、footerView ,激化监听
					resetPullStateForActionUp();
					break;
			}
		}
		if(pullState==DONE){
			this.setVerticalScrollBarEnabled(true);
		}else{
			this.setVerticalScrollBarEnabled(false);
		}
		
		//下拉和释放状态屏蔽滚动条
		if(pullState==PULL_DOWN_STATE||pullState==RELEASE_TO_REFRESH){
			return true;
		}
		//上拉和下保持滚动条在下面
		if(pullState==PULL_UP_STATE){
			//this.fullScroll(ScrollView.FOCUS_DOWN);
			//启动线程设置滚动条到最下面,上面代码使用后会抖动
			if(!isSetScrolling)new ScrollTask().execute(SET_SCROLL_DOWN);
			return true;
		}
		//释放状态屏蔽滚动条
		if(pullState==RELEASE_TO_LOADING)return true;
		return super.onTouchEvent(event);
	}
	/*
	 * 改变 headerView 高度
	 */
	private void changeheaderViewHeight(int tempY) {

		if (pullState == DONE) {
			Log.v("TouthY","没有任何操作");
			pullState = headerView.setStartRefresh();
		}

		if (pullState == PULL_DOWN_STATE || pullState == RELEASE_TO_REFRESH) {
			
			int pdtop=0;
			pdtop=-1 * headContentHeight + tempY / RATIO;
			Log.v("TouthY", pdtop+"");
			pullState = headerView.setPadding(headContentHeight,pdtop);
		}
	}

	/*
	 * 改变 footerView 高度
	 */
	private void changefooterViewHeight(int tempY) {
		//底部头如果是隐藏的话直接返回
		if (footerView.getVisibility() == View.GONE) {
			return;
		}
		//如果scrollview已经滚动到底部的话就显示向上拉动加载更多
		if (getScrollY() + getHeight()>=getChildAt(0).getMeasuredHeight()) {
			if (pullState == DONE) {
				pullState = footerView.setStartLoad();
			}
		}
		//如果状态是向上拉动或松手加载更多就进行下面操作
		if (pullState == PULL_UP_STATE || pullState == RELEASE_TO_LOADING) {
			int pb=0;
			pb=(Math.abs(-tempY) / RATIO);
			Log.v("bottom",pb+"");
			pullState = footerView.setPadding(footContentHeight,pb);
		}
	}

	/*
	 * 当手离开屏幕后重置 pullState 状态及 headerView、footerView，激活监听事件
	 */
	private void resetPullStateForActionUp() {
		if (pullState != REFRESHING && pullState != LOADING) {

			// 松开手刷新
			if (pullState == RELEASE_TO_REFRESH) {
				pullState = headerView.setRefreshing();
				headerView.setPaddingTop(0);
				//松手刷新等于重新加载，直接把下面加载更多的状态重置
				footerView.resetloadOver();
				footerView.setStartLoad();
				if(onPullListener!=null)onPullListener.refresh();
			}
			// 松开手加载更多
			else if (pullState == RELEASE_TO_LOADING) {
				pullState = footerView.setLoading();
				footerView.setPaddingButtom();
				if(onPullListener!=null)onPullListener.loadMore();
				setShowAnimation(footerView,300);
			}
			// 重置到最初状态
			else {
				headerView.setPaddingTop(-1 * headContentHeight);
				//这个地方判断内容是不是满屏(等待实现)
				footerView.setPaddingButtom();
				pullState = DONE;
			}
		}
	}

	/**
	 * 判断是否可以上下拉刷新加载手势
	 * 
	 * @return true：可以
	 */
	private boolean isLoadable() {
		if (pullState == REFRESHING || pullState == LOADING)
			return false;
		return true;
	}

	/**
	 * 重置下拉刷新按钮状态并隐藏
	 */
	public void setheaderViewReset() {
		headerView.setStartRefresh();
		headerView.setPaddingTop(-1 * headContentHeight);
		pullState = DONE;
	}

	/**
	 * 加载更多按钮不可见
	 */
	public void setfooterViewGone() {
		footerView.setStartLoad();
		footerView.hide();
		pullState = DONE;
	}
	/**
	 * 加载更多按钮可见
	 */
	public void setfooterViewShow() {
		footerView.setStartLoad();
		footerView.show();
		pullState = DONE;
	}
	/**
	 * 设置页脚文本加载完成时状态
	 */
	public void setfooterLoadOverText(String str){
		if(TextUtils.isEmpty(str))str="加载完成";
		footerView.setloadOverText(str);
		pullState = DONE;
	}
	/**
	 * 加载更多按钮重置为加载更多状态
	 */
	public void setfooterViewReset() {
		footerView.resetloadOver();
		footerView.setStartLoad();
		pullState = DONE;
	}

	/**
	 * 如果：高度>0,则有父类完全决定子窗口高度大小；否则，由子窗口自己觉得自己的高度大小
	 * 
	 * 设置 headerView、HootViwe 的 LayoutParams 属性
	 * 
	 * @param childView
	 */
	private void measureView(View childView) {
		ViewGroup.LayoutParams p = childView.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		childView.measure(childWidthSpec, childHeightSpec);
	}
	/**
	* View渐隐动画效果
	*
	*/
	private void setHideAnimation( View view, int duration ){
	    if( null == view || duration < 0 ){
	        return;
	    }
	    if( null != mHideAnimation ){
	        mHideAnimation.cancel();
	    }
	    mHideAnimation = new AlphaAnimation(1.0f, 0.1f);
	    mHideAnimation.setDuration( duration );
	    mHideAnimation.setFillAfter( true );
	    view.startAnimation( mHideAnimation );
	}
	/**
	* View渐现动画效果
	*
	*/
	private void setShowAnimation( View view, int duration ){
	    if( null == view || duration < 0 ){
	        return;
	    }
	    if( null != mShowAnimation ){
	    	//if(mShowAnimation.cancel()	instanceof null);
	        mShowAnimation.cancel();
	    }
	    mShowAnimation = new AlphaAnimation(0.1f, 1.0f);
	    mShowAnimation.setDuration( duration );
	    mShowAnimation.setFillAfter( true );
	    view.startAnimation( mShowAnimation );
	} 
	public interface OnPullListener {
		void refresh();

		void loadMore();
	}

	public void setOnPullListener(OnPullListener onPullListener) {
		this.onPullListener = onPullListener;
	}
   class ScrollTask extends AsyncTask<Integer, Integer, Integer> {
	   
        @Override
        protected Integer doInBackground(Integer... speed) {
            // 根据传入的速度来滚动界面，当滚动到达左边界或右边界时，跳出循环。
        	isSetScrolling=true;
        	int dbottom = speed[0];
            while (true) {
            	if(pullState==DONE){
            		isSetScrolling=false;
            		break;
            	}
                // 为了要有滚动效果产生，每次循环使线程睡眠20毫秒，这样肉眼才能够看到滚动动画。
            	publishProgress(dbottom);
                sleep(1);
            }
            return 0;
        }
 
        @Override
        protected void onProgressUpdate(Integer... ptop) {
        	setScroll(ptop[0]);
        }
 
        @Override
        protected void onPostExecute(Integer ptop) {
        }
        private void setScroll(int t){
        	if(t==SET_SCROLL_UP){
        		pullscrollView.fullScroll(FOCUS_UP);
        	}else if(t==SET_SCROLL_DOWN){
        		pullscrollView.fullScroll(FOCUS_DOWN);
        	}
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

/**
* 
* @since 2015 07 23
* @author www.zhaokeli.com
*/
class HeaderView extends LinearLayout {
	private int			curtop;//
	private ProgressBar	progressBar;
	private ImageView	arrows;
	private TextView	tvRefresh;
	private TextView	tvDate;
	private String		refreshDate;
	private boolean		isArrowsUp	= true;
	private HeaderView  headerView;

	public HeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}

	public HeaderView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.pull_header, this);
		headerView=this;
		//this.setBackgroundColor( getResources().getColor(android.R.color.background_dark));
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
/*		if(TextUtils.isEmpty(refreshDate))refreshDate="未刷新过";
		tvDate.setText(refreshDate);*/
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		refreshDate="上次更新:"+sdf.format(new java.util.Date());
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
		//移动过程中设置顶部
		this.setPadding(0, currentHeight, 0, 0);
		//if(TextUtils.isEmpty(refreshDate))refreshDate="未刷新过";
		tvDate.setText(refreshDate);
/*		curtop=headerView.getPaddingTop();
		new ScrollTask().execute(currentHeight);*/
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


/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */
class FooterView extends LinearLayout {
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
			return PullScrollView.DONE;
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
			return PullScrollView.DONE;
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
