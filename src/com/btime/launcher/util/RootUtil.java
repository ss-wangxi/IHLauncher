package com.btime.launcher.util;

import java.io.DataOutputStream;
import java.io.File;

import android.text.TextUtils;

public class RootUtil {

    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * @return 应用程序是/否获取Root权限
     */
    public static boolean upgradeRootPermission() {
        boolean succ = false;
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            succ = (process.waitFor() == 0);
        } catch (Exception e) {
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return succ;
    }
    
    /**
     * root 权限copy文件，仅供测试使用
     * @param src 源路径
     * @param dest 目标路径
     */
    public static boolean copyDirectory(String src, String dest) {
        boolean succ = false;
        
        if (!TextUtils.isEmpty(src) && !TextUtils.isEmpty(dest)) {
            File srcFile = new File(src);
            if (srcFile.exists() && srcFile.isDirectory()) {
                new File(dest).mkdirs();
                Process process = null;
                DataOutputStream os = null;
                try {
                    process = Runtime.getRuntime().exec("su");
                    os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes("cp -r " + src + " " + dest + "\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    succ = (process.waitFor() == 0);
                } catch (Exception e) {
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                        process.destroy();
                    } catch (Exception e) {
                    }
                }
            }
        }

        return succ;
    }
    
    /**
     * root 权限修改文件权限，仅供测试使用
     * @param file 需要修改的文件
     * @param rights 权限码
     */
    public static boolean chmod(File file, String rights) {
        boolean succ = false;
        
        if (file != null) {
            Process process = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("chmod "+rights+" "+file.getAbsolutePath()+"\n");
                os.writeBytes("exit\n");
                os.flush();
                succ = (process.waitFor() == 0);
            } catch (Exception e) {
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    process.destroy();
                } catch (Exception e) {
                }
            }
        }
        return succ;
    }
    
    /**
     * root权限执行cmd
     * @param cmd
     * @return
     */
    public static boolean sudo(String cmd) {
        boolean succ = false;
        if (!TextUtils.isEmpty(cmd)) {
            Process process = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(cmd +"\n");
                os.writeBytes("exit\n");
                os.flush();
                succ = (process.waitFor() == 0);
            } catch (Exception e) {
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    process.destroy();
                } catch (Exception e) {
                }
            }
        }
        return succ;
    }
    
}