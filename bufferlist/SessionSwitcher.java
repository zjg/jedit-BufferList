/*
 * SessionSwitcher.java - toolbar for switching between jEdit sessions
 * Copyright (c) 2001 Dirk Moebius
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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;


/**
 * A control panel for switching between jEdit sessions.
 *
 * @author Dirk Moebius
 */
public class SessionSwitcher extends JToolBar implements ActionListener
{

	public SessionSwitcher(View view) {
		super();
		this.view = view;

		Insets nullInsets = new Insets(0,0,0,0);

		combo = new JComboBox();
		combo.setEditable(false);
		combo.addActionListener(this);

		save = new JButton(GUIUtilities.loadIcon("Save24.gif"));
		save.setMargin(nullInsets);
		save.setToolTipText(jEdit.getProperty("bufferlist.switcher.save.tooltip"));
		save.setFocusPainted(false);
		save.addActionListener(this);

		saveAs = new JButton(GUIUtilities.loadIcon("SaveAs24.gif"));
		saveAs.setMargin(nullInsets);
		saveAs.setToolTipText(jEdit.getProperty("bufferlist.switcher.saveAs.tooltip"));
		saveAs.setFocusPainted(false);
		saveAs.addActionListener(this);

		reload = new JButton(GUIUtilities.loadIcon("Redo24.gif"));
		reload.setMargin(nullInsets);
		reload.setToolTipText(jEdit.getProperty("bufferlist.switcher.reload.tooltip"));
		reload.setFocusPainted(false);
		reload.addActionListener(this);

		prefs = new JButton(GUIUtilities.loadIcon("Preferences24.gif"));
		prefs.setMargin(nullInsets);
		prefs.setToolTipText(jEdit.getProperty("bufferlist.switcher.prefs.tooltip"));
		prefs.setFocusPainted(false);
		prefs.addActionListener(this);

		setFloatable(false);
		putClientProperty("JToolBar.isRollover", Boolean.TRUE);

		addSeparator(new Dimension(5,5));  // just add a little space at begin, looks better

		if (jEdit.getBooleanProperty("bufferlist.switcher.showTitle", true))
			add(new JLabel(jEdit.getProperty("bufferlist.switcher.title")));

		add(combo);
		addSeparator();
		add(save);
		add(saveAs);
		add(reload);
		add(prefs);

		if (jEdit.getProperty("bufferlist.switcher.show", "bufferlist").equals("view")) {
			// if SessionSwitcher is displayed as View ToolBar, add some glue at the
			// end of the toolbar, so that the combo box doesn't get too long:
			add(Box.createGlue());
		}

		updateComboBox();
	}


	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == combo) {
			Object selectedSession = combo.getSelectedItem();
			if (selectedSession != null)
				SessionManager.getInstance().setCurrentSession(view, selectedSession.toString());
		}
		else if (evt.getSource() == save)
			SessionManager.getInstance().saveCurrentSession(view);
		else if (evt.getSource() == saveAs)
			SessionManager.getInstance().saveCurrentSessionAs(view);
		else if (evt.getSource() == reload)
			SessionManager.getInstance().reloadCurrentSession(view);
		else if (evt.getSource() == prefs)
			SessionManager.getInstance().showSessionManagerDialog(view);

		updateComboBox();
	}


	private void updateComboBox() {
		Vector model = SessionManager.getSessionNames();

		combo.removeActionListener(this);
		combo.setModel(new DefaultComboBoxModel(model));
		combo.setSelectedItem(SessionManager.getInstance().getCurrentSession());
		combo.addActionListener(this);
	}


	private View view;
	private JComboBox combo;
	private JButton save;
	private JButton saveAs;
	private JButton reload;
	private JButton prefs;

}

