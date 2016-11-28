package cc.snser.launcher.util;

import java.text.DecimalFormat;

/**
 * b kb M G 单位转换
 *
 */
public class SizeUtil {

	private static final DecimalFormat df = new DecimalFormat("0.00");//2个小数位的 double M
	private static final double Kb = 1024;//  1kb
	private static final double Mb = 1048576;//  1Mb
	private static final double Gb = 1073741824;//  1G
	/**
	 * byte  转化为 Mb
	 * @param bytes
	 * @return
	 */
	public static String byte2Mb(long bytes){
		return df.format(((double)bytes)/Mb);
	}
	
	public static String kb2Mb(int kbSize){
		return df.format((double)kbSize/Kb);
	}
	
	public static String getAutoSize(long bytes) {
		if (bytes < 1024) {
			return bytes + "B";
		} else if (bytes >= 1024 && bytes < 1048576) {
			return df.format((double) bytes / Kb) + "KB";
		} else {
			return byte2Mb(bytes) + "MB";
		}
	}
	
	public static String humanReadableByteCount(long bytes) {
	    int unit = 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = ("KMGTPE").charAt(exp-1) + "";
	    String a = df.format( bytes / Math.pow(unit, exp));
	    if(a.indexOf(".") > 0){
	        //正则表达
	              a = a.replaceAll("0+?$", "");//去掉后面无用的零
	              a = a.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
	     }
	    return String.format("%s %sB", a, pre);
	}
	
	/**
	 * 
	 * @param totalSize
	 * @param percent
	 * @return
	 */
	public static long getSizeByPercent(long totalSize,long percent){
		return totalSize*percent/100;
	}
	/**
	 * 通过下载大小 计算进度
	 * @param progress
	 * @param size
	 * @return
	 */
	public static int getPercent(long progress,long size){
		if (progress == 0) {
			return 0;
		}
		if (size == 0) {
			return 0;
		}
		if (progress >= size) {
			return 100;
		}
		if (progress < size) {
			int i = (int) (progress*100/size);
			return i;
		}
		return 0;
	}
	
}
