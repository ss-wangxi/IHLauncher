package cc.snser.launcher.model.font;

import android.content.Context;
import android.graphics.Typeface;

public class TextTypefaceCustomSdCard extends TextTypefaceCustom {

    public TextTypefaceCustomSdCard(Context context, int type, String lable, String path) {
        super(context, type, lable, lable, path);
    }

    @Override
    public Typeface getTypeface() throws Exception {
        return TextTypeface.getTypefaceFromCache(mContext, TYPEFACE_TYPE_SDCARD, mPath, mPackageName);
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
        return "sdcard";
    }

    static TextTypefaceCustomSdCard getTextTypefaceById(Context context, String id) {
        String name = id.substring(id.lastIndexOf("/") + 1, id.lastIndexOf("."));
        TextTypefaceCustomSdCard t = new TextTypefaceCustomSdCard(context, TYPEFACE_TYPE_SDCARD, name, id);
        return t;
    }
}
