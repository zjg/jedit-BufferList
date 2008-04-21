/*{{{ header
 * BufferListRenderer.java
 * Copyright (c) 2000-2002 Dirk Moebius
 * Copyright (c) 2004 Karsten Pilz
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package bufferlist;

// {{{ imports
import gnu.regexp.RE;
import gnu.regexp.REException;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.WeakHashMap;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

// }}}

public class BufferListRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = 1L;

	// {{{ constants
	private static final String USER_HOME = System.getProperty("user.home");

	private static final String USER_HOME_SEP = USER_HOME + java.io.File.separator;// }}}

	// {{{ instance variables
	private View view;

	/**
	 * Coloring rules.
	 */
	private ArrayList<ColorEntry> colors;

	/**
	 * Cache of colors for file names.
	 */
	private WeakHashMap<String, Color> name2color;

	private Color colNormal = UIManager.getColor("Tree.foreground");

	private Color colSelected = UIManager.getColor("Tree.selectionForeground");

	/**
	 * Font of leaf for invisible buffer.
	 */
	private Font fontNormal;

	/**
	 * Font of leaf for visible buffer.
	 */
	private Font fontSelected;

	/**
	 * The way we should clip labels.
	 */
	private int textClipping;

	/**
	 * Tree control.
	 */
	private JTree tree;

	/**
	 * Row in the tree control.
	 */
	private int row;// }}}

	// {{{ +BufferListRenderer(View) : <init>
	public BufferListRenderer(View view)
	{
		this.view = view;
		name2color = new WeakHashMap<String, Color>();
		textClipping = jEdit.getIntegerProperty("bufferlist.textClipping", 1);

		Font font = jEdit.getFontProperty("bufferlist.font", UIManager.getFont("Tree.font"));
		fontNormal = font.deriveFont(font.isItalic() ? Font.ITALIC : Font.PLAIN);
		fontSelected = font.deriveFont(font.isItalic() ? Font.BOLD | Font.ITALIC : Font.BOLD);
	} // }}}
	
	// {{{ +getTreeCellRendererComponent(JTree, Object, boolean, boolean,
	// boolean, int, boolean) : Component
	/**
	 * Configures the renderer.
	 * 
	 * @return this
	 */
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
		boolean isExpanded, boolean isLeaf, int row, boolean hasFocus)
	{
		// configure this component
		super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row,
			hasFocus);

		this.tree = tree;
		this.row = row;

		BufferListTreeNode node = (BufferListTreeNode) value;
		if (node.isBuffer())
		{
			// Buffer entry
			Buffer buffer = node.getBuffer();
			String name = buffer.getName();
			setText(name);
			setToolTipText(node.getUserPath());
			setIcon(buffer.getIcon());
			setFont(buffer == view.getBuffer() ? fontSelected : fontNormal);
			setForeground(isSelected ? colSelected : getColor(name));
		}
		else if (node.isDirNode())
		{
			// Directory entry
			String path = (String) node.getUserObject();
			if (jEdit.getBooleanProperty("bufferlist.shortenHome", true))
			{
				if (path.equals(USER_HOME))
				{
					path = "~";
				}
				else if (path.startsWith(USER_HOME_SEP))
				{
					path = "~" + path.substring(USER_HOME.length());
				}
			}
			// setText((node.isExpanded()?"+":"-")+node.getReused()+":"+obj.toString());
			// // NOTE: debug only
			setText(path);
			setToolTipText(node.getUserPath());
			setIcon(null);
			setFont(fontNormal);
			setForeground(isSelected ? colSelected : colNormal);
		}

		return this;
	} // }}}

	// {{{ +paintComponent(Graphics g) : void
	public void paintComponent(Graphics g)
	{
		if (textClipping != 0)
		{
			String toShow = getText();
			Rectangle bounds = tree.getRowBounds(row);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(toShow);
			int textStart = (int) bounds.getX();

			if (getIcon() != null)
			{
				textStart += getIcon().getIconWidth() + getIconTextGap();
			}

			if (textStart < tree.getParent().getWidth()
				&& textStart + width > tree.getParent().getWidth())
			{
				// figure out how much to clip
				int availableWidth = tree.getParent().getWidth() - textStart
					- fm.stringWidth("...");
				int shownChars = 0;
				for (int i = 1; i < toShow.length(); i++)
				{
					width = (textClipping == 1) // clip at start
					? fm.stringWidth(toShow.substring(toShow.length() - i, toShow.length()))
						: fm.stringWidth(toShow.substring(0, i));
					if (width < availableWidth)
					{
						shownChars++;
					}
					else
					{
						break;
					}
				}

				if (shownChars > 0)
				{
					// ask the node whether it wants to be clipped at the start
					// or
					// at the end of the string
					if (textClipping == 1)
					{
						toShow = "..."
							+ toShow.substring(toShow.length() - shownChars, toShow.length());
					}
					else
					{
						toShow = toShow.substring(0, shownChars) + "...";
					}
					setText(toShow);
				}
			}
		}

		super.paintComponent(g);
	} // }}}

	// {{{ -getColor(String) : Color
	/**
	 * @param name
	 *            of the file
	 * @return color of label for this file name.
	 */
	private Color getColor(String name)
	{
		Color col = name2color.get(name);
		if (col != null)
		{
			return col;
		}
		loadColors();
		for (ColorEntry entry : colors)
		{
			if (entry.re.isMatch(name))
			{
				name2color.put(name, entry.color);
				return entry.color;
			}
		}
		return colNormal;
	} // }}}

	// {{{ -loadColors() : void
	private void loadColors()
	{
		if (colors != null)
		{
			return;
		}
		synchronized (BufferListRenderer.class)
		{
			colors = new ArrayList<ColorEntry>();
			if (!jEdit.getBooleanProperty("vfs.browser.colorize"))
			{
				return;
			}
			try
			{
				String glob;
				int i = 0;
				while ((glob = jEdit.getProperty("vfs.browser.colors." + i + ".glob")) != null)
				{
					colors.add(new ColorEntry(new RE(StandardUtilities.globToRE(glob)), jEdit
						.getColorProperty("vfs.browser.colors." + i + ".color", colNormal)));
					i++;
				}
			}
			catch (REException e)
			{
				Log.log(Log.ERROR, BufferList.class, "Error loading file list colors:");
				Log.log(Log.ERROR, BufferList.class, e);
			}
		}
	} // }}}

	// {{{ +class ColorEntry
	public static class ColorEntry
	{
		/**
		 * Regular expression for file name.
		 */
		RE re;

		/**
		 * Color for files of this type.
		 */
		Color color;

		ColorEntry(RE re, Color color)
		{
			this.re = re;
			this.color = color;
		}
	} // }}}
}
