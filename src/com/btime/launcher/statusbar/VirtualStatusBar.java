package com.btime.launcher.statusbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import cc.snser.launcher.Launcher;

import com.btime.launcher.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VirtualStatusBar extends LinearLayout {
    //private Launcher mLauncher;
    
    private TextView mTxtTime;
    private ImageView mImgWifi;
    private ImageView mImgAp;
    private ImageView mImgBluetooth;
    private ImageView mImgSim;
    private ImageView mImgSdcard;
    private ImageView mImgFm;
    private ImageView mImgMute;
    
    private SimpleDateFormat mDateFormat12 = new SimpleDateFormat("hh:mm", Locale.getDefault());
    private SimpleDateFormat mDateFormat24 = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public VirtualStatusBar(Context context) {
        super(context);
        initLayout(context);
    }

    public VirtualStatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public VirtualStatusBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initLayout(context);
    }
    
    private void initLayout(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.virtual_statusbar, this, true);
    }
    
    @Override
    protected void onFinishInflate() {
        mTxtTime = (TextView)findViewById(R.id.virtual_statusbar_time);
        mImgWifi = (ImageView)findViewById(R.id.virtual_statusbar_wifi);
        mImgAp = (ImageView)findViewById(R.id.virtual_statusbar_ap);
        mImgBluetooth = (ImageView)findViewById(R.id.virtual_statusbar_bluetooth);
        mImgSim = (ImageView)findViewById(R.id.virtual_statusbar_sim);
        mImgSdcard = (ImageView)findViewById(R.id.virtual_statusbar_sdcard);
        mImgFm = (ImageView)findViewById(R.id.virtual_statusbar_fm);
        mImgMute = (ImageView)findViewById(R.id.virtual_statusbar_mute);
    }
    
    public void setLauncher(Launcher launcher) {
        //mLauncher = launcher;
    }
    
    public void setTimeStatus(Date date, boolean is24HourFormat, boolean isTimeZoneChanged) {
        if (mTxtTime != null) {
            if (isTimeZoneChanged) {
                mDateFormat12.setTimeZone(TimeZone.getDefault());
                mDateFormat24.setTimeZone(TimeZone.getDefault());
            }
            mTxtTime.setText(is24HourFormat ? mDateFormat24.format(date) : mDateFormat12.format(date));
        }
    }
    
    public void setWifiStatus(boolean isConnected, int signalLevel) {
        if (mImgWifi != null) {
            if (isConnected) {
                mImgWifi.setImageResource(VirtualStatusBarIcons.Wifi.getIcon(signalLevel));
                mImgWifi.setVisibility(View.VISIBLE);
            } else {
                mImgWifi.setVisibility(View.GONE);
            }
        }
    }
    
    public void setApStatus(boolean isEnabled) {
        if (mImgAp != null) {
            if (isEnabled) {
                mImgAp.setImageResource(VirtualStatusBarIcons.Ap.getIcon(isEnabled));
                mImgAp.setVisibility(View.VISIBLE);
            } else {
                mImgAp.setVisibility(View.GONE);
            }
        }
    }
  public void setBluetoothStatus(boolean isConnected) {
  if (mImgBluetooth != null) {
      if (isConnected) {
          mImgBluetooth.setImageResource(VirtualStatusBarIcons.Bluetooth.getIcon(isConnected));
          mImgBluetooth.setVisibility(View.VISIBLE);
      } else {
          mImgBluetooth.setVisibility(View.GONE);
      }
  }
}
    
    public void setSimStatus(boolean isReady, int type, int signalLevel) {
        if (mImgSim != null) {
            mImgSim.setImageResource(VirtualStatusBarIcons.Sim.getIcon(isReady, type, signalLevel));
        }
    }
    
    public void setSdcardStatus(boolean isReady) {
        if (mImgSdcard != null) {
            if (isReady) {
                mImgSdcard.setImageResource(VirtualStatusBarIcons.Sdcard.getIcon(isReady));
                mImgSdcard.setVisibility(View.VISIBLE);
            } else {
                mImgSdcard.setVisibility(View.GONE);
            }
        }
    }
    
    public void setFmStatus(boolean isEnabled) {
        if (mImgFm != null) {
            if (isEnabled) {
                mImgFm.setImageResource(VirtualStatusBarIcons.Fm.getIcon(isEnabled));
                mImgFm.setVisibility(View.VISIBLE);
            } else {
                mImgFm.setVisibility(View.GONE);
            }
        }
    }
    
    public void setMuteStatus(boolean isEnabled) {
        if (mImgMute != null) {
            if (isEnabled) {
                mImgMute.setImageResource(VirtualStatusBarIcons.Mute.getIcon(isEnabled));
                mImgMute.setVisibility(View.VISIBLE);
            } else {
                mImgMute.setVisibility(View.GONE);
            }
        }
    }

}
