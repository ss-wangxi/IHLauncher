package cc.snser.launcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.btime.launcher.util.XLog;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;

public class LocaleCfgMgr {
	private final String TAG = "LocaleCfgMgr";
	private LocaleConfiguration sLocaleConfiguration = null;
	private final String PREFERENCES = "launcher.preferences";
	private static LocaleCfgMgr sInstance;
	private Launcher mLauncher;
	
	public static LocaleCfgMgr getInstance(){
		if(sInstance == null){
			sInstance = new LocaleCfgMgr();
		}
		
		return sInstance;
	}
	
	public void setLauncher(Launcher launcher){
		mLauncher = launcher;
	}
	
	public void checkForLocaleChange(){
		if(mLauncher == null) return;
		
		if(sLocaleConfiguration == null){
			initLocaleCfg();
		}else {
			if(isLocaleChange()){
				mLauncher.onLocaleCfgChange();
			}
		}
	}
	
	private void initLocaleCfg() {
		if (sLocaleConfiguration == null) {
			new AsyncTask<Void, Void, LocaleConfiguration>() {
				@Override
				protected LocaleConfiguration doInBackground(Void... unused) {
					LocaleConfiguration localeConfiguration = new LocaleConfiguration();
					readConfiguration(mLauncher, localeConfiguration);
					return localeConfiguration;
				}

				@Override
				protected void onPostExecute(LocaleConfiguration result) {
					sLocaleConfiguration = result;
					isLocaleChange(); // recursive, but now with a locale configuration
				}
			}.execute();
		}
	}
	
	private boolean isLocaleChange(){
		if(sLocaleConfiguration == null) return false;
		
		final Configuration configuration =  mLauncher.getResources().getConfiguration();

		final String previousLocale = sLocaleConfiguration.locale;
		final String locale = configuration.locale.toString();

		final int previousMcc = sLocaleConfiguration.mcc;
		final int mcc = configuration.mcc;

		final int previousMnc = sLocaleConfiguration.mnc;
		final int mnc = configuration.mnc;

		boolean localeChanged = !locale.equals(previousLocale)
				|| mcc != previousMcc || mnc != previousMnc;

		if (localeChanged) {
			sLocaleConfiguration.locale = locale;
			sLocaleConfiguration.mcc = mcc;
			sLocaleConfiguration.mnc = mnc;

			final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
			new Thread("WriteLocaleConfiguration") {
				@Override
				public void run() {
					writeConfiguration(mLauncher, localeConfiguration);
				}
			}.start();
			return true;
		}
		
		return false;
	}

	
	private void readConfiguration(Context context,LocaleConfiguration configuration) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(context.openFileInput(PREFERENCES));
			configuration.locale = in.readUTF();
			configuration.mcc = in.readInt();
			configuration.mnc = in.readInt();
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// Ignore
		} catch (Throwable e) {
			XLog.e(TAG, "Failed to read the configuration", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}
	
	private void writeConfiguration(Context context,LocaleConfiguration configuration) {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(context.openFileOutput(PREFERENCES,
					Context.MODE_PRIVATE));
			out.writeUTF(configuration.locale);
			out.writeInt(configuration.mcc);
			out.writeInt(configuration.mnc);
			out.flush();
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// noinspection ResultOfMethodCallIgnored
			try {
				context.getFileStreamPath(PREFERENCES).delete();
			} catch (Exception ee) {
				// ignore
			}
		} catch (Throwable e) {
			XLog.e(TAG, "Failed to write the configuration", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}
	
	private class LocaleConfiguration {
		public String locale;
		public int mcc = -1;
		public int mnc = -1;
	}
}
