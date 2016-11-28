package mobi.intuitit.android.widget;

import java.util.HashMap;

import cc.snser.launcher.App;
import cc.snser.launcher.Utils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.support.v4.util.AlphaGroup;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.WidgetListAdapter.ViewHolder;

/**
 *
 * @author Bo & Koxx
 *
 */
public abstract class WidgetSpace extends AlphaGroup {

    private static final String TAG = "WidgetSpace";

    private static final boolean LOGD = true;

    private static final boolean CLEAR_DATA_CACHE = true;

    private static final boolean FORCE_FREE_MEMORY = true;

    protected boolean mAllowLongPress;

    protected static final int INVALID_SCREEN = -1;

    protected int mLastScreen;

    protected int mCurrentScreen = 0;

    protected int mNextScreen = INVALID_SCREEN;

    private Runnable mAllowLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            mAllowLongPress = true;
        }
    };

    protected OnScrollListener mListViewOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE) {
                postDelayed(mAllowLongPressRunnable, 500);
            } else {
                removeCallbacks(mAllowLongPressRunnable);
                mAllowLongPress = false;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    public WidgetSpace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WidgetSpace(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetSpace(Context context) {
        super(context);
    }

    public OnScrollListener getListViewOnScrollListener() {
        return mListViewOnScrollListener;
    }

    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
        // do nothing here
    }

    private int getIntExtra(Intent intent, String name, int defaultValue) {
        try {
            return intent.getIntExtra(name, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     */
    BroadcastReceiver mAnimationProvider = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            XLog.i("AnimationProvider", "" + intent);

            int widgetId = getIntExtra(intent, AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (widgetId < 0)
                widgetId = getIntExtra(intent, LauncherIntent.Extra.EXTRA_APPWIDGET_ID, -1);
            if (widgetId < 0) {
                XLog.e(TAG, "Scroll Provider cannot get a legal widget id");
                return;
            }

            AppWidgetHostView widgetView = null;

            try {
                // Find widget view in the current view
                widgetView = findWidget(mCurrentScreen, widgetId);

                // Do corresponding animation on it
                if (LauncherIntent.Action.ACTION_START_FRAME_ANIMATION.equals(action)) {
                    actFrameAnimation(widgetView, getIntExtra(intent,
                            LauncherIntent.Extra.EXTRA_IMAGEVIEW_ID, 0), intent, true);
                } else if (LauncherIntent.Action.ACTION_STOP_FRAME_ANIMATION.equals(action)) {
                    actFrameAnimation(widgetView, getIntExtra(intent,
                            LauncherIntent.Extra.EXTRA_IMAGEVIEW_ID, 0), intent, false);
                } else if (LauncherIntent.Action.ACTION_START_TWEEN_ANIMATION.equals(action)) {
                    startTweenAnimation(widgetView, getIntExtra(intent,
                            LauncherIntent.Extra.EXTRA_VIEW_ID, 0), intent);
                }
            } catch (FrameAnimationException ae) {
                // Reset the action and broadcast
                if (widgetView != null)
                    intent.setComponent(widgetView.getAppWidgetInfo().provider);
                getContext().sendBroadcast(
                        intent.setAction(ae.mAction).putExtra(
                                LauncherIntent.Extra.EXTRA_ERROR_MESSAGE, ae.getMessage()));
            } catch (TweenAnimationException ae) {
                // Reset the action and broadcast
                if (widgetView != null)
                    intent.setComponent(widgetView.getAppWidgetInfo().provider);
                getContext().sendBroadcast(
                        intent.setAction(ae.mAction).putExtra(
                                LauncherIntent.Extra.EXTRA_ERROR_MESSAGE, ae.getMessage()));
            } catch (Exception e) {
                // TODO may be flurry may help me collect this kind of
                // exceptions
                e.printStackTrace();
            }
        }

        /**
         *
         * @param widgetView
         * @param imgViewId
         * @param intent
         * @param start
         *            true to start, false to stop
         * @throws AnimationException
         */
        void actFrameAnimation(AppWidgetHostView widgetView, int imgViewId, Intent intent,
                boolean start) throws FrameAnimationException {

            if (widgetView == null)
                throw new FrameAnimationException("Cannot find queried widget "
                        + getIntExtra(intent, AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                        + " in the current screen.");

            // Start animation
            try {
                ImageView imgView = (ImageView) widgetView.findViewById(imgViewId);
                AnimationDrawable ad = (AnimationDrawable) (imgView).getDrawable();

                if (ad == null)
                    return;

                if (start) {
                    // Start the animation
                    ad.start();
                    getContext()
                            .sendBroadcast(
                                    intent
                                            .setComponent(widgetView.getAppWidgetInfo().provider)
                                            .setAction(
                                                    LauncherIntent.Notification.NOTIFICATION_FRAME_ANIMATION_STARTED));
                } else {
                    // Stop the animation
                    ad.stop();
                    getContext().sendBroadcast(
                                    intent
                                            .setComponent(widgetView.getAppWidgetInfo().provider)
                                            .setAction(
                                                    LauncherIntent.Notification.NOTIFICATION_FRAME_ANIMATION_STOPPED));
                }
            } catch (Exception e) {
                throw new FrameAnimationException(
                        "Fail to start frame animation on queried ImageView: " + imgViewId);
            }

        }

        /**
         *
         * @param widgetView
         * @param viewId
         * @param intent
         * @param start
         *            true to start, false to stop
         * @throws TweenAnimationException
         * @throws AnimationException
         */
        void startTweenAnimation(AppWidgetHostView widgetView, int viewId, Intent intent)
                throws Exception, TweenAnimationException {

            if (widgetView == null)
                throw new NullPointerException("Cannot find queried widget "
                        + getIntExtra(intent, AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                        + " in the current screen.");

            int animId = getIntExtra(intent, LauncherIntent.Extra.EXTRA_ANIMATION_ID, -1);

            try {
                // Create a context for resources loading later
                Context remoteContext = getContext().createPackageContext(
                        widgetView.getAppWidgetInfo().provider.getPackageName(),
                        Context.CONTEXT_IGNORE_SECURITY);

                // Load animation from remote context
                Animation a = AnimationUtils.loadAnimation(remoteContext, animId);
                a.setAnimationListener(new TweenAnimListener(intent.setComponent(widgetView
                        .getAppWidgetInfo().provider)));

                // Generally, I don't recommend this, for you never know if the
                // user is still
                // staying at this screen when the animation start
                long animStartTime = intent.getLongExtra(
                        LauncherIntent.Extra.EXTRA_ANIMATION_STARTTIME, -1);
                if (animStartTime > 0) {
                    a.setStartTime(animStartTime);
                }

                // Check and verify
                if (a.getRepeatCount() == Animation.INFINITE)
                    a.setRepeatCount(0);
                if (a.getRepeatCount() > 10)
                    a.setRepeatCount(10);

                // Start the animation
                widgetView.findViewById(viewId).startAnimation(a);

            } catch (NameNotFoundException e) {
                throw new TweenAnimationException("Cannot load resources");
            } catch (Exception e) {
                e.printStackTrace();
                throw new TweenAnimationException("Cannot start animation: " + animId);
            }
        }

        /**
         *
         * @author bo
         *
         */
        class TweenAnimListener implements AnimationListener {

            Intent mIntent;

            TweenAnimListener(Intent intent) {
                mIntent = intent;
            }

            public void onAnimationEnd(Animation animation) {
            	getContext().sendBroadcast(
                                mIntent
                                        .setAction(LauncherIntent.Notification.NOTIFICATION_TWEEN_ANIMATION_ENDED));
                mIntent = null;
                animation.setAnimationListener(null);
            }

            public void onAnimationRepeat(Animation animation) {
            	getContext().sendBroadcast(
                                mIntent
                                        .setAction(LauncherIntent.Notification.NOTIFICATION_TWEEN_ANIMATION_REPEATED));
            }

            public void onAnimationStart(Animation animation) {
            	getContext().sendBroadcast(
                                mIntent
                                        .setAction(LauncherIntent.Notification.NOTIFICATION_TWEEN_ANIMATION_STARTED));
            }

        }

    };

    /**
     * Look for a widget in all screens
     *
     * @param appWidgetId
     * @return
     */
    final AppWidgetHostView findWidget(int appWidgetId) {
        AppWidgetHostView wv;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            wv = findWidget(i, appWidgetId);
            if (wv != null)
                return wv;
        }
        return null;
    }

    /**
     * Find widget in a given screen
     *
     * @param screen
     * @param appWidgetId
     * @return
     */
    final AppWidgetHostView findWidget(int screen, int appWidgetId) {
        if (appWidgetId < 0)
            return null;

        View child = getChildAt(screen);
        if (child == null)
            return null;

        if (child instanceof AppWidgetHostView) {
            AppWidgetHostView widgetView = (AppWidgetHostView) child;
            if (widgetView.getAppWidgetId() == appWidgetId)
                return widgetView;
        } else if (child instanceof ViewGroup) {
            ViewGroup cells = (ViewGroup) getChildAt(screen);
            for (int i = cells.getChildCount() - 1; i >= 0; i--) {
                View widgetView = cells.getChildAt(i);
                if (widgetView instanceof AppWidgetHostView) {
                    if (((AppWidgetHostView) widgetView).getAppWidgetId() == appWidgetId)
                        return (AppWidgetHostView) widgetView;
                }
            }
        }

        return null;
    }

    ScrollViewProvider mScrollViewProvider = new ScrollViewProvider();

    // listview informations storage for each provider data Uri
    class ScrollViewInfos {
        AbsListView lv = null;
        int widgetId = -1;
        ContentObserver obs;
        Handler obsHandler;
        ScrollableBaseAdapter lvAdapter;
        CharSequence key;
    }

    static HashMap<String, ScrollViewInfos> mScrollViewCursorInfos = new HashMap<String, ScrollViewInfos>();

    // Test if this widget is scrollable
    public synchronized boolean isWidgetScrollable(int widgetId) {
        for (ScrollViewInfos item : mScrollViewCursorInfos.values()) {
            if (item.widgetId == widgetId)
                return true;
        }
        return false;
    }
    public synchronized boolean unbindWidgetScrollableId(int widgetId) {
        XLog.d(TAG, "trying to completely unallocate widget ID="+widgetId);
        CharSequence keyToDelete=null;
        for (ScrollViewInfos item : mScrollViewCursorInfos.values()) {
            XLog.d(TAG, "Comparing widget ID="+item.widgetId);
            if (item.widgetId == widgetId){
                XLog.d(TAG, "trying to completely unallocate widget stuff...");
                AppWidgetHostView widgetView = findWidget(widgetId);
                XLog.d(TAG, "Widget view to KILL:"+widgetView);
                Context remoteContext=null;
                if (widgetView != null) {
                    AppWidgetProviderInfo info = widgetView.getAppWidgetInfo();
                    if (info != null && info.provider != null) {
                        final String packageName = info.provider.getPackageName();
                        try {
                            remoteContext = getContext().createPackageContext(
                                    packageName,
                                    Context.CONTEXT_IGNORE_SECURITY);
                        } catch (NameNotFoundException e) {
                            XLog.e(TAG, "couldn't find widget id:"+widgetId);
                        }
                    } else
                        remoteContext = null;
                }
                if(remoteContext==null)
                    remoteContext=getContext();
                if(item.lv!=null){
                    XLog.d(TAG, "Trying to KILL the ListView...");
                    if(item.lvAdapter!=null){
                        item.lvAdapter.dropCache(remoteContext);
                    }
                    Utils.setAdapter(item.lv, null);
                    item.lv = null;
                }
                remoteContext.getContentResolver().unregisterContentObserver(item.obs);
                item.obsHandler = null;
                item.obs = null;
                keyToDelete=item.key;
                item=null;
                ListViewImageManager.getInstance().clearCacheForWidget(getContext(), widgetId);
            }
        }
        if(keyToDelete!=null)mScrollViewCursorInfos.remove(keyToDelete);
        ListViewImageManager.getInstance().unbindDrawables();

        if (FORCE_FREE_MEMORY) {
            System.gc();
        }
        XLog.d(TAG, "AFTER REMOVING, Our Scrollable widgets array contains:"+mScrollViewCursorInfos.size());
        return false;
    }

    class ScrollViewProvider extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            XLog.i("WidgetSpace - onReceive: ", "" + intent);

            // Try to get the widget view
            int widgetId = getIntExtra(intent, AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (widgetId < 0)
                widgetId = getIntExtra(intent, LauncherIntent.Extra.EXTRA_APPWIDGET_ID, -1);
            if (widgetId < 0) {
                XLog.e(TAG, "Scroll Provider cannot get a legal widget id");
                return;
            }

            AppWidgetHostView widgetView = findWidget(widgetId);

            if (widgetView == null) {
            	context.sendBroadcast(
                        intent.setAction(LauncherIntent.Error.ERROR_SCROLL_CURSOR).putExtra(
                                LauncherIntent.Extra.EXTRA_ERROR_MESSAGE,
                                "Cannot find app widget with id: " + widgetId));
                return;
            }

            final ComponentName appWidgetProvider = widgetView.getAppWidgetInfo().provider;

            String error = "unknow action";
            if (TextUtils.equals(action, LauncherIntent.Action.ACTION_SCROLL_WIDGET_START)) {
                // Drop the old stuff first!
                error = releaseScrollable(context, intent, widgetView);
                if (error == null)
                    error = makeScrollable(context, intent, widgetView);

            } else if (TextUtils.equals(action,
                    LauncherIntent.Action.ACTION_SCROLL_WIDGET_SELECT_ITEM)) {
                error = setSelection(context, intent, widgetView);
            } else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_SCROLL_WIDGET_CLOSE)) {
                error = releaseScrollable(context, intent, widgetView);
            } else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_SCROLL_WIDGET_CLEAR_IMAGE_CACHE)) {
                error = ListViewImageManager.getInstance().clearCacheForWidget(context, widgetId);
            }
            if (error == null) {
                // send finish signal
                intent.setComponent(appWidgetProvider);
                context.sendBroadcast(intent.setAction(LauncherIntent.Action.ACTION_FINISH));
            } else {
                // send error message
                intent.setComponent(appWidgetProvider);
                context.sendBroadcast(
                        intent.setAction(LauncherIntent.Error.ERROR_SCROLL_CURSOR).putExtra(
                                LauncherIntent.Extra.EXTRA_ERROR_MESSAGE, error));
            }
        }

        private synchronized String makeScrollable(Context context, Intent intent,
                AppWidgetHostView widgetView) {
            // get the dummy view to replace
            final int dummyViewId = getIntExtra(intent, LauncherIntent.Extra.EXTRA_VIEW_ID, -1);
            if (dummyViewId <= 0)
                return "Dummy view id needed.";

            final ComponentName appWidgetProvider = widgetView.getAppWidgetInfo().provider;
            final int appWidgetId = widgetView.getAppWidgetId();

            try {
                // Create a context for loading resources
                Context remoteContext = getContext().createPackageContext(
                        widgetView.getAppWidgetInfo().provider.getPackageName(),
                        Context.CONTEXT_IGNORE_SECURITY);

                AbsListView lv = null;

                View dummyView = widgetView.findViewById(dummyViewId);
                if (dummyView == null)
                    return "Dummy view needed.";

                if (dummyView instanceof AbsListView)
                    lv = (AbsListView) dummyView;
                else {
                    dummyView = null;
                    if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS)) {
                        SimpleRemoteViews rvs = (SimpleRemoteViews)intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS);
                        dummyView = rvs.apply(remoteContext, null);
                        if (dummyView instanceof AbsListView) {
                            lv = (AbsListView) dummyView;
                            if (!replaceView(widgetView, dummyViewId, lv))
                                return "Cannot replace the dummy with the list view inflated from the passed RemoteViews.";
                        } else
                            return "could not create AbsListView from the passed RemoteViews";
                    } else {
                        // inflate listview
                        final int listViewResId = getIntExtra(intent,
                                LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, -1);
                        if (listViewResId <= 0) {
                            // try to post the newly created listview to the widget
                            lv = postListView(widgetView, dummyViewId);
                            if (lv == null)
                                return "Cannot create the default list view.";
                        } else {
                            // Inflate it
                            LayoutInflater inflater = LayoutInflater.from(remoteContext);
                            dummyView = inflater.inflate(listViewResId, null);
                            if (dummyView instanceof AbsListView) {
                                lv = (AbsListView) dummyView;
                                if (!replaceView(widgetView, dummyViewId, lv))
                                    return "Cannot replace the dummy with the list view inflated from the passed layout resource id.";
                            } else
                                return "Cannot inflate a list view from the passed layout resource id.";
                        }
                    }
                }
                String cursorDataUriString = intent
                        .getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI);
                ScrollViewInfos listViewInfos = mScrollViewCursorInfos.get(cursorDataUriString);
                final boolean newListView = listViewInfos == null;
                // new widget, initialize observer
                if (newListView) {

                    listViewInfos = new ScrollViewInfos();

                    final ScrollableBaseAdapter lvAdapter;
                    if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS))
                        lvAdapter = new WidgetRemoteViewsListAdapter(remoteContext, intent,
                            appWidgetProvider, appWidgetId, dummyViewId);
                    else
                        lvAdapter = new WidgetListAdapter(remoteContext,
                            intent, appWidgetProvider, appWidgetId, dummyViewId);
                    // create listener for content Provider data modification
                    WidgetDataChangeListener widgetDataChangeListener = new WidgetDataChangeListener() {
                        @Override
                        public void onChange() {
                             lvAdapter.notifyToRegenerate();
                        }
                    };

                    // register contentProvider observer
                    listViewInfos.obs = new WidgetContentObserver(listViewInfos.obsHandler,
                            widgetDataChangeListener);
                    Uri uriToObserver = Uri.parse(intent
                            .getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI));
                    context.getContentResolver().registerContentObserver(uriToObserver, true,
                            listViewInfos.obs);

                    // store new adapter
                    listViewInfos.lvAdapter = lvAdapter;

                    if (LOGD)
                        XLog.d(TAG, "makeScrollable : recreate listview adapter");
                } else {
                    if (LOGD)
                        XLog.d(TAG, "makeScrollable : restore listview adapter");
                    if (listViewInfos.lvAdapter instanceof WidgetRemoteViewsListAdapter)
                        ((WidgetRemoteViewsListAdapter)listViewInfos.lvAdapter).updateFromIntent(intent);
                }

                Utils.setAdapter(lv, listViewInfos.lvAdapter);

                // finish listview configuration
                if ((listViewInfos.lvAdapter instanceof WidgetListAdapter) &&
                    !((WidgetListAdapter)listViewInfos.lvAdapter).mItemChildrenClickable)
                    lv.setOnItemClickListener(new WidgetItemListener(appWidgetProvider,
                            appWidgetId, dummyViewId));
                lv.setFocusableInTouchMode(false);
                lv.setOnScrollListener(mListViewOnScrollListener);

                // store informations in static memory
                listViewInfos.widgetId = appWidgetId;
                listViewInfos.lv = lv;
                listViewInfos.key=cursorDataUriString;
                mScrollViewCursorInfos.put(cursorDataUriString, listViewInfos);

                // force listview position if asked
                int position = getIntExtra(intent,
                        LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_POSITION, -1);
                if (position >= 0)
                    lv.setSelection(position);

                if (!newListView && CLEAR_DATA_CACHE &&
                        (listViewInfos.lvAdapter instanceof WidgetListAdapter)) {
                    listViewInfos.lvAdapter.notifyToRegenerate();
                }

                if (FORCE_FREE_MEMORY) {
                    System.gc();
                }
                XLog.d(TAG, "AFTER ADDING, Our Scrollable widgets array contains:"+mScrollViewCursorInfos.size());
                return null;
            } catch (Throwable e) {
                return e.getMessage();
            }
        }

        private String setSelection(Context context, Intent intent, AppWidgetHostView widgetView) {

            try {
                // select item at position
                String cursorDataUriString = intent
                        .getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI);
                int position = getIntExtra(intent,
                        LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_POSITION, 0);
                ScrollViewInfos cursorInfos = mScrollViewCursorInfos.get(cursorDataUriString);
                if (cursorInfos != null) {
                    cursorInfos.lv.setSelection(position);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        private synchronized String releaseScrollable(Context context, Intent intent,
                AppWidgetHostView widgetView) {

            try {

                // destroy listview
                String cursorDataUriString = intent
                        .getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI);
                ScrollViewInfos listViewInfos = mScrollViewCursorInfos.get(cursorDataUriString);
                if (listViewInfos != null) {
                    if (listViewInfos.lv != null)
                        Utils.setAdapter(listViewInfos.lv, null);
                    listViewInfos.lv = null;
                    context.getContentResolver().unregisterContentObserver(listViewInfos.obs);
                    listViewInfos.obsHandler = null;
                    listViewInfos.obs = null;
                    if (listViewInfos.lvAdapter != null)
                        listViewInfos.lvAdapter.dropCache(context);
                    mScrollViewCursorInfos.remove(cursorDataUriString);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }



        class WidgetItemListener implements OnItemClickListener {

            ComponentName mAppWidgetProvider;
            int mAppWidgetId;
            int mListViewId;

            WidgetItemListener(ComponentName cname, int id, int viewId) {
                mAppWidgetProvider = cname;
                mAppWidgetId = id;
                mListViewId = viewId;
            }

            public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
                try {

                    ViewHolder holder = (ViewHolder) view.getTag();
                    Object tag = holder.lvClickItemTag;

                    if (tag != null && tag instanceof String) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) tag));
                        getContext().startActivity(intent);
                    } else {
                        Intent intent = new Intent(LauncherIntent.Action.ACTION_ITEM_CLICK);
                        intent.setComponent(mAppWidgetProvider);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                                .putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_ID, mListViewId);
                        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, pos);

                        Rect srcRect = new Rect();
                        final int[] location = new int[2];
                        view.getLocationOnScreen(location);
                        srcRect.left = location[0];
                        srcRect.top = location[1];
                        srcRect.right = srcRect.left + view.getWidth();
                        srcRect.bottom = srcRect.top + view.getHeight();
                        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS, srcRect);

                        getContext().sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         *
         * @param widgetView
         * @param dummyViewId
         * @return whether the dummy view is replaced by listview
         */
        ListView postListView(AppWidgetHostView widgetView, int dummyViewId) {
            ListView lv = new ListView(getContext());
            lv.setCacheColorHint(0);

            if (replaceView(widgetView, dummyViewId, lv))
                return lv;
            else
                return null;
        }

        /**
         *
         * @param vg
         * @param id
         * @param replacement
         * @return
         */
        boolean replaceView(ViewGroup vg, int id, View replacement) {
            View child;
            boolean result = false;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                child = vg.getChildAt(i);
                if (child.getId() == id) {
                    // Remove the dummy
                    vg.removeView(child);
                    // Set the replacement id to be the old one
                    replacement.setId(id);
                    // Put the replacement in
                    vg.addView(replacement, i, child.getLayoutParams());
                    return true;
                } else if (child instanceof ViewGroup)
                    result |= replaceView((ViewGroup) child, id, replacement);
            }
            return result;
        }
    }

    /**
     * Register receivers given by this workspace
     */
    public void registerProvider() {
        XLog.i(TAG, "registerProvider");
        final Context context = getContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(LauncherIntent.Action.ACTION_START_FRAME_ANIMATION);
        filter.addAction(LauncherIntent.Action.ACTION_STOP_FRAME_ANIMATION);
        filter.addAction(LauncherIntent.Action.ACTION_START_TWEEN_ANIMATION);
        App.getApp().registerLocalReceiver(mAnimationProvider, filter);

        IntentFilter scrollFilter = new IntentFilter();
        scrollFilter.addAction(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
        scrollFilter.addAction(LauncherIntent.Action.ACTION_SCROLL_WIDGET_CLOSE);
        scrollFilter.addAction(LauncherIntent.Action.ACTION_SCROLL_WIDGET_CLEAR_IMAGE_CACHE);
        scrollFilter.addAction(LauncherIntent.Action.ACTION_SCROLL_WIDGET_SELECT_ITEM);
        App.getApp().registerLocalReceiver(mScrollViewProvider, scrollFilter);
    }

    /**
     * Unregister receivers given by this workspace
     */
    public void unregisterProvider() {
        final Context context = getContext();
        unregisterReceiver(context, mAnimationProvider);
        unregisterReceiver(context, mScrollViewProvider);
    }

    /**
     * So en exception in unregistering last receiver will not bypass the second one
     *
     * @param context
     * @param receiver
     */
    void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        try {
            App.getApp().unRegisterLocalReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @author bo
     *
     */
    class AnimationException extends Exception {

        private static final long serialVersionUID = 5532638962504199766L;

        public String mAction;

        AnimationException(String action, String msg) {
            super(msg);
            mAction = action;

        }
    }

    class FrameAnimationException extends AnimationException {
        /**
         *
         */
        private static final long serialVersionUID = -3873320976176961709L;

        public FrameAnimationException(String msg) {
            super(LauncherIntent.Error.ERROR_FRAME_ANIMATION, msg);
        }

    }

    class TweenAnimationException extends AnimationException {

        /**
         *
         */
        private static final long serialVersionUID = 1459429645365799185L;

        public TweenAnimationException(String msg) {
            super(LauncherIntent.Error.ERROR_TWEEN_ANIMATION, msg);
        }

    }

}
