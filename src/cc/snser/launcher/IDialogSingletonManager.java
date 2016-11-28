package cc.snser.launcher;

import android.app.Dialog;

public interface IDialogSingletonManager{
    public boolean isDialogShowing();
    public void setShowingDlg(Dialog dlg);
}