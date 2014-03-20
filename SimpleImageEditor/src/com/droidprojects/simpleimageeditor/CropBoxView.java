package com.droidprojects.simpleimageeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom view that handles the dragging of crop box region on the screen.
 * @author Fouad
 */
public class CropBoxView extends View {

	private static final int WIDTH_RECT_STROKE 	= 5;	// width of crop box stroke
	private static final int RADIUS_CIRCLE 		= 100;	// Radius of circular region for handling touch
	private static final int CROP_DIM_MIN 		= 100;	// Minimum dimension of crop region
	private static final int INVALID_POINTER_ID = -1;
	
	// Rect that represents crop region
	private Rect 	mRect;
	// Circles which listen to touch events for resizing mRect
	private RectF 	mCircleTop;		
	private RectF 	mCircleRight;
	private RectF 	mCircleBottom;
	private RectF 	mCircleLeft;
	// Arrow bitmaps
	private Bitmap 	mArrowLeft;
	private Bitmap 	mArrowTop;
	private Bitmap 	mArrowRight;
	private Bitmap 	mArrowBottom;
	
	// Paints for drawing
	private Paint 	mRectPaint;
	private Paint 	mBackgroundPaint;
	private Paint 	mArrowPaint;
	private Paint 	mInactivePaint;
	
	// Touch related information
	private int 	mActivePointerId 	= INVALID_POINTER_ID;
	private int 	mDraggedEdge 		= 0; 
	private Point 	mCropDragPoint 		= null;
	
	/**
	 * The class that implements this listener receives updates when the crop box is moved
	 * @author Fouad
	 */
	public interface CropBoxViewListener{
		public void onCropBoxMoved();
	}
	
	public CropBoxView(Context context) {
		super(context);
		init();
	}

	private void init(){
		// Initialize the shapes and images
		mRect = new Rect();
		mCircleTop = new RectF();
		mCircleRight = new RectF();
		mCircleBottom = new RectF();
		mCircleLeft = new RectF();		
		mArrowLeft = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
		Matrix matrix = new Matrix();
	    matrix.postRotate(90);
	    mArrowTop = Bitmap.createBitmap(mArrowLeft, 0, 0, mArrowLeft.getWidth(), mArrowLeft.getHeight(), matrix, true);
	    matrix.postRotate(90);
	    mArrowRight = Bitmap.createBitmap(mArrowLeft, 0, 0, mArrowLeft.getWidth(), mArrowLeft.getHeight(), matrix, true);
	    matrix.postRotate(90);
	    mArrowBottom = Bitmap.createBitmap(mArrowLeft, 0, 0, mArrowLeft.getWidth(), mArrowLeft.getHeight(), matrix, true);
	    
	    // Initialize background paint
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setAlpha(0);

		// Paint that draws crop region
		mRectPaint = new Paint();
		mRectPaint.setColor(Color.YELLOW);
		mRectPaint.setStyle(Paint.Style.STROKE);
		mRectPaint.setStrokeWidth(WIDTH_RECT_STROKE);
		
		// Paint that draws arrows 
		mArrowPaint = new Paint();
		mArrowPaint.setColor(Color.BLUE);
		mArrowPaint.setStyle(Paint.Style.FILL);
		
		// Paint that draws the inactive region outside the crop box
		mInactivePaint = new Paint();
		mInactivePaint.setColor(Color.BLACK);
		mInactivePaint.setAlpha(128);
	}
	
