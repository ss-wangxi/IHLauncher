package cc.snser.launcher.ui.dragdrop;

import java.util.HashMap;
import java.util.Vector;

import android.util.FloatMath;
import android.view.MotionEvent;
import cc.snser.launcher.App;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.util.XLog;

public class MultiTouchHandler {

    private static final String TAG = "Launcher.MultiTouchHandler";
    private Launcher mLauncher;
    private DragController mDragController;

    private PointerHandler m2PointerHandler;
    private PointerHandler mMultiTouchMoveHandler;
    private PointerHandler m3PointerHandler;

    private static final int MOVE_UP = 1;
    private static final int MOVE_DOWN = 2;

    public MultiTouchHandler() {
        m2PointerHandler = new Touch2PointerHandler();
        mMultiTouchMoveHandler = new MultiTouchMoveHandler();
        m3PointerHandler = new Touch3PointerHandler();
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }
    void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    void handleTouchEvent(MotionEvent ev) {
        boolean result = mMultiTouchMoveHandler.handleMotionEvent(ev);
        if (!result) {
            result = m2PointerHandler.handleMotionEvent(ev);
        }
        if(!result){
        	result = m3PointerHandler.handleMotionEvent(ev);
        }
    }

    interface PointerHandler {
        public boolean handleMotionEvent(MotionEvent ev);
        public void clearState();
    }

    private float square(float value) {
        return value * value;
    }

    class MultiTouchMoveHandler implements PointerHandler {
        private static final int TOUCH_STATE_IDLE = 0; // 初始化多点触摸状态
        private static final int TOUCH_STATE_CHECKING = 1; // 触摸点的条件已满足按下
        private static final int TOUCH_STATE_WAIT_OTHER_POINT = 2; // 两点按下后，一点抬起

        private static final boolean ENABLE_POINTER_DISTANCE_LIMIT = true;

        private static final int MAX_TOUCH_SUPPORT = 5;
        private static final int MIN_TOUCH_COUNT = 2;

        private float[][] mLastPosition = new float[MAX_TOUCH_SUPPORT][3];
        private float mMovedLength = 0;
        private int mTouchState = TOUCH_STATE_IDLE; // 记录触摸状态
        private boolean mTriggered = false;

        private final int GESTURE_MOVE_DOWN_THRESHOLD = Utilities.dip2px(App.getApp(), 2);//手势失效的距离变化阈值
        private final int DIS_TRIGGER_CHANGE_THRESHOLD = Utilities.dip2px(App.getApp(), 30);//手势移动触发动作所需要的阈值
        private final int GESTURE_INVALID_DISCHANGE_THRESHOLD = Utilities.dip2px(App.getApp(), 10);//手势失效的距离变化阈值

        private int mMoveDirection;

        @Override
        public boolean handleMotionEvent(MotionEvent ev) {
            if (mLauncher == null) {
                return false;
            }

            if (mLauncher.isWorkspaceVisible()) {
                if (mLauncher.getWorkspace().isScrolling()) {
                    return false;
                }
            } else {
                return false;
            }

            int iTouchPointsCount = ev.getPointerCount();
            final int action = ev.getAction();

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    clearState();
                    if (iTouchPointsCount < MIN_TOUCH_COUNT) {
                        // ignore
                        return false;
                    }
                    handleTouchEvent(ev);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (iTouchPointsCount < MIN_TOUCH_COUNT) {
                        // ignore
                        return false;
                    }
                    if (mTriggered) {
                        return true;
                    }

                    mMoveDirection = handleTouchEvent(ev);//方法里会更新trigger的值

                    if (mMoveDirection == MOVE_UP || mMoveDirection == MOVE_DOWN) {
                        mTriggered = true;

                        if (mMoveDirection == MOVE_UP) {
                        }
                    }

                    return mTriggered;
                case MotionEvent.ACTION_UP:// 最后一个点抬起
                    clearState();
                    return false;
                case MotionEvent.ACTION_POINTER_UP:// 一个点抬起
                    return false;
                default:
                    break;
            }
            return false;
        }

