/*
 * BufferListPlugin.java
 * Copyright (c) 2000,2001 Dirk Moebius
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


import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.DockableWindow;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.CreateDockableWindow;
import org.gjt.sp.jedit.msg.EditPaneUpdate;


/**
 * The BufferList plugin.
 *
 * @author Dirk Moebius
 */
public class BufferListPlugin extends EBPlugin {

	public static final String NAME = "bufferlist";


	public void start() {
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST, NAME);
	}


	public void createMenuItems(Vector menuItems) {
		menuItems.addElement(GUIUtilities.loadMenu("bufferlist.menu"));
	}


	public void createOptionPanes(OptionsDialog od) {
		od.addOptionPane(new BufferListOptionPane());
	}


	public void handleMessage(EBMessage message) {
		if (message instanceof CreateDockableWindow) {
			CreateDockableWindow cmsg = (CreateDockableWindow)message;
			if (cmsg.getDockableWindowName().equals(NAME)) {
				cmsg.setDockableWindow(new BufferList(cmsg.getView(), cmsg.getPosition()));
			}
		}
		else if (message instanceof BufferUpdate) {
			BufferUpdate bu = (BufferUpdate) message;
			if (bu.getWhat() == BufferUpdate.CREATED || bu.getWhat() == BufferUpdate.CLOSED) {
				if (jEdit.getBooleanProperty("bufferlist.autoshow", false)) {
					// FIXME: how to get the view the cursor is in?
					View view = jEdit.getFirstView();
					if (view != null)
						view.getDockableWindowManager().addDockableWindow(NAME);
				}
			}
		}
		else if (message instanceof EditPaneUpdate) {
			EditPaneUpdate epu = (EditPaneUpdate) message;
			if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				if (jEdit.getBooleanProperty("bufferlist.autoshow", false)) {
					View view = ((EditPane) epu.getSource()).getView();
					if (view != null)
						view.getDockableWindowManager().addDockableWindow(NAME);
				}
			}
		}
	}

}