	/**
	 * Calculate the dimensions of crop region and shapes that move with it.
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	private void setupShapes(float left, float top, float right, float bottom){
		
		mRect.set((int)left, (int)top, (int)right, (int)bottom);
		
		float tempLeft = left/2.f+right/2.f-RADIUS_CIRCLE/2.f;
		float tempRight = left/2.f+right/2.f+RADIUS_CIRCLE/2.f;
		float tempTop = top-RADIUS_CIRCLE/2.f;
		float tempBottom = top+RADIUS_CIRCLE/2.f;
		mCircleTop.set(tempLeft, tempTop, tempRight, tempBottom);
		
		tempLeft = right-RADIUS_CIRCLE/2.f;
		tempRight = right+RADIUS_CIRCLE/2.f;
		tempTop = top/2.f+bottom/2.f-RADIUS_CIRCLE/2.f;
		tempBottom = top/2.f+bottom/2.f+RADIUS_CIRCLE/2.f;
		mCircleRight.set(tempLeft, tempTop, tempRight, tempBottom);
		
		tempLeft = left/2.f+right/2.f-RADIUS_CIRCLE/2.f;
		tempRight = left/2.f+right/2.f+RADIUS_CIRCLE/2.f;
		tempTop = bottom-RADIUS_CIRCLE/2.f;
		tempBottom = bottom+RADIUS_CIRCLE/2.f;
		mCircleBottom.set(tempLeft, tempTop, tempRight, tempBottom);
		
		tempLeft = left-RADIUS_CIRCLE/2.f;
		tempRight = left+RADIUS_CIRCLE/2.f;
		tempTop = top/2.f+bottom/2.f-RADIUS_CIRCLE/2.f;
		tempBottom = top/2.f+bottom/2.f+RADIUS_CIRCLE/2.f;
		mCircleLeft.set(tempLeft, tempTop, tempRight, tempBottom);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// Get width and height spec
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		
		// Adjust crop box dims according to the minimum crop dim.
		int left, top, right, bottom;
		if (w>2*CROP_DIM_MIN){
			left = CROP_DIM_MIN/2;
			right = w-CROP_DIM_MIN/2;
		}else{
			left = 0;
			right = w;
		}
		if (h>2*CROP_DIM_MIN){
			top = CROP_DIM_MIN/2;
			bottom = h-CROP_DIM_MIN/2;
		}else{
			top = 0;
			bottom = h;
		}
		
		setupShapes(left, top, right, bottom);		
	}

	@Override
	protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPaint(mBackgroundPaint);
        canvas.drawRect(0, 0, getWidth(), mCircleTop.centerY(), mInactivePaint);
        canvas.drawRect(0, mCircleTop.centerY(), mCircleLeft.centerX(), mCircleBottom.centerY(), mInactivePaint);
        canvas.drawRect(mCircleRight.centerX(), mCircleTop.centerY(), getWidth(), mCircleBottom.centerY(), mInactivePaint);
        canvas.drawRect(0, mCircleBottom.centerY(), getWidth(), getHeight(), mInactivePaint);
        canvas.drawRect(mRect, mRectPaint);
        canvas.drawBitmap(mArrowLeft, null, mCircleLeft, mArrowPaint);
        canvas.drawBitmap(mArrowTop, null, mCircleTop, mArrowPaint);
        canvas.drawBitmap(mArrowRight, null, mCircleRight, mArrowPaint);
        canvas.drawBitmap(mArrowBottom, null, mCircleBottom, mArrowPaint);
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		// Get action type
		final int action = event.getActionMasked(); 
        
	    switch (action) { 
	    case MotionEvent.ACTION_DOWN: {
	        // Save the ID of this pointer (for dragging)
	        mActivePointerId = event.getPointerId(0);
	        float x = event.getX(0);
	        float y = event.getY(0);
	        
	        // Handle drag events
	        if 		(handleEdgeDragEvents(x, y, MotionEvent.ACTION_DOWN))		{}
	        else if	(handleCropRegionDragEvents(x, y, MotionEvent.ACTION_DOWN))	{}
	        	
	        break;
	    }
	    case MotionEvent.ACTION_MOVE:
	    case MotionEvent.ACTION_UP:   
	    case MotionEvent.ACTION_CANCEL: {
	    	final int pointerIndex = 
	                event.findPointerIndex(mActivePointerId);
	        float x = event.getX(pointerIndex);
	        float y = event.getY(pointerIndex);
	        
	        // Handle drag events and invalidate as needed.
	        if 		(handleEdgeDragEvents(x, y, action))		{
	        	((CropBoxViewListener) getContext()).onCropBoxMoved();
	        	invalidate();
	        }
	        else if	(handleCropRegionDragEvents(x, y, action)) 	{
	        	((CropBoxViewListener) getContext()).onCropBoxMoved();
	        	invalidate();
	        }
	        
	        if (action != MotionEvent.ACTION_MOVE)
	        	mActivePointerId = INVALID_POINTER_ID;
	        
	        break;
	    }
	        
	    case MotionEvent.ACTION_POINTER_UP: {
	            
	        final int pointerIndex = event.getActionIndex(); 
	        final int pointerId = event.getPointerId(pointerIndex); 

	        if (pointerId == mActivePointerId) {
	            // This was our active pointer going up. Choose a new
	            // active pointer and adjust accordingly.
	            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
	            mActivePointerId = event.getPointerId(newPointerIndex);
	        }
	        break;
	    }
	    }       
	    return true;
	}
	
	/**
	 * Handles any events when the edge of crop box is dragged.
	 * @param x
	 * @param y
	 * @param action
	 * @return false if not handled.
	 */
	private boolean handleEdgeDragEvents(float x, float y, int action){
		switch (action){
		case MotionEvent.ACTION_DOWN:
			
			// Save which edge was dragged.
			if (mCircleLeft.contains(x, y))
				mDraggedEdge = 1;
			else if (mCircleTop.contains(x, y))
				mDraggedEdge = 2;
			else if (mCircleRight.contains(x,y))
				mDraggedEdge = 3;
			else if (mCircleBottom.contains(x, y))
				mDraggedEdge = 4;
			else
				return false;
			return true;
		case MotionEvent.ACTION_MOVE:
			if (mDraggedEdge!=0){
				
				float left = mCircleLeft.centerX();
				float top = mCircleTop.centerY();
				float right = mCircleRight.centerX();
				float bottom = mCircleBottom.centerY();
				
				// Limit the crop box dragging so that the lines of the box dont go very close
				switch (mDraggedEdge){
				case 1:
					left = (x > right-CROP_DIM_MIN) ? (right-CROP_DIM_MIN) : x;
					left = Math.max(left, 0);
					break;
				case 2:
					top = (y > bottom-CROP_DIM_MIN) ? (bottom-CROP_DIM_MIN) : y;
					top = Math.max(top, 0);
					break;
				case 3:
					right = (x < left+CROP_DIM_MIN) ? (left+CROP_DIM_MIN) : x;
					right = Math.min(right, getWidth());
					break;
				case 4:
					bottom = (y < top+CROP_DIM_MIN) ? (top+CROP_DIM_MIN) : y;
					bottom = Math.min(bottom, getHeight());
					break;
				}
				
				// Adjust the graphics on the screen accordingly
				setupShapes(left, top, right, bottom);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mDraggedEdge!=0){
				mDraggedEdge=0;
				return true;
			}
			break;
		}
		return false;
	}
	
