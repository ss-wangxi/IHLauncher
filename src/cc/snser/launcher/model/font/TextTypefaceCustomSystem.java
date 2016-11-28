package cc.snser.launcher.model.font;

import android.content.Context;
import android.graphics.Typeface;

public class TextTypefaceCustomSystem extends TextTypefaceCustom {

    public TextTypefaceCustomSystem(Context context, int type, String lable, String path) {
        super(context, type, lable, lable, path);
    }

    @Override
    public Typeface getTypeface() {
        return TextTypeface.getTypefaceFromCache(mContext, TYPEFACE_TYPE_SYSTEM, mPath, mPackageName);
    }

    @Override
    public String getId() {
        return mPath;
    }

    @Override
    public String getName() {
        return mLable;
    }

    @Override
    public String getTypeString() {
        return SYSTEM_STRING;
    }

    @Override
    public boolean isInUsing() {
        return false;
    }

    static TextTypefaceCustomSystem getTextTypefaceById(Context context, String id) {
        String lable = id.substring(id.lastIndexOf('/') + 1, id.lastIndexOf('.'));
        TextTypefaceCustomSystem t = new TextTypefaceCustomSystem(context, TYPEFACE_TYPE_SYSTEM, lable, id);
        return t;
    }
}
