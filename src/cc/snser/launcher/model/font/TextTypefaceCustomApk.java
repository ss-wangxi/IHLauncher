
package cc.snser.launcher.model.font;

import cc.snser.launcher.apps.utils.AppUtils;
import android.content.Context;
import android.graphics.Typeface;

public class TextTypefaceCustomApk extends TextTypefaceCustom {

    public TextTypefaceCustomApk(Context context, int type, String packageName, String lable, String path) {
        super(context, type, packageName, lable, path);
    }

    @Override
    public Typeface getTypeface() throws Exception {
        return TextTypeface.getTypefaceFromCache(mContext, TYPEFACE_TYPE_APP, mPath, mPackageName);
    }

    @Override
    public String getId() {
        return mPackageName + "/" + mPath + "/" + mLable;
    }

    @Override
    public String getName() {
        return mPath.substring(mPath.lastIndexOf('/') + 1, mPath.lastIndexOf('.'));
    }

    @Override
    public String getTypeString() {
        return mLable;
    }

    static TextTypefaceCustomApk getTextTypefaceById(Context context, String id) {
        int first = id.indexOf('/');
        int end = id.lastIndexOf('/');

        String packageName = id.substring(0, first);
        String lable = id.substring(end + 1, id.length());
        String path = id.substring(first + 1, end);

        TextTypefaceCustomApk t = new TextTypefaceCustomApk(context, TYPEFACE_TYPE_APP, packageName, lable, path);
        return t;
    }

    public boolean isValid() {
        return AppUtils.isPackageExists(mContext, mPackageName);
    }
}
