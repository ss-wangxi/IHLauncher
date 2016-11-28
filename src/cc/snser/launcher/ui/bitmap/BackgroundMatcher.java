package cc.snser.launcher.ui.bitmap;

import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

/** analyze icon image and give a suggest background board color*/
public class BackgroundMatcher {
    private static boolean sInited = false;

    /**
     * the rectangle that exactly contains all the opaque colors of the icon.
     *
     * only use this after one of {@link #match} method called
     * */
    public static final Rect sOpaqueRect = new Rect();

    public static boolean isRoundRect(Context context, Bitmap bmp) {
        if (!sInited) {
            initStatics(context);
        }

        if (bmp == null) {
            return false;
        }

        final int width = bmp.getWidth();
        final int height = bmp.getHeight();

        int[] colors = new int[width * height];
        bmp.getPixels(colors, 0, width, 0, 0, width, height);

        return isRoundRect(colors, width, height);
    }

    private static void initStatics(Context context) {
        delta = (int) (2 * context.getResources().getDisplayMetrics().density / 1.5f + 0.5f);
        radius = (int) (20 * context.getResources().getDisplayMetrics().density * (1 - Math.sqrt(2) / 2) + 0.5f);
        edgeVariation = context.getResources().getDisplayMetrics().density * 1.8f;

        sInited = true;
    }

    private static int[] offsets = new int[16];
    private static int delta = 2;
    private static int radius = 10; // 圆角最大半径, 10dip
    private static float edgeVariation = 1;

    // 检查横向和纵向的4等分线与图片非透明区域的边缘交点能否围成一个正方形，以及通过2条对角线与图片非透明区域的4个交点计算出圆角半径是否在一个合理范围内，一共16个采样点
    private static boolean isRoundRect(int[] colors, int width, int height) {
        Arrays.fill(offsets, 0);

        boolean hitOpaque;
        int s = height / 4 * width;
        int depth = width / 2;
        int alphaThres = 128;

        // 查找左边缘
        for (int j = 0; j < 3; j++) {
            hitOpaque = false;
            for (int i = 0; i < depth; i++) {
                if ((colors[(j + 1) * s + i] & 0xff000000) >>> 24 > alphaThres) { // 透明度大于50%
                    offsets[j] = i;
                    hitOpaque = true;
                    break;
                }
            }
            if (!hitOpaque) {
                return false;
            }
        }

        // 检查左边缘是否平直
        if (squareDeviation(offsets[0], offsets[1], offsets[2]) > edgeVariation) {
            return false;
        }

        // 查找右边缘
        for (int j = 0; j < 3; j++) {
            hitOpaque = false;
            for (int i = 0; i < depth; i++) {
                if ((colors[(j + 1) * s + width - i - 1] & 0xff000000) >>> 24 > alphaThres) {
                    offsets[j + 3] = i;
                    hitOpaque = true;
                    break;
                }
            }
            if (!hitOpaque) {
                return false;
            }
        }

        // 检查右边缘是否平直
        if (squareDeviation(offsets[3], offsets[4], offsets[5]) > edgeVariation) {
            return false;
        }

        // 水平方向检查镂空
        for (int j = 0; j < 3; j++) {
            for (int i = offsets[j]; i < width - offsets[j + 3]; i++) {
                if (colors[s * (j + 1) + i] >>> 24 == 0) {
                    return false;
                }
            }
        }

        s = width / 4;
        depth = height / 2;

        // 查找上边缘
        for (int j = 0; j < 3; j++) {
            hitOpaque = false;
            for (int i = 0; i < depth; i++) {
                if ((colors[(j + 1) * s + i * width] & 0xff000000) >>> 24 > alphaThres) {
                    offsets[j + 6] = i;
                    hitOpaque = true;
                    break;
                }
            }
            if (!hitOpaque) {
                return false;
            }
        }

        // 检查上边缘是否平齐
        if (squareDeviation(offsets[6], offsets[7], offsets[8]) > edgeVariation) {
            return false;
        }

        // 查找下边缘
        for (int j = 0; j < 3; j++) {
            hitOpaque = false;
            for (int i = 0; i < depth; i++) {
                if ((colors[s * (j + 1) + (height - i - 1) * width] & 0xff000000) >>> 24 > alphaThres) {
                    offsets[j + 9] = i;
                    hitOpaque = true;
                    break;
                }
            }
            if (!hitOpaque) {
                return false;
            }
        }

        // 检查下边缘是否平齐
        if (squareDeviation(offsets[9], offsets[10], offsets[11]) > edgeVariation) {
            return false;
        }

        // 竖直方向检查镂空
        for (int j = 0; j < 3; j++) {
            for (int i = offsets[j + 6]; i < height - offsets[j + 9]; i++) {
                if (colors[s * (j + 1) + i * width] >>> 24 == 0) {
                    return false;
                }
            }
        }

        sOpaqueRect.left = (int) average(offsets[0], offsets[1], offsets[2]);
        sOpaqueRect.right = (int) (width - average(offsets[3], offsets[4], offsets[5]));
        sOpaqueRect.top = (int) average(offsets[6], offsets[7], offsets[8]);
        sOpaqueRect.bottom = (int) (width - average(offsets[9], offsets[10], offsets[11]));

        final float w = sOpaqueRect.width();
        final float h = sOpaqueRect.height();

        if (w == 0 || h == 0) {
            return false;
        }

        final float ratio = w > h ? w / h : h / w;
        if (ratio > 1.05f) {
            return false;
        }

        int up = Math.min(width, height);
        float ws = width * 1.0f / up;
        float hs = height * 1.0f / up;

        for (int i = 0; i < up; i++) {
            final int r = (int) (i * hs);
            final int c = (int) (i * ws);
            if ((colors[width * r + c] & 0xff000000) >>> 24 > alphaThres) {
                offsets[12] = i;
                break;
            }
        }

        for (int i = 0; i < up; i++) {
            final int r = (int) (height - i * hs) - 1;
            final int c = (int) (width - i * ws) - 1;
            if ((colors[width * r + c] & 0xff000000) >>> 24 > alphaThres) {
                offsets[13] = i;
                break;
            }
        }

        for (int i = 0; i < up; i++) {
            final int r = (int) (i * hs);
            final int c = (int) (width - i * ws) - 1;
            if ((colors[width * r + c] & 0xff000000) >>> 24 > alphaThres) {
                offsets[14] = i;
                break;
            }
        }

        for (int i = 0; i < up; i++) {
            final int r = (int) (height - i * hs) - 1;
            final int c = (int) (i * ws);
            if ((colors[width * r + c] & 0xff000000) >>> 24 > alphaThres) {
                offsets[15] = i;
                break;
            }
        }

        // 检查四个圆角大小
        if (squareDeviation(offsets[12], offsets[13], offsets[14], offsets[15]) > delta * delta
                && (offsets[12] > radius * 0.8f || offsets[13] > radius * 0.8f || offsets[14] > radius * 0.8f || offsets[15] > radius * 0.8f)) {
            return false;
        }

        if (offsets[0] > offsets[12]) {
            return false;
        }

        if (offsets[12] - offsets[0] > radius) {
            return false;
        }

        return true;
    }

    // 方差
    private static final float squareDeviation(int ... values) {
        if (values == null || values.length == 0) {
            return -1;
        }

        float aver = average(values);

        float sv = 0;

        for (int v : values) {
            sv += (v - aver) * (v - aver);
        }

        return (float)Math.sqrt(sv);
    }

    private static float average(int ... values) {
        float sum = 0;

        for (int v : values) {
            sum += v;
        }

        return sum / values.length;
    }
}
