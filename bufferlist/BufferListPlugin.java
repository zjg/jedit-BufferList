/*
 * BufferListPlugin.java
 * Copyright (c) 2000,2001 Dirk Moebius
 *
 * :tabSize=4:indentSize=4:noTabs=false:maxLineLen=0:
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


package bufferlist;


import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.DockableWindow;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;


/**
 * The BufferList plugin.
 *
 * @author Dirk Moebius
 */
public class BufferListPlugin extends EBPlugin
{

	public static final String NAME = "bufferlist";


	public void start() {
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST, NAME);

		boolean restore = jEdit.getBooleanProperty("restore");
		boolean restore_cli =  jEdit.getBooleanProperty("restore.cli");
		boolean autosave = jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true);

		if ((!restore || !restore_cli) && autosave) {
			// Show a warning if either "Restore previously open files on startup"
			// or "Restore even if file names were specified on the command line"
			// is off and Session Autosave is on:
			showInfoMessage("bufferlist.manager.info.restore_autosave");
		}
		else if (!restore) {
			// "Restore previously open files on startup" is off.
			// The last open session won't be restored.
			// Show an information dialog.
			showInfoMessage("bufferlist.manager.info.restore");
		}
	}


	public void createMenuItems(Vector menuItems) {
		menuItems.addElement(GUIUtilities.loadMenu("bufferlist.menu"));
	}


	public void createOptionPanes(OptionsDialog od) {
		od.addOptionPane(new BufferListOptionPane());
	}


	public void handleMessage(EBMessage message) {
		if (message instanceof ViewUpdate) {
			ViewUpdate vu = (ViewUpdate) message;
			if (vu.getWhat() == ViewUpdate.CREATED) {
				String showWhere = jEdit.getProperty("bufferlist.switcher.show", "bufferlist");
				if (showWhere.equals("view"))
					addSessionSwitcher(vu.getView());
			}
			else if (vu.getWhat() == ViewUpdate.CLOSED) {
				viewSessionSwitchers.remove(vu.getView());
			}
		}
		else if (message instanceof CreateDockableWindow) {
			CreateDockableWindow cmsg = (CreateDockableWindow)message;
			if (cmsg.getDockableWindowName().equals(NAME))
				cmsg.setDockableWindow(new BufferList(cmsg.getView(), cmsg.getPosition()));
		}
		else if (message instanceof BufferUpdate) {
			BufferUpdate bu = (BufferUpdate) message;
			if (autoshow
				&& bu.getView() != null
				&& (bu.getWhat() == BufferUpdate.CREATED || bu.getWhat() == BufferUpdate.CLOSED))
				bu.getView().getDockableWindowManager().addDockableWindow(NAME);
		}
		else if (message instanceof EditPaneUpdate) {
			EditPaneUpdate epu = (EditPaneUpdate) message;
			if (autoshow && epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				View view = ((EditPane) epu.getSource()).getView();
				if (view != null)
					view.getDockableWindowManager().addDockableWindow(NAME);
			}
		}
		else if (message instanceof EditorExitRequested) {
			EditorExitRequested eer = (EditorExitRequested) message;
			// remember the last open session:
			SessionManager mgr = SessionManager.getInstance();
			mgr.saveCurrentSessionProperty();
			// if autosave sessions is on, save current session silently:
			if (jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true)) {
				Log.log(Log.DEBUG, this, "autosaving current session...");
				mgr.saveCurrentSession(eer.getView(), true);
			}
		}
		else if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}


	private void propertiesChanged() {
		autoshow = jEdit.getBooleanProperty("bufferlist.switcher.autoshow", false);

		String showWhere = jEdit.getProperty("bufferlist.switcher.show", "bufferlist");
		boolean add = showWhere.equals("view");
		View view = jEdit.getFirstView();
		while (view != null) {
			if (add)
				addSessionSwitcher(view);
			else
				removeSessionSwitcher(view);
			view = view.getNext();
		}
	}


	private void addSessionSwitcher(View view) {
		SessionSwitcher switcher = (SessionSwitcher) viewSessionSwitchers.get(view);
		if (switcher == null) {
			switcher = new SessionSwitcher(view);
			view.addToolBar(switcher);
			viewSessionSwitchers.put(view, switcher);
		}
	}


	private void removeSessionSwitcher(View view) {
		SessionSwitcher switcher = (SessionSwitcher) viewSessionSwitchers.get(view);
		if (switcher != null) {
			view.removeToolBar(switcher);
			viewSessionSwitchers.remove(view);
		}
	}


	private void showInfoMessage(String key) {
		if (!jEdit.getBooleanProperty(key + ".notAgain")) {
			String title = jEdit.getProperty(key + ".title");
			String msg = jEdit.getProperty(key + ".message");
			String msg2 = jEdit.getProperty("bufferlist.manager.info.dontShowAgain");
			JCheckBox notAgain = new JCheckBox(msg2, false);
			JOptionPane.showMessageDialog(null, new Object[] { msg, notAgain },
				title, JOptionPane.INFORMATION_MESSAGE);
			jEdit.setBooleanProperty(key + ".notAgain", notAgain.isSelected());
		}
	}


	private boolean autoshow = false;
	private Hashtable viewSessionSwitchers = new Hashtable();

}

