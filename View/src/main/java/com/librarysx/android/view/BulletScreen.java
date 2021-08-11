package com.librarysx.android.view;
import android.content.*;
import android.graphics.*;
import android.opengl.*;
import android.os.*;
import android.util.*;
import android.view.*;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;

import android.opengl.Matrix;
import java.io.*;

public class BulletScreen extends SurfaceView implements SurfaceHolder.Callback
{
	public static SurfaceTexture texImage;
	private static int texId,texId1,programId,width,height,framebufferId,texImageId;
	private static FloatBuffer vertex;
	private Callback c;
	private static final float[] vertexData = {
		1f,  1f,  1f,  1f,
		-1f,  1f,  0f,  1f,
		-1f, -1f,  0f,  0f,
		1f,  1f,  1f,  1f,
		-1f, -1f,  0f,  0f,
		1f, -1f,  1f,  0f
	};
	private float[] ratio;
	private float aspectH,aspectW;
	public void setFramebufferID(int framebufferId,int texImageId)
	{
		this.framebufferId = framebufferId;
		this.texImageId=texImageId;
	}

	public int getFramebufferID()
	{
		return framebufferId;
	}
	public int getVideoTexID(){
		return texId1;
	}
	public void setAspectH(float aspectH)
	{
		this.aspectH = aspectH;
	}
	public float getAspectH()
	{
		return aspectH;
	}
	public void setAspectW(float aspectW)
	{
		this.aspectW = aspectW;
	}
	public float getAspectW()
	{
		return aspectW;
	}
	private int createTextureID(int type)
	{
		int[] texture = new int[1];
		GLES20.glGenTextures(1, texture, 0);
		GLES20.glBindTexture(type, texture[0]);
		GLES20.glTexParameterf(type,
							   GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameterf(type,
							   GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameteri(type,
							   GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
		GLES20.glTexParameteri(type,
							   GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
		return texture[0];
	}
	public void setVideoConfig(int w, int h)
	{
		ratio = new float[16];
		if (w != width || h != height)getMatrix(ratio, h, w, height, width);
		aspectH = 0;
		aspectW = 0;
	}
	private void getMatrix(float[] matrix, int imgWidth, int imgHeight, int viewWidth, int
						   viewHeight)
	{
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0)
		{
            float sWhView=(float)viewWidth / viewHeight;
            float sWhImg=(float)imgWidth / imgHeight;
            float[] projection=new float[16];
            float[] camera=new float[16];
            /*if (sWhImg > sWhView)
			{*/
            //Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
            /*}
			else
			{*/
            Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
            //}
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }
	private FloatBuffer createBuffer(float[] vertexData)
	{
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
        buffer.put(vertexData, 0, vertexData.length).position(0);
        return buffer;
    }
	public void setCallback(Callback c)
	{
		this.c = c;
	}
	@Override
	public void surfaceCreated(SurfaceHolder p1)
	{
		glThread = new HandlerThread("BulletRendererThread");
		glThread.start();
		s = new Handler(glThread.getLooper(), new GLWorker());
		GLWorker.HandlingRunnable r=new GLWorker.HandlingRunnable(){
			private static final String FRAGMENT_SHADER = "" +
			"#extension GL_OES_EGL_image_external : require\n" +
			"precision mediump float;\n" +
			"uniform samplerExternalOES uTextureSampler;\n" +
			"uniform sampler2D bullet;\n" +
			"uniform vec2 aspect;\n" +
			"varying vec2 vTextureCoord;\n" +
			"varying vec2 sTextureCoord;\n" +
			"void main() \n" +
			"{\n" +
			"  vec2 uv=vTextureCoord+aspect;\n"+
			"  vec4 vCameraColor=vec4(0.0);"+
			"  if(uv.x<=1.0||uv.y>0.0||uv.x>0.0||uv.y<=1.0){\n"+
			"  vCameraColor = texture2D(uTextureSampler, uv);\n" +
			"  }\n"+
			"  vec4 r = texture2D(bullet,vec2(sTextureCoord.x,1.0-sTextureCoord.y));\n" +
			"  gl_FragColor = mix(vCameraColor,r,r.a);\n" +
			"}\n";
			private static final String VERTEX_SHADER = "" +
			"attribute vec4 aPosition;\n" +
			"attribute vec4 aTextureCoordinate;\n" +
			"uniform mat4 videoMatrix;\n" +
			"uniform mat4 videoCamera;\n" +
			"varying vec2 vTextureCoord;\n" +
			"varying vec2 sTextureCoord;\n" +
			"void main()\n" +
			"{\n" +
			"  vTextureCoord = (videoMatrix*videoCamera*aTextureCoordinate).xy;\n" +
			"  sTextureCoord = aTextureCoordinate.xy;\n" +
			"  gl_Position = aPosition;\n" +
			"}\n";
			@Override
			public void run(int w, int h)
			{
				vertex = createBuffer(vertexData);
				int vshaid=GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
				GLES20.glShaderSource(vshaid, VERTEX_SHADER);
				GLES20.glCompileShader(vshaid);
				int[] compiled=new int[1];
				GLES20.glGetShaderiv(vshaid, GLES20.GL_COMPILE_STATUS, compiled, 0);
				if (compiled[0] == 0)
				{
					Log.e("Shader", "Could not compile shader:FRAGMENT");
					Log.e("Shader", "GLES20 Error:" + GLES20.glGetShaderInfoLog(vshaid));
					GLES20.glDeleteShader(vshaid);
					vshaid = 0;
				}
				int fshaid=GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
				GLES20.glShaderSource(fshaid, FRAGMENT_SHADER);
				GLES20.glCompileShader(fshaid);
				compiled = new int[1];
				GLES20.glGetShaderiv(fshaid, GLES20.GL_COMPILE_STATUS, compiled, 0);
				if (compiled[0] == 0)
				{
					Log.e("Shader", "Could not compile shader:FRAGMENT");
					Log.e("Shader", "GLES20 Error:" + GLES20.glGetShaderInfoLog(fshaid));
					GLES20.glDeleteShader(fshaid);
					fshaid = 0;
				}
				programId = GLES20.glCreateProgram();
				GLES20.glAttachShader(programId, fshaid);
				GLES20.glAttachShader(programId, vshaid);
				GLES20.glLinkProgram(programId);
				GLES20.glUseProgram(programId);
				texId = createTextureID(GLES20.GL_TEXTURE_2D);
				texId1 = createTextureID(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
				texImage = new SurfaceTexture(texId1);
				texImage.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener(){
						@Override
						public void onFrameAvailable(SurfaceTexture p1)
						{
							Message e=Message.obtain(s, GLWorker.MSG_RUN_HANDLER, new Renderer());
							e.sendToTarget();
							e = Message.obtain(s, GLWorker.MSG_SWAP_BUFFER);
							e.sendToTarget();
						}
					});
				if (c != null)c.onCreate();
			}
		};
		Message e=Message.obtain(s, GLWorker.MSG_SURFACE_CREATE, p1);
		e.sendToTarget();
		e = Message.obtain(s, GLWorker.MSG_RUN_HANDLER, r);
		e.sendToTarget();
	}

	@Override
	public void surfaceChanged(SurfaceHolder p1, int p2, int p3, int p4)
	{
		width = p3;
		height = p4;
		Message e=Message.obtain(s, GLWorker.MSG_SURFACE_CHANGE, p3, p4);
		e.sendToTarget();
		if (c != null)c.onChange();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder p1)
	{
		GLWorker.HandlingRunnable r=new GLWorker.HandlingRunnable(){
			@Override
			public void run(int w, int h)
			{
				GLES20.glDeleteProgram(programId);
				GLES20.glDeleteTextures(2, new int[]{texId,texId1}, 0);
			}
		};
		Message e=Message.obtain(s, GLWorker.MSG_RUN_HANDLER, r);
		e.sendToTarget();
		e = Message.obtain(s, GLWorker.MSG_SURFACE_DESTROY);
		e.sendToTarget();
		glThread.quitSafely();
		if (c != null)c.onDestroy();
	}

	private Handler s;
	private BulletWorker w;
	private static HandlerThread glThread;
	private ArrayList<Object> cache,cache1;
	private static Rect irect;
	public void pauseBulletMove()
	{
		cache = new ArrayList<>();
		for (Bullet b:w.bt)
		{
			cache.add(b.speed);
			b.speed = 0;
		}
	}
	public void restoreBulletMove()
	{
		int index=0;
		for (Bullet b:w.bt)
		{
			b.speed = cache.get(index);
			index++;
		}
		cache.clear();
	}
	public void invisibleBullet()
	{
		cache1 = new ArrayList<>();
		for (Bullet b:w.bt)
		{
			cache1.add(b.shouldDraw);
			b.shouldDraw = false;
		}
	}
	public void visibleBullet()
	{
		int index=0;
		for (Bullet b:w.bt)
		{
			b.shouldDraw = cache1.get(index);
			index++;
		}
		cache1.clear();
	}
	public Bitmap print(int sheight)
	{
		int maxw=0,maxh = 0;
		for (Bullet b:w.bt)
		{
			if (b.x > maxw + b.textWidth)maxw = b.x + b.textWidth;
			if (b.y > maxw + b.textSize * 2)maxh = b.y + b.textSize * 2;
		}
		Bitmap bmp1=Bitmap.createBitmap(maxw, (sheight == -1) ?maxh: sheight, Bitmap.Config.ARGB_8888);
		Canvas bmp=new Canvas(bmp1);
		Paint pt=new Paint();
		pt.setAntiAlias(true);
		for (Bullet b:w.bt)
		{
			if (b.x < 0 || !b.shouldDraw)continue;
			b.draw.draw(bmp, pt, b);
		}
		return bmp1;
	}
	public void ingoreRect(Rect r)
	{
		this.irect = r;
	}
	public void sortBullet(){
		w.bt=BulletSort.sort(w.bt,height);
	}
	public BulletScreen(Context ctx)
	{
		super(ctx);
		getHolder().addCallback(this);
		w = new BulletWorker();
		w.handleMessage(w.MSG_BEGIN, null);
	}
	public BulletWorker getWorker()
	{
		return w;
	}
	public Bullet getBullet(final int x, final int y)
	{
		for (Bullet b:w.bt)
		{
			int xz=b.x,yz=b.y,xc=b.x + b.textWidth,yc=b.y + b.textSize * 2;
			if (x - b.x < 0 || y - b.y < 0 || x - b.x > b.textWidth || y - b.y > b.textSize * 2)continue;
			for (int py=yz;py < yc;py++)
			{
				for (int px=xz;px < xc;px++)
				{
					if (x == px && y == py)
					{
						return b;
					}
				}
			}
		}
		return null;
	}
	public void seekTo(int xoffset){
		for(Bullet b:w.bt){
			b.x+=xoffset*b.speed;
		}
	}
	public void restoreToFirst(){
		seekTo((int)w.getWalkCount());
	}
	public long getWalkCount(){
		return w.walkCount;
	}
	public float[] getViewMatrix(){
		return ratio;
	}
	public void addBullet(Bullet b){
		w.handleMessage(BulletScreen.BulletWorker.MSG_UPDATE,b);
	}
	public void addBulletQueue(Bullet b){
		w.handleMessage(w.MSG_JOIN_QUEUE,b);
	}
	public static class BulletWorker
	{
		public static int MSG_UPDATE=0x54,
		MSG_BEGIN=0X66,
		MSG_DESTROY=0X55,
		MSG_DRAW=0X11,
		MSG_WALK=0X24,
		MSG_DELETE=0,
		MSG_JOIN_QUEUE=2;
		private ArrayList<Bullet> bt;
		private boolean waiting;
		private ArrayList<Bullet> queue;
		private long walkCount;
		public long getWalkCount()
		{
			return walkCount;
		}
		public boolean handleMessage(final int msg, final Object obj)
		{
			if (msg == MSG_BEGIN)
			{
				bt = new ArrayList<>();
			}
			else if (msg == MSG_UPDATE)
			{
				if (waiting)return false;
				bt.add((Bullet)obj);
			}
			else if (msg == MSG_DESTROY)
			{
				if (waiting)
				{return false;}
				bt.clear();
			}
			else if (msg == MSG_DRAW)
			{
				Canvas bmp=(Canvas)obj;
				Paint pt=new Paint();
				pt.setAntiAlias(true);
				//bmp.drawColor(Color.argb(0,0,0,0));
				waiting = true;
				for (Bullet b:bt)
				{
					if (b.x < -b.textWidth || b.x > width || !b.shouldDraw)continue;
					if (irect != null)
					{
						bmp.clipRect(irect, android.graphics.Region.Op.DIFFERENCE);
					}
					b.draw.draw(bmp, pt, b);
					if (b.hasSurvivalTime)
					{
						if (b.startTime == 0)
						{
							b.startTime = System.currentTimeMillis();
						}
						if (System.currentTimeMillis() - b.startTime >= b.survivalTime)
						{
							b.message = "";
							b.shouldDraw = false;
						}
					}
				}
				waiting = false;
				if (queue != null)
				{
					bt.addAll(queue);
					queue = null;
				}
			}
			else if (msg == MSG_WALK)
			{
				walkCount++;
				waiting = true;
				for (Bullet b:bt)
				{
					b.x -= b.speed;
				}
				waiting = false;
				if (queue != null)
				{
					bt.addAll(queue);
					queue = null;
				}
			}
			else if (msg == MSG_DELETE)
			{
				if (waiting)return false;
				if (obj instanceof int)
				{
					bt.remove((int)obj);
				}
				else if (obj instanceof Bullet)
				{
					bt.remove(obj);
				}
			}
			else if (msg == MSG_JOIN_QUEUE)
			{
				queue=new ArrayList<>();
				queue.add((Bullet)obj);
			}
			return true;
		}
	}
	private static class GLWorker implements Handler.Callback
	{
		private static int MSG_SURFACE_CREATE=0,
		MSG_SURFACE_CHANGE=1,
		MSG_SURFACE_DESTROY=2,
		MSG_RUN_HANDLER=4,
		MSG_SWAP_BUFFER=5;
		private EGLDisplay mEglDisplay;
		private EGLContext mEglContext;
		private EGLSurface mEglSurface;

		private int width,height;
		@Override
		public boolean handleMessage(Message msg)
		{
			if (msg.what == MSG_SURFACE_CREATE)
			{
				SurfaceHolder h=(SurfaceHolder)msg.obj;
				mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
				int[] version = new int[2];

				EGL14.eglInitialize(mEglDisplay, version, 0, version, 1);
				int confAttr[] = {
					EGL14.EGL_DEPTH_SIZE,16,
					EGL14.EGL_RED_SIZE, 8,
					EGL14.EGL_GREEN_SIZE, 8,
					EGL14.EGL_BLUE_SIZE, 8,
					EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
					EGL14.EGL_NONE
				};
				EGLConfig[] configs = new EGLConfig[1];
				int[] numConfigs = new int[1];
				EGL14.eglChooseConfig(mEglDisplay, confAttr, 0, configs, 0, 1, numConfigs, 0);
				int ctxAttr[] = {
					EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
					EGL14.EGL_NONE
				};
				mEglContext = EGL14.eglCreateContext(mEglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0);
				int[] surfaceAttr = {
					EGL14.EGL_NONE
				};
				mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, configs[0], h, surfaceAttr, 0);
				EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
			}
			else if (msg.what == MSG_SURFACE_DESTROY)
			{
				EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
									 EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
				EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
				EGL14.eglDestroyContext(mEglDisplay, mEglContext);
				EGL14.eglTerminate(mEglDisplay);
			}
			else if (msg.what == MSG_SURFACE_CHANGE)
			{
				width = msg.arg1;
				height = msg.arg2;
			}
			else if (msg.what == MSG_RUN_HANDLER)
			{
				HandlingRunnable r=(HandlingRunnable)msg.obj;
				r.run(width, height);
			}
			else if (msg.what == MSG_SWAP_BUFFER)
			{
				EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
			}
			return true;
		}
		public interface HandlingRunnable
		{
			public void run(int w, int h);
		}
	}
	public class Renderer implements GLWorker.HandlingRunnable
	{

		private int uTextureSamplerLocation;

		private int aTextureCoordLocation;

		private int aPositionLocation;

		private int uTextureMatrixLocation;

		private int bulletLocation;

		private int cameraLocation;

		private int aspectLocation;
		@Override
		public void run(int w1, int h)
		{
			if(framebufferId!=0&&texImageId!=0){
				GLES20.glViewport(0, 0, w1, h);
				texImage.updateTexImage();
				GLES20.glDisable(GLES20.GL_DITHER);
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebufferId);
				BulletScreen.this.c.onDrawFrameBuffer();
				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texImageId);
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
				Bitmap c=Bitmap.createBitmap(w1, h, Bitmap.Config.ARGB_8888);
				Canvas cv=new Canvas(c);
				w.handleMessage(w.MSG_DRAW, cv);
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, c, 0);
				w.handleMessage(w.MSG_WALK, null);
				aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition");
				aTextureCoordLocation = GLES20.glGetAttribLocation(programId, "aTextureCoordinate");
				uTextureMatrixLocation = GLES20.glGetUniformLocation(programId, "videoMatrix");
				uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "uTextureSampler");
				bulletLocation = GLES20.glGetUniformLocation(programId, "bullet");
				cameraLocation = GLES20.glGetUniformLocation(programId, "videoCamera");
				aspectLocation = GLES20.glGetUniformLocation(programId, "aspect");
				float[] mat=new float[16];
				texImage.getTransformMatrix(mat);
				GLES20.glUniform1i(uTextureSamplerLocation, 0);
				GLES20.glUniform1i(bulletLocation, 1);
				GLES20.glUniform2f(aspectLocation, aspectW, aspectH);
				GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, mat, 0);
				GLES20.glUniformMatrix4fv(cameraLocation, 1, false, ratio, 0);
				if (vertex != null)
				{
					vertex.position(0);
					GLES20.glEnableVertexAttribArray(aPositionLocation);
					GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 16, vertex);
					vertex.position(2);
					GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
					GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 16, vertex);
				}
				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
				BulletScreen.this.c.onDraw();
				return;
			}
			GLES20.glViewport(0, 0, w1, h);
			texImage.updateTexImage();
			GLES20.glDisable(GLES20.GL_DITHER);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId1);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
			Bitmap c=Bitmap.createBitmap(w1, h, Bitmap.Config.ARGB_8888);
			Canvas cv=new Canvas(c);
			w.handleMessage(w.MSG_DRAW, cv);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, c, 0);
			w.handleMessage(w.MSG_WALK, null);
			aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition");
			aTextureCoordLocation = GLES20.glGetAttribLocation(programId, "aTextureCoordinate");
			uTextureMatrixLocation = GLES20.glGetUniformLocation(programId, "videoMatrix");
			uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "uTextureSampler");
			bulletLocation = GLES20.glGetUniformLocation(programId, "bullet");
			cameraLocation = GLES20.glGetUniformLocation(programId, "videoCamera");
			aspectLocation = GLES20.glGetUniformLocation(programId, "aspect");
			float[] mat=new float[16];
			texImage.getTransformMatrix(mat);
			GLES20.glUniform1i(uTextureSamplerLocation, 0);
			GLES20.glUniform1i(bulletLocation, 1);
			GLES20.glUniform2f(aspectLocation, aspectW, aspectH);
			GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, mat, 0);
			GLES20.glUniformMatrix4fv(cameraLocation, 1, false, ratio, 0);
			if (vertex != null)
			{
				vertex.position(0);
				GLES20.glEnableVertexAttribArray(aPositionLocation);
				GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 16, vertex);
				vertex.position(2);
				GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
				GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 16, vertex);
			}
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
			BulletScreen.this.c.onDraw();
		}
	}
	public static class Bullet implements Serializable
	{
		public int x,y,textSize;
		public String message;
		public int color=Color.WHITE;
		public int textWidth;
		public int speed=3;
		public long survivalTime=0;
		private long startTime;
		public Bundle data;
		public Draw draw=new Draw(){
			@Override
			public void draw(Canvas cv, Paint pt, BulletScreen.Bullet e)
			{
				defaultDraw(cv, pt, e);
			}
		};
		public boolean shouldDraw,hasSurvivalTime;
		public Bullet(boolean data)
		{
			if (data)
			{
				this.data = new Bundle();
			}
			shouldDraw = true;
			hasSurvivalTime = false;
		}
		public Bullet()
		{
			shouldDraw = true;
			hasSurvivalTime = false;
		}
		public void computeTextWidth()
		{
			Paint pt=new Paint();
			pt.setTextSize(12 + textSize);
			textWidth = (int)pt.measureText(message);
		}
	}
	public static abstract class Draw
	{
		public abstract void draw(Canvas cv, Paint pt, Bullet e)
		public void defaultDraw(Canvas bmp, Paint pt, Bullet b)
		{
			if(b.textWidth==0)b.computeTextWidth();
			int x=b.x,y=b.y;
			pt.setColor(Color.BLACK);
			pt.setStrokeWidth(6);
			pt.setStyle(Paint.Style.STROKE);
			pt.setFakeBoldText(true);
			bmp.drawText(b.message, x, y, pt);
			pt.setColor(b.color);
			pt.setStrokeWidth(0);
			pt.setFakeBoldText(false);
			pt.setStyle(Paint.Style.FILL);
			pt.setTextSize(b.textSize);
			bmp.drawText(b.message, x, y, pt);
		}
	}
	public static class Callback
	{
		public void onCreate(){}
		public void onChange(){}
		public void onDestroy(){}
		public void onDraw(){}
		public void onDrawFrameBuffer(){}
	}
	public static class BulletSort
	{
		private ArrayList<Bullet> b;
		public BulletSort()
		{
			b = new ArrayList<>();
		}
		public void add(Bullet bx)
		{
			this.b.add(bx);
		}
		public ArrayList sort(int dh)
		{
			ArrayList<Bullet> sorted=new ArrayList<>();
			int lx=0,ly=0,maxi = 0;
			for (Bullet n:b)
			{
				Bullet k=n;
				k.x = lx;
				k.y = ly;
				if (maxi < n.textWidth)maxi = n.textWidth;
				sorted.add(k);
				ly += n.textSize * 2;
				if (ly >= dh)
				{
					ly = 0;
					lx += maxi;
				}
			}
			return sorted;
		}
		public static ArrayList<Bullet> sort(ArrayList<Bullet> b,int dh)
		{
			ArrayList<Bullet> sorted=new ArrayList<>();
			int lx=0,ly=0,maxi = 0;
			for (Bullet n:b)
			{
				Bullet k=n;
				k.x = lx;
				k.y = ly;
				if (maxi < n.textWidth)maxi = n.textWidth;
				sorted.add(k);
				ly += n.textSize * 2;
				if (ly >= dh)
				{
					ly = 0;
					lx += maxi;
				}
			}
			return sorted;
		}
	}
}
