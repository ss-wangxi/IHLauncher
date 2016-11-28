
package cc.snser.launcher.model.font;

import android.content.Context;

/**
 * typeface searched in mobile
 *
 * @author yangkai
 */
public abstract class TextTypefaceCustom extends TextTypeface {
    protected final String mPackageName;

    public TextTypefaceCustom(Context context, int type, String packageName, String lable, String path) {
        super(context, type, lable, path);

        mPackageName = packageName;
    }
}
