package cc.snser.launcher.model;

import java.util.ArrayList;
import java.util.List;

public class FavoriteGap {
	private List<GapPosition> mGapPos = new ArrayList<GapPosition>();
	private static FavoriteGap sInstance = null;
	
	 /**
     * Get a singleton instance.
     * */
    public static final FavoriteGap getInstance() {
        if (sInstance == null) {
            sInstance = new FavoriteGap();
        }
        return sInstance;
    }
    
    public void addGapPosition(GapPosition gapPos)
    {
    	if(!mGapPos.contains(gapPos)){
    		mGapPos.add(gapPos);	
    	}
    }
    
    public GapPosition getGapPosition()
    {
    	if(!mGapPos.isEmpty())
    		return mGapPos.remove(0);
    	else
    		return null;
    }
    
    public static class GapPosition
    {
    	
		public int mContainer = -1;
    	public int mScreen = -1;
    	public int mCellX = -1;
    	public int mCellY = -1;
    	
    	@Override
		public String toString() {
			
			return "Container: "+mContainer+", Screen: "+mScreen+", CellX: "+mCellX+", CellY: "+mCellY;
		}
    	
    	@Override
		public boolean equals(Object arg0) {
			if(arg0 instanceof GapPosition){
				final GapPosition rhs = (GapPosition)arg0;
				
				return (rhs.mContainer == this.mContainer) &&
						(rhs.mScreen == this.mScreen) &&
						(rhs.mCellX == this.mCellX) &&
						(rhs.mCellY == this.mCellY);
			}else {
				return super.equals(arg0);	
			}
		}
    	
    }
}
