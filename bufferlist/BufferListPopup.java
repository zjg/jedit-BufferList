/*{{{ header
 * BufferListPopup.java - provides popup actions for BufferList
 * Copyright (c) 2000-2002 Dirk Moebius
 *
 * :tabSize=4:indentSize=4:noTabs=false:maxLineLen=0:folding=explicit:collapseFolds=1:
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package bufferlist;

// {{{ imports
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.search.DirectoryListSet;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.search.SearchDialog;

// }}}

/**
 * A popup menu for BufferList.
 * 
 * @author Dirk Moebius
 */
public class BufferListPopup extends JPopupMenu
{
	private static final long serialVersionUID = 1L;

	// {{{ instance variables
	private View view;

	private JTree tree;

	private TreePath[] sel;

	private String dir;

	// }}}

	// {{{ +BufferListPopup(View, JTree, TreePath[]) : <init>
	public BufferListPopup(View view, JTree tree, TreePath[] sel, List<MenuEntries> extensions)
	{
		this.view = view;
		this.tree = tree;
		this.sel = sel;

		// check whether user selected at most one directory
		if (sel != null)
		{
			String lastDir = null;
			for (int i = 0; i < sel.length; ++i)
			{
				BufferListTreeNode node = (BufferListTreeNode) sel[i].getLastPathComponent();
				Object obj = node.getUserObject();
				if (obj instanceof String)
				{
					dir = (String) obj;
				}
				else if (obj instanceof Buffer)
				{
					dir = MiscUtilities.getParentOfPath(((Buffer) obj).getPath());
				}
				if (lastDir == null)
				{
					lastDir = dir;
				}
				else
				{
					if (!lastDir.equals(dir))
					{
						dir = null; // more than one dir in selection
						break;
					}
				}
			}
		}

		Buffer viewBuffer = view.getBuffer();
		Buffer selectedBuffer = null;
		String title = jEdit.getProperty("bufferlist.popup.title.allFiles");

		if (sel != null)
		{
			title = jEdit.getProperty("bufferlist.popup.title.multipleSelection");
			if (sel.length == 1)
			{
				// one entry selected
				BufferListTreeNode node = (BufferListTreeNode) sel[0].getLastPathComponent();
				Object obj = node.getUserObject();
				if (obj instanceof Buffer)
				{
					selectedBuffer = (Buffer) obj;
					title = selectedBuffer.getName();
				}
			}
		}

		add(title).setEnabled(false);
		addSeparator();

		if (selectedBuffer != null && selectedBuffer != viewBuffer)
		{
			add(createMenuItem("goto"));
		}

		if (selectedBuffer != null)
		{
			add(createMenuItem("open-in-new-view"));
		}

		if (sel != null)
		{
			add(createMenuItem("close"));
			add(createMenuItem("save"));
		}

		if (selectedBuffer != null)
		{
			add(createMenuItem("save-as"));
		}

		if (sel != null)
		{
			add(createMenuItem("reload"));
		}

		addSeparator();
		add(createMenuItem("expand-all"));
		add(createMenuItem("collapse-all"));

		if (dir != null)
		{
			addSeparator();
			add(createMenuItem("browse"));
			add(createMenuItem("search"));
		}

		if (sel != null)
		{
			addSeparator();
			add(createMenuItem("copy-paths"));
		}

		// process any menu extensions
		for (int i = 0; i < extensions.size(); ++i)
		{
			addSeparator();
			// casting here is safe as the entries have already been checked
			// when they were
			// added to the List in the BufferListPlugin class.
			MenuEntries me = extensions.get(i);
			me.addEntries(this, view, tree, sel);
		}
	} // }}}

	// {{{ -createMenuItem(String) : JMenuItem
	private JMenuItem createMenuItem(String name)
	{
		String label = jEdit.getProperty("bufferlist.popup." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	} // }}}

	// {{{ -class ActionHandler
	private class ActionHandler implements ActionListener
	{
		// {{{ +actionPerformed(ActionEvent) : void
		public void actionPerformed(ActionEvent evt)
		{
			String actionCommand = evt.getActionCommand();

			if (actionCommand.equals("expand-all"))
			{
				TreeTools.expandAll(tree);
			}
			else if (actionCommand.equals("collapse-all"))
			{
				TreeTools.collapseAll(tree);
			}
			else if (actionCommand.equals("browse"))
			{
				GUIUtilities.showVFSFileDialog(view, dir, VFSBrowser.BROWSER, true);
			}
			else if (actionCommand.equals("search"))
			{
				SearchAndReplace.setSearchFileSet(new DirectoryListSet(dir, "*[^~#]", true));
				SearchDialog.showSearchDialog(view, "", SearchDialog.DIRECTORY);
			}
			else if (sel != null)
			{
				StringBuffer pathStrings = new StringBuffer();
				for (int i = 0; i < sel.length; ++i)
				{
					if (i > 0)
					{
						pathStrings.append('\n');
					}

					BufferListTreeNode node = (BufferListTreeNode) sel[i].getLastPathComponent();
					Object obj = node.getUserObject();

					if (obj instanceof String)
					{
						pathStrings.append((String) obj);
					}
					else if (obj instanceof Buffer)
					{
						Buffer buffer = (Buffer) obj;
						pathStrings.append(buffer.getPath());
						if (actionCommand.equals("goto"))
						{
							view.setBuffer(buffer);
						}
						else if (actionCommand.equals("open-in-new-view"))
						{
							jEdit.newView(view, buffer);
						}
						else if (actionCommand.equals("close"))
						{
							jEdit.closeBuffer(view, buffer);
						}
						else if (actionCommand.equals("save"))
						{
							buffer.save(view, null);
						}
						else if (actionCommand.equals("save-as"))
						{
							buffer.saveAs(view, true);
						}
						else if (actionCommand.equals("reload"))
						{
							buffer.reload(view);
						}
					}
				}

				if (actionCommand.equals("copy-paths"))
				{
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
						new StringSelection(pathStrings.toString()), null);
				}
			}

			// help out the garbage collector
			view = null;
			tree = null;
			sel = null;
		} // }}}
	} // }}}
}
