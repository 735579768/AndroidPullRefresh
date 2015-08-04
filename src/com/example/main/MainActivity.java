package com.example.main;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.androidpullrefresh.PullRefreshScrollView;
import com.androidpullrefresh.PullRefreshScrollView.OnPullListener;
import com.example.androidpulltest.R;
/**
 * 
 * @since 2015 07 23
 * @author www.zhaokeli.com
 */

public class MainActivity extends Activity implements OnPullListener {

	private PullRefreshScrollView	pullScrollView;
	private Context mContext;
	private Button addbtn;
	private LinearLayout	contentLayout;
	private ListView		listView;
	private List<String>	data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.mContext=this;
		addbtn=(Button)findViewById(R.id.addbtn);
		addbtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				pullScrollView.addBodyLayoutFile(mContext,R.layout.layout_content);
			}
			
		});
		//取下拉刷新对象
		pullScrollView = (PullRefreshScrollView) findViewById(R.id.scroll);
		pullScrollView.setfooterEnabled(false);
		//取要显示的内容视图
		pullScrollView.addBodyLayoutFile(this,R.layout.layout_content);
		//内容视图设置下拉监听
		pullScrollView.setOnPullListener(this);
			
			
		 //初始化列表数据
		listView = (ListView)findViewById(R.id.ListViewTest);
		data = new ArrayList<String>();
		for (int i = 0; i < 19; i++) {
			data.add("列表数据 " + i);
		}
		listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, data));

		
		//如果初始化时scrollview的滚动条不天最顶部的话用下面的代码延时把它设置到顶部
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				pullScrollView.fullScroll(ScrollView.FOCUS_UP);
			}
			
		}, 10);
		
	}

	@Override
	public void refresh() {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				pullScrollView.setheaderViewReset();
				data.add(0, "刷新测试数据");
				((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
				pullScrollView.setfooterEnabled(true);
			}

		}, 2000);
	}

	@Override
	public void loadMore() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if(listView.getAdapter().getCount()>20){
				//加载完毕
					pullScrollView.setfooterLoadOverText(null);
				}else{
					pullScrollView.setfooterViewReset();
					data.add("加载更多数据");
					((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
				}	
			}
		}, 2000);
	}
}
