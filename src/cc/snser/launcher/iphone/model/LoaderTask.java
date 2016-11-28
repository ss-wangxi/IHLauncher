package cc.snser.launcher.iphone.model;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import cc.snser.launcher.DeferredHandler;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.IconFsCache;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.iphone.model.LauncherModelIphone.AllAppsList;
import cc.snser.launcher.iphone.model.LauncherModelIphone.AllShortcutsList;
import cc.snser.launcher.model.LauncherModel;
import cc.snser.launcher.model.LoaderTaskBase;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.style.SettingPreferences;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import java.util.ArrayList;
import java.util.List;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;
import static cc.snser.launcher.iphone.model.LauncherModelIphone.TAG;

/**
 * Runnable for the thread that loads the contents of the launcher:
 *   - workspace icons
 *   - widgets
 *   - all apps icons
 * <p><b>注：该类是工作在Launcher-Daemon线程中的代码</b></p>
 */
public class LoaderTask extends LoaderTaskBase {

    protected static final int ITEMS_CHUNK = 1/*6*/; // batch size for the workspace icons

    protected static final boolean DEBUG_LOADERS = LOGD_ENABLED;

    protected LauncherModelIphone mLauncherModel;

    protected Object mLock;
    protected DeferredHandler mDeferredHandler;
    protected IconCache mIconCache;
    protected IconFsCache mIconFsCache;
    protected int mBatchSize; // 0 is all apps at once

    protected AllAppsList mAllAppsList;
    protected AllShortcutsList mAllShortcutsList;

    /**need reload after onresume in launcher*/
    protected boolean mLoadAndBindStepFinished;
    protected int mThreadId = -1;
    private DatabaseBinder mDatabaseBinder = null;

    LoaderTask(Context context, LauncherModelIphone launcherModel, boolean isLaunching, Handler onFinishhandler, boolean flushAllApps) {
        super(launcherModel, context, isLaunching, onFinishhandler, flushAllApps);

        mLauncherModel = launcherModel;

        mLock = launcherModel.mLock;
        mDeferredHandler = launcherModel.getHandler();
        mIconCache = IconCache.getInstance(context);
        mIconFsCache = IconFsCache.getInstance(context);
        mBatchSize = context.getResources().getInteger(R.integer.config_allAppsBatchSize);

        mAllAppsList = launcherModel.mAllAppsList;
        mAllShortcutsList = launcherModel.mAllShortcutsList;
    }

    @Override
    public void run() {
        if (mFlushAllApps) {
            mLauncherModel.mAllAppsLoaded = false;
            mIconCache.flush();
        }
        
        mThreadId = android.os.Process.myTid();
        // Optimize for end-user experience: if the Launcher is up and // running with the
        // All Apps interface in the foreground, load All Apps first. Otherwise, load the
        // workspace first (default).
        final Callbacks cbk = mLauncherModel.getCallbacks();
        boolean firstLoad = SettingPreferences.isFirstLoad();
        final boolean reloadWorkspaceLayout = cbk != null ? (cbk.isFirst() || firstLoad) : true;

        /*if (reloadWorkspaceLayout) {
            mLauncherModel.getPackageCategoryManager().startPreloadLocalCategoryMap(mContext);
        }*///MethodTracing效果明显，实际效果不大

        if (cbk != null) {
            //若桌面正在加载，且桌面启动过程中被pause，则重新加载
            cbk.setIgnoreOnResumeNeedsLoad(!mIsLaunching);
        }
        //mSheduleThread.start();
        keep_running: {
            // Elevate priority when Home launches for the first time to avoid
            // starving at boot time. Staring at a blank home is not cool.
            setThreadPriority(mIsLaunching ? android.os.Process.THREAD_PRIORITY_DEFAULT : android.os.Process.THREAD_PRIORITY_BACKGROUND);

            if (DEBUG_LOADERS) {
                XLog.d(TAG, "step 1: loading workspace");
            }

            if (!mLauncherModel.mAllAppsLoaded) {
                if (cbk == null || !cbk.isFirst()) {
                    loadHiddenApps();
                }
                // 删除所有记录的launcher application数据结构
                //Bug 217076 - 20130308-【行列数】更换行列数后，隐藏列表中的应用图标丢失，无法再找回。mAllAppsLoaded没有设置，导致没有sync而丢失了allApplication中的应用
                mLauncherModel.getDataModel().removeAllApps();
            }

            // 加载绑定桌面
            mDatabaseBinder = new DatabaseBinder(mContext, this, firstLoad);
            ArrayList<HomeDesktopItemInfo> shortcutInfos = mDatabaseBinder.load();
            ArrayList<HomeItemInfo> addToBottomItems = mDatabaseBinder.addToBottomItems;
            
            if (!reloadWorkspaceLayout) {
                // Tell the workspace that we're done.
                sendFinishBindingHome();
                waitForIdle2();
            }
            
            if (mStopped) {
                break keep_running;
            }

            //禁用SyncBinder added by snsermail@gmail.com
            //SyncBinder syncBinder = new SyncBinder(mContext, this, cbk, shortcutInfos, reloadWorkspaceLayout, mDatabaseBinder);
            //syncBinder.sync(!mLauncherModel.mAllAppsLoaded);
            doSomethingAfterSynced(addToBottomItems);

            mLauncherModel.excuteAllPackageUpdate();

            if (reloadWorkspaceLayout) {
                // Tell the workspace that we're done.
                sendFinishBindingHome();
            }

            //禁用SyncBinder added by snsermail@gmail.com
            //syncBinder.updatePendingResolveIconTitleList(mContext, cbk);

            if (mStopped) {
                break keep_running;
            }

            mLauncherModel.mAllAppsLoaded = true;
            
            

            if (mStopped) {
                break keep_running;
            }

            if (mOnFinishHandler != null) {
                postRunnable(getLauncherModel().getCallbacks(), new Runnable() {
                    @Override
                    public void run() {
                        if (mOnFinishHandler != null) {
                            mOnFinishHandler.sendEmptyMessage(LauncherModel.MSG_LOADER_TASK_FINISHED);
                            mOnFinishHandler = null;
                        }
                    }
                });
            }
        }
        
        if(mFlushAllApps){
        	//与flush配套调用
        	mIconCache.recycle();
        }

        if (!mStopped) {
            mIconFsCache.saveIconsToCacheFile(new ArrayList<HomeDesktopItemInfo>(mAllAppsList.data));
        }

        if (!mStopped) {
            mIconFsCache.enableCache();
        }

        
        // Clear out this reference, otherwise we end up holding it until all of the
        // callback runnables are done.
        mContext = null;
        mThreadId = -1;

        synchronized (mLock) {
            // If we are still the last one to be scheduled, remove ourselves.
            if (mLauncherModel.mLoaderTask == LoaderTask.this) {
                mLauncherModel.mLoaderTask = null;
            }
        }

        // Trigger a gc to try to clean up after the stuff is done, since the
        // renderscript allocations aren't charged to the java heap.
        mDeferredHandler.post(new Runnable() {
            @Override
            public void run() {
                System.gc();
            }
        });

    }


