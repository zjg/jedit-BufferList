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
import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
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
		Log.log(Log.DEBUG, this, "setCurrentSession: currentSession=" + currentSession + " newSession=" + newSession);

		if (newSession.equals(currentSession))
			return true;

		if (jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true)) {
			File currentSessionFile = new File(createSessionFileName(currentSession));
			if (currentSessionFile.exists()) {
				saveSession(view, currentSession);
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
		Buffer buffer = loadSession(newSession, true);
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
	 * (Re)Loads the current session, after closing all open buffers.
	 * This should be used on jEdit startup only.
	 * @param  view  the view that should be used, where the sessions current
	 *    open buffer is showed.
	 */
	public void loadCurrentSession(View view) {
		Log.log(Log.DEBUG, this, "loadCurrentSession: currentSession=" + currentSession);

		// close all open buffers:
		// NOTE: can't use jEdit.closeAllBuffers(View) here, because we don't want jEdit to show dialogs
		Buffer buffer = jEdit.getFirstBuffer();
		while (buffer != null) {
			jEdit._closeBuffer(view, buffer);
			buffer = buffer.getNext();
		}

		// load session:
		buffer = loadSession(currentSession, true);

		// set current buffer:
		if (buffer != null && view != null)
			view.setBuffer(buffer);
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
	 * @param silently  if false, show a dialog that the current session has been saved.
	 */
	public void saveCurrentSession(View view, boolean silently) {
		saveSession(view, currentSession);
		jEdit.setProperty(SESSION_PROPERTY, currentSession);
		if (!silently)
			GUIUtilities.message(view, "bufferlist.switcher.save.saved", new Object[] { currentSession });
	}


	/**
	 * Save current session property only, without saving the current list of
	 * open files.
	 */
	public void saveCurrentSessionProperty() {
		jEdit.setProperty(SESSION_PROPERTY, currentSession);
	}


	public void saveCurrentSessionAs(View view) {
		String name = inputSessionName(view, currentSession);

		if (name == null)
			return;

		File file = new File(createSessionFileName(name));
		if (file.exists()) {
			int answer = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("bufferlist.switcher.saveAs.exists.message", new Object[] { name }),
				jEdit.getProperty("bufferlist.switcher.saveAs.exists.title"),
				JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return;
		}

		saveSession(view, name);
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


	/**
	 * Converts a session name (eg, default) to a full path name
	 * (eg, /home/slava/.jedit/sessions/default.session)
	 * @since BufferList 0.4.1
	 */
	public static String createSessionFileName(String session) {
		String filename = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "sessions", session);

		if (!filename.toLowerCase().endsWith(".session"))
			filename = filename + ".session";

		return filename;
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


	/**
	 * Loads a session.
	 * @param session The file name, relative to $HOME/.jedit/sessions
	 *     (.session suffix not required)
	 * @param ignoreNotFound If false, an exception will be printed if
	 *     the session doesn't exist. If true, it will silently fail
	 * @return the buffer that was used at last in this session, may be null.
	 * @since BufferList 0.4.1
	 */
	private static Buffer loadSession(String session, boolean ignoreNotFound) {
		String filename = createSessionFileName(session);
		Buffer buffer = null;

		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;

			while ((line = in.readLine()) != null) {
				Buffer _buffer = readSessionCommand(line);
				if (_buffer != null)
					buffer = _buffer;
			}

			in.close();
		}
		catch (FileNotFoundException fnf) {
			Log.log(Log.NOTICE, SessionManager.class, fnf);
			if (ignoreNotFound)
				return null;
			String[] args = { filename };
			GUIUtilities.error(null, "filenotfound", args);
		}
		catch (IOException io) {
			Log.log(Log.ERROR, SessionManager.class, io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(null, "ioerror", args);
		}

		return buffer;
	}


	/**
	 * Saves the session
	 * @param view The view this is being saved from. The saved caret
	 *     information and current buffer is taken from this view
	 * @param session The file name, relative to $HOME/.jedit/sessions
	 *     (.session suffix not required)
	 * @since BufferList 0.4.1
	 */
	private static void saveSession(View view, String session) {
		view.getEditPane().saveCaretInfo();

		String lineSep = System.getProperty("line.separator");
		String filename = createSessionFileName(session);
		Buffer buffer = jEdit.getFirstBuffer();

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));

			while (buffer != null) {
				if (!buffer.isUntitled()) {
					writeSessionCommand(view, buffer, out);
					out.write(lineSep);
				}
				buffer = buffer.getNext();
			}

			out.close();
		}
		catch (IOException io) {
			Log.log(Log.ERROR, SessionManager.class, io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(null, "ioerror", args);
		}
	}


	/**
	 * Parse one line from a session file.
	 * @since BufferList 0.4.1
	 */
	private static Buffer readSessionCommand(String line) {
		String path = null;
		Integer selStart = null;
		Integer selEnd = null;
		Integer firstLine = null;
		Integer horizontalOffset = null;
		boolean current = false;

		// handle path:XXX for backwards compatibility
		// with jEdit 2.2 sessions
		if (line.startsWith("path:"))
			line = line.substring(5);

		StringTokenizer st = new StringTokenizer(line, "\t");
		path = st.nextToken();

		// ignore all tokens except for 'current' to maintain
		// compatibility with jEdit 2.2 sessions
		while(st.hasMoreTokens()) {
			String token = st.nextToken();

			if (token.equals("current"))
				current = true;
		}

		if (path == null)
			return null;

		Buffer buffer = jEdit.openFile(null, path);
		if (buffer == null)
			return null;

		return (current ? buffer : null);
	}


	/**
	 * Writes one line to a session file.
	 * @since BufferList 0.4.1
	 */
	private static void writeSessionCommand(View view, Buffer buffer, Writer out) throws IOException {
		out.write(buffer.getPath());
		if (view.getBuffer() == buffer)
			out.write("\tcurrent");
	}


	/** The name of the current session. */
	private String currentSession;


	/** The singleton instance. */
	private static SessionManager instance = null;

}
