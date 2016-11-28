package cc.snser.launcher.widget;

import cc.snser.launcher.ui.dragdrop.DragController;

public interface IWorkspaceContext {
	public void invalidateWorkspace();
	public void invalidateDockbar();
	
	public boolean isEditMode();
	
	public int getCurrentScreen();
	public boolean isScrolling();
	public int getScrollX();
	
	public DragController getDragController();
}
