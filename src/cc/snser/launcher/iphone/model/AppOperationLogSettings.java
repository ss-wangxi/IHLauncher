package cc.snser.launcher.iphone.model;

import android.net.Uri;
import android.provider.BaseColumns;


public class AppOperationLogSettings {
	public static final class Log implements BaseColumns{
		public static final String COMPONENT = "component";
		public static final String LAST_CALLED_TIME = "last_called_time";
		public static final String CALLED_NUM = "called_num";
		
		public static final Uri CONTENT_URI = Uri.parse("content://" + AppOperationLogProvider.AUTHORITY
				  + "/" + AppOperationLogProvider.TABLE_APPLOG);
		
		
	}
}
