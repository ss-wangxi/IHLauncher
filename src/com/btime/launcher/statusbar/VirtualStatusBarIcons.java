package com.btime.launcher.statusbar;

import com.btime.launcher.R;

public class VirtualStatusBarIcons {
    public static class Wifi {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_wifi_signal0, 
            R.drawable.virtual_statusbar_wifi_signal1, 
            R.drawable.virtual_statusbar_wifi_signal2, 
            R.drawable.virtual_statusbar_wifi_signal3, 
            R.drawable.virtual_statusbar_wifi_signal4
        };
        public static final int SIGNAL_LEVEL_COUNT = ICON.length;
        public static int getIcon(int signalLevel) {
            final int signalLevelIndex = signalLevel >= 0 && signalLevel < SIGNAL_LEVEL_COUNT ? signalLevel : 0;
            return ICON[signalLevelIndex];
        }
    }
    
    public static class Ap {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_ap_enabled 
        };
        public static int getIcon(boolean isEnabled) {
            return ICON[isEnabled ? 0 : 0];
        }
    }
    
    public static class Bluetooth {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_bluetooth_connected, 
            R.drawable.virtual_statusbar_bluetooth_disconnected
        };
        public static int getIcon(boolean isConnected) {
              return ICON[isConnected ? 0 : 1];         
      }
    }
    
    public static class Sim {
        private static final int ICON_ABSENT = R.drawable.virtual_statusbar_sim_absent;
        private static final int [][] ICON = {
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_unknown_signal1, 
             R.drawable.virtual_statusbar_sim_unknown_signal2, 
             R.drawable.virtual_statusbar_sim_unknown_signal3, 
             R.drawable.virtual_statusbar_sim_unknown_signal4}, 
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_g_signal1, 
             R.drawable.virtual_statusbar_sim_g_signal2, 
             R.drawable.virtual_statusbar_sim_g_signal3, 
             R.drawable.virtual_statusbar_sim_g_signal4}, 
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_e_signal1, 
             R.drawable.virtual_statusbar_sim_e_signal2, 
             R.drawable.virtual_statusbar_sim_e_signal3, 
             R.drawable.virtual_statusbar_sim_e_signal4}, 
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_3g_signal1, 
             R.drawable.virtual_statusbar_sim_3g_signal2, 
             R.drawable.virtual_statusbar_sim_3g_signal3, 
             R.drawable.virtual_statusbar_sim_3g_signal4}, 
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_h_signal1, 
             R.drawable.virtual_statusbar_sim_h_signal2, 
             R.drawable.virtual_statusbar_sim_h_signal3, 
             R.drawable.virtual_statusbar_sim_h_signal4}, 
            {R.drawable.virtual_statusbar_sim_unknown_signal0, 
             R.drawable.virtual_statusbar_sim_4g_signal1, 
             R.drawable.virtual_statusbar_sim_4g_signal2, 
             R.drawable.virtual_statusbar_sim_4g_signal3, 
             R.drawable.virtual_statusbar_sim_4g_signal4}
        };
        public static final int TYPE_COUNT = ICON.length;
        public static final int SIGNAL_LEVEL_COUNT = ICON[0].length;
        public static int getIcon(boolean isReady, int type, int signalLevel) {
            if (isReady) {
                final int typeIndex = type >= 0 && type < TYPE_COUNT ? type : 0; 
                final int signalLevelIndex = signalLevel >= 0 && signalLevel < SIGNAL_LEVEL_COUNT ? signalLevel : 0; 
                return ICON[typeIndex][signalLevelIndex];
            } else {
                return ICON_ABSENT;
            }
        }
    }
    
    public static class Sdcard {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_sdcard_ready
        };
        public static int getIcon(boolean isReady) {
            return ICON[isReady ? 0 : 0];
        }
    }
    
    public static class Fm {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_fm_enabled
        };
        public static int getIcon(boolean isEnabled) {
            return ICON[isEnabled ? 0 : 0];
        }
    }
    
    public static class Mute {
        private static final int [] ICON = {
            R.drawable.virtual_statusbar_mute_enabled
        };
        public static int getIcon(boolean isEnabled) {
            return ICON[isEnabled ? 0 : 0];
        }
    }
}
