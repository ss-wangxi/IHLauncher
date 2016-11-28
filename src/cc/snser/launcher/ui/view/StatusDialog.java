package cc.snser.launcher.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import cc.snser.launcher.util.StatusBarTransparentUtils;

public class StatusDialog extends Dialog {
    public StatusDialog(Context context, View view) {
        super(context);
        StatusBarTransparentUtils.setSystemUiTransparent(view, true);
    }
}
