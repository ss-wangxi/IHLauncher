/*
 * Copyright (C) 2007 The Android Open Source Project
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

package mobi.intuitit.android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Method;
import java.util.ArrayList;

import com.btime.launcher.util.XLog;


/**
* A class that describes a view hierarchy that can be displayed in
* another process. The hierarchy is inflated from a layout resource
* file, and this class provides some basic operations for modifying
* the content of the inflated hierarchy.
*/
public class SimpleRemoteViews implements Parcelable {

   /**
    * The resource ID of the layout file. (Added to the parcel)
    */
   private int mLayoutId;

   /**
    * An array of actions to perform on the view tree once it has been
    * inflated
    */
   protected ArrayList<Action> mActions;


   /**
    * Exception to send when something goes wrong executing an action
    *
    */
   public static class ActionException extends RuntimeException {
    private static final long serialVersionUID = 4846278673349592851L;
    public ActionException(Exception ex) {
           super(ex);
       }
       public ActionException(String message) {
           super(message);
       }
   }

   /**
    * Base class for all actions that can be performed on an
    * inflated view.
    *
    */
   protected abstract static class Action implements Parcelable {
       public abstract void apply(View root) throws ActionException;

       public int describeContents() {
           return 0;
       }
   }

   private class SetLayoutSize extends Action {
       private static final int TAG = 5;
       public static final int WIDTH = 1;

       private int mMode;
       private int mViewId;
       private int mValue;

       public SetLayoutSize(Parcel parcel) {
           mViewId = parcel.readInt();
           mMode = parcel.readInt();
           mValue = parcel.readInt();
       }

       public void writeToParcel(Parcel dest, int flags) {
           dest.writeInt(TAG);
           dest.writeInt(mViewId);
           dest.writeInt(mMode);
           dest.writeInt(mValue);
       }


       @Override
       public void apply(View root) throws ActionException {
           View target = root.findViewById(mViewId);
           if (target != null) {
               ViewGroup.LayoutParams lps = target.getLayoutParams();
               if (mMode == WIDTH)
                   lps.width = mValue;
               else
                   lps.height = mValue;
               target.setLayoutParams(lps);
           }
       }
   }


   /**
    * Equivalent to calling a combination of {@link Drawable#setAlpha(int)},
    * {@link Drawable#setColorFilter(int, android.graphics.PorterDuff.Mode)},
    * and/or {@link Drawable#setLevel(int)} on the {@link Drawable} of a given view.
    * <p>
    * These operations will be performed on the {@link Drawable} returned by the
    * target {@link View#getBackground()} by default.  If targetBackground is false,
    * we assume the target is an {@link ImageView} and try applying the operations
    * to {@link ImageView#getDrawable()}.
    * <p>
    * You can omit specific calls by marking their values with null or -1.
    */
   private class SetDrawableParameters extends Action {
       public SetDrawableParameters(Parcel parcel) {
           viewId = parcel.readInt();
           targetBackground = parcel.readInt() != 0;
           alpha = parcel.readInt();
           colorFilter = parcel.readInt();
           boolean hasMode = parcel.readInt() != 0;
           if (hasMode) {
               filterMode = PorterDuff.Mode.valueOf(parcel.readString());
           } else {
               filterMode = null;
           }
           level = parcel.readInt();
       }

       public void writeToParcel(Parcel dest, int flags) {
           dest.writeInt(TAG);
           dest.writeInt(viewId);
           dest.writeInt(targetBackground ? 1 : 0);
           dest.writeInt(alpha);
           dest.writeInt(colorFilter);
           if (filterMode != null) {
               dest.writeInt(1);
               dest.writeString(filterMode.toString());
           } else {
               dest.writeInt(0);
           }
           dest.writeInt(level);
       }

       @Override
       public void apply(View root) {
           final View target = root.findViewById(viewId);
           if (target == null) {
               return;
           }

           // Pick the correct drawable to modify for this view
           Drawable targetDrawable = null;
           if (targetBackground) {
               targetDrawable = target.getBackground();
           } else if (target instanceof ImageView) {
               ImageView imageView = (ImageView) target;
               targetDrawable = imageView.getDrawable();
           }

           if (targetDrawable != null) {
               // Perform modifications only if values are set correctly
               if (alpha != -1) {
                   targetDrawable.setAlpha(alpha);
               }
               if (colorFilter != -1 && filterMode != null) {
                   targetDrawable.setColorFilter(colorFilter, filterMode);
               }
               if (level != -1) {
                   targetDrawable.setLevel(level);
               }
           }
       }

       int viewId;
       boolean targetBackground;
       int alpha;
       int colorFilter;
       PorterDuff.Mode filterMode;
       int level;

       public final static int TAG = 3;
   }

   /**
    * Equivalent to calling
    * {@link android.view.View#setOnClickListener(android.view.View.OnClickListener)}
    * to launch the provided {@link PendingIntent}.
    */
   protected class SetOnClickPendingIntent extends Action {
       public SetOnClickPendingIntent(int id, PendingIntent pendingIntent) {
           this.viewId = id;
           this.pendingIntent = pendingIntent;
       }

