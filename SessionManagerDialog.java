/*
 * SessionManagerDialog.java - a dialog for managing sessions
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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.util.Log;


/**
 * A modal dialog for managing jEdit sessions and session files.
 *
 * @author Dirk Moebius
 */
public class SessionManagerDialog extends EnhancedDialog implements ActionListener, ListSelectionListener {

	public SessionManagerDialog(View view, String currentSession) {
		super(view, jEdit.getProperty("bufferlist.manager.title"), true);
		this.currentSession = currentSession;

		lSessions = new JList(SessionManager.getSessionNames());
		lSessions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lSessions.addListSelectionListener(this);
		lSessions.addMouseListener(new MouseHandler()); // for double-clicks

		JScrollPane scrSessions = new JScrollPane(lSessions);
		scrSessions.setPreferredSize(new Dimension(200, 100));

		bRename = new JButton(jEdit.getProperty("bufferlist.manager.rename"));
		bRename.addActionListener(this);

		bDelete = new JButton(jEdit.getProperty("bufferlist.manager.delete"));
		bDelete.addActionListener(this);

		bChangeTo = new JButton(jEdit.getProperty("bufferlist.manager.changeTo"));
		bChangeTo.setDefaultCapable(true);
		bChangeTo.addActionListener(this);

		bClose = new JButton(jEdit.getProperty("bufferlist.manager.close"));
		bClose.addActionListener(this);

		Insets inset10 = new Insets(10, 10, 10, 10);
		Insets insetButton = new Insets(10, 0, 0, 10);
		Insets insetLast = new Insets(10, 0, 10, 10);

		getContentPane().setLayout(new GridBagLayout());
		addComponent(scrSessions, 1, 3, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, inset10);
		addComponent(bRename, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insetButton);
		addComponent(bDelete, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insetButton);
		addComponent(bChangeTo, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insetButton);
		addComponent(new JSeparator(), GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, inset10);
		addComponent(new JPanel(), 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, inset10);
		addComponent(bClose, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insetLast);
		getRootPane().setDefaultButton(bChangeTo);

		lSessions.setSelectedValue(currentSession, true);

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	}


	/**
	 * Invoked when ENTER or the Ok button is pressed.
	 * Switches the session and closes the dialog. Don't show it again afterwards.
	 */
	public void ok() {
		if (lSessions.getSelectedValue() != null)
			selectedSession = lSessions.getSelectedValue().toString();
		setVisible(false);
		dispose();
	}


	/**
	 * Invoked when ESC or the Cancel button is pressed.
	 * Closes the dialog. Don't show it again afterwards.
	 */
	public void cancel() {
		setVisible(false);
		dispose();
	}


	public String getSelectedSession() {
		return selectedSession;
	}


	/**
	 * Invoked when any of the buttons is pressed.
	 */
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == bClose) {
			cancel();
		}
		else if (evt.getSource() == bChangeTo) {
			ok();
		}
		else if (evt.getSource() == bRename) {
			String oldName = lSessions.getSelectedValue().toString();
			String newName = SessionManager.inputSessionName(this, oldName);
			if (newName != null) {
				File oldFile = new File(SessionManager.createSessionFileName(oldName));
				File newFile = new File(SessionManager.createSessionFileName(newName));
				boolean ok = oldFile.renameTo(newFile);
				setNewListModel();
				if (ok) {
					lSessions.setSelectedValue(newName, true);
					if (oldName.equals(currentSession))
						currentSession = newName;
				} else {
					GUIUtilities.error(this, "bufferlist.manager.error.rename", new Object[] { oldFile, newFile });
				}
			}
		}
		else if (evt.getSource() == bDelete) {
			String name = lSessions.getSelectedValue().toString();
			File file = new File(SessionManager.createSessionFileName(name));
			boolean ok = file.delete();
			setNewListModel();
			if (ok) {
				lSessions.setSelectedValue("default", true);
				if (name.equals(currentSession))
					currentSession = null;
			} else {
				GUIUtilities.error(this, "bufferlist.manager.error.delete",	new Object[] { file });
			}
		}
	}


	/**
	 * Invoked when the selected item in the lSessions list changes.
	 */
	public void valueChanged(ListSelectionEvent evt) {
		Object[] values = lSessions.getSelectedValues();
		if (values == null || values.length == 0) {
			// no selection
			bRename.setEnabled(false);
			bDelete.setEnabled(false);
			bChangeTo.setEnabled(false);
		} else {
			boolean isCurrentSession = values[0].toString().equals(currentSession);
			boolean isDefaultSession = values[0].toString().equalsIgnoreCase("default");
			bChangeTo.setEnabled(!isCurrentSession);
			bRename.setEnabled(!isDefaultSession);
			bDelete.setEnabled(!isDefaultSession);
		}
	}


	private void setNewListModel() {
		final Vector listData = SessionManager.getSessionNames();
		lSessions.setModel(new AbstractListModel() {
			public int getSize() { return listData.size(); }
			public Object getElementAt(int i) { return listData.elementAt(i); }
		});
	}


	/**
	 * Convenience method for adding components to the GridBagLayout of this dialog.
	 */
	private void addComponent(Component comp,
			int gridwidth, int gridheight,
			double weightx, double weighty,
			int anchor, int fill,
			Insets insets)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth = gridwidth;
		constraints.gridheight = gridheight;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.anchor = anchor;
		constraints.fill = fill;
		constraints.insets = insets;

		GridBagLayout gridBag = (GridBagLayout) getContentPane().getLayout();
		gridBag.setConstraints(comp, constraints);
		getContentPane().add(comp);
	}


	/**
	 * A <tt>MouseListener</tt> for handling double-clicks on the sessions list.
	 */
	private class MouseHandler extends MouseAdapter {

		public void mousePressed(MouseEvent evt) {
			if (evt.getSource() == lSessions && evt.getClickCount() == 2) {
				ok();
			}
		}

	}


	JList lSessions; // cannot be private due to compiler bug
	private JButton bRename;
	private JButton bDelete;
	private JButton bChangeTo;
	private JButton bClose;
	private String selectedSession;
	private String currentSession;

}