        @Override
        public void clearState() {
            for (int i = 0; i < mLastPosition.length; i++) {
                for (int j = 0; j < mLastPosition[0].length; j++) {
                    mLastPosition[i][j] = -1;
                }
            }
            mMovedLength = 0;
            mTouchState = TOUCH_STATE_IDLE;
            mTriggered = false;
        }

        private int handleTouchEvent(MotionEvent ev) {
            int iTouchPointsCount = ev.getPointerCount();
            if (Constant.LOGD_ENABLED) {
                XLog.d(TAG, "touchPointCount = " + iTouchPointsCount);
            }

            iTouchPointsCount = iTouchPointsCount > MAX_TOUCH_SUPPORT ? MAX_TOUCH_SUPPORT : iTouchPointsCount;
            if (mTouchState == TOUCH_STATE_IDLE) {
                resetToInitCheckingState(ev);
            } else if (mTouchState == TOUCH_STATE_CHECKING) {
                int direction = 0;

                for (int i = 0; i < iTouchPointsCount; i++) {
                    if (mLastPosition[i][1] < ev.getY(i) - GESTURE_MOVE_DOWN_THRESHOLD) {
                        if (direction == -1) {
                            resetToInitCheckingState(ev);
                            return 0;
                        }
                        direction = 1;
                    } else if (mLastPosition[i][1] > ev.getY(i) + GESTURE_MOVE_DOWN_THRESHOLD) {
                        if (direction == 1) {
                            resetToInitCheckingState(ev);
                            return 0;
                        }
                        direction = -1;
                    }
                    /*
                  //计算角度是否符合要求；
                    if (mLastPosition[i][1] < ev.getY(i) - GESTURE_MOVE_DOWN_THRESHOLD) {
                        if (Constant.LOGI_ENABLED) {
                            XLog.i(TAG, "finger move down! pointerId " + i + " move from " + mLastPosition[i][1] + " to " + ev.getY());
                        }
                        resetToInitCheckingState(ev);
                       return 0;
                    }*/
                  //计算pointer之间距离的变化是否超出阈值
                    if (iTouchPointsCount > 1 && i > 0) {
                        float distance = FloatMath.sqrt(square(ev.getX(i) - ev.getX(i - 1)) + square(ev.getY(i) - ev.getY(i - 1)));//计算距离变化值
                        if (Math.abs(mLastPosition[i][2]) < 0.0001f) {//这是一个新增的点
                            if (Constant.LOGI_ENABLED) {
                                XLog.i(TAG, "new pointer touched");
                            }
                            mLastPosition[i][2] = distance;
                        } else if (ENABLE_POINTER_DISTANCE_LIMIT && Math.abs(mLastPosition[i][2] - distance) > GESTURE_INVALID_DISCHANGE_THRESHOLD) {//距离变化超过了阈值
                            if (Constant.LOGW_ENABLED) {
                                XLog.w(TAG, "distance change outof threshold");
                            }
                            resetToInitCheckingState(ev);//重新初始化数据
                            return 0;
                        }
                    }
                }
                //计算移动的距离之和，用距离的平均值，或者y变化的平均值
                float deltaY = 0;
                for (int i = 0; i < iTouchPointsCount; i++) {
                    deltaY += ev.getY(i) - mLastPosition[i][1];
                }
                mMovedLength += (deltaY / iTouchPointsCount);
                if (Constant.LOGI_ENABLED) {
                    XLog.d(TAG, "movedLength = " + mMovedLength);
                }
                //更新数据
                for (int i = 0; i < iTouchPointsCount; i++) {
                    mLastPosition[i][0] = ev.getX(i);
                    mLastPosition[i][1] = ev.getY(i);
                }

                //计算是否触发事件
                if (mMovedLength > DIS_TRIGGER_CHANGE_THRESHOLD) {
                    //成功了！
                    if (Constant.LOGI_ENABLED) {
                        XLog.i(TAG, "multi pointer triggerd! moved:" + mMovedLength);
                    }
                    return MOVE_DOWN;
                } else if (mMovedLength < -DIS_TRIGGER_CHANGE_THRESHOLD) {
                    //成功了！
                    if (Constant.LOGI_ENABLED) {
                        XLog.i(TAG, "multi pointer triggerd! moved:" + mMovedLength);
                    }
                    return MOVE_UP;
                }
            }

            return 0;
        }

