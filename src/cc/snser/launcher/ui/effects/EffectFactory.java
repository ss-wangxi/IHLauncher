
package cc.snser.launcher.ui.effects;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.FloatMath;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.RuntimeConfig;
import cc.snser.launcher.apps.model.ItemInfo;

import com.shouxinzm.launcher.util.DeviceUtils;

/**
 * <p>
 * Represents the factory which holds all the effects for the workspace.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public class EffectFactory {
    private static List<EffectInfo> allEffects;

    private static SparseArray<EffectInfo> allEffectsMap;

    private static Camera camera = new Camera();

    private static Matrix sMatrix = new Matrix();

    private static PaintFlagsDrawFilter sAntiAliesFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public static List<EffectInfo> getAllRandomableEffectTypes() {
        ArrayList<EffectInfo> eff = new ArrayList<EffectInfo>();
        List<EffectInfo> effects = loadEffectsList();
        for (EffectInfo effect : effects) {
            if (effect.type >= 0) {
                eff.add(effect);
            }
        }
        return eff;
    }

    public static int getRandomEffectType() {
        List<EffectInfo> allRandomableEffectTypes = getAllRandomableEffectTypes();

        int index = (int) (allRandomableEffectTypes.size() * Math.random());
        EffectInfo ef = allRandomableEffectTypes.get(index);
        ef.onRefresh();

        ((EffectInfoProxy)getEffectByType(TYPE_RANDOM)).setEffectInfo(ef);

        return ef.type;
    }

    public static EffectInfo getEffectByType(int type) {
        SparseArray<EffectInfo> effects = loadEffectsMap();

        return effects.get(type);
    }

    public static List<EffectInfo> getAllEffects() {
        List<EffectInfo> effects = loadEffectsList();
        List<EffectInfo> effectsList = new LinkedList<EffectInfo>();
        for (EffectInfo ef : effects) {
            effectsList.add(ef);
        }
        return effectsList;
    }

    private static SparseArray<EffectInfo> loadEffectsMap() {
        if (allEffectsMap == null) {
            List<EffectInfo> effects = loadEffectsList();

            allEffectsMap = new SparseArray<EffectInfo>();
            for (EffectInfo effect : effects) {
                allEffectsMap.put(effect.type, effect);
            }
        }
        return allEffectsMap;
    }

    public static final int TYPE_CLASSIC = 0;

    public static final int TYPE_RANDOM = -1;

    public static final int TYPE_CUBE_OUTSIDE = 1;

    public static final int TYPE_STACK = 2;

    public static final int TYPE_FLIPPY = 3;

    public static final int TYPE_ICON_COLLECTION = 4;

    public static final int TYPE_ROLL_AWAY = 5;

    public static final int TYPE_CHORD = 6;

    public static final int TYPE_SNAKE = 7;

    public static final int TYPE_CYLINDER = 8;

    public static final int TYPE_SPHERE = 9;

    public static final int TYPE_EXTRUSION = 10;

    /**
     * Load all effects the launcher have
     *
     * @return
     */
    private static List<EffectInfo> loadEffectsList() {
        if (allEffects != null) {
            return allEffects;
        } else {
            allEffects = new ArrayList<EffectInfo>();

            allEffects.add(new Classic());

            allEffects.add(new RandomEffect());   // 随机

            allEffects.add(new Sphere()); // 球

            allEffects.add(new Cylinder()); // 圆柱

            allEffects.add(new Snake()); // 贪吃蛇

            allEffects.add(new CubeOutside());     // 立方体

            allEffects.add(new Stack());    // 层叠

            allEffects.add(new Flippy());   // 追风

            allEffects.add(new IconCollection()); // 聚散

            allEffects.add(new RollAway()); // 车轮

            allEffects.add(new Chord());    // 琴弦

            allEffects.add(new Extrusion()); // 推拉
        }

        return allEffects;
    }

    private static final class Classic extends EffectInfo {

        Classic() {
            super(TYPE_CLASSIC, "pref_k_transformation_classic", "@string/transformation_type_classic");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            Matrix matrix = childTransformation.getMatrix();

            if (offset != 0) {
                if (isPortrait) {
                    matrix.postTranslate(offset, 0);
                } else {
                    matrix.postTranslate(0, offset);
                }

                childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

                return true;
            }

            childTransformation.setTransformationType(Transformation.TYPE_IDENTITY);
            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            return null;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return true;
        }
    }

    private static final class RandomEffect extends EffectInfoProxy {
        RandomEffect() {
            super(TYPE_RANDOM, "pref_k_transformation_random", "@string/transformation_type_random");
        }
    }

    private static final class CubeOutside extends EffectInfo {
        CubeOutside() {
            super(TYPE_CUBE_OUTSIDE, "pref_k_transformation_cube_outside", "@string/transformation_type_cube_outside");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();
            final Matrix matrix = childTransformation.getMatrix();

            if (radio > 0) {
                applyLeft(1.0f - radio, childMeasuredWidth, childMeasuredHeight, matrix);
            } else {
                applyRight(1.0f + radio, childMeasuredWidth, childMeasuredHeight, matrix);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            return null;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return true;
        }

        public void applyLeft(float ratio, int w, int h, Matrix matrix) {
            // 倾斜角度 = 倾斜比例 * 最大倾斜角度
            final float deg = (1.0f - ratio) * -75.0f;
            camera.save();
            camera.rotateY(deg);
            camera.getMatrix(matrix);
            camera.restore();
            matrix.preTranslate(-w, -h / 2);
            matrix.postTranslate(w, h / 2);
        }

        public void applyRight(float ratio, int w, int h, Matrix matrix) {
            // 倾斜角度 = 倾斜比例 * 最大倾斜角度
            final float deg = (1.0f - ratio) * 75.0f;
            camera.save();
            camera.rotateY(deg);
            camera.getMatrix(matrix);
            camera.restore();
            matrix.preTranslate(0.0f, -h / 2);
            matrix.postTranslate(0.0f, h / 2);
        }

    }

    private static final class Stack extends EffectInfo {
        Stack() {
            super(TYPE_STACK, "pref_k_transformation_stack", "@string/transformation_type_stack");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            if (radio <= 0.0F) {
                return false;
            }

            childTransformation.setAlpha(1.0F - radio);

            float scale = 0.4F * (1.0F - radio) + 0.6F;
            matrix.setScale(scale, scale);
            if (isPortrait) {
                matrix.postTranslate((1.0F - scale) * childMeasuredWidth * 3.0F + offset, (1.0F - scale) * childMeasuredHeight * 0.5F);
            } else {
                matrix.postTranslate((1.0F - scale) * childMeasuredWidth * 0.5F, (1.0F - scale) * childMeasuredHeight * 3.0F + offset);
            }

            childTransformation.setTransformationType(Transformation.TYPE_BOTH);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            return null;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return true;
        }

        @Override
        public int getAlphaTransitionType() {
            return 1;
        }
    }

    private static final class Flippy extends EffectInfo {

        float decorAlpha;

        Flippy() {
            super(TYPE_FLIPPY, "pref_k_transformation_flippy", "@string/transformation_type_flippy");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            final float radioABS = Math.abs(radio);

            camera.save();
            camera.translate(0, 0, parentView.getMeasuredWidth() * radioABS / 4.0f);
            if (isPortrait) {
                camera.rotateY(30.0F * radio);
            } else {
                camera.rotateX(-30.0F * radio);
            }
            camera.getMatrix(matrix);
            camera.restore();

            float scale = radioABS / 5.0f;
            if (radio < 0) {
                if (isPortrait) {
                    matrix.preTranslate(-childMeasuredWidth / 2.0f, -childMeasuredHeight / 2.0F);
                    matrix.postTranslate(offset + (1 + scale) * childMeasuredWidth / 2.0f, childMeasuredHeight / 2.0F);
                } else {
                    matrix.preTranslate(-childMeasuredHeight / 2.0F, -childMeasuredWidth / 2.0f);
                    matrix.postTranslate(childMeasuredHeight / 2.0F, offset + (1 + scale) * childMeasuredWidth / 2.0F);
                }
            } else {
                if (isPortrait) {
                    matrix.preTranslate(-childMeasuredWidth / 2.0f, -childMeasuredHeight / 2.0F);
                    matrix.postTranslate(offset + (1 + scale) * childMeasuredWidth / 2.0f, childMeasuredHeight / 2.0F);
                } else {
                    matrix.preTranslate(-childMeasuredHeight / 2.0F, -childMeasuredWidth / 2.0f);
                    matrix.postTranslate(childMeasuredHeight / 2.0F, offset + (1 + scale) * childMeasuredWidth / 2.0F);
                }
            }

            decorAlpha = 0.5f - Math.abs((radioABS - 0.5f));

            float alpha = (1.0f - radioABS) / 2 + 0.5F;
            childTransformation.setAlpha(alpha);
            childTransformation.setTransformationType(Transformation.TYPE_BOTH);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            return null;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return true;
        }

        @Override
        public boolean hasScreenDecor() {
            return true;
        }

        @Override
        public float getScreenDecorAlpha() {
            return decorAlpha;
        }

        @Override
        public int getAlphaTransitionType() {
            return 1;
        }
    }

    private static final class IconCollection extends EffectInfo {

        IconCollection() {
            super(TYPE_ICON_COLLECTION, "pref_k_transformation_icon_collection", "@string/transformation_type_icon_collection");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            Matrix matrix = childTransformation.getMatrix();

            if (offset != 0) {
                if (isPortrait) {
                    matrix.postTranslate(offset, 0);
                } else {
                    matrix.postTranslate(0, offset);
                }
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();
            float parentMeasuredWidth = parentView.getMeasuredWidth();
            float parentMeasuredHeight = parentView.getMeasuredHeight();

            float scale = 0.0F;
            float distanceWidth;
            float distanceHeight;

            sMatrix.reset();
            Matrix matrix = sMatrix;

            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            distanceWidth = parentView.getLeft() + parentMeasuredWidth / 2.0F - childView.getLeft() - childMeasuredWidth / 2.0F;
            distanceHeight = parentView.getTop() + parentMeasuredHeight / 2.0F - childView.getTop() - childMeasuredHeight / 2.0F;

            if (isPortrait) {
                distanceWidth -= itemInfo.screen * parentMeasuredWidth;
            } else {
                distanceHeight -= itemInfo.screen * parentMeasuredHeight;
            }

            if (itemInfo.spanX > 1 || itemInfo.spanY > 1) {
                scale = 1.0F / Math.max(itemInfo.spanX, itemInfo.spanY);
            }

            final float radioABS = Math.abs(radioX);
            if (radioABS <= 0.5) {
                float extraWidth = 0.0f;
                float extraHeight = 0.0f;
                if (scale != 0.0F) {
                    scale = (scale - 1.0F) * Math.abs(radioX) * 2.0F + 1.0F;
                    matrix.preScale(scale, scale);
                    matrix.preTranslate(-childMeasuredWidth / 2.0F, -childMeasuredHeight / 2.0F);
                    extraWidth = childMeasuredWidth / 2.0F;
                    extraHeight = childMeasuredHeight / 2.0F;
                }
                matrix.postTranslate(extraWidth + radioABS * 2.0F * distanceWidth, extraHeight + radioABS * 2.0F * distanceHeight);
            } else {
                float extraWidth = 0.0f;
                float extraHeight = 0.0f;
                if (scale != 0.0F) {
                    scale = (scale - 1.0F) * (1 - radioABS) * 2.0F + 1.0F;
                    matrix.preScale(scale, scale);
                    matrix.preTranslate(-childMeasuredWidth / 2.0F, -childMeasuredHeight / 2.0F);
                    extraWidth = childMeasuredWidth / 2.0F;
                    extraHeight = childMeasuredHeight / 2.0F;
                }
                if (isPortrait) {
                    matrix.postTranslate(extraWidth + radioABS * 2.0F * distanceWidth, extraHeight + (1 - radioABS) * 2.0F * distanceHeight);
                } else {
                    matrix.postTranslate(extraWidth + (1 - radioABS) * 2.0F * distanceWidth, extraHeight + radioABS * 2.0F * distanceHeight);
                }
            }

            return apply(canvas, childView, drawingTime, isCellLayoutNeedAntiAlias() ? sAntiAliesFilter : null, matrix, 1.0F, callback);
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }
    }

    private static final class RollAway extends EffectInfo {

        RollAway() {
            super(TYPE_ROLL_AWAY, "pref_k_transformation_roll_away", "@string/transformation_type_roll_away");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            if (radio <= -0.5F) {
                matrix.postRotate((1 + radio) * 2.0F * -90.0F, childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
            } else if (radio >= 0.5F) {
                matrix.postRotate((1 - radio) * 2.0F * 90.0F, childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
            }

            if (offset != 0) {
                if (isPortrait) {
                    matrix.postTranslate(offset, 0);
                } else {
                    matrix.postTranslate(0, offset);
                }
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            int count = parentView.getChildCount();

            if (count == 0) {
                return null;
            }

            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();
            float parentMeasuredWidth = parentView.getMeasuredWidth();
            float parentMeasuredHeight = parentView.getMeasuredHeight();

            float scale = 0.0F;
            float distanceWidth;
            float distanceHeight;
            float distanceDegrees;

            sMatrix.reset();
            Matrix matrix = sMatrix;

            int cellX = itemInfo.cellX;
            int cellY = itemInfo.cellY;

            int index = 0;

            for (int i = 0; i < count; i++) {
                View view = parentView.getChildAt(i);

                if (view.getTag() instanceof ItemInfo) {
                    int otherCellX = ((ItemInfo) view.getTag()).cellX;
                    int otherCellY = ((ItemInfo) view.getTag()).cellY;

                    if (otherCellY < cellY) {
                        index++;
                    } else if (otherCellY == cellY && otherCellX < cellX) {
                        index++;
                    }
                }
            }

            float angle = 360.0F / count * index;
            CellLayout cellLayout = (CellLayout) parentView;
            float r = cellLayout.getCellShortAxisDistance() * 1.25F;
            distanceDegrees = 90.0F + angle;

            if (radioX >= 0.5F) {
                distanceDegrees += 90.0F;
                angle += 90.0F;
                if (angle >= 360.0F) {
                    angle -= 360.0F;
                }
            } else if (radioX <= -0.5f) {
                distanceDegrees -= 90.0f;
                angle -= 90.0f;
                if (angle < 0f) {
                    angle += 360.0f;
                }
            }

            if (itemInfo.spanX > 1 || itemInfo.spanY > 1) {
                scale = 1.0F / Math.max(itemInfo.spanX, itemInfo.spanY);
            }

            distanceWidth = parentView.getLeft() + parentMeasuredWidth / 2.0F - childView.getLeft() - childMeasuredWidth / 2.0F
                    + (float) (r * Math.cos((angle <= 180.0F ? angle : 360.0F - angle) * Math.PI / 180.0));
            distanceHeight = parentView.getTop() + parentMeasuredHeight / 2.0F - childView.getTop() - childMeasuredHeight / 2.0F
                    - (float) (r * Math.sin((angle <= 180.0F ? angle : angle - 360.0F) * Math.PI / 180.0));

            if (isPortrait) {
                distanceWidth -= itemInfo.screen * parentMeasuredWidth;
            } else {
                distanceHeight -= itemInfo.screen * parentMeasuredHeight;
            }

            if (Math.abs(radioX) <= 0.5) {
                if (scale != 0.0F) {
                    if (Math.abs(radioX) <= 0.25) {
                        scale = (scale - 1.0F) * Math.abs(radioX) * 4.0F + 1.0F;
                    }
                    matrix.postTranslate(-childMeasuredWidth / 2.0F, -childMeasuredHeight / 2.0F);
                    matrix.postScale(scale, scale);
                    matrix.postTranslate(childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
                }
                matrix.postRotate(-Math.abs(radioX) * 2.0F * distanceDegrees, childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
                matrix.postTranslate(Math.abs(radioX) * 2.0F * distanceWidth, Math.abs(radioX) * 2.0F * distanceHeight);
            } else {
                if (scale != 0.0F) {
                    matrix.postTranslate(-childMeasuredWidth / 2.0F, -childMeasuredHeight / 2.0F);
                    matrix.postScale(scale, scale);
                    matrix.postTranslate(childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
                }
                matrix.postRotate(-distanceDegrees, childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);
                matrix.postTranslate(distanceWidth, distanceHeight);
            }

            return apply(canvas, childView, drawingTime, isCellLayoutNeedAntiAlias() ? sAntiAliesFilter : null, matrix, 1.0F, callback);
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }
    }

    private static final class Chord extends EffectInfoWithWidgetCache {

        Chord() {
            super(TYPE_CHORD, "pref_k_transformation_chord", "@string/transformation_type_chord");
            if (DeviceUtils.isAfterApiLevel18()) {
                mIsUsingWidgetCache = true;
            }
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            if (isPortrait) {
                matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
            } else {
                matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            boolean ret = true;
            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            if (itemInfo.spanX <= 1) {
                beforeTransformChild(canvas, childView.getContext(), childView.getLeft(), childView.getTop());
                transformChild(parentView, childView, radioX, offset, currentScreen, itemInfo.cellX, canvas);
                ret = afterTransformChild(canvas, childView, drawingTime, -childView.getLeft(), -childView.getTop(), null, callback);
                return ret;
            } else {

                if (mIsUsingWidgetCache) {
                    cacheBitmap = getWidgetBitmap(childView, itemInfo.screen, childView.getWidth(), childView.getHeight(), 0, 0);
                }

                if (radioX >= 0) {
                    for (int i = 0; i < itemInfo.spanX; i++) {
                        beforeTransformChild(canvas, childView.getContext(), childView.getLeft() + childView.getWidth() / itemInfo.spanX * i, childView.getTop());
                        transformChild(parentView, childView, radioX, offset, currentScreen, itemInfo.cellX + i, canvas);
                        ret = afterTransformChild(canvas, childView, drawingTime, -childView.getLeft() - childView.getWidth() / itemInfo.spanX * i, -childView.getTop(), new Rect(childView.getLeft()
                                + childView.getWidth() / itemInfo.spanX * i, childView.getTop(), childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth()
                                / itemInfo.spanX, childView.getTop() + childView.getHeight()), callback);
                    }
                } else {
                    for (int i = itemInfo.spanX - 1; i >= 0; i--) {
                        beforeTransformChild(canvas, childView.getContext(), childView.getLeft() + childView.getWidth() / itemInfo.spanX * i, childView.getTop());
                        transformChild(parentView, childView, radioX, offset, currentScreen, itemInfo.cellX + i, canvas);
                        ret = afterTransformChild(canvas, childView, drawingTime, -childView.getLeft() - childView.getWidth() / itemInfo.spanX * i, -childView.getTop(), new Rect(childView.getLeft()
                                + childView.getWidth() / itemInfo.spanX * i, childView.getTop(), childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth()
                                / itemInfo.spanX, childView.getTop() + childView.getHeight()), callback);
                    }
                }
            }
            return ret;
        }

        private void beforeTransformChild(Canvas canvas, Context context, float translateX, float translateY) {
            canvas.save();
            if (LauncherSettings.getRenderPerformanceMode(context) == LauncherSettings.RENDER_PERFORMANCE_MODE_PREFER_QUALITY) {
                canvas.setDrawFilter(sAntiAliesFilter);
            }
            canvas.translate(translateX, translateY);
        }

        private boolean afterTransformChild(Canvas canvas, View childView, long drawingTime, float translateX, float translateY, Rect r, Callback callback) {
            boolean ret = true;
            canvas.translate(translateX, translateY);

            if (mIsUsingWidgetCache && r != null && cacheBitmap != null) {
                Rect src = new Rect(r);
                src.offset(-childView.getLeft(), -childView.getTop());
                canvas.drawBitmap(cacheBitmap, src, r, mCachePaint);
            } else {
                if (r != null) {
                    canvas.clipRect(r, Op.REPLACE);
                    childView.invalidate();
                }
                ret = callback.onEffectApplied(canvas, childView, drawingTime);
            }

            canvas.restore();

            return ret;
        }

        private float transformChild(ViewGroup parentView, View childView, float radio, int offset, int currentScreen, int index, Canvas canvas) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            sMatrix.reset();
            Matrix matrix = sMatrix;

            ItemInfo itemInfo = (ItemInfo) childView.getTag();
            childMeasuredWidth = childMeasuredWidth / itemInfo.spanX;
            childMeasuredHeight = childMeasuredHeight / itemInfo.spanY;

            boolean tranformation = false;

            float degree = 0f;
            float begin = 0f;
            float end = 0f;
            float mid = 0f;

            CellLayout cellLayout = (CellLayout) parentView;
            if (itemInfo.screen == currentScreen) {
                if (radio >= 0) {
                    begin = (cellLayout.getCountX() - index - 1.0f) / cellLayout.getCountX();
                    end = (cellLayout.getCountX() - index - 0f) / cellLayout.getCountX();
                    mid = (end - begin) / 2.0f + begin;
                    if (radio >= begin && radio < mid) {
                        tranformation = true;
                        degree = -90f * (radio - begin) / (mid - begin);
                    } else if (radio < begin) {
                        tranformation = true;
                        degree = 0f;
                    } else {
                        tranformation = true;
                        degree = 90f;
                    }
                } else {
                    begin = -(index + 0f) / cellLayout.getCountX();
                    end = -(index + 1.0f) / cellLayout.getCountX();
                    mid = (end - begin) / 2.0f + begin;
                    if (radio > mid && radio <= begin) {
                        tranformation = true;
                        degree = 90f * (radio - begin) / (mid - begin);
                    } else if (radio > begin) {
                        tranformation = true;
                        degree = 0f;
                    } else {
                        tranformation = true;
                        degree = 90f;
                    }
                }
            } else {
                if (radio >= 0) {
                    begin = (cellLayout.getCountX() - index - 0.0f) / cellLayout.getCountX();
                    end = (cellLayout.getCountX() - index - 1.0f) / cellLayout.getCountX();
                    mid = (begin - end) / 2.0f + end;
                    if (radio <= mid && radio > end) {
                        tranformation = true;
                        degree = -90f + 90f * (radio - mid) / (end - mid);
                    } else if (radio < mid) {
                        degree = 0f;
                        tranformation = true;
                    } else {
                        tranformation = true;
                        degree = 90f;
                    }
                } else {
                    begin = -(index + 1.0f) / cellLayout.getCountX();
                    end = -(index + 0.0f) / cellLayout.getCountX();
                    mid = (begin - end) / 2.0f + end;
                    if (radio < end && radio >= mid) {
                        tranformation = true;
                        degree = 90f - 90f * (radio - mid) / (end - mid);
                    } else if (radio < end) {
                        tranformation = true;
                        degree = 90f;
                    } else {
                        tranformation = true;
                        degree = 0f;
                    }
                }
            }

            if (tranformation) {
                camera.save();
                camera.rotateY(degree);
                camera.getMatrix(matrix);
                camera.restore();

                matrix.preTranslate(-childMeasuredWidth / 2.0F, -childMeasuredHeight / 2.0F);
                matrix.postTranslate(childMeasuredWidth / 2.0F, childMeasuredHeight / 2.0F);

                canvas.concat(matrix);
            }
            return degree;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }

        @Override
        public boolean isScreenFused() {
            return true;
        }
    }

    private static final class Extrusion extends EffectInfo {

        Extrusion() {
            super(TYPE_EXTRUSION, "pref_k_transformation_extrusion", "@string/transformation_type_extrusion");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();
            final Matrix matrix = childTransformation.getMatrix();

            if (isPortrait) {
                matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
            } else {
                matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
            }

            if (radio > 0) { // left
                radio = 1.0f - radio;
                matrix.setScale(radio, 1, childMeasuredWidth, childMeasuredHeight / 2);
            } else { // right
                radio = 1.0f + radio;
                matrix.setScale(radio, 1, 0, childMeasuredHeight / 2);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            return null;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return true;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return true;
        }
    }

    private static final class Snake extends EffectInfo {

        Snake() {
            super(TYPE_SNAKE, "pref_k_transformation_snake", "@string/transformation_type_snake");
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            if (isPortrait) {
                matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
            } else {
                matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            int count = parentView.getChildCount();

            if (count == 0) {
                return null;
            }

            int cellX = itemInfo.cellX;
            int cellY = itemInfo.cellY;

            CellLayout cellLayout = (CellLayout) parentView;
            int countX = cellLayout.getCountX();
            int countY = cellLayout.getCountY();

            if (cellX < 0 || cellX >= countX || cellY < 0 || cellY >= countY) {
                return null;
            }

            // TODO isPortrait
            float distanceWidth = 0;
            float distanceHeight = 0;
            float alpha = 1.0f;

            sMatrix.reset();
            Matrix matrix = sMatrix;

            int countAll = countX * countY;
            int cellLongAxisDistance = cellLayout.getCellLongAxisDistance();
            int cellShortAxisDistance = cellLayout.getCellShortAxisDistance();

            boolean flag = true;
            int xSteps = 0;
            int ySteps = 0;
            if (radioX > 0) {
                ySteps += countY;
                radioX -= 1;
                flag = false;
            }

            int[][] snakeSteps = getSnakeSteps(countX, countY)[cellX][cellY];

            int index = (int) (Math.abs(radioX) * countAll);
            for (int i = 0; i < index; i++) {
                xSteps += snakeSteps[i][0];
                ySteps += snakeSteps[i][1];
            }

            if (!flag && countY % 2 == 1) {
                for (int i = 0; i < countX; i++) {
                    xSteps += snakeSteps[index + i][0];
                }
            }

            int position = (countY - 1 - cellY) * countX + (cellY % 2 == 0 ? cellX : (countX - 1 - cellX)) + 1;
            int offsetX;
            int offsetY;
            if (position + index == countAll) {
                if (flag) {
                    offsetX = 1;
                    offsetY = 0;
                } else {
                    if (countY % 2 == 0) {
                        xSteps++;
                    } else {
                        xSteps--;
                    }
                    ySteps--;
                    if (countY % 2 == 0) {
                        offsetX = -1;
                    } else {
                        offsetX = 1;
                    }
                    offsetY = 0;
                }
            } else {
                if (!flag && countY % 2 == 1) {
                    offsetX = snakeSteps[index + countX][0];
                } else {
                    offsetX = snakeSteps[index][0];
                }
                offsetY = snakeSteps[index][1];
            }

            if (cellY + ySteps >= countY || cellY + ySteps < 0) {
                alpha = 0f;
            }

            if (alpha == 0f) {
                return Boolean.FALSE;
            }

            distanceWidth = xSteps * cellShortAxisDistance;
            distanceHeight = ySteps * cellLongAxisDistance;

            distanceWidth += offsetX * cellShortAxisDistance * (Math.abs(radioX) * countAll - index);
            distanceHeight += offsetY * cellLongAxisDistance * (Math.abs(radioX) * countAll - index);

            matrix.postTranslate(distanceWidth, distanceHeight);

            return apply(canvas, childView, drawingTime, isCellLayoutNeedAntiAlias() ? sAntiAliesFilter : null, matrix, alpha, callback);
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }

        @Override
        public boolean isScreenFused() {
            return true;
        }

        private static int[][][][] sSnakeSteps;

        private static int[][][][] getSnakeSteps(int countX, int countY) {
            if (sSnakeSteps == null || sSnakeSteps.length != countX || sSnakeSteps[0].length != countY) {
                sSnakeSteps = new int[countX][countY][countX * countY + countX + 1][2];
                for (int i = 0; i < countY; i++) {
                    for (int j = 0; j < countX; j++) {
                        int index = 0;
                        if (i % 2 == 0) {
                            for (; index < countX - 1 - j; index++) {
                                sSnakeSteps[j][i][index][0] = 1;
                                sSnakeSteps[j][i][index][1] = 0;
                            }
                        } else {
                            for (; index < j; index++) {
                                sSnakeSteps[j][i][index][0] = -1;
                                sSnakeSteps[j][i][index][1] = 0;
                            }
                        }
                        for (int k = 0; k < sSnakeSteps[j][i].length - index; k++) {
                            if (k % countX == 0) {
                                sSnakeSteps[j][i][index + k][0] = 0;
                                sSnakeSteps[j][i][index + k][1] = -1;
                            } else {
                                if (i % 2 == 0) {
                                    sSnakeSteps[j][i][index + k][0] = (k / countX % 2 == 0) ? -1 : 1;
                                } else {
                                    sSnakeSteps[j][i][index + k][0] = (k / countX % 2 == 0) ? 1 : -1;
                                }
                                sSnakeSteps[j][i][index + k][1] = 0;
                            }
                        }
                    }
                }
            }
            return sSnakeSteps;
        }
    }

    private static final class Cylinder extends EffectInfoWithWidgetCache {

        private static final float MIN_ALPHA = 0.2f;

        // 动画开始时，渐变区域
        private static final float TRANSITION_POSITIVE = 0.01f;

        // 动画结束时，渐变区域
        private static final float TRANSITION_NEGATIVE = 0.5f;

        Cylinder() {
            super(TYPE_CYLINDER, "pref_k_transformation_cylinder", "@string/transformation_type_cylinder");
            if (DeviceUtils.isAfterApiLevel18()) {
                mIsUsingWidgetCache = true;
            }
            if (interpolator == null) {
                interpolator = new AccelerateInterpolator(16);
            }
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            float radioABS = Math.abs(radio);
            float gradual = 1f;
            if (radioABS > 0.9f) {
                if (!RuntimeConfig.sLauncherInTouching || RuntimeConfig.sLauncherInTouching && !mCylinderform) {
                    gradual = (1 - radioABS) / 0.05f - 1;
                }
            }
//
            if (radioABS > 0.9f && gradual != 1f) {
                matrix.postScale(gradual, 1f, radio > 0 ? childMeasuredWidth : 0, 0);
            }

            if (isPortrait) {
                matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
            } else {
                matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            boolean ret = true;
            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            if (LauncherSettings.getRenderPerformanceMode(parentView.getContext()) == LauncherSettings.RENDER_PERFORMANCE_MODE_PREFER_QUALITY) {
                canvas.setDrawFilter(sAntiAliesFilter);
            }

            if (itemInfo.spanX <= 1) {
                ret = transformChild(parentView, childView, radioX, itemInfo.cellX, 0, 1, canvas, drawingTime, callback, null, isPortrait, itemInfo.screen, currentScreen);
            } else {

                if (mIsUsingWidgetCache) {
                    cacheBitmap = getWidgetBitmap(childView, itemInfo.screen, childView.getWidth(), childView.getHeight(), 0, 0);
                }

                if (radioX >= 0) {
                    for (int i = 0; i < itemInfo.spanX; i++) {
                        ret = transformChild(parentView, childView, radioX, itemInfo.cellX + i, i, itemInfo.spanX, canvas, drawingTime, callback,
                                new Rect(childView.getLeft() + childView.getWidth() / itemInfo.spanX * i,
                                        childView.getTop(),
                                        childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth() / itemInfo.spanX,
                                        childView.getTop() + childView.getHeight()),
                                        isPortrait,
                                        itemInfo.screen,
                                        currentScreen);
                    }
                } else {
                    for (int i = itemInfo.spanX - 1; i >= 0; i--) {
                        ret = transformChild(parentView, childView, radioX, itemInfo.cellX + i, i, itemInfo.spanX, canvas, drawingTime, callback,
                                new Rect(childView.getLeft() + childView.getWidth() / itemInfo.spanX * i,
                                        childView.getTop(),
                                        childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth() / itemInfo.spanX,
                                        childView.getTop() + childView.getHeight()),
                                        isPortrait,
                                        itemInfo.screen,
                                        currentScreen);
                    }
                }
            }

            return ret;
        }

        private boolean mCurrentGradualed = false;

        private boolean mCurrentGradualing = false;

        private boolean mNextGradualing = false;

        private boolean mNextGradualed = false;

        // 用户手指是否在按下
        private boolean mTouching = false;

        // 用户手指按下动作被忽略了
        private boolean mTouchDownIgnored = false;

        private int mTouchDownScreenIndex = -1;

        @Override
        public void onTouchDown(boolean isScrolling) {
            super.onTouchDown(isScrolling);

            mTouching = true;

            if (isScrolling) {
                mTouchDownIgnored = true;
                return;
            }

            mCurrentGradualed = false;
            mCurrentGradualing = false;

            mNextGradualed = false;
            mNextGradualing = false;

            mTouchDownScreenIndex = -1;
        }

        @Override
        public void onTouchUpCancel(boolean isScrolling) {
            mTouching = false;

            if (mTouchDownIgnored && isScrolling) {
                mTouchDownIgnored = false;
                return;
            }

            mCurrentGradualed = false;
            mNextGradualed = false;
        }

        // 圆柱是否形成
        private boolean mCylinderform;

        private boolean transformChild(ViewGroup parentView, View childView, float radio, int cellX, int index, int spanX, Canvas canvas, long drawingTime, Callback callback, Rect r,
                boolean isPortrait, int screenIndex, int currentScreen) {
            final float parentMeasuredWidth = parentView.getMeasuredWidth();
            final float parentMeasuredHeight = parentView.getMeasuredHeight();
            final CellLayout cellLayout = (CellLayout) parentView;
            final Context context = cellLayout.getContext();
            int countX = cellLayout.getCountX();
            float gradualFactor = 1F;
            float alphalFactor = 1F;
            float t = mTouching ? TRANSITION_POSITIVE: TRANSITION_NEGATIVE;
            float radioABS = Math.abs(radio);

            if (mTouchDownScreenIndex == -1 && radioABS < t && screenIndex == currentScreen) {
                mTouchDownScreenIndex = screenIndex;
            }

            if (radioABS < t) {
//                float cellFactor = (mTouching || screenIndex == mTouchDownScreenIndex) ? 1 : (radio > 0 ? (countX - cellX) * 1f / countX : (cellX + 1f) / countX);

                if (!mCurrentGradualed) {
//                    gradualFactor = (float) (1 - Math.pow(1 - radioABS / t, 2)) * cellFactor * 0.9f;
                    mCurrentGradualing = true;
                } else if (!mNextGradualed) {
                    if (screenIndex != currentScreen) {
//                        gradualFactor = (float) (1 - Math.pow(1 - radioABS / t, 2)) * cellFactor * 0.9f;
                        mNextGradualing = true;
                    }
                }
            } else {
                if (radioABS < (1 - t)) {
                    if (mCurrentGradualing) {
                        mCurrentGradualed = true;
                        mCurrentGradualing = false;
                    } else if (mNextGradualing) {
                        mNextGradualed = true;
                        mNextGradualing = false;
                    }
                }

                if (radioABS > (1 - t) && (mCurrentGradualing || mNextGradualing)) {
//                    gradualFactor = 0.9f;
                    alphalFactor = -radioABS / t + 1 / t;
                }
            }

            sMatrix.reset();
            final Matrix matrix = sMatrix;
			float yDegree = -90 + 180f / (countX * 2) + cellX * 180f / countX
					- 180f * radio;
//			if (yDegree > 0) {
//			    yDegree = (yDegree - 180) * 0.9f + 180;
//			} else {
//			    yDegree = (yDegree + 180) * 0.9f - 180;
//			}
            float alpha = (float) (1 + FloatMath.cos(ONE_DEGREE * yDegree));
            if (alpha > 1) {
                alpha = 1;
                if (radioABS > 0.9f) {
                    alpha = 10 * (1.0f - radioABS);
                }
            } else if (alpha < MIN_ALPHA * 2) {
                alpha = MIN_ALPHA * 2;
            }

            if (radioABS < 0.1f) {
                if (!RuntimeConfig.sLauncherInTouching || RuntimeConfig.sLauncherInTouching && !mCylinderform) {
                    gradualFactor = interpolator.getInterpolation(radioABS / 0.1f);
                }
                if (!RuntimeConfig.sLauncherInTouching)
                    mCylinderform = false;
            } else {
                if (alpha == 1.0f)
                    mCylinderform = true;
            }

            boolean ret = true;

//            float radius = parentMeasuredWidth / 2f * 0.9f;
//            float cc = childView.getLeft() + childView.getMeasuredWidth() * (index * 2 + 1) / (2 * spanX);
//            float cx = (parentMeasuredWidth / 2f + radius * FloatMath.sin(yDegree * ONE_DEGREE) - cc) * gradualFactor + cc;
//            float ry = yDegree * gradualFactor;
//            float tz = radius * (1 - FloatMath.cos(ry * ONE_DEGREE));
//            float tx = cc - cx;
//            camera.save();
//            camera.translate(0, 0, tz);
//            camera.rotateY(ry);
//            camera.getMatrix(matrix);
//            camera.restore();
//
//            canvas.save();
//            canvas.translate(cx, parentMeasuredHeight / 2f);
//            canvas.concat(matrix);
//            canvas.translate(-cx, -parentMeasuredHeight / 2f);
//            canvas.translate(-tx, 0);

//            float transFactor = Utils.isPortrait(context) ? (useHighScreenDensity() ? 3.74f : 2.5f) : (useHighScreenDensity() ? 6.65f : 4.5f);
            camera.save();
            camera.rotateY(yDegree * gradualFactor);
//            camera.translate(0, 0, -parentMeasuredWidth / transFactor);
            camera.translate(0, 0, -288f);

            camera.getMatrix(matrix);
            camera.restore();

            float scale = 5f / (context.getResources().getDisplayMetrics().widthPixels / 80f);
            matrix.preScale(scale, scale);

            canvas.save();
            canvas.scale(1 / scale / 2f, 1 / scale / 2f, parentMeasuredWidth / 2, parentMeasuredHeight / 2);

            canvas.translate(parentMeasuredWidth / 2, parentMeasuredHeight / 2);
            canvas.concat(matrix);
            canvas.translate(-parentMeasuredWidth / 2, -parentMeasuredHeight / 2);

            final float transX = parentMeasuredWidth / 2 - childView.getLeft() - childView.getMeasuredWidth() * (index * 2 + 1) / (2 * spanX);
            canvas.translate(transX * gradualFactor, 0);

//            float alpha = Math.abs(yDegree) > 90 ? (1f - Math.abs(((Math.abs(yDegree) % 180) - 90)) * (1f - MIN_ALPHA) / 90f) : 1f;

            if (mIsUsingWidgetCache && r != null && cacheBitmap != null) {
                Rect src = new Rect(r);
                src.offset(-childView.getLeft(), -childView.getTop());
                if (alpha < 1.0f) {
                    mCachePaint.setAlpha((int) (alpha * gradualFactor * alphalFactor * 255f));
                } else {
                    mCachePaint.setAlpha(255);
                }
                canvas.drawBitmap(cacheBitmap, src, r, mCachePaint);
            } else {
                boolean alphaApplied = false;
                if (alpha < 1.0F) {
                    if (callback.onApplyAlpha(childView, alpha * gradualFactor * alphalFactor)) {
                        alphaApplied = true;
                    } else {
                        final int cl = childView.getLeft();
                        final int ct = childView.getTop();
                        final int cr = childView.getRight();
                        final int cb = childView.getBottom();
                        //TODO 4.3中此方法导致速度巨慢，暂时屏蔽
                        canvas.saveLayerAlpha(cl, ct, cr, cb, (int) (255 * alpha * gradualFactor * alphalFactor), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG);
                    }
                } else {
                    callback.onApplyAlpha(childView, 1f);
                }

                if (r != null) {
                    canvas.clipRect(r, Op.REPLACE);
                    childView.invalidate();
                }
                ret = callback.onEffectApplied(canvas, childView, drawingTime);

                if (alpha < 1.0F && !alphaApplied) {
                    canvas.restore();
                }
            }

            canvas.restore();
            return ret;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }

        @Override
        public boolean drawChildrenOrderByMoveDirection() {
            return true;
        }

        public boolean useDefaultScroller() {
            return true;
        }

        @Override
        public boolean isScreenFused() {
            return true;
        }

        @Override
        public int getAlphaTransitionType() {
            return 2;
        }
    }
    private static final float ONE_DEGREE = 0.01745329F;

    private static final class Sphere extends EffectInfoWithWidgetCache {

        private static final float MAX_X_DEGREE = 50f;

        private static final float MIN_ALPHA = 0.05f;

        // 动画开始时，渐变区域
        private static final float TRANSITION_POSITIVE = 0.01f;

        // 动画结束时，渐变区域
        private static final float TRANSITION_NEGATIVE = 0.2f;

        // Y方向滑动比例是否需要重置
        private boolean mRadioYNeedInit;

        // Y方向之前的滑动比例
        private float mLastRadioY;

        private int mTouchDownScreenIndex = -1;

        private boolean mCurrentGradualed = false;

        private boolean mCurrentGradualing = false;

        private boolean mNextGradualing = false;

        private boolean mNextGradualed = false;

        // 用户手指是否在按下
        private boolean mTouching = false;

        // 用户手指按下动作被忽略了
        private boolean mTouchDownIgnored = false;

        // 球是否形成
        private boolean mSphereform;

        Sphere() {
            super(TYPE_SPHERE, "pref_k_transformation_sphere", "@string/transformation_type_sphere");
            if (DeviceUtils.isAfterApiLevel18()) {
                mIsUsingWidgetCache = true;
            }
            if (interpolator == null) {
                interpolator = new DecelerateInterpolator();
            }
        }

        @Override
        public boolean getWorkspaceChildStaticTransformation(ViewGroup parentView, View childView, Transformation childTransformation, float radio, int offset, int currentScreen, boolean isPortrait) {
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            Matrix matrix = childTransformation.getMatrix();

            if (isPortrait) {
                matrix.postTranslate(childMeasuredWidth * radio + offset, 0.0F);
            } else {
                matrix.postTranslate(0.0F, childMeasuredHeight * radio + offset);
            }

            childTransformation.setTransformationType(Transformation.TYPE_MATRIX);

            return true;
        }

        @Override
        public Boolean applyCellLayoutChildTransformation(ViewGroup parentView, Canvas canvas, View childView, long drawingTime, Callback callback, float radioX, int offset, float radioY,
                int currentScreen, boolean isPortrait) {
            boolean ret = true;
            ItemInfo itemInfo = (ItemInfo) childView.getTag();

            if (LauncherSettings.getRenderPerformanceMode(parentView.getContext()) == LauncherSettings.RENDER_PERFORMANCE_MODE_PREFER_QUALITY) {
                canvas.setDrawFilter(sAntiAliesFilter);
            }

            if (itemInfo.spanX <= 1) {
                ret = transformChild(parentView, childView, radioX, radioY,
                        itemInfo.cellX, 0, 1,
                        itemInfo.cellY, 0, 1,
                        canvas, drawingTime, callback, null, isPortrait, itemInfo.screen, currentScreen);
            } else {

                if (mIsUsingWidgetCache) {
                    cacheBitmap = getWidgetBitmap(childView, itemInfo.screen, childView.getWidth(), childView.getHeight(), 0, 0);
                }

                if (radioX >= 0) {
                    for (int i = 0; i < itemInfo.spanX; i++) {
                        for (int j = 0; j < itemInfo.spanY; j++) {
                            ret = transformChild(parentView, childView, radioX, radioY,
                                    itemInfo.cellX + i, i, itemInfo.spanX,
                                    itemInfo.cellY + j, j, itemInfo.spanY,
                                    canvas, drawingTime, callback,
                                    new Rect(childView.getLeft() + childView.getWidth() / itemInfo.spanX * i,
                                            childView.getTop() + childView.getHeight() / itemInfo.spanY * j,
                                            childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth() / itemInfo.spanX,
                                            childView.getTop() + childView.getHeight() / itemInfo.spanY * j + childView.getHeight() / itemInfo.spanY),
                                    isPortrait, itemInfo.screen, currentScreen);
                        }
                    }
                } else {
                    for (int i = itemInfo.spanX - 1; i >= 0; i--) {
                        for (int j = itemInfo.spanY - 1; j >= 0; j--) {
                            ret = transformChild(parentView, childView, radioX, radioY,
                                    itemInfo.cellX + i, i, itemInfo.spanX,
                                    itemInfo.cellY + j, j, itemInfo.spanY,
                                    canvas, drawingTime, callback,
                                    new Rect(childView.getLeft() + childView.getWidth() / itemInfo.spanX * i,
                                            childView.getTop() + childView.getHeight() / itemInfo.spanY * j,
                                            childView.getLeft() + childView.getWidth() / itemInfo.spanX * i + childView.getWidth() / itemInfo.spanX,
                                            childView.getTop() + childView.getHeight() / itemInfo.spanY * j + childView.getHeight() / itemInfo.spanY),
                                    isPortrait, itemInfo.screen, currentScreen);
                        }
                    }
                }
            }

            return ret;
        }

        @Override
        public void onTouchDown(boolean isScrolling) {
            super.onTouchDown(isScrolling);

            mTouching = true;
            if (isScrolling) {
                mTouchDownIgnored = true;
                return;
            }

            mCurrentGradualed = false;
            mCurrentGradualing = false;

            mNextGradualed = false;
            mNextGradualing = false;

            mRadioYNeedInit = true;
            mTouchDownScreenIndex = -1;
        }

        @Override
        public void onTouchUpCancel(boolean isScrolling) {
            mTouching = false;
            if (mTouchDownIgnored && isScrolling) {
                mTouchDownIgnored = false;
                return;
            }

            mRadioYNeedInit = false;

            mCurrentGradualed = false;
            mNextGradualed = false;

            mTouchDownIgnored = false;
        }

        private boolean transformChild(ViewGroup parentView, View childView, float radio, float radioY, int cellX, int indexX, int spanX, int cellY, int indexY, int spanY, Canvas canvas, long drawingTime,
                Callback callback, Rect r, boolean isPortrait, int screenIndex, int currentScreen) {
            final float parentMeasuredWidth = parentView.getMeasuredWidth();
            final float parentMeasuredHeight = parentView.getMeasuredHeight();
            final CellLayout cellLayout = (CellLayout) parentView;
            final Context context = cellLayout.getContext();
            sMatrix.reset();
            final Matrix matrix = sMatrix;

            int countX = cellLayout.getCountX();
            int countY = cellLayout.getCountY();
            cellY = countY - 1 - cellY;

            float gradualFactor = 1F;
            float alphalFactor = 1F;
            float t = mTouching ? TRANSITION_POSITIVE: TRANSITION_NEGATIVE;
            float radioAbs = Math.abs(radio);

            float radioYDiff = 0f;
            if (mTouchDownScreenIndex == -1 && radioAbs < t && screenIndex == currentScreen) {
                mTouchDownScreenIndex = screenIndex;
            }

            if (mRadioYNeedInit) {
                mLastRadioY = radioY;
                mRadioYNeedInit = false;
            } else {
                radioYDiff = radioY - mLastRadioY;
            }

            if (radioAbs < t) {

                if (!mCurrentGradualed) {
                    mCurrentGradualing = true;
                } else if (!mNextGradualed) {
                    if (screenIndex != currentScreen) {
                        mNextGradualing = true;
                    }
                }
            } else {
                if (radioAbs < (1 - t)) {
                    if (mCurrentGradualing) {
                        mCurrentGradualed = true;
                        mCurrentGradualing = false;
                    } else if (mNextGradualing) {
                        mNextGradualed = true;
                        mNextGradualing = false;
                    }
                }

                if (radioAbs > (1 - t) && (mCurrentGradualing || mNextGradualing)) {
                    alphalFactor = -radioAbs / t + 1 / t;
                }
            }

            float yDegree = -90 + 180f / (countX * 2) + cellX * 180f / countX - 180f * radio;
            float xDegree = MAX_X_DEGREE - 90 + (180F - MAX_X_DEGREE * 2) * cellY / (countY - 1);
            float alpha = (float) (1 + Math.cos(ONE_DEGREE * yDegree));
            float preXDegree = radioYDiff * 360;

            if (alpha > 1) {
                alpha = 1;
                if (radioAbs > 0.9f) {
                    alpha = 10 * (1.0f - radioAbs);
                }
            } else if (alpha < MIN_ALPHA * 2) {
                alpha = MIN_ALPHA * 2;
            }

            if (radioAbs < 0.1f) {
                if (!RuntimeConfig.sLauncherInTouching || RuntimeConfig.sLauncherInTouching && !mSphereform) {
                    gradualFactor = interpolator.getInterpolation(radioAbs / 0.1f);
                }
                if (!RuntimeConfig.sLauncherInTouching)
                    mSphereform = false;
            } else {
                if (alpha == 1.0f)
                    mSphereform = true;
            }

            if (Math.abs(preXDegree) > 90F) {
                preXDegree = preXDegree > 0 ? 90f : -90f;
			}

            camera.save();
            camera.rotateX(-preXDegree * gradualFactor);
            camera.rotateY(yDegree * gradualFactor);
            camera.rotateX(xDegree * gradualFactor);
            camera.translate(0, 0, -288);
            camera.getMatrix(matrix);
            camera.restore();

            float scale = 5f / (context.getResources().getDisplayMetrics().widthPixels / 80f);
            matrix.preScale(scale, scale);

            canvas.save();
            canvas.scale(1 / scale / 2f, 1 / scale / 2f, parentMeasuredWidth / 2, parentMeasuredHeight / 2);
            canvas.translate(parentMeasuredWidth / 2, parentMeasuredHeight / 2);
            canvas.concat(matrix);
            canvas.translate(-parentMeasuredWidth / 2, -parentMeasuredHeight / 2);

            final float transX = parentMeasuredWidth / 2 - childView.getLeft() - childView.getMeasuredWidth() * (indexX * 2 + 1) / (2 * spanX);
            final float transY = parentMeasuredHeight / 2 - childView.getTop() - childView.getMeasuredHeight() * (indexY * 2 + 1) / (2 * spanY);
            canvas.translate(transX * gradualFactor, transY * gradualFactor);


            boolean ret = true;

            if (mIsUsingWidgetCache && r != null && cacheBitmap != null) {
                Rect src = new Rect(r);
                src.offset(-childView.getLeft(), -childView.getTop());
                if (alpha < 1.0f) {
                    mCachePaint.setAlpha((int) (alpha * gradualFactor * alphalFactor * 255f));
                } else {
                    mCachePaint.setAlpha(255);
                }
                canvas.drawBitmap(cacheBitmap, src, r, mCachePaint);
            } else {
                boolean alphaApplied = false;
                if (alpha < 1.0F) {
                    if (screenIndex != currentScreen) {
                        float n = (float) (Math.sin(Math.toRadians(180f * Math.abs(radio))));
                        alphalFactor *= n;
                    }

                    if (callback.onApplyAlpha(childView, alpha * gradualFactor * alphalFactor)) {
                        alphaApplied = true;
                    } else {
                        final int cl = childView.getLeft();
                        final int ct = childView.getTop();
                        final int cr = childView.getRight();
                        final int cb = childView.getBottom();

                        canvas.saveLayerAlpha(cl, ct, cr, cb, (int) (255 * alpha * gradualFactor * alphalFactor), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG); //TODO 4.3中此方法导致速度巨慢，暂时屏蔽
                    }
                } else {
                    callback.onApplyAlpha(childView, 1f);
                }

                if (r != null) {
                    canvas.clipRect(r, Op.REPLACE);
                    childView.invalidate();
                }
                ret = callback.onEffectApplied(canvas, childView, drawingTime);

                if (alpha < 1.0F && !alphaApplied) {
                    canvas.restore();
                }
            }

            canvas.restore();
            return ret;
        }

        @Override
        public boolean isWorkspaceNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean isCellLayoutNeedAntiAlias() {
            return false;
        }

        @Override
        public boolean needInvalidateHardwareAccelerated() {
            return true;
        }

        @Override
        public boolean canEnableWholePageDrawingCache() {
            return false;
        }

        @Override
        public boolean drawChildrenOrderByMoveDirection() {
            return true;
        }

        public boolean useDefaultScroller() {
            return true;
        }

        @Override
        public boolean isScreenFused() {
            return true;
        }

        @Override
        public void onRefresh() {
            onTouchDown(false);
        }

        @Override
        public int getAlphaTransitionType() {
            return 2;
        }
    }

    /**********************
     * 一些以前写好的特效但是没有使用的
     ***************/

    /*
     * allEffects.add(new EffectInfo(2, 6,
     * "pref_k_transformation_cube_inside",
     * "@string/transformation_type_cube_inside") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix(); camera.save(); if
     * (isPortrait) { camera.rotateY(60.0F * radio); } else {
     * camera.rotateX(-60.0F * radio); } camera.getMatrix(matrix);
     * camera.restore(); matrix.preTranslate(-childMeasuredWidth / 2.0F,
     * -childMeasuredHeight / 2.0F);
     * matrix.postTranslate(childMeasuredWidth / 2.0F,
     * childMeasuredHeight / 2.0F); if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_MATRIX); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(6, 3,
     * "pref_k_transformation_left_page",
     * "@string/transformation_type_leftpage") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio));
     * camera.save(); if (isPortrait) {
     * camera.translate(-childMeasuredWidth / 2.0F * Math.abs(radio) /
     * 3.0F, childMeasuredHeight / 2.0F, -childMeasuredWidth / 2.0F *
     * radio); camera.rotateY(-30.0F * radio); } else {
     * camera.translate(-childMeasuredWidth / 2.0F, childMeasuredHeight
     * / 2.0F Math.abs(radio) / 3.0F, -childMeasuredHeight / 2.0F *
     * radio); camera.rotateX(30.0F * radio); }
     * camera.getMatrix(matrix); camera.restore(); if (isPortrait) {
     * matrix.postTranslate(childMeasuredWidth * radio,
     * childMeasuredHeight / 2.0F); } else {
     * matrix.postTranslate(childMeasuredWidth / 2.0F,
     * childMeasuredHeight * radio); } if (offset != 0) { if
     * (isPortrait) { matrix.postTranslate(offset, 0); } else {
     * matrix.postTranslate(0, offset); } }
     * childTransformation.setTransformationType
     * (Transformation.TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(10, 9,
     * "pref_k_transformation_page_slide_up",
     * "@string/transformation_type_page_slide_up") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio)); if
     * (isPortrait) { matrix.postTranslate(0.0F, -Math.abs(radio) *
     * childMeasuredHeight); } else {
     * matrix.postTranslate(-Math.abs(radio) * childMeasuredWidth,
     * 0.0F); } if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(11, 10,
     * "pref_k_transformation_vertical_scrolling",
     * "@string/transformation_type_vertical_scrolling") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio));
     * matrix.postTranslate(childMeasuredWidth * radio, radio *
     * childMeasuredHeight); if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(12, 11,
     * "pref_k_transformation_page_fade",
     * "@string/transformation_type_page_fade") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio)); if
     * (isPortrait) { matrix.postTranslate(childMeasuredWidth * radio,
     * 0.0F); } else { matrix.postTranslate(0.0F, childMeasuredHeight *
     * radio); } if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(13, 12,
     * "pref_k_transformation_page_zoom",
     * "@string/transformation_type_page_zoom") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio));
     * matrix.preScale(1 + radio, 1 + radio);
     * matrix.preTranslate(-childMeasuredWidth / 2.0F,
     * -childMeasuredHeight / 2.0F); if (isPortrait) {
     * matrix.postTranslate( childMeasuredWidth / 2.0F +
     * childMeasuredWidth * radio, childMeasuredHeight / 2.0F); } else {
     * matrix.postTranslate(childMeasuredWidth / 2.0F,
     * childMeasuredHeight / 2.0F + childMeasuredHeight * radio); } if
     * (offset != 0) { if (isPortrait) { matrix.postTranslate(offset,
     * 0); } else { matrix.postTranslate(0, offset); } }
     * childTransformation
     * .setTransformationType(Transformation.TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(15, 14,
     * "pref_k_transformation_carousel",
     * "@string/transformation_type_carousel") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio)); if (radio
     * <= 0) { float scale = 0.5F * (1.0F + radio) + 0.5F;
     * matrix.preScale(scale, scale);
     * matrix.preTranslate(-childMeasuredWidth / 2.0F,
     * -childMeasuredHeight / 2.0F); if (isPortrait) {
     * matrix.postTranslate(childMeasuredWidth / 2.0F +
     * childMeasuredWidth radio / 2.0F, childMeasuredHeight / 2.0F); }
     * else { matrix.postTranslate(childMeasuredWidth / 2.0F,
     * childMeasuredHeight / 2.0F + childMeasuredHeight * radio / 2.0F);
     * } } else { float scale = 1.0F + 3.0F * radio;
     * matrix.preScale(scale, scale); if (isPortrait) {
     * matrix.preTranslate(0.0F, -childMeasuredHeight / 2.0F);
     * matrix.postTranslate(2 * radio * childMeasuredWidth,
     * childMeasuredHeight / 2.0F); } else {
     * matrix.preTranslate(-childMeasuredWidth / 2.0F, 0.0F);
     * matrix.postTranslate(childMeasuredWidth / 2.0F, 2 * radio
     * childMeasuredHeight); } } if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(17, 16,
     * "pref_k_transformation_icon_scatter",
     * "@string/transformation_type_icon_scatter") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { Matrix matrix =
     * childTransformation.getMatrix(); if (offset != 0) { if
     * (isPortrait) { matrix.postTranslate(offset, 0); } else {
     * matrix.postTranslate(0, offset); } }
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredHeight = childView.getMeasuredHeight(); float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * parentMeasuredWidth = parentView.getMeasuredWidth(); float
     * parentMeasuredHeight = parentView.getMeasuredHeight(); float
     * distanceWidth; float distanceHeight; Matrix matrix =
     * childTransformation.getMatrix(); ItemInfo itemInfo = (ItemInfo)
     * childView.getTag(); if (isPortrait) { if (radio <= 0.0F) {
     * distanceWidth = parentView.getLeft() - childView.getLeft(); }
     * else { distanceWidth = parentView.getRight() -
     * childView.getRight(); } distanceHeight = parentView.getTop() +
     * parentMeasuredHeight / 2.0F - childView.getTop() -
     * childMeasuredHeight / 2.0F; } else { if (radio <= 0.0F) {
     * distanceHeight = parentView.getTop() - childView.getTop(); } else
     * { distanceHeight = parentView.getBottom() -
     * childView.getBottom(); } distanceWidth = parentView.getLeft() +
     * parentMeasuredWidth / 2.0F - childView.getLeft() -
     * childMeasuredWidth / 2.0F; } if (isPortrait) { distanceWidth -=
     * itemInfo.screen * parentMeasuredWidth; } else { distanceHeight -=
     * itemInfo.screen * parentMeasuredHeight; }
     * matrix.postTranslate(-Math.abs(radio) * distanceWidth,
     * -Math.abs(radio) distanceHeight);
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; } });
     */

    /*
     * allEffects.add(new EffectInfo(19, 18,
     * "pref_k_transformation_wave",
     * "@string/transformation_type_wave") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix();
     * childTransformation.setAlpha(1.0F - Math.abs(radio)); if
     * (isPortrait) { matrix.postTranslate(childMeasuredWidth * radio,
     * 0.0F); } else { matrix.postTranslate(0.0F, childMeasuredHeight *
     * radio); } if (offset != 0) { if (isPortrait) {
     * matrix.postTranslate(offset, 0); } else { matrix.postTranslate(0,
     * offset); } }
     * childTransformation.setTransformationType(Transformation
     * .TYPE_BOTH); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix(); camera.save(); if
     * (isPortrait) { camera.rotateY(180.0F * radio); } else {
     * camera.rotateX(-180.0F * radio); } camera.getMatrix(matrix);
     * camera.restore(); matrix.preTranslate(-childMeasuredWidth / 2.0F,
     * -childMeasuredHeight / 2.0F);
     * matrix.postTranslate(childMeasuredWidth / 2.0F,
     * childMeasuredHeight / 2.0F);
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; } });
     */

    /*
     * allEffects.add(new EffectInfo(21, 20,
     * "pref_k_transformation_stairs_down_right",
     * "@string/transformation_type_stairs_down_right") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix(); if (radio < 0) { float
     * scale = 0.4F * (1.0F + radio) + 0.6F; matrix.preScale(scale,
     * scale); if (isPortrait) { matrix.preTranslate(0,
     * -childMeasuredHeight / 2.0F); matrix.postTranslate(0,
     * childMeasuredHeight / 2.0F); } else {
     * matrix.preTranslate(-childMeasuredWidth / 2.0F, 0);
     * matrix.postTranslate(childMeasuredWidth / 2.0F, 0); } } else {
     * float scale = 0.2F * radio + 1.0F; matrix.preScale(scale, scale);
     * matrix.preTranslate(-childMeasuredWidth, -childMeasuredHeight /
     * 2.0F); matrix.postTranslate(childMeasuredWidth,
     * childMeasuredHeight / 2.0F); } if (offset != 0) { if (isPortrait)
     * { matrix.postTranslate(offset, 0); } else {
     * matrix.postTranslate(0, offset); } }
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(22, 21,
     * "pref_k_transformation_stairs_down_left",
     * "@string/transformation_type_stairs_down_left") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix(); if (radio < 0) { float
     * scale = -0.2F * radio + 1.0F; matrix.preScale(scale, scale); if
     * (isPortrait) { matrix.preTranslate(0, -childMeasuredHeight /
     * 2.0F); matrix.postTranslate(0, childMeasuredHeight / 2.0F); }
     * else { matrix.preTranslate(-childMeasuredWidth / 2.0F, 0);
     * matrix.postTranslate(childMeasuredWidth / 2.0F, 0); } } else {
     * float scale = 0.4F * (1.0F - radio) + 0.6F;
     * matrix.preScale(scale, scale);
     * matrix.preTranslate(-childMeasuredWidth, -childMeasuredHeight /
     * 2.0F); matrix.postTranslate(childMeasuredWidth,
     * childMeasuredHeight / 2.0F); } if (offset != 0) { if (isPortrait)
     * { matrix.postTranslate(offset, 0); } else {
     * matrix.postTranslate(0, offset); } }
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

    /*
     * allEffects.add(new EffectInfo(23, 22,
     * "pref_k_transformation_squash",
     * "string/transformation_type_squash") {
     * @Override public boolean
     * getWorkspaceChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { float
     * childMeasuredWidth = childView.getMeasuredWidth(); float
     * childMeasuredHeight = childView.getMeasuredHeight(); Matrix
     * matrix = childTransformation.getMatrix(); if (radio < 0) {
     * matrix.preScale(1 + radio, 1); } else if (radio > 0) { if
     * (isPortrait) { matrix.preTranslate(childMeasuredWidth * radio,
     * 0.0F); } else { matrix.preTranslate(0.0F, childMeasuredHeight *
     * radio); } matrix.preScale(1 - radio, 1); } if (offset != 0) { if
     * (isPortrait) { matrix.postTranslate(offset, 0); } else {
     * matrix.postTranslate(0, offset); } }
     * childTransformation.setTransformationType
     * (Transformation.TYPE_MATRIX); return true; }
     * @Override public boolean
     * getCellLayoutChildStaticTransformation(ViewGroup parentView, View
     * childView, Transformation childTransformation, float radio, int
     * offset, int currentScreen, boolean isPortrait) { return false; }
     * });
     */

}