	/**
	 * Handles any events that drag the crop box as a whole instead of dragging
	 * just one edge.
	 * @param x
	 * @param y
	 * @param action
	 * @return returns false if event not handled.
	 */
	private boolean handleCropRegionDragEvents(float x, float y, int action){
		switch (action){
		case MotionEvent.ACTION_DOWN:
			if (mRect.contains((int)x,(int)y)){
				mCropDragPoint = new Point(x,y);
				return true;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mCropDragPoint!=null){
				
				// Calculate the difference in the touch location and crop box center
				float dx = x-mCropDragPoint.x;
				float dy = y-mCropDragPoint.y;
				
				// Update the saved coords
				mCropDragPoint.x = x;
				mCropDragPoint.y = y;
				
				// limit the dx and dy so that the crop box remains within the bounds of screen
				dx = (mRect.left+dx < 0) ? mRect.left : dx;
				dx = (mRect.right+dx > getWidth()) ? getWidth()-mRect.right : dx;
				dy = (mRect.top+dy < 0) ? mRect.top : dy;
				dy = (mRect.bottom+dy > getHeight()) ? getHeight()-mRect.bottom : dy;
				
				// Apply the results to crop box
				float left = mRect.left+dx;
				float top = mRect.top+dy;
				float right = mRect.right+dx;
				float bottom = mRect.bottom+dy;
				
				// Set up all shapes in screen accordingly
				setupShapes(left, top, right, bottom);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mCropDragPoint!=null){
				mCropDragPoint = null;
				return true;
			}
			break;
		}
		return false;
	}
	
	/**
	 * Returns Crop region
	 * @return
	 */
	public Rect getCropRegion(){
		return new Rect(mRect);
	}
}
