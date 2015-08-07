package com.androidpullrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {
	private int scrollY;
	private int startY;
	public MyScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public MyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.widget.ScrollView#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		//如果下拉状态不是空的话默认直接到父类处理
		Log.v("state",PullRefreshLinearView.pullState+"");
		if(PullRefreshLinearView.pullState!=PullRefreshLinearView.DONE){
			
			return false;
		}else{
			onTouchEvent(event);
		}
		return super.onInterceptTouchEvent(event);
	}
	/* (non-Javadoc)
	 * @see android.widget.ScrollView#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
			switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					scrollY = getScrollY();
					startY = (int) event.getY();
					break;

				case MotionEvent.ACTION_MOVE:

					int tempY = (int) event.getY() - startY;
					Log.v("yidong","位移:"+tempY+"--滚动条位置："+scrollY);
					// 如果 ScrollViwe 滑到最顶端，且有下拉刷新手势，则激活下拉刷新动作
					if (tempY > 0 && scrollY == 0) {
						//historyY=tempY;
						PullRefreshLinearView.pullState=PullRefreshLinearView.PULL_DOWN_STATE;
					}
					
					// 如果 ScrollViwe 滑倒最底端，且有上拉刷加载更多手势，则激活上拉加载更多动作
					else if (tempY < 0 ) { // 上拉加载更多
						PullRefreshLinearView.pullState=PullRefreshLinearView.PULL_DOWN_STATE;
					}
					break;

				case MotionEvent.ACTION_UP:
					//重置 headerView、footerView ,激化监听
				//	resetPullStateForActionUp();
					break;
			}
		return super.onTouchEvent(event);
	}

}
