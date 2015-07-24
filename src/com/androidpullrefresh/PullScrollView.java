package com.androidpullrefresh;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */
public class PullScrollView extends ScrollView {

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
	private static final int	DONE				= 7;

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
		//判断屏蔽滚动条直接返回
		if(pullState != DONE && pullState!=LOADING && pullState!=REFRESHING)return true;
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
				if(onPullListener!=null)onPullListener.refresh();
			}
			// 松开手加载更多
			else if (pullState == RELEASE_TO_LOADING) {
				pullState = footerView.setLoading();
				footerView.setPaddingButtom();
				if(onPullListener!=null)onPullListener.loadMore();
			}
			// 重置到最初状态
			else {
				headerView.setPaddingTop(-1 * headContentHeight);
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
	 * 加载更多按钮不可见
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

	public interface OnPullListener {
		void refresh();

		void loadMore();
	}

	public void setOnPullListener(OnPullListener onPullListener) {
		this.onPullListener = onPullListener;
	}

}
