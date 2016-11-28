package cc.snser.launcher.util;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.btime.launcher.util.XLog;

import static cc.snser.launcher.Constant.LOGE_ENABLED;

public class CellImagePool {
    public interface Callback {
        public void attach(Image img);
        public void detach(Image img);
    }

    private static final String TAG = "Launcher.CellImagePool";

    private static final int POOL_MAX = 108;

    private static int sPoolMax = POOL_MAX;

    private static int sCount = 0;

    private static Image sPoolHead;
    private static Image sPoolTail;

    private static ArrayList<WeakReference<Image>> sRecycler = new ArrayList<WeakReference<Image>>();

    public static final void release(Image image) {
        if (image == sPoolTail && image == sPoolHead) {
            sPoolTail = null;
            sPoolHead = null;
        } else if (image == sPoolTail) {
            sPoolTail = image.prev;
        } else if (image == sPoolHead) {
            sPoolHead = image.next;
        } else {
            Image prev = image.prev;
            Image next = image.next;
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }
        }

        image.prev = null;
        image.next = null;

        image.recycle();

        sCount--;
    }

    public static final Image obtain(Callback owner) {
        Image img = null;

        // new node
        if (sCount < sPoolMax) {
            try {
                img = new Image();
            } catch (OutOfMemoryError e) {
                // ignore
                if (LOGE_ENABLED) {
                    XLog.e(TAG, "Failed to create new pooled image.");
                }
            }

            if (img != null) {
                if (img.bitmap == null) {
                    img = null;
                } else if (sCount == 0) {
                    sPoolTail = img;
                }
                if (img != null) {
                    sCount++;
                }
            }
        }

        // multiplexing
        if (img == null) {
            if (sPoolTail == null) {
                return null;
            }
            Image i = sPoolTail;
            while (i != null && i.callback == owner) {
                i = i.prev;
            }
            img = i;
            if (i != sPoolTail && i != null && img.prev != null && img.next != null) {
                img.prev.next = img.next;
                img.next.prev = img.prev;
            } else {
                img = sPoolTail;
                sPoolTail = img.prev;
                sPoolTail.next = null;
            }
        }

        // up to head
        if (sPoolHead != null) {
            sPoolHead.prev = img;
        }

        img.next = sPoolHead;
        img.prev = null;
        sPoolHead = img;

        // bind
        img.bindKey(owner);

        return img;
    }

    /*
    private static void recyle(Image img) {
        img.prev = sPoolTail;

        img.bindKey(null);

        if (sPoolTail == null) {
            sPoolHead = img;
            img.next = null;
        } else {
            sPoolTail.next = img;
        }

        sPoolTail = img;
        sCount++;
    }

    private static void collect() {
        ArrayList<WeakReference<Image>> recycler = sRecycler;
        Image img;
        for (WeakReference<Image> reference : recycler) {
            img = reference.get();
            if (img != null) {
                if (sCount < sPoolMax) {
                    recyle(img);
                } else {
                    img.recycle();
                }
            }
        }

        sRecycler.clear();
    }*/

    public static void unlay() {
        ArrayList<WeakReference<Image>> recycler = sRecycler;
        Image head = sPoolHead;
        Image img;
        while (head != null) {
            img = head;
            head = img.next;
            img.bindKey(null); // detach
            recycler.add(new WeakReference<Image>(img));
        }
        sPoolHead = null;
        sPoolTail = null;
        sCount = 0;
    }

    public static void clear() {
        Image head = sPoolHead;
        Image img;
        while (head != null) {
            img = head;
            head = img.next;
            img.recycle(); // recycle
        }
        sPoolHead = null;
        sPoolTail = null;
        sCount = 0;
    }

    /*
    private static void resetPoolMax(int poolMax) {
        if (sCount > poolMax) {
            int i = poolMax - sCount;
            Image tail = sPoolTail;
            Image img;
            while (i > 0) {
                img = tail;
                tail = img.prev;
                img.recycle();
                i--;
            }
            sPoolTail = tail;
            sCount = poolMax;
        }
        sPoolMax = poolMax;
    }*/

    public static boolean isEnabled(int width, int height) {
        return Image.sDefWidth == width && Image.sDefHeight == height;
    }

    public static void setDefault(int width, int height) {
        if (width != Image.sDefWidth || height != Image.sDefHeight) {
            Image.sDefWidth = width;
            Image.sDefHeight = height;

            clear();
        }
    }

    public static class Image {
        private static int sDefWidth;
        private static int sDefHeight;

        protected Image next;
        protected Image prev;

        public Bitmap bitmap;
        public Callback callback;

        protected Image() {
            bitmap = Bitmap.createBitmap(sDefWidth, sDefHeight, Bitmap.Config.ARGB_8888);
        }

        protected void recycle() {
            if (bitmap != null && !bitmap.isRecycled()) {
                if (callback != null) {
                    callback.detach(this);
                    callback = null;
                }
                bitmap.recycle();
                bitmap = null;
            }
        }

        public void bindKey(Callback key) {
            if (key != callback) {
                if (callback != null) {
                    callback.detach(this);
                    callback = null;
                }
                if (key != null) {
                    key.attach(this);
                    callback = key;
                }
            }
        }

        public boolean checkAlive(Object key) {
            return bitmap != null && !bitmap.isRecycled()
                    && key.equals(callback);
        }
    }
}
