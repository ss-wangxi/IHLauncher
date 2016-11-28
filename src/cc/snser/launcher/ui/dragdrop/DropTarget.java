/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.snser.launcher.ui.dragdrop;

import android.graphics.Rect;

/**
 * Interface defining an object that can receive a drag.
 *
 */
public interface DropTarget {

    class DragObject {
        public int x = -1;
        public int y = -1;

        /** X offset from the upper-left corner of the cell to where we touched.  */
        public int xOffset = -1;

        /** Y offset from the upper-left corner of the cell to where we touched.  */
        public int yOffset = -1;

        /** This indicates whether a drag is in final stages, either drop or cancel. It
         * differentiates onDragExit, since this is called when the drag is ending, above
         * the current drag target, or when the drag moves off the current drag object.
         */
        public boolean dragComplete = false;
        public boolean isCancelingDrag = false;

        /** The view that moves around while you drag.  */
        public DragView dragView = null;

        /** The data associated with the object being dragged */
        public Object dragInfo = null;

        /** Where the drag originated */
        public DragSource dragSource = null;

        /** Defers removing the DragView from the DragLayer until after the drop animation. */
        public boolean deferDragViewCleanupPostAnimation = false;

        public float velocityX = 0;
        public float velocityY = 0;
//        public View mOriginator = null;
        /**
         * 该拖拽物的覆盖矩形，用于探测是否和目标区域重叠，如果为空则用其它已有信息替代<br>
         * TODO: 设计为final
         */
//        public Rect mOverlay;

        /**
         * 是否可以合成文件夹<br>
         * <br>
         * <b>注：该值拖拽启动之后被初始化，拖拽过程使用，考虑设计为final性质的变量</b>
         * TODO: 设计为final
         */
//        public boolean mFolderable;

        /**
         * 拖拽出来的对象的Parent View，不同Drag Source有不同的表示，可以为null，主要用于Drop Complete处理<br>
         * 暂时设计：后续可以设计为抽象类型，不一定要用View<br>
         * TODO: 设计为final
         */
//        public View mSourceParent;

        /**
         * 拖拽落位的Parent View，不同Drag Source有不同的表示，可以为null，主要用于Drop Complete处理<br>
         * 暂时设计：后续可以设计为抽象类型，不一定要用View<br>
         */
//        public View mTargetParent;

        /**
         * 落下时屏幕x坐标
         * TODO: 目前仅在drop有处理
         */
        public int mDropScreenX;

        /**
         * 落下时屏幕x坐标
         * TODO: 目前仅在drop有处理
         */
        public int mDropScreenY;

        /**
         * 落下时drag view中心点的屏幕x坐标
         * TODO: 目前仅在drop有处理
         */
        public int mDragViewScreenX;

        /**
         * 落下时drag view中心点的屏幕x坐标
         * TODO: 目前仅在drop有处理
         */
        public int mDragViewScreenY;

        /**
         * 落下时x方向速度
         * TODO: 目前仅在drop有处理
         */
        public float mDropVelocityX;

        /**
         * 落下时y方向速度
         * TODO: 目前仅在drop有处理
         */
        public float mDropVelocityY;


        public DragObject() {
        }

        @Override
        public String toString() {
            return "x: " + x + ", y: " + y + ", xOffset: " + xOffset + ", yOffset: " + yOffset;
        }
    }

    /**
     * Handle an object being dropped on the DropTarget
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the original
     *          touch happened
     * @param yOffset Vertical offset with the object being dragged where the original
     *          touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     *
     */
    void onDrop(DragObject dragObject);

    void onDragEnter(DragObject dragObject);

    void onDragOver(DragObject dragObject);

    void onDragExit(DragObject dragObject, DropTarget dropTarget);

    /**
     * Check if a drop action can occur at, or near, the requested location.
     * This may be called repeatedly during a drag, so any calls should return
     * quickly.
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the
     *            original touch happened
     * @param yOffset Vertical offset with the object being dragged where the
     *            original touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     * @return True if the drop will be accepted, false otherwise.
     */
    boolean acceptDrop(DragObject dragObject);

    // These methods are implemented in Views
    void getHitRect(Rect outRect);
    void getLocationOnScreen(int[] loc);
    int getLeft();
    int getTop();
}
