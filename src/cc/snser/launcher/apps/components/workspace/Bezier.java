package cc.snser.launcher.apps.components.workspace;

/**
 * 二次贝塞尔曲线
 * */
public class Bezier {

    private float startX;
    private float startY;
    private float ctrlX;
    private float ctrlY;
    private float endX;
    private float endY;

    public Bezier(float startX, float startY, float ctrlX, float ctrlY, float endX, float endY) {
        this.startX = startX;
        this.startY = startY;
        this.ctrlX = ctrlX;
        this.ctrlY = ctrlY;
        this.endX = endX;
        this.endY = endY;
    }

    public Bezier() { }

    public void setStartPoint(float x, float y) {
        startX = x;
        startY = y;
    }

    public void setControlPoint(float x, float y) {
        ctrlX = x;
        ctrlY = y;
    }

    public void setEndPoint(float x, float y) {
        endX = x;
        endY = y;
    }

    public void getCoordinate(float t, float[] out) {
        if (out == null || out.length < 2) {
            return;
        }
        out[0] = function(t, startX, ctrlX, endX);
        out[1] = function(t, startY, ctrlY, endY);
    }

    /**
     * 二次贝塞尔函数
     * <p>
     * B(t) = (1-t)^2 * P1 + 2t(1-t) * P2 + t^2 * P3
     * </p>
     * */
    private static float function(float t, float start, float ctrl, float end) {
        return (1 - t) * (1 - t) * start + 2 * t * (1 - t) * ctrl + t * t * end;
    }
}
