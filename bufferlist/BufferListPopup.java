/*
 * BufferListPopup.java - provides popup actions for BufferList
 * Copyright (c) 2000 Dirk Moebius
 * With inspiration from Jason Ginchereau and Slava Pestov
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */


package bufferlist;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.BufferHistory;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;


/**
 * A popup menu for BufferList.
 *
 * @author   Dirk Moebius
 */
public class BufferListPopup extends JPopupMenu
{

	private View view;
	private String path;


	public BufferListPopup(
			View view,
			String path,
			boolean isOpenFilesList,
			boolean isCurrent)
	{
		this.view = view;
		this.path = path;

		if (isOpenFilesList) {
			if (!isCurrent)
				add(createMenuItem("goto"));
			add(createMenuItem("open-view"));
			addSeparator();
			add(createMenuItem("close"));
			add(createMenuItem("save"));
			add(createMenuItem("save-as"));
			addSeparator();
			add(createMenuItem("reload"));
		} else {
			add(createMenuItem("open"));
			add(createMenuItem("open-view"));
			addSeparator();
			add(createMenuItem("remove"));
		}
	}


	private JMenuItem createMenuItem(String name) {
		String label = jEdit.getProperty("bufferlist.popup." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	}


	class ActionHandler implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			String actionCommand = evt.getActionCommand();

			if (actionCommand.equals("goto")) {
				Buffer buffer = jEdit.getBuffer(path);
				if (buffer != null)
					view.setBuffer(buffer);
			}
			else if (actionCommand.equals("open")) {
				jEdit.openFile(view, path);
			}
			else if (actionCommand.equals("open-view")) {
				Buffer buffer = jEdit.openFile(null, path);
				if (buffer != null)
					jEdit.newView(view, buffer);
			}
			else if (actionCommand.equals("close")) {
				Buffer buffer = jEdit.getBuffer(path);
				if (buffer != null)
					jEdit.closeBuffer(view, buffer);
			}
			else if (actionCommand.equals("save")) {
				Buffer buffer = jEdit.getBuffer(path);
				if (buffer != null)
					buffer.save(view, null);
			}
			else if (actionCommand.equals("save-as")) {
				Buffer buffer = jEdit.getBuffer(path);
				if (buffer != null)
					buffer.saveAs(view, true);
			}
			else if (actionCommand.equals("reload")) {
				Buffer buffer = jEdit.getBuffer(path);
				if (buffer != null)
					buffer.reload(view);
			}
			else if (actionCommand.equals("remove")) {
				Vector history = BufferHistory.getBufferHistory();
				for (int i = history.size() - 1; i >= 0; --i) {
					BufferHistory.Entry entry = (BufferHistory.Entry) history.elementAt(i);
					if (entry.path.equals(path)) {
						history.removeElementAt(i);
						jEdit.propertiesChanged();
						break;
					}
				}
			}

			view = null;
		}
	}

}

