package cc.snser.launcher.model.font;

import android.content.Context;
import android.graphics.Typeface;

/**
 * system default typeface
 *
 * @author yangkai
 */
public class TextTypefaceDefault extends TextTypeface {

    private final Typeface mTypeface;

    public TextTypefaceDefault(Context context, int type, String lable, Typeface typeface) {
        super(context, type, lable, null);

        mTypeface = typeface;
    }

    @Override
    public Typeface getTypeface() {
        return mTypeface;
    }

    @Override
    public String getId() {
        return mLable;
    }

    @Override
    public String getName() {
        return mLable;
    }

    @Override
    public String getTypeString() {
        return SYSTEM_STRING;
    }

    public static TextTypefaceDefault getDefaultTextTypeface(Context context) {
        return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "DEFAULT", Typeface.DEFAULT);
    }

    static TextTypefaceDefault getTextTypefaceById(Context context, String id) {
        if ("DEFAULT_BOLD".equals(id)) {
            return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "DEFAULT_BOLD", Typeface.DEFAULT_BOLD);
        } else if ("SANS_SERIF".equals(id)) {
            return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "SANS_SERIF", Typeface.SANS_SERIF);
        } else if ("SERIF".equals(id)) {
            return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "SERIF", Typeface.SERIF);
        } else if ("MONOSPACE".equals(id)) {
            return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "MONOSPACE", Typeface.MONOSPACE);
        }

        return new TextTypefaceDefault(context, TYPEFACE_TYPE_DEFAULT, "DEFAULT", Typeface.DEFAULT);
    }

    public boolean isValid() {
        return true;
    }
}
