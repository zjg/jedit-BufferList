/*{{{ header
 * BufferListPlugin.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *}}}
 */
package bufferlist;

//{{{ imports
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * The BufferList plugin.
 *
 * @author Dirk Moebius
 */
public class BufferListPlugin extends EBPlugin
{
	//{{{ +start() : void
	public void start()
	{
		// nothing to do...
	} //}}}

	//{{{ +createMenuItems(Vector) : void
	public void createMenuItems(Vector menuItems)
	{
		// menuItems.addElement(GUIUtilities.loadMenu("bufferlist.menu"));
	} //}}}

	//{{{ +createOptionPanes(OptionsDialog) : void
	public void createOptionPanes(OptionsDialog od)
	{
		od.addOptionPane(new BufferListOptionPane());
	} //}}}

	//{{{ +handleMessage(EBMessage) : void
	public void handleMessage(EBMessage message)
	{
		if(message instanceof BufferUpdate)
		{
			BufferUpdate bu = (BufferUpdate) message;
			if(jEdit.getBooleanProperty("bufferlist.autoshow", false)
				&& bu.getView() != null
				&& (bu.getWhat() == BufferUpdate.CREATED || bu.getWhat() == BufferUpdate.CLOSED))
			{
				bu.getView().getDockableWindowManager().addDockableWindow("bufferlist");
			}
		}
		else if(message instanceof EditPaneUpdate)
		{
			EditPaneUpdate epu = (EditPaneUpdate) message;
			if(jEdit.getBooleanProperty("bufferlist.autoshow", false)
				&& epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
			{
				View view = ((EditPane) epu.getSource()).getView();
				if(view != null)
					view.getDockableWindowManager().addDockableWindow("bufferlist");
			}
		}
	} //}}}
}

