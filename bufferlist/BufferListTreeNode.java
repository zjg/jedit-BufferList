/*{{{ header
 * BufferListNode - tree node representation for BufferList
 * Copyright (c) 2004 Karsten Pilz
 *
 * :tabSize=4:indentSize=4:noTabs=false:maxLineLen=0:folding=explicit:collapseFolds=0:
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *}}}
 */
package bufferlist;

//{{{ imports
import org.gjt.sp.jedit.*;
import javax.swing.tree.DefaultMutableTreeNode;

//}}}

/**
 * This is the tree node.
 */
public class BufferListTreeNode extends DefaultMutableTreeNode {
	//{{{instance variables
	/**
	 *
	 */
	private String user_path;

	private boolean used = false;

	private boolean expanded = false;

	private boolean isConnected = false;

	// private int reused = 0;	// NOTE: debug only
	//}}}

	//{{{ +BufferListTreeNode(Object userObject) : <init>
	public BufferListTreeNode(Object userObject) {
		super(userObject);
		init(userObject);
	} //}}}

	//{{{ +BufferListTreeNode(Object userObject, boolean allowsChildren) : <init>
	public BufferListTreeNode(Object userObject, boolean allowsChildren) {
		super(userObject, allowsChildren);
		init(userObject);
	} //}}}

	public void reset() {
		// reused += 1;	// NOTE: debug only
		used = false;
		expanded = false;
		isConnected = false;
		if (isDirNode() && user_path != null) {
			setUserObject((String) user_path);
		}
	}

	private void init(Object userObject) {
		reset();
		if (userObject instanceof Buffer)
			user_path = ((Buffer) userObject).getPath();
		else if (userObject instanceof String)
			user_path = (String) userObject;
		else
			user_path = "ERROR";
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public void setConnected() {
		this.isConnected = true;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	/*
	 public String getReused()
	 {
	 Integer obj = new Integer(reused);
	 return obj.toString();
	 }
	 */

	public String getUserPath() {
		return user_path;
	}

	public Buffer getBuffer() {
		return (Buffer) this.getUserObject();
	}

	public boolean isExpanded() {
		return this.expanded;
	}

	public boolean isBuffer() {
		return (this.getUserObject() instanceof Buffer);
	}

	public boolean isDirNode() {
		return (this.getUserObject() instanceof String);
	}
}

