/*
 * BufferListOptionPane.java - plugin options pane for BufferList
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


import javax.swing.JCheckBox;
import javax.swing.UIManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.FontSelector;


/**
 * This is the option pane that jEdit displays for BufferList's options.
 */
public class BufferListOptionPane extends AbstractOptionPane
{

	private FontSelector fontSel;
	private JCheckBox bAutoShow;
	private JCheckBox bCloseFilesOnDoubleClick;


	public BufferListOptionPane()
	{
		super("bufferlist");
	}


	public void _init()
	{
		fontSel = new FontSelector(jEdit.getFontProperty("bufferlist.font", UIManager.getFont("Tree.font")));

		bAutoShow = new JCheckBox(jEdit.getProperty("options.bufferlist.autoshow"),
			jEdit.getBooleanProperty("bufferlist.autoshow", false));

		bCloseFilesOnDoubleClick = new JCheckBox(jEdit.getProperty("options.bufferlist.closeFilesOnDoubleClick"),
			jEdit.getBooleanProperty("bufferlist.closeFilesOnDoubleClick", true));


		addComponent(jEdit.getProperty("options.bufferlist.font"), fontSel);
		addComponent(bAutoShow);
		addComponent(bCloseFilesOnDoubleClick);
	}


	public void _save()
	{
		jEdit.setFontProperty("bufferlist.font", fontSel.getFont());
		jEdit.setBooleanProperty("bufferlist.closeFilesOnDoubleClick", bCloseFilesOnDoubleClick.isSelected());
		jEdit.setBooleanProperty("bufferlist.autoshow", bAutoShow.isSelected());
	}

}