    private void doSomethingAfterSynced(final ArrayList<? extends HomeItemInfo> items) {
        if (items != null && items.size() > 0) {
            final Callbacks oldCallbacks = getLauncherModel().getCallbacks();
            postRunnable(getLauncherModel().getCallbacks(), new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindMissedItem(items);
                    }
                }
            });
        }
    }

    private void sendFinishBindingHome() {
     // Tell the workspace that we're done.
        final Callbacks oldCallbacks = mLauncherModel.getCallbacks();
        postRunnable(oldCallbacks, new Runnable() {
            @Override
            public void run() {
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.finishBindingInHome();
                }
            }
        });

    }

    private List<Runnable> mTaskQueue = new ArrayList<Runnable>();
    private Object mSignalObj = new Object();
    /**
     * 投递任务到UI线程
     * @param oldCallbacks
     * @param runnable
     */
    protected void postRunnable(final Callbacks oldCallbacks, final Runnable runnable) {
        if (oldCallbacks != null) {
            mDeferredHandler.post(runnable);
        	/*synchronized (mTaskQueue) {
				mTaskQueue.add(runnable);
			}*/
        }
    }
    
    private Thread mSheduleThread = new Thread(new Runnable(){

		@Override
		public void run() {
			for(;;){
				boolean task = false;
				synchronized (mTaskQueue) {
					task = mTaskQueue.size() > 0;
				}
				
				if(task){
					mHandler.sendEmptyMessage(5510);
		        	synchronized (mSignalObj) {
						try{
							mSignalObj.wait();
						}catch(Exception e){
							
						}
					}
				}
					try{
						Thread.sleep(20);
					}catch(Exception e){
						
					}
			}
			
		}
    	
    });
    
    private Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		switch(msg.what){
    		case 5510:
    			mHandler.removeMessages(5510);
    			Runnable task = null;
    			synchronized (mTaskQueue) {
					if(mTaskQueue.size() > 0){
						task = mTaskQueue.get(0);
						mTaskQueue.remove(0);
					}
				}
    			
    			if(task != null){
    				task.run();
    			}
    			
    			synchronized (mSignalObj) {
					try{
						mSignalObj.notify();
					}catch(Exception e){
						
					}
    			}
    			break;
    		}
    	}
    };

    void yieldLoader() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    LauncherModelIphone getLauncherModel() {
        return mLauncherModel;
    }

    //不使用idleHandler，避免被launcher handler的循环调用message阻塞
    private void waitForIdle2() {
        synchronized (LoaderTask.this) {
            final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
            postRunnable(getLauncherModel().getCallbacks(), new Runnable() {

                @Override
                public void run() {
                    synchronized (LoaderTask.this) {
                        mLoadAndBindStepFinished = true;
                        LoaderTask.this.notify();
                        if (LOGD_ENABLED) {
                            XLog.d(TAG, "notify invoke");
                        }
                    }
                }
            });

            while (!mStopped && !mLoadAndBindStepFinished) {
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "wait ... stoped=" + mStopped + " finished=" + mLoadAndBindStepFinished);
                }
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (LOGD_ENABLED) {
                XLog.d(TAG, "wait done: stoped=" + mStopped + " finished=" + mLoadAndBindStepFinished);
            }

            if (DEBUG_LOADERS) {
                XLog.d(TAG, "waited "
                        + (SystemClock.uptimeMillis() - workspaceWaitTime)
                        + "ms for previous step to finish binding");
            }
        }
    }

    protected void setThreadPriority(int priority) {
        synchronized (mLock) {
            try {
                if (mThreadId >= 0) {
                    android.os.Process.setThreadPriority(mThreadId, priority);
                }
            } catch (Exception e) {
                if (LOGE_ENABLED) {
                    XLog.e(TAG, "Failed to set priority " + priority + " of the loader.", e);
                }
            }
        }
    }

    boolean stopped() {
        return mStopped;
    }

    AllAppsList getAllAppsList() {
        return mAllAppsList;
    }

    AllShortcutsList getAllShortcutsList() {
        return mAllShortcutsList;
    }
}

