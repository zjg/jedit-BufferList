/*
 * BufferListRenderer.java
 * Copyright (c) 2000-2002 Dirk Moebius
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */


package bufferlist;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import gnu.regexp.RE;
import gnu.regexp.REException;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;


public class BufferListRenderer extends DefaultTreeCellRenderer
{

	public BufferListRenderer(View view)
	{
		this.view = view;
	}


	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean isSelected,
		boolean isExpanded,
		boolean isLeaf,
		int row,
		boolean hasFocus)
	{
		JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree,
			value, isSelected, isExpanded, isLeaf, row, hasFocus);

		Object obj = ((DefaultMutableTreeNode)value).getUserObject();
		if(obj instanceof Buffer)
		{
			// Buffer entry
			Buffer buffer = (Buffer) obj;
			String name = buffer.getName();
			comp.setText(name);
			comp.setIcon(buffer.getIcon());
			comp.setFont(buffer == view.getBuffer() ? fontSelected : fontNormal);
			comp.setForeground(isSelected ? colSelected : getColor(name));
		}
		else if(obj instanceof String)
		{
			// Directory entry
			comp.setText(obj != null ? obj.toString() : "");
			comp.setIcon(null);
			comp.setFont(fontNormal);
			comp.setForeground(isSelected ? colSelected : colNormal);
		}

		return comp;
	}


	private Color getColor(String name)
	{
		if(name2color == null)
			name2color = new Hashtable();

		Color col = (Color) name2color.get(name);
		if(col != null)
			return col;

		loadColors();

		for(int i = 0; i < colors.size(); i++)
		{
			ColorEntry entry = (ColorEntry)colors.elementAt(i);
			if(entry.re.isMatch(name))
			{
				name2color.put(name, entry.color);
				return entry.color;
			}
		}

		return colNormal;
	}


	private void loadColors()
	{
		if(colors != null)
			return;

		synchronized(LOCK)
		{
			colors = new Vector();

			if(!jEdit.getBooleanProperty("vfs.browser.colorize"))
				return;

			try
			{
				String glob;
				int i = 0;
				while((glob = jEdit.getProperty("vfs.browser.colors." + i + ".glob")) != null)
				{
					colors.addElement(new ColorEntry(
						new RE(MiscUtilities.globToRE(glob)),
						jEdit.getColorProperty(
						"vfs.browser.colors." + i + ".color",
						colNormal)));
					i++;
				}
			}
			catch(REException e)
			{
				Log.log(Log.ERROR, BufferList.class, "Error loading file list colors:");
				Log.log(Log.ERROR, BufferList.class, e);
			}
		}
	}


	static class ColorEntry
	{
		RE re;
		Color color;

		ColorEntry(RE re, Color color)
		{
			this.re = re;
			this.color = color;
		}
	}


	private View view;
	private Vector colors;
	private Hashtable name2color;
	private Color colNormal = UIManager.getColor("Tree.foreground");
	private Color colSelected = UIManager.getColor("Tree.selectionForeground");
	private Font font = jEdit.getFontProperty("bufferlist.font", UIManager.getFont("Tree.font"));
	private Font fontNormal = font.deriveFont(Font.PLAIN);
	private Font fontSelected = font.deriveFont(Font.BOLD);

	private static Object LOCK = new Object();

}


