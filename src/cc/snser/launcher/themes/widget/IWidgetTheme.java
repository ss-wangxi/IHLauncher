package cc.snser.launcher.themes.widget;

import java.io.Serializable;

import org.w3c.dom.Document;

import android.graphics.Bitmap;

public interface IWidgetTheme extends Serializable{

    boolean DEBUG = false;

    public static final String CONFIG_FILE = "theme.xml";

   
    Bitmap loadBitmap(String file);

    
    Document loadXml();

    
    Integer getRequiredSpecification();

    
    Integer getVersionCode();

    
    String getPackageName();

    
    Integer getDefaultDensity();
}
