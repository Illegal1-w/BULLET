package com.librarysx.BuildTest.android;

import android.app.*;
import android.os.*;
import com.librarysx.android.view.*;
import android.widget.*;
import android.view.*;
import android.media.*;
import java.io.*;
import android.graphics.Color;
import android.view.View.*;
import android.graphics.*;
import android.view.inputmethod.*;
import android.content.*;

public class MainActivity extends Activity 
{
	private MediaPlayer cam;
	//private boolean touched=false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		try{
		getActionBar().hide();
        final BulletScreen s=new BulletScreen(this);
		//s.ingoreRect(new Rect(88,88,123,123));
		int c=(int)(9000*Math.random())+100;
		BulletScreen.Bullet bxb=new BulletScreen.Bullet();
		bxb.color=Color.argb(1.0f,(float)Math.random(),(float)Math.random(),(float)Math.random());
		bxb.message="你好，一共有"+c+"条弹幕";
		bxb.textSize=25;
		bxb.x=0;
		bxb.y=65;
		bxb.survivalTime=8000;
		bxb.hasSurvivalTime=true;
		bxb.computeTextWidth();
		bxb.speed=0;
		s.getWorker().handleMessage(BulletScreen.BulletWorker.MSG_UPDATE,bxb);
		for(int i=0;i<c;i++){
		boolean drawh=Math.random()<0.1;
		BulletScreen.Bullet bb=new BulletScreen.Bullet(drawh);
		if(drawh){
			bb.data.putInt("cx",(int)(720*Math.random()));
			bb.data.putInt("cy",(int)(1554*Math.random()));
			bb.data.putInt("s",(int)(45*Math.random()));
		}
		bb.color=Color.argb(1f,(float)Math.random(),(float)Math.random(),(float)Math.random());
		bb.message=Math.random()>0.4?"What?:"+i:"??????????????????????????:"+i;
		bb.textSize=25;
		bb.x=(int)(720+Math.random()*20000);
		bb.y=(int)(1554*Math.random());
		if(drawh){
			bb.draw = new BulletScreen.Draw(){
				@Override
				public void draw(Canvas bmp,Paint pt,BulletScreen.Bullet b)
				{
					int cx=b.data.getInt("cx");
					int cy=b.data.getInt("cy");
					int c=b.data.getInt("s");
					bmp.save();
					for(int i=0;i<c;i++){
					bmp.rotate(i,cx,cy);
					//bmp.drawCircle(cx+i*2,cy+i*2,22,pt);
					defaultDraw(bmp,pt,b);
					}
					bmp.restore();
				}
			};
		}
		//bb.computeTextWidth();
		bb.speed=(int)(Math.random()*20)+3;
		s.addBullet(bb);
		}
			s.setCallback(new BulletScreen.Callback(){
					@Override
					public void onDraw()
					{
						
					}

					@Override
					public void onCreate()
					{
						cam=new MediaPlayer();
						try
						{
							cam.setDataSource("/sdcard/test.mp4");
							cam.setLooping(true);
							cam.prepare();
							cam.setSurface(new Surface(s.texImage));
							cam.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener(){
									@Override
									public void onVideoSizeChanged(MediaPlayer p1, int p2, int p3)
									{
										s.setVideoConfig(p2,p3);
									}
								});
						}
						catch (Exception e)
						{e.printStackTrace();
						Toast.makeText(MainActivity.this,e.toString(),3000).show();}
						cam.start();
					}
					@Override
					public void onChange()
					{
						s.sortBullet();
					}
					@Override
					public void onDestroy()
					{
						cam.pause();
					}
				});
			s.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View p1)
					{
						try{
						//if(e.getAction()!=e.ACTION_DOWN)return true;
						/*BulletScreen.Bullet bb=new BulletScreen.Bullet();
						bb.color=Color.argb(1.0f,(float)Math.random(),(float)Math.random(),(float)Math.random());
						bb.message="你好";
						bb.textSize=25;
						bb.x=(int)e.getX();
						bb.y=(int)e.getY();
						bb.computeTextWidth();
						s.getWorker().handleMessage(BulletScreen.BulletWorker.MSG_UPDATE,bb);*/
						/*if(e.getAction()==e.ACTION_MOVE)return false;
						BulletScreen.Bullet b=s.getBullet((int)e.getX(),(int)e.getY());
						if(b!=null){
						b.speed=0;
						b.hasSurvivalTime=true;
						b.survivalTime=5000;
						}
						FileOutputStream out=new FileOutputStream("/sdcard/bullet_shot.png");
						s.print(-1).compress(Bitmap.CompressFormat.PNG,100,out);
						out.close();*/
						s.seekTo(40);
							final EditText et=new EditText(MainActivity.this);
							et.setInputType(EditorInfo.TYPE_CLASS_TEXT);
							new AlertDialog.Builder(MainActivity.this)
								.setTitle("FastComment")
								.setView(et)
								.setPositiveButton("confirm", new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface p1, int p2){
										String text=et.getText().toString();
										BulletScreen.Bullet b=new BulletScreen.Bullet();
										b.message=text;
										b.x=720;
										b.y=500;
										b.textSize=30;
										b.speed=8;
										//b.computeTextWidth();
										s.addBulletQueue(b);
									}})
								.show();
						}catch(Throwable ee){
							ee.printStackTrace();
							Toast.makeText(MainActivity.this,ee.toString(),3000).show();
						}
						//return true;
					}
				});
		setContentView(s);
		}catch(Throwable e){
			e.printStackTrace();
			Toast.makeText(this,e.toString(),3000).show();
		}
    }
}
