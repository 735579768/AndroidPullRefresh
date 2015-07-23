# AndroidPullRefresh
<?xml version="1.0" encoding="utf-8"?>
<com.androidpullrefresh.PullScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scroll"
    android:layout_width="fill_parent"
    android:layout_height="fill_parent"
    android:background="#ffffff" />
    
    	//取下拉刷新对象
		pullScrollView = (PullScrollView) findViewById(R.id.scroll);
		pullScrollView.setShowFooter(false);//不显示底部
		//取要显示的内容视图
		 contentLayout=(LinearLayout)pullScrollView.addBodyLayoutFile(this,R.layout.layout_content);
		//内容视图设置下拉监听
		pullScrollView.setOnPullListener(this);
