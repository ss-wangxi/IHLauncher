package com.btime.launcher.util;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public class SignatureUtils {
	public static String getSignatureMD5(Context context, String packageName) {
        String md5Digest = null;
        try {
            PackageManager packageManager = context.getPackageManager();
 
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
            Signature[] signs = packageInfo.signatures;
            Signature sign = signs[0];
            md5Digest = parseSignature(sign.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5Digest;
}
 
    private static String parseSignature(byte[] signature) {
        String digest = null;
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory
                    .generateCertificate(new ByteArrayInputStream(signature));
 
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(cert.getEncoded());
            digest = byteArrayToHex(md5.digest());
 
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
			e.printStackTrace();
		}
        return digest;
    }
    
    private static String byteArrayToHex(byte[] byteArray) {  
    	  
    	   // 首先初始化一个字符数组，用来存放每个16进制字符  
    	   char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F' };  
    	  
    	   // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））  
    	   char[] resultCharArray =new char[byteArray.length * 2];  
    	  
    	   // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去      	  
    	   int index = 0;  
    	  
    	   for (byte b : byteArray) {  
    	      resultCharArray[index++] = hexDigits[b>>> 4 & 0xf];  
    	      resultCharArray[index++] = hexDigits[b& 0xf];  
    	   }  
    	   // 字符数组组合成字符串返回  
    	   return new String(resultCharArray); 
    }
}