       public SetOnClickPendingIntent(Parcel parcel) {
           viewId = parcel.readInt();
           pendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
       }


       public void writeToParcel(Parcel dest, int flags) {
           dest.writeInt(TAG);
           dest.writeInt(viewId);
           pendingIntent.writeToParcel(dest, 0 /* no flags */);
       }

       @Override
       public void apply(View root) {
           final View target = root.findViewById(viewId);
           if (target != null && pendingIntent != null) {
               OnClickListener listener = new OnClickListener() {
                   public void onClick(View v) {
                       // Find target view location in screen coordinates and
                       // fill into PendingIntent before sending.
                       final int[] location = new int[2];
                       v.getLocationOnScreen(location);
                       Rect srcRect = new Rect();
                       srcRect.left = location[0];
                       srcRect.top = location[1];
                       srcRect.right = srcRect.left + v.getWidth();
                       srcRect.bottom = srcRect.top + v.getHeight();

                       final Intent intent = new Intent();
                       intent.setSourceBounds(srcRect);
                       try {
                           pendingIntent.send(v.getContext(), 0, intent, null, null);
                       } catch (PendingIntent.CanceledException e) {
                           XLog.e("SetOnClickPendingIntent", "Cannot send pending intent: ", e);
                       }
                   }
               };
               target.setOnClickListener(listener);
           }
       }

       int viewId;
       PendingIntent pendingIntent;

       public final static int TAG = 1;
   }

   /**
    * Base class for the reflection actions.
    */
   protected class ReflectionAction extends Action {
       static final int TAG = 2;

       static final int BOOLEAN = 1;
       static final int BYTE = 2;
       static final int SHORT = 3;
       static final int INT = 4;
       static final int LONG = 5;
       static final int FLOAT = 6;
       static final int DOUBLE = 7;
       static final int CHAR = 8;
       static final int STRING = 9;
       static final int CHAR_SEQUENCE = 10;
       static final int URI = 11;
       static final int BITMAP = 12;
       static final int BUNDLE = 13;

       int viewId;
       String methodName;
       int type;
       Object value;

       ReflectionAction(int viewId, String methodName, int type, Object value) {
           this(viewId, methodName, type);
           this.value = value;
       }

       protected ReflectionAction(int viewId, String methodName, int type) {
           this.viewId = viewId;
           this.methodName = methodName;
           this.type = type;
       }

       ReflectionAction(Parcel in) {
           this.viewId = in.readInt();
           this.methodName = in.readString();
           this.type = in.readInt();
           readValue(in);
       }

       protected void readValue(Parcel in) {
           switch (this.type) {
               case BOOLEAN:
                   this.value = in.readInt() != 0;
                   break;
               case BYTE:
                   this.value = in.readByte();
                   break;
               case SHORT:
                   this.value = (short)in.readInt();
                   break;
               case INT:
                   this.value = in.readInt();
                   break;
               case LONG:
                   this.value = in.readLong();
                   break;
               case FLOAT:
                   this.value = in.readFloat();
                   break;
               case DOUBLE:
                   this.value = in.readDouble();
                   break;
               case CHAR:
                   this.value = (char)in.readInt();
                   break;
               case STRING:
                   this.value = in.readString();
                   break;
               case CHAR_SEQUENCE:
                   this.value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                   break;
               case URI:
                   this.value = Uri.CREATOR.createFromParcel(in);
                   break;
               case BITMAP:
                   this.value = Bitmap.CREATOR.createFromParcel(in);
                   break;
               case BUNDLE:
                   this.value = in.readBundle();
                   break;
               default:
                   break;
           }
       }

       public void writeToParcel(Parcel out, int flags) {
           out.writeInt(getTag());
           out.writeInt(this.viewId);
           out.writeString(this.methodName);
           out.writeInt(this.type);
           writeValue(out, flags);
       }

       protected int getTag() {
           return TAG;
       }

       protected void writeValue(Parcel out, int flags) {
           switch (this.type) {
               case BOOLEAN:
                   out.writeInt((Boolean) this.value ? 1 : 0);
                   break;
               case BYTE:
                   out.writeByte((Byte) this.value);
                   break;
               case SHORT:
                   out.writeInt((Short) this.value);
                   break;
               case INT:
                   out.writeInt((Integer) this.value);
                   break;
               case LONG:
                   out.writeLong((Long) this.value);
                   break;
               case FLOAT:
                   out.writeFloat((Float) this.value);
                   break;
               case DOUBLE:
                   out.writeDouble((Double) this.value);
                   break;
               case CHAR:
                   out.writeInt((int)((Character)this.value).charValue());
                   break;
               case STRING:
                   out.writeString((String)this.value);
                   break;
               case CHAR_SEQUENCE:
                   TextUtils.writeToParcel((CharSequence)this.value, out, flags);
                   break;
               case URI:
                   ((Uri)this.value).writeToParcel(out, flags);
                   break;
               case BITMAP:
                   ((Bitmap)this.value).writeToParcel(out, flags);
                   break;
               case BUNDLE:
                   out.writeBundle((Bundle) this.value);
                   break;
               default:
                   break;
           }
       }

