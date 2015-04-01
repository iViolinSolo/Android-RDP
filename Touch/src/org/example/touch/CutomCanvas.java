package org.example.touch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class CutomCanvas extends ImageView{
	
	private Context mContext=null;
	private final String TAG = "CutomCanvas";
	
	public CutomCanvas(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public CutomCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public CutomCanvas(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		AppDelegate appDel = ((AppDelegate)mContext.getApplicationContext());
		Bitmap imgBitmap = appDel.imgBitmap;
		canvas.drawBitmap(imgBitmap, 0, 0, null);
		imgBitmap = null;
		Log.d(TAG, "=======Image draw...=======");
		
	}
	
	public void refresh() {
		invalidate();
	}
		
}
