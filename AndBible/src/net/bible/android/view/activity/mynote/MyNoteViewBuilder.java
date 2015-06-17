package net.bible.android.view.activity.mynote;

import net.bible.android.control.ControlFactory;
import net.bible.android.view.activity.base.DocumentView;
import android.app.Activity;
import android.view.ViewGroup;

/**
 * Build a MyNote TextView for viewing or editing notes
 * 
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's authors.
 * @author Martin Denham [mjdenham at gmail dot com]
 */
public class MyNoteViewBuilder {

	private MyNoteEditTextView myNoteText;
	private static final int MYNOTE_TEXT_ID = 992;
	
	private Activity mainActivity;
	
	private static final String TAG = "MyNoteViewBuilder";

	public MyNoteViewBuilder(Activity mainActivity) {
		this.mainActivity = mainActivity;
		
        myNoteText = new MyNoteEditTextView(this.mainActivity);
        
        myNoteText.setId(MYNOTE_TEXT_ID);
	}
	
	/** return true if the current page should show a NyNote
	 */
	public boolean isMyNoteViewType() {
		return ControlFactory.getInstance().getCurrentPageControl().isMyNoteShown();
	}
	
	public void addMyNoteView(ViewGroup parent) {
    	boolean isMynoteTextEdit = isMyNoteViewShowing(parent);
    	parent.setTag(TAG);

    	if (!isMynoteTextEdit) {
    		parent.addView(myNoteText);
    		mainActivity.registerForContextMenu(myNoteText);
    	}
	}

	public void removeMyNoteView(ViewGroup parent) {
    	boolean isMynoteTextEdit = isMyNoteViewShowing(parent);
    	parent.setTag("");
    	
    	if (isMynoteTextEdit) {
    		parent.removeView(myNoteText);
    		mainActivity.unregisterForContextMenu(myNoteText);
    	}
	}

	public DocumentView getView() {
		return myNoteText;
	}

	private boolean isMyNoteViewShowing(ViewGroup parent) {
		Object tag = parent.getTag();
		return tag!=null && tag.equals(TAG);
	}
}
