/*{{{ header
 * BufferList.java
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
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * A dockable panel that contains a list of open files.
 *
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent 
{
	//{{{ instance variables
	private View view;
	private String position;
	private TextAreaFocusHandler textAreaFocusHandler;
	private JTree tree;
	private JScrollPane scrTree;
	private DefaultTreeModel model;
	private DefaultMutableTreeNode rootNode;
	private boolean sortIgnoreCase;
	private Buffer lastBuffer = null;
	private JLabel bufferCountsLabel = new JLabel();
	//}}}

	//{{{ +BufferList(View, String) : <init>
	public BufferList(View view, final String position)
	{
		super(new BorderLayout(0, 5));

		this.view = view;
		this.position = position;
		this.textAreaFocusHandler = new TextAreaFocusHandler();
		this.sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");

		// tree:
		tree = new JTree();
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.putClientProperty("JTree.lineStyle", "Horizontal");
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.addMouseListener(new MouseHandler());
		tree.addKeyListener(new KeyHandler());
		createModel();
		ToolTipManager.sharedInstance().registerComponent(tree);

		// scrollpane for tree:
		scrTree = new JScrollPane(tree);

		// overall layout:
		updateBufferCounts();
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, bufferCountsLabel);
		panel.add(BorderLayout.CENTER, scrTree);
		add(panel);

		handlePropertiesChanged();

		if(position.equals(DockableWindowManager.FLOATING))
		{
			requestFocusOpenFiles();
		}

		currentBufferChanged();

		if(jEdit.getBooleanProperty("bufferlist.startExpanded"))
		{
			TreeTools.expandAll(tree);
		}
		// move tree scrollbar to the left, ie. show left side of the tree:
		SwingUtilities.invokeLater(new Runnable()
		{
			//{{{ +run() : void
			public void run()
			{
				JScrollBar hbar = scrTree.getHorizontalScrollBar();
				hbar.setValue(hbar.getMinimum());
			} //}}}
		});
	} //}}}

	//{{{ +requestFocusOpenFiles() : void
	/**
	 * Invoked by action "bufferlist-to-front" only;
	 * sets the focus on the table of open files.
	 * @since BufferList 0.5
	 * @see actions.xml
	 */
	public void requestFocusOpenFiles()
	{
		DefaultMutableTreeNode node = getNode(view.getBuffer());
		TreePath path = new TreePath(node.getPath());
		tree.requestFocus();
		tree.expandPath(path);
		tree.setSelectionPath(path);
	} //}}}

	//{{{ +getCurrentBuffer() : Buffer
	/**
	 * @deprecated use @link{org.gjt.sp.jedit.View#getBuffer()} instead.
	 * @return the current buffer.
	 * @since BufferList 0.6.2
	 */
	public Buffer getCurrentBuffer()
	{
		return view.getBuffer();
	} //}}}

	//{{{ +nextBuffer() : void
	/**
	 * Go to next buffer in open files list.
	 * @since BufferList 0.5
	 */
	public void nextBuffer()
	{
		Buffer buffer = view.getBuffer();
		DefaultMutableTreeNode node = getNode(buffer);
		DefaultMutableTreeNode next = node.getNextSibling();
		if(next == null)
		{
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
			DefaultMutableTreeNode nextParent = parent.getNextSibling();
			if(nextParent == null)
				nextParent = (DefaultMutableTreeNode) rootNode.getFirstChild();
			next = (DefaultMutableTreeNode) nextParent.getFirstChild();
		}

		Buffer nextBuffer = (Buffer) next.getUserObject();
		view.goToBuffer(nextBuffer);
	} //}}}

	//{{{ +previousBuffer() : void
	/**
	 * Go to previous buffer in open files list.
	 * @since BufferList 0.5
	 */
	public void previousBuffer()
	{
		Buffer buffer = view.getBuffer();
		DefaultMutableTreeNode node = getNode(buffer);
		DefaultMutableTreeNode prev = node.getPreviousSibling();
		if(prev == null)
		{
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
			DefaultMutableTreeNode prevParent = parent.getPreviousSibling();
			if(prevParent == null)
			{
				prevParent = (DefaultMutableTreeNode) rootNode.getLastChild();
			}
			prev = (DefaultMutableTreeNode) prevParent.getLastChild();
		}

		Buffer prevBuffer = (Buffer) prev.getUserObject();
		view.goToBuffer(prevBuffer);
	} //}}}

	//{{{ +addNotify() : void
	/**
	 * Invoked when the component is created;
	 * adds focus event handlers to all EditPanes of the View
	 * associated with this BufferList.
	 */
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);

		if(view != null)
		{
			EditPane[] editPanes = view.getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
				editPanes[i].getTextArea().addFocusListener(textAreaFocusHandler);
		}
	} //}}}

	//{{{ +removeNotify() : void
	/**
	 * Invoked when the component is removed;
	 * removes the focus event handlers from all EditPanes.
	 */
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);

		// removes focus event handlers from all EditPanes of the View
		// associated with this BufferList:
		if(view != null)
		{
			EditPane[] editPanes = view.getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
				editPanes[i].getTextArea().removeFocusListener(textAreaFocusHandler);
		}
	} //}}}

	//{{{ +handleMessage(EBMessage) : void
	/** Handle jEdit EditBus messages */
	public void handleMessage(EBMessage message)
	{
		if(message instanceof BufferUpdate)
		{
			handleBufferUpdate((BufferUpdate)message);
		}
		else if(message instanceof EditPaneUpdate)
		{
			handleEditPaneUpdate((EditPaneUpdate)message);
		}
		else if(message instanceof PropertiesChanged)
		{
			handlePropertiesChanged();
		}
	} //}}}

	//{{{ -handleBufferUpdate(BufferUpdate) : void
	private void handleBufferUpdate(BufferUpdate bu)
	{
		if(bu.getWhat() == BufferUpdate.CREATED)
		{
			insertNode(bu.getBuffer());
		}
		else if(bu.getWhat() == BufferUpdate.CLOSED)
		{
			removeNode(bu.getBuffer());
		}
		else if(bu.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			updateNode(bu.getBuffer());
		}
		else if(bu.getWhat() == BufferUpdate.SAVED)
		{
			TreePath[] expandedPaths = TreeTools.getExpandedPaths(tree);
			createModel();
			TreeTools.setExpandedPaths(tree, expandedPaths);
		}

		updateBufferCounts();
	} //}}}

	//{{{ -handleEditPaneUpdate(EditPaneUpdate) : void
	private void handleEditPaneUpdate(EditPaneUpdate epu)
	{
		// View v = ((EditPane) epu.getSource()).getView();
		View v = epu.getEditPane().getView();
		if(v != view)
			return; // not for this BufferList instance

		if(epu.getWhat() == EditPaneUpdate.CREATED)
		{
			epu.getEditPane().getTextArea().addFocusListener(textAreaFocusHandler);
		}
		else if(epu.getWhat() == EditPaneUpdate.DESTROYED)
		{
			epu.getEditPane().getTextArea().removeFocusListener(textAreaFocusHandler);
		}
		else if(epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
		{
			currentBufferChanged();
		}

	} //}}}

	//{{{ -handlePropertiesChanged() : void
	private void handlePropertiesChanged()
	{
		boolean newSortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		if(sortIgnoreCase != newSortIgnoreCase)
		{
			sortIgnoreCase = newSortIgnoreCase;
			createModel();
		}

		// set new cell renderer to change fonts:
		tree.setCellRenderer(new BufferListRenderer(view));
	} //}}}

	//{{{ -updateBufferCounts() : void
	private void updateBufferCounts()
	{
		int dirtyBuffers = 0;
		Buffer buffers[] = jEdit.getBuffers();
		for(int i = 0; i < buffers.length; i++)
		{
			if(buffers[i].isDirty())
			{
				dirtyBuffers++;
			}
		}
		bufferCountsLabel.setText(jEdit.getProperty("bufferlist.openfiles.label") + jEdit.getBufferCount() + " " + jEdit.getProperty("bufferlist.dirtyfiles.label") + dirtyBuffers);

	} //}}}

	//{{{ -_getDir(Buffer)_ : String
	private static String getDir(Buffer buffer)
	{
		return buffer.getVFS().getParentOfPath(buffer.getPath());
	} //}}}

	//{{{ -createModel() : void
	/**
	 * Sets a new tree model.
	 */
	private void createModel()
	{
		Buffer[] buffers = jEdit.getBuffers();

		MiscUtilities.quicksort(buffers, new MiscUtilities.Compare()
		{
			public int compare(Object obj1, Object obj2)
			{
				if(obj1 == obj2)
					return 0;
				else
				{
					Buffer buf1 = (Buffer) obj1;
					Buffer buf2 = (Buffer) obj2;
					String dir1 = getDir(buf1);
					String dir2 = getDir(buf2);
					int cmpDir = MiscUtilities.compareStrings(dir1, dir2, sortIgnoreCase);
					if(cmpDir == 0)
						return MiscUtilities.compareStrings(buf1.getName(), buf2.getName(), sortIgnoreCase);
					else
						return cmpDir;
				}
			}
		});

		final Object root = new Object()
		{
			public String toString()
			{
				return "ROOT";
			}
		};

		rootNode = new DefaultMutableTreeNode(root);
		DefaultMutableTreeNode dirNode = null;
		String lastDir = null;

		for(int i = 0; i < buffers.length; ++i)
		{
			Buffer buffer = buffers[i];
			String dir = getDir(buffer);
			if(lastDir == null || dirNode == null || !dir.equals(lastDir))
			{
				dirNode = new DefaultMutableTreeNode(dir, true);
				rootNode.add(dirNode);
				lastDir = dir;
			}
			dirNode.add(new DefaultMutableTreeNode(buffer, false));
		}

		model = new DefaultTreeModel(rootNode);
		tree.setModel(model);
	} //}}}

	//{{{ -getNode(Buffer) : DefaultMutableTreeNode
	/**
	 * @return the tree node for the jEdit buffer, or null if the buffer
	 *  cannot be found in the current tree model.
	 */
	private DefaultMutableTreeNode getNode(Buffer buffer)
	{
		Enumeration enum = rootNode.depthFirstEnumeration();
		while(enum.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) enum.nextElement();
			if(node.getUserObject() == buffer)
				return node;
		}
		return null;
	} //}}}

	//{{{ -insertNode(Buffer) : void
	private void insertNode(Buffer buffer)
	{
		String dir = getDir(buffer);
		String name = buffer.getName();
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(buffer);

		// search for the right place to insert:
		Enumeration dirs = rootNode.children();
		int dirNo = 0;
		while(dirs.hasMoreElements())
		{
			DefaultMutableTreeNode dirNode = (DefaultMutableTreeNode) dirs.nextElement();
			int cmp = MiscUtilities.compareStrings(dirNode.getUserObject().toString(), dir, sortIgnoreCase);
			if(cmp == 0)
			{
				// found directory node; insert at the right place in the node's children
				Enumeration children = dirNode.children();
				int childNo = 0;
				while(children.hasMoreElements())
				{
					DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) children.nextElement();
					String nodeName = ((Buffer)nextNode.getUserObject()).getName();
					if(MiscUtilities.compareStrings(nodeName, name, sortIgnoreCase) >= 0)
						break;
					++childNo;
				}
				dirNode.insert(newNode, childNo);
				model.nodesWereInserted(dirNode, new int[] { childNo });
				return;
			}
			else if(cmp > 0)
				break;
			++dirNo;
		}

		// directory node not yet there; create one:
		DefaultMutableTreeNode newDirNode = new DefaultMutableTreeNode(dir);
		newDirNode.insert(newNode, 0);
		rootNode.insert(newDirNode, dirNo);
		model.nodesWereInserted(rootNode, new int[] { dirNo });

		// expand the new directory node:
		TreePath path = new TreePath(newNode.getPath());
		tree.makeVisible(path);
	} //}}}

	//{{{ -removeNode(Buffer) : void
	private void removeNode(Buffer buffer)
	{
		DefaultMutableTreeNode node = getNode(buffer);
		if(node == null)
			return;

		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		int index = parent.getIndex(node);
		parent.remove(index);

		// if parent directory node has no children, remove that node, too:
		if(parent.getChildCount() == 0)
		{
			int parentIndex = rootNode.getIndex(parent);
			rootNode.remove(parentIndex);
			model.nodesWereRemoved(rootNode, new int[] { parentIndex }, new Object[] { parent });
		}
		else
			model.nodesWereRemoved(parent, new int[] { index }, new Object[] { node });
	} //}}}

	//{{{ -updateNode(Buffer) : void
	private void updateNode(Buffer buffer)
	{
		DefaultMutableTreeNode node = getNode(buffer);
		if(node == null)
			return;

		model.nodeChanged(node);
	} //}}}

	//{{{ -currentBufferChanged() : void
	/**
	 * Called after the current buffer has changed; notifies the cell
	 * renderer and makes sure the current buffer is visible.
	 */
	private void currentBufferChanged()
	{
		Buffer buffer = view.getBuffer();
		DefaultMutableTreeNode node = getNode(buffer);
		if(node == null)
			return;

		// Expand tree to show current buffer
		TreePath path = new TreePath(node.getPath());
		tree.expandPath(path);

		// Set new cell renderer to draw to current buffer bold
		tree.setCellRenderer(new BufferListRenderer(view));

		// Make sure path is visible.
		// Note: can't use tree.scrollPathToVisible(path) here, because it moves
		// the enclosing JScrollPane horizontally; but we want vertical movement
		// only.
		tree.makeVisible(path);
		Rectangle bounds = tree.getPathBounds(path);
		if(bounds != null)
		{
			bounds.width = 0;
			bounds.x = 0;
			tree.scrollRectToVisible(bounds);
		}
	} //}}}

	//{{{ -closeWindowAndFocusEditPane() : void
	private void closeWindowAndFocusEditPane()
	{
		if(position.equals(DockableWindowManager.FLOATING))
		{
			DockableWindowManager wm = view.getDockableWindowManager();
			wm.removeDockableWindow("bufferlist");
		}
		view.getTextArea().requestFocus();
	} //}}}

	//{{{ -class TextAreaFocusHandler
	/**
	 * Listens for a TextArea to get focus, to make the appropiate buffer
	 * in the BufferList bold.
	 */
	private class TextAreaFocusHandler extends FocusAdapter
	{
		//{{{ +focusGained(FocusEvent) : void
		public void focusGained(FocusEvent evt)
		{
			Component comp = SwingUtilities.getAncestorOfClass(EditPane.class, (Component) evt.getSource());
			if(comp == null)
				return;

			Buffer buffer = ((EditPane)comp).getBuffer();
			if(buffer != lastBuffer)
				currentBufferChanged();
		} //}}}
	} //}}}

	//{{{ -class MouseHandler
	/**
	 * A mouse listener for the buffer list.
	 */
	private class MouseHandler extends MouseAdapter
	{
		//{{{ +mouseClicked(MouseEvent) : void
		/**
		 * invoked when the mouse button has been clicked (pressed and
		 * released) on the buffer list.
		 */
		public void mouseClicked(MouseEvent e)
		{
			// first exclude what we don't handle
			if((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
				return;
			if((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
				return;
			if(e.isAltDown() || e.isAltGraphDown() || e.isMetaDown()
				|| e.isShiftDown() || e.isControlDown())
				return;
			if(e.getClickCount() > 2)
				return;

			e.consume();

			TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
			if(path == null)
				return;

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			Object obj = node.getUserObject();
			if(obj instanceof String)
				return;

			Buffer buffer = (Buffer) obj;
			if(e.getClickCount() == 2 && jEdit.getBooleanProperty("bufferlist.closeFilesOnDoubleClick", true))
			{
				// left mouse double press: close buffer
				jEdit.closeBuffer(view, buffer);
			}
			else
			{
				// left mouse single press: open buffer
				view.goToBuffer(buffer);
			}
		} //}}}

		//{{{ +mousePressed(MouseEvent) : void
		public void mousePressed(MouseEvent e)
		{
			if(e.isPopupTrigger())
				showPopup(e);
		} //}}}

		//{{{ +mouseReleased(MouseEvent) : void
		public void mouseReleased(MouseEvent e)
		{
			if(e.isPopupTrigger())
				showPopup(e);
		} //}}}

		//{{{ -showPopup(MouseEvent) : void
		private void showPopup(MouseEvent e)
		{
			e.consume();

			// if user didn't select any buffer, or selected only one buffer,
			// then select entry at mouse position:
			TreePath[] paths = tree.getSelectionPaths();
			if(paths == null || paths.length == 1)
			{
				TreePath locPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				if(locPath != null)
				{
					Rectangle nodeRect = tree.getPathBounds(locPath);
					if(nodeRect != null && nodeRect.contains(e.getX(), e.getY()))
					{
						paths = new TreePath[] { locPath };
						tree.setSelectionPath(locPath);
					}
				}
			}

			// check whether user selected a directory node:
			if(paths != null)
			{
				for(int i = 0; i < paths.length; ++i)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
					Object obj = node.getUserObject();
					if(obj != null && obj instanceof String)
					{
						// user selected directory node; select all entries below it:
						Enumeration children = node.children();
						while(children.hasMoreElements())
						{
							DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
							TreePath path = new TreePath(childNode.getPath());
							tree.addSelectionPath(path);
						}
					}
				}
			}

			// create & show popup
			paths = tree.getSelectionPaths();
			BufferListPopup popup = new BufferListPopup(view, tree, paths);
			popup.show(tree, e.getX() + 1, e.getY() + 1);
		} //}}}
	} //}}}

	//{{{ -class KeyHandler
	/**
	 * A key handler for the buffer list.
	 */
	private class KeyHandler extends KeyAdapter
	{
		//{{{ +keyPressed(KeyEvent) : void
		public void keyPressed(KeyEvent evt)
		{
			if(evt.isConsumed())
				return;

			int kc = evt.getKeyCode();
			if(kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_CANCEL)
			{
				evt.consume();
				tree.clearSelection();
				closeWindowAndFocusEditPane();
			}
			else if(kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_ACCEPT)
			{
				evt.consume();
				TreePath[] sel = tree.getSelectionPaths();
				if(sel != null && sel.length > 0)
				{
					if(sel.length > 1)
					{
						GUIUtilities.error(BufferList.this, "bufferlist.error.tooMuchSelection", null);
						return;
					}
					else
					{
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) sel[0].getLastPathComponent();
						Object obj = node.getUserObject();
						if(obj instanceof Buffer)
							view.setBuffer((Buffer)obj);
					}
				}
				closeWindowAndFocusEditPane();
			}
		} //}}}
	} //}}}
}
