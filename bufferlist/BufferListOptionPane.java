/*{{{ header
 * BufferListOptionPane.java - plugin options pane for BufferList
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
 *}}}
 */
package bufferlist;

// {{{ imports
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.FontSelector;

// }}}

/**
 * This is the option pane that jEdit displays for BufferList's options.
 */
public class BufferListOptionPane extends AbstractOptionPane
{
	private static final long serialVersionUID = 1L;

	// {{{instance variables
	private FontSelector fontSel;

	private JCheckBox bAutoShow;

	private JCheckBox bCloseFilesOnDoubleClick;

	private JCheckBox bStartExpanded;

	private JCheckBox bFlatTree;

	private JCheckBox bShortenHome;

	private JRadioButton rbTextClipStart;

	private JRadioButton rbTextClipEnd;

	private JRadioButton rbTextClipNone; // }}}

	// {{{ +BufferListOptionPane() : <init>
	public BufferListOptionPane()
	{
		super("bufferlist");
	} // }}}

	// {{{ +_init() : void
	public void _init()
	{
		fontSel = new FontSelector(jEdit.getFontProperty("bufferlist.font", UIManager
			.getFont("Tree.font")));

		bAutoShow = new JCheckBox(jEdit.getProperty("options.bufferlist.autoshow"), jEdit
			.getBooleanProperty("bufferlist.autoshow", false));

		bCloseFilesOnDoubleClick = new JCheckBox(jEdit
			.getProperty("options.bufferlist.closeFilesOnDoubleClick"), jEdit.getBooleanProperty(
			"bufferlist.closeFilesOnDoubleClick", true));

		bStartExpanded = new JCheckBox(jEdit.getProperty("options.bufferlist.startExpanded"), jEdit
			.getBooleanProperty("bufferlist.startExpanded", false));

		bFlatTree = new JCheckBox(
			jEdit.getProperty("options.bufferlist.displayMode"),
			jEdit.getIntegerProperty("bufferlist.displayMode", BufferList.DISPLAY_MODE_FLAT_TREE) == BufferList.DISPLAY_MODE_FLAT_TREE);

		bShortenHome = new JCheckBox(jEdit.getProperty("options.bufferlist.shortenHome"), jEdit
			.getBooleanProperty("bufferlist.shortenHome", true));

		rbTextClipStart = new JRadioButton(jEdit
			.getProperty("options.bufferlist.textClipping.start"));
		rbTextClipEnd = new JRadioButton(jEdit.getProperty("options.bufferlist.textClipping.end"));
		rbTextClipNone = new JRadioButton(jEdit.getProperty("options.bufferlist.textClipping.none"));
		ButtonGroup bgTextClip = new ButtonGroup();
		bgTextClip.add(rbTextClipStart);
		bgTextClip.add(rbTextClipEnd);
		bgTextClip.add(rbTextClipNone);
		int textClipping = jEdit.getIntegerProperty("bufferlist.textClipping", 1);
		switch (textClipping)
		{
		case 0:
			rbTextClipNone.setSelected(true);
			break;
		case 1:
		default:
			rbTextClipStart.setSelected(true);
			break;
		case 2:
			rbTextClipEnd.setSelected(true);
			break;
		}

		addComponent(jEdit.getProperty("options.bufferlist.font"), fontSel);
		addComponent(bAutoShow);
		addComponent(bCloseFilesOnDoubleClick);
		addComponent(bStartExpanded);
		addComponent(bFlatTree);
		addComponent(bShortenHome);
		addComponent(new JLabel(jEdit.getProperty("options.bufferlist.textClipping.label")));
		addComponent("  ", rbTextClipStart);
		addComponent("  ", rbTextClipEnd);
		addComponent("  ", rbTextClipNone);
	} // }}}

	// {{{ +_save() : void
	public void _save()
	{
		jEdit.setFontProperty("bufferlist.font", fontSel.getFont());
		jEdit.setBooleanProperty("bufferlist.closeFilesOnDoubleClick", bCloseFilesOnDoubleClick
			.isSelected());
		jEdit.setBooleanProperty("bufferlist.autoshow", bAutoShow.isSelected());
		jEdit.setBooleanProperty("bufferlist.startExpanded", bStartExpanded.isSelected());
		jEdit.setIntegerProperty("bufferlist.displayMode",
			bFlatTree.isSelected() ? BufferList.DISPLAY_MODE_FLAT_TREE
				: BufferList.DISPLAY_MODE_HIERARCHICAL);
		jEdit.setBooleanProperty("bufferlist.shortenHome", bShortenHome.isSelected());
		jEdit.setIntegerProperty("bufferlist.textClipping", rbTextClipNone.isSelected() ? 0
			: rbTextClipStart.isSelected() ? 1 : 2);
	} // }}}
}