       @SuppressWarnings({ "rawtypes" })
       private Class getParameterType() {
           switch (this.type) {
               case BOOLEAN:
                   return boolean.class;
               case BYTE:
                   return byte.class;
               case SHORT:
                   return short.class;
               case INT:
                   return int.class;
               case LONG:
                   return long.class;
               case FLOAT:
                   return float.class;
               case DOUBLE:
                   return double.class;
               case CHAR:
                   return char.class;
               case STRING:
                   return String.class;
               case CHAR_SEQUENCE:
                   return CharSequence.class;
               case URI:
                   return Uri.class;
               case BITMAP:
                   return Bitmap.class;
               case BUNDLE:
                   return Bundle.class;
               default:
                   return null;
           }
       }

       @SuppressWarnings({ "unchecked", "rawtypes" })
       @Override
       public void apply(View root) {
           final View view = root.findViewById(viewId);
           if (view == null) {
               throw new ActionException("can't find view: 0x" + Integer.toHexString(viewId));
           }

           Class param = getParameterType();
           if (param == null) {
               throw new ActionException("bad type: " + this.type);
           }

           Class klass = view.getClass();
           Method method;
           try {
               method = klass.getMethod(this.methodName, getParameterType());
           }
           catch (NoSuchMethodException ex) {
               throw new ActionException("view: " + klass.getName() + " doesn't have method: "
                       + this.methodName + "(" + param.getName() + ")");
           }

           try {
               method.invoke(view, getValue(root.getContext()));
           }
           catch (Exception ex) {
               throw new ActionException(ex);
           }
       }

       protected Object getValue(Context context) {
           return this.value;
       }
   }

   /**
    * Create a new RemoteViews object that will display the views contained
    * in the specified layout file.
    *
    * @param packageName Name of the package that contains the layout resource
    * @param layoutId The id of the layout resource
    */
   public SimpleRemoteViews(int layoutId) {
       mLayoutId = layoutId;
   }

   /**
    * Reads a RemoteViews object from a parcel.
    *
    * @param parcel
    */
   public SimpleRemoteViews(Parcel parcel) {
       mLayoutId = parcel.readInt();
       int count = parcel.readInt();
       if (count > 0) {
           mActions = new ArrayList<Action>(count);
           for (int i=0; i<count; i++) {
               int tag = parcel.readInt();
               Action act = loadActionFromParcel(tag, parcel);
               if (act != null)
                   mActions.add(act);
               else
                   throw new ActionException("Tag " + tag + " not found");
           }
       }
   }

   protected Action loadActionFromParcel(int tag, Parcel parcel) {
       switch (tag) {
               case SetLayoutSize.TAG:
                   return new SetLayoutSize(parcel);
               case SetOnClickPendingIntent.TAG:
                   return new SetOnClickPendingIntent(parcel);
               case SetDrawableParameters.TAG:
                   return new SetDrawableParameters(parcel);
               case ReflectionAction.TAG:
                   return new ReflectionAction(parcel);
               default:
                   return null;
       }
   }

   public int getLayoutId() {
       return mLayoutId;
   }

   /**
    * Add an action to be executed on the remote side when apply is called.
    *
    * @param a The action to add
    */
   protected void addAction(Action a) {
       if (mActions == null) {
           mActions = new ArrayList<Action>();
       }
       mActions.add(a);
   }

   /**
    * Inflates the view hierarchy represented by this object and applies
    * all of the actions.
    *
    * <p><strong>Caller beware: this may throw</strong>
    *
    * @param context Default context to use
    * @param parent Parent that the resulting view hierarchy will be attached to. This method
    * does <strong>not</strong> attach the hierarchy. The caller should do so when appropriate.
    * @return The inflated view hierarchy
    */
   public View apply(Context context, ViewGroup parent) {
       View result;

       LayoutInflater inflater = LayoutInflater.from(context);
       result = inflater.inflate(mLayoutId, parent);
       performApply(result);

       return result;
   }

   private void performApply(View v) {
       try
       {
           if (mActions != null) {
               final int count = mActions.size();
               for (int i = 0; i < count; i++) {
                   Action a = mActions.get(i);
                   a.apply(v);
               }
           }
       } catch (OutOfMemoryError e) {
           System.gc();
       }
   }


   public int describeContents() {
       return 0;
   }

   public void writeToParcel(Parcel dest, int flags) {
       dest.writeInt(mLayoutId);
       int count;
       if (mActions != null) {
           count = mActions.size();
       } else {
           count = 0;
       }
       dest.writeInt(count);
       for (int i=0; i<count; i++) {
           Action a = mActions.get(i);
           a.writeToParcel(dest, 0);
       }
   }

   /**
    * Parcelable.Creator that instantiates RemoteViews objects
    */
   public static final Parcelable.Creator<SimpleRemoteViews> CREATOR = new Parcelable.Creator<SimpleRemoteViews>() {
       public SimpleRemoteViews createFromParcel(Parcel parcel) {
           return new SimpleRemoteViews(parcel);
       }

       public SimpleRemoteViews[] newArray(int size) {
           return new SimpleRemoteViews[size];
       }
   };
}