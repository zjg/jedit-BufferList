/*
 * SessionManager.java
 * Copyright (c) 2001 Dirk Moebius
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


import java.awt.Component;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.Sessions;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.util.Log;


/**
 * A singleton class that holds a current session and has methods to switch
 * between sessions.
 *
 * @author Dirk Moebius
 */
public class SessionManager {

	public final static String SESSION_PROPERTY = "bufferlist.currentSession";


	/** Returns the singleton instance */
	public static SessionManager getInstance() {
		if (instance == null) {
			instance = new SessionManager();
		}
		return instance;
	}


	/**
	 * Switch to new session.
	 * @param view  view for displaying error messages
	 * @param newSession  the new session name
	 * @return false, if the new session could not be set.
	 */
	public boolean setCurrentSession(View view, String newSession) {
		Log.log(Log.DEBUG, this, "setNewSession: currentSession=" + currentSession + " newSession=" + newSession);

		if (newSession.equals(currentSession))
			return true;

		if (jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true)) {
			File currentSessionFile = new File(Sessions.createSessionFileName(currentSession));
			if (currentSessionFile.exists()) {
				Sessions.saveSession(view, currentSession);
			} else {
				// The current session file has been deleted, probably by the SessionManagerDialog.
				// Do nothing, because save would recreate it.
			}
		}

		// close all open buffers, if closeAll option is set:
		if (jEdit.getBooleanProperty("bufferlist.switcher.closeAll", true)) {
			if (!jEdit.closeAllBuffers(view))
				return false;
			VFSManager.waitForRequests();
			if (VFSManager.errorOccurred())
				return false;
		}


		// load new session:
		Buffer buffer = Sessions.loadSession(newSession, true);
		VFSManager.waitForRequests();
		if (VFSManager.errorOccurred())
			return false;

		currentSession = newSession;
		jEdit.setProperty(SESSION_PROPERTY, currentSession);
		jEdit.propertiesChanged();

		if (buffer != null)
			view.setBuffer(buffer);

		return true;
	}


	/** Gets the current session name. */
	public String getCurrentSession() {
		return currentSession;
	}


	/**
	 * Save current session and show a dialog that it has been saved.
	 * @param view  view for displaying error messages
	 */
	public void saveCurrentSession(View view) {
		saveCurrentSession(view, false);
	}


	/**
	 * Save current session.
	 * @param view  view for displaying error messages
	 * @param silent  if true, show a dialog that the current session has been saved.
	 */
	public void saveCurrentSession(View view, boolean silent) {
		Sessions.saveSession(view, currentSession);
		jEdit.setProperty(SESSION_PROPERTY, currentSession);
		if (!silent)
			GUIUtilities.message(view, "bufferlist.switcher.save.saved", new Object[] { currentSession });
	}


	public void saveCurrentSessionAs(View view) {
		String name = inputSessionName(view, currentSession);

		if (name == null)
			return;

		File file = new File(Sessions.createSessionFileName(name));
		if (file.exists()) {
			int answer = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("bufferlist.switcher.saveAs.exists.message", new Object[] { name }),
				jEdit.getProperty("bufferlist.switcher.saveAs.exists.title"),
				JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return;
		}

		Sessions.saveSession(view, name);
		currentSession = name;
		jEdit.setProperty("bufferlist.session", currentSession);
		jEdit.propertiesChanged();
	}


	public void showSessionManagerDialog(View view) {
		SessionManagerDialog dlg = new SessionManagerDialog(view, currentSession);
		String newSession = dlg.getSelectedSession();
		if (newSession != null) {
			setCurrentSession(view, newSession);
		}
		jEdit.propertiesChanged();
	}


	public static Vector getSessionNames() {
		String path = MiscUtilities.constructPath(jEdit.getSettingsDirectory(), "sessions");
		String[] files = new File(path).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".session");
			}
		});

		MiscUtilities.quicksort(files, new MiscUtilities.StringICaseCompare());

		Vector v = new Vector();
		boolean foundDefault = false;
		for (int i=0; i < files.length; i++) {
			String name = files[i].substring(0, files[i].length() - 8);
			if (name.equalsIgnoreCase("default")) {
				// default session always first
				v.insertElementAt(name, 0);
				foundDefault = true;
			} else {
				v.addElement(name);
			}
		}

		if (!foundDefault)
			v.insertElementAt("default", 0);

		return v;
	}


	/**
	 * Shows an input dialog asking for a session name as long as a valid
	 * session name is entered or the dialog is cancelled.
	 * A session name is valid if it doesn't contains the following characters:
	 * File.separatorChar, File.pathSeparatorChar and ':'.
	 *
	 * @param relativeTo  position the input dialog relative to this component.
	 * @param defaultName  a default session name to display in the input dialog; may be null.
	 * @return the new session name without trailing ".session", or null, if
	 *     the dialog was cancelled.
	 */
	public static String inputSessionName(Component relativeTo, String defaultName) {
		String name = defaultName;

		do {
			name = GUIUtilities.input(relativeTo, "bufferlist.switcher.saveAs.input", name);
			if (name != null) {
				name = name.trim();
				if (name.length() == 0)
					GUIUtilities.error(relativeTo, "bufferlist.switcher.saveAs.error.empty", null);
				if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
					|| name.indexOf(';') >= 0 || name.indexOf(':') >= 0) {
					GUIUtilities.error(relativeTo, "bufferlist.switcher.saveAs.error.illegalChars", new Object[] { "/  \\  ;  :" });
					name = "";
				}
			}
		} while (name != null && name.length() == 0);

		if (name != null && name.toLowerCase().endsWith(".session")) {
			name = name.substring(0, name.length() - 8);
		}

		return name;
	}


	private SessionManager() {
		 currentSession = jEdit.getProperty(SESSION_PROPERTY, "default");
	}


	/** The name of the current session. */
	private String currentSession;


	/** The singleton instance. */
	private static SessionManager instance = null;

}