        private void resetToInitCheckingState(MotionEvent ev) {
            int iTouchPointsCount = ev.getPointerCount();
            for (int i = 0; i < iTouchPointsCount && i < mLastPosition.length; i++) {
                mLastPosition[i][0] = ev.getX(i);
                mLastPosition[i][1] = ev.getY(i);
              //计算距离初始值
                if (i > 0) {
                    mLastPosition[i][2] = (float) Math.sqrt(square(mLastPosition[i][0] - mLastPosition[i - 1][0]) + square(mLastPosition[i][1] - mLastPosition[i - 1][1]));
                }
            }
            mMovedLength = 0;
            mTouchState = TOUCH_STATE_CHECKING;
        }

    }

    class Touch2PointerHandler implements PointerHandler {

     // DragLayer中手指合并手势触发屏幕管理
        private static final float DOUBLE_FINGER_DIS_TRIGGER = 100.0f;
        private static final int TOUCH_STATE_RESET = 0; // 初始化多点触摸状态
        private static final int TOUCH_STATE_BIGEN = 1; // 两个点按下
        private static final int TOUCH_STATE_WAIT_OTHER_POINT = 2; // 两点按下后，一点抬起
        private static final boolean ENABLE_ONE_UP_PROCESS = false;
        private static final int TOUCH_SUPPORT = 2;
        private final int GESTURE_VALID_DISCHANGE_THRESHOLD = Utilities.dip2px(App.getApp(), 10);//手势距离变化阈值

        private float[][] mLastPosition = new float[TOUCH_SUPPORT][3];

        private int mTouchState = TOUCH_STATE_RESET; // 记录触摸状态
        private float mOldDis = 0; // 初始两点距离
        private float mLastUpX = 0; // 最后点的坐标
        private float mLastUpY = 0;

        private boolean mIgnoreTouchHandle = false;

        @Override
        public boolean handleMotionEvent(MotionEvent ev) {
            final int action = ev.getAction() & MotionEvent.ACTION_MASK;

            try {
                if (mLauncher == null || Workspace.sInEditMode || !mLauncher.isWorkspaceVisible() || mLauncher.getWorkspace().isScrolling()) {
                    return false;
                }

                if (mDragController != null && mDragController.isDragging()) {
                    return false;
                }

                int iTouchPointsCount = ev.getPointerCount();

                if (iTouchPointsCount > 2) {
                    mIgnoreTouchHandle = true;
                }

                switch (action) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        clearState();
                        if (iTouchPointsCount < TOUCH_SUPPORT) {
                            // ignore
                            return false;
                        }
                        for (int i = 0; i < iTouchPointsCount && i < mLastPosition.length; i++) {
                            mLastPosition[i][0] = ev.getX(i);
                            mLastPosition[i][1] = ev.getY(i);
                        }
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        if (mIgnoreTouchHandle) {
                            return false;
                        }

                        if (iTouchPointsCount <= 1) {
                            // ignore
                        } else if (iTouchPointsCount == 2 && (mTouchState == TOUCH_STATE_RESET)) {
                            float x = ev.getX(1) - ev.getX(0);
                            float y = ev.getY(1) - ev.getY(0);
                            mOldDis = FloatMath.sqrt(x * x + y * y);
                            mTouchState = TOUCH_STATE_BIGEN;
                        } else if (iTouchPointsCount == 2 && (mTouchState == TOUCH_STATE_BIGEN)) {
                            float x = ev.getX(1) - ev.getX(0);
                            float y = ev.getY(1) - ev.getY(0);
                            final float newDis = FloatMath.sqrt(x * x + y * y);
                            if ((mOldDis - newDis) > DOUBLE_FINGER_DIS_TRIGGER) {
                                if (Constant.LOGD_ENABLED) {
                                    XLog.d(TAG, "showSreenManager start");
                                }
                                for (int i = 0; i < iTouchPointsCount; i++) {
                                    if (Math.sqrt(square(mLastPosition[i][0] - ev.getX(i)) + square(mLastPosition[i][1] - ev.getY(i))) < GESTURE_VALID_DISCHANGE_THRESHOLD) {
                                        mIgnoreTouchHandle = true;
                                    }
                                }
                                //这版先移除双只内滑, 设定首页功能
                                /*if (!mIgnoreTouchHandle) {
                                    showSreenManager();
                                    mIgnoreTouchHandle = true;
                                }*/
                            } else if ((newDis - mOldDis) > DOUBLE_FINGER_DIS_TRIGGER) {
                                if (Constant.LOGD_ENABLED) {
                                    XLog.d(TAG, "showFullWallpaper start");
                                }
                                mIgnoreTouchHandle = true;
                            }
                        }

                        return true;
                    case MotionEvent.ACTION_UP:// 最后一个点抬起
                        if (mIgnoreTouchHandle) {
                            return false;
                        }
                        if (ENABLE_ONE_UP_PROCESS) {
                            if (mTouchState == TOUCH_STATE_WAIT_OTHER_POINT) {
                                float x = mLastUpX - ev.getX(0);
                                float y = mLastUpY - ev.getY(0);
                                final float newDis = FloatMath.sqrt(x * x + y * y);
                                if ((mOldDis - newDis) > DOUBLE_FINGER_DIS_TRIGGER) {
                                    if (Constant.LOGD_ENABLED) {
                                        XLog.d(TAG, "showSreenManager start");
                                    }
                                } else if ((newDis - mOldDis) > DOUBLE_FINGER_DIS_TRIGGER) {
                                    if (Constant.LOGD_ENABLED) {
                                        XLog.d(TAG, "showFullWallpaper start");
                                    }
                                }
                            }
                        } else {
                            clearState();
                        }

                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        return false;
                    case MotionEvent.ACTION_POINTER_UP:// 一个点抬起
                        if (ENABLE_ONE_UP_PROCESS) {
                            if (mTouchState == TOUCH_STATE_BIGEN) {
                                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                                mLastUpX = ev.getX(pointerIndex);
                                mLastUpY = ev.getY(pointerIndex);
                                mTouchState = TOUCH_STATE_WAIT_OTHER_POINT;
                            }
                        }
                        return false;
                    default:
                        break;
                }
                return true;
            } finally {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    clearState();
                }
            }
        }

        @Override
        public void clearState() {
            for (int i = 0; i < mLastPosition.length; i++) {
                for (int j = 0; j < mLastPosition[0].length; j++) {
                    mLastPosition[i][j] = -1;
                }
            }

            mTouchState = TOUCH_STATE_RESET;
            mOldDis = 0;
            mLastUpX = 0;
            mLastUpY = 0;
            mIgnoreTouchHandle = false;
        }

    }
    
    class Touch3PointerHandler implements PointerHandler{
    	private HashMap<Integer, Vector<Point>> mGestureData = new HashMap<Integer, Vector<Point>>();
    	private final int TOUCH_SUPPORT = 3;
    	
		@Override
		public boolean handleMotionEvent(MotionEvent ev) {
			if (mLauncher == null || Workspace.sInEditMode || !mLauncher.isWorkspaceVisible() || mLauncher.getWorkspace().isScrolling()) {
				return false;
			}
			
			final int action = ev.getAction() & MotionEvent.ACTION_MASK;
			final int iTouchPointsCount = ev.getPointerCount();
			
			switch (action) {
			case MotionEvent.ACTION_POINTER_DOWN:
				clearState();
				
				if (iTouchPointsCount != TOUCH_SUPPORT) {
					return false;
				}
				sampleGestureData(iTouchPointsCount,ev);
				return false;
			case MotionEvent.ACTION_MOVE:
				sampleGestureData(iTouchPointsCount, ev);
				if(handleGesture()){
					clearState();
				}
				
				return true;
			case MotionEvent.ACTION_UP:
				handleGesture();
				clearState();
				return false;
			}

			return false;
		}

		@Override
		public void clearState() {
			mGestureData.clear();
		}
		
		private void sampleGestureData(int iTouchPointsCount,MotionEvent event){
			if(iTouchPointsCount != TOUCH_SUPPORT ) return;
			
			for (int i = 0; i < iTouchPointsCount; i++) {
				Vector<Point> points = mGestureData.get(i);
				if(points == null){
					points = new Vector<Point>();
				}
				
				Point point = new Point(event.getX(i), event.getY(i));
				if(!points.contains(point)){
					points.add(point);	
				}
				
				mGestureData.put(i, points);
			}
		}
		
		private int min(int a,int b,int c){
			return (a <= b) ?(a <= c ? a : c) :(b <= c ? b :c);
		}
		
		private boolean handleGesture(){
			if(threeFingerPinch()){
//				ToastUtils.showMessage(mLauncher, "长按拖动屏幕调整屏幕顺序，或点击\"小房子\"设定主页",Toast.LENGTH_LONG);
				return true;
			}
			
			return false;
		}
		
		/**
		 * 三指“撮”手势 threeFingerPinch
		 * 计算三个点连接起来所构成的三角形面积，是否呈缩小趋势
		 * @return
		 */
		private boolean threeFingerPinch(){
			if(mGestureData.isEmpty() || mGestureData.size() != TOUCH_SUPPORT) return false;
			
			//计算面积
			Vector<Float> triArea = new Vector<Float>();
			Vector<Point> pointsA = mGestureData.get(0);
			Vector<Point> pointsB = mGestureData.get(1);
			Vector<Point> pointsC = mGestureData.get(2);
			
			//至少得有3个点吧
			int minsize = min(pointsA.size(), pointsB.size(), pointsC.size());
			if(minsize < 3) return false;
			
			for(int i = 0;i < minsize;i++){
				TriangleVertex triangleVertex = new TriangleVertex(pointsA.get(i), pointsB.get(i), pointsC.get(i));
				triArea.add(triangleVertex.getArea());
				XLog.d("threeFingerPinch", "triArea:"+triangleVertex.getArea());
			}
			 
			int unrecongnized = 0;
			for(int i = 1; i < triArea.size();i++){
				if((triArea.get(i) == TriangleVertex.INVALID_TRIANGEL )|| (triArea.get(i) - triArea.get(i-1) > 0)){
					unrecongnized++;
				}
			}
			
			float percent = 100.0f*(minsize - unrecongnized)/minsize;
			XLog.d("threeFingerPinch", "unrecongnized:"+unrecongnized+" percent:"+percent);
			return percent > 95.f;
		}
		
		class Point{
    		public float mX;
    		public float mY;
    		
    		
    		@Override
			public boolean equals(Object arg0) {
				Point rhs = (Point)arg0;
				return rhs.mX == mX && rhs.mY == mY;
			}


			public Point(float x,float y){
    			mX = x;
    			mY = y;
    		}
    	}
    	
    	class TriangleVertex{
    		public Point aPoint;
    		public Point bPoint;
    		public Point cPoint;
    		public static final int INVALID_TRIANGEL = -1;
    		private float a = -1;
    		private float b = -1;
    		private float c = -1;
    		
    		public TriangleVertex(Point a,Point b,Point c){
    			aPoint = a;
    			bPoint = b;
    			cPoint = c;
    			
    			this.a = caculateSide(a, b);
    			this.b = caculateSide(b, c);
    			this.c = caculateSide(c, a);
    		}
    		
    		public float getArea(){
    			if(!checkValidTriangle()) return INVALID_TRIANGEL;
    			
    			float s = (a + b + c)/2;
    			return (float)Math.sqrt(s * (s - a) * (s - b) * (s - c));
    		}
    		
    		private boolean checkValidTriangle(){
    			return (a + b) >= c && (a - b) <= c;
    		}
    		
    		private float caculateSide(Point a,Point b){
    			return (float)Math.sqrt(square(a.mX - b.mX) + square(a.mY - b.mY));
    		}
    	}
    }
    
}
