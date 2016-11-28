package cc.snser.launcher.model.font;

import java.io.File;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Utils;
import cc.snser.launcher.ui.utils.PrefConstants;
import cc.snser.launcher.ui.utils.PrefUtils;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.shouxinzm.launcher.util.DeviceUtils;

/**
 * typeface wrapper for setted to textview, can get all typeface in mobile
 * @author yangkai
 *
 */
public abstract class TextTypeface {

    public static final int TYPEFACE_TYPE_DEFAULT = 0;

    public static final int TYPEFACE_TYPE_SYSTEM = 1;

    public static final int TYPEFACE_TYPE_APP = 2;

    public static final int TYPEFACE_TYPE_SDCARD = 3;

    protected static final String SYSTEM_STRING = "system";

    protected final Context mContext;
    protected final int mType;
    protected final String mLable;
    protected final String mPath;

    private static BroadcastReceiver mReceiver;

    private static IntentFilter mFilter;


    public TextTypeface(Context context, int type, String lable, String path) {
        mContext = context;
        mType = type;
        mLable = lable;
        mPath = path;
    }

    public int getType() {
        return mType;
    }

    public abstract Typeface getTypeface() throws Exception;

    public abstract String getId();

    public abstract String getName();

    public abstract String getTypeString();

    public boolean isInUsing() {
        if (TextTypeface.getCurrentTypefaceType(mContext) != mType) {
            return false;
        }
        return getId().equals(TextTypeface.getCurrentTypefaceId(mContext));
    }

    public String getDisplayText() {
        return getName() + " [" + getTypeString() + "]";
    }

    public void apply() {
        PrefUtils.setIntPref(mContext, PrefConstants.KEY_TYPEFACE_CURRENT_TYPE, mType);
        PrefUtils.setStringPref(mContext, PrefConstants.KEY_TYPEFACE_CURRENT_ID, getId());
        WorkspaceIconUtils.setAndSaveWorkspaceIconTextTypeface(mContext, this);
    }

    public static TextTypeface getCurrentTypeface(Context context) {
        int type = getCurrentTypefaceType(context);
        String id = getCurrentTypefaceId(context);
        TextTypeface typeface = null;

        switch (type) {
            case TYPEFACE_TYPE_DEFAULT:
                typeface = TextTypefaceDefault.getTextTypefaceById(context, id);
                break;
            case TYPEFACE_TYPE_SYSTEM:
                typeface = TextTypefaceCustomSystem.getTextTypefaceById(context, id);
                break;
            case TYPEFACE_TYPE_APP:
                typeface = TextTypefaceCustomApk.getTextTypefaceById(context, id);
                break;
            case TYPEFACE_TYPE_SDCARD:
                typeface = TextTypefaceCustomSdCard.getTextTypefaceById(context, id);
                break;
            default:
                typeface = TextTypefaceDefault.getTextTypefaceById(context, id);
                break;
        }

        if (!typeface.isValid()) {
            typeface = TextTypefaceDefault.getDefaultTextTypeface(context);
        }

        return typeface;
    }

    public static TextTypefaceDefault getDefaultTextTypeface(Context context) {
        return TextTypefaceDefault.getDefaultTextTypeface(context);
    }

    public static int getCurrentTypefaceType(Context context) {
        return PrefUtils.getIntPref(context, PrefConstants.KEY_TYPEFACE_CURRENT_TYPE, TYPEFACE_TYPE_DEFAULT);
    }

    public static String getCurrentTypefaceId(Context context) {
        return PrefUtils.getStringPref(context, PrefConstants.KEY_TYPEFACE_CURRENT_ID, "");
    }

    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<String, Typeface>();

    private static boolean mLastLoadTypefaceFailed = false;

    private static String sLastLoadFailedPath = null;

    public boolean isValid() {
        return new File(mPath).exists();
    }

    public static Typeface getTypefaceFromCache(final Context context, int type, String path, String packageName) {
        synchronized (typefaceCache) {
            if (!typefaceCache.containsKey(path)) {
                if (mLastLoadTypefaceFailed && sLastLoadFailedPath != null && path.equals(sLastLoadFailedPath)) {
                    return null;
                }

                Typeface t = null;
                switch (type) {
                    case TYPEFACE_TYPE_APP:
                        try {
                            Context mRemoteContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY
                                    | Context.CONTEXT_INCLUDE_CODE);
                            t = Typeface.createFromAsset(mRemoteContext.getAssets(), "fonts/" + path);
                        } catch (Exception e) {
                            mLastLoadTypefaceFailed = true;
                        }
                        break;
                    case TYPEFACE_TYPE_SYSTEM:
                    case TYPEFACE_TYPE_SDCARD:
                    default:
                        try {
                            t = Typeface.createFromFile(path);
                        } catch (Exception e) {
                            mLastLoadTypefaceFailed = true;
                        }
                        break;
                }

                if (t != null) {
                    typefaceCache.put(path, t);
                }
            }

            if (mLastLoadTypefaceFailed) {
                if (mReceiver == null) {
                    registerReceiver(context);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mReceiver != null) {
                                unRegisterReceiver(context);
                            }
                        }
                    }, DeviceUtils.isSonyLT29i() || DeviceUtils.isLenovoA390t() ? 5 * DateUtils.MINUTE_IN_MILLIS : DateUtils.MINUTE_IN_MILLIS); // for bug 249728
                }

                sLastLoadFailedPath = path;
                return null;
            }

            return typefaceCache.get(path);
        }
    }

    private static void registerReceiver(Context context) {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();

                    if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                        Utils.startLauncher(context, Constant.FLAG_RESTART_LAUNCHER_FOR_ICON_TEXT_SIZE);

                        mLastLoadTypefaceFailed = false;
                        sLastLoadFailedPath = null;

                        unRegisterReceiver(context);
                    }
                }
            };
        }

        if (mFilter == null) {
            mFilter = new IntentFilter();
            mFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            mFilter.addDataScheme("file");
        }

        try {
            context.registerReceiver(mReceiver, mFilter);
        } catch (Exception e) {
            // ignore
        }
    }

    private static void unRegisterReceiver(Context context) {
        try {
            context.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // ignore
        } finally {
            mFilter = null;
            mReceiver = null;
        }
    }
}
