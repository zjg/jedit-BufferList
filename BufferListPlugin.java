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
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.CreateDockableWindow;


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
				cmsg.setDockableWindow(new BufferList(cmsg.getView(),
					cmsg.getPosition()));
			}
		}
	}
}

