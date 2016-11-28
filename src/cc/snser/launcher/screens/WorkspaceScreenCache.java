package cc.snser.launcher.screens;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.SparseLongArray;
import android.view.View;

/**
 * 在从正常模式切换到编辑模式时，对需要Hide的CellLayout缓存，以便从编辑模式切回正常模式时，快速恢复
 * @author shishengyi
 *
 */

public class WorkspaceScreenCache {
	
	private HashMap<Long,ArrayList<View>> mCacheMap;
	public WorkspaceScreenCache(){
		mCacheMap = new HashMap<Long,ArrayList<View>>();
	}
	
	public void addScreen(long folderid,View v){
		ArrayList<View> items = mCacheMap.get(folderid);
		if( items != null ){
			items.add(v);
		}else{
			items = new ArrayList<View>();
			items.add(v);
			mCacheMap.put(folderid, items);
		}
	}
	
	public ArrayList<View> getCacheItems(long folderid){
		return mCacheMap.get(folderid);
	}
	
	public int getCacheCount(){
		return mCacheMap.size();
	}
	
	public void clear(){
		mCacheMap.clear();
	}
}
