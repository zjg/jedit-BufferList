/*{{{ header
 * BufferList.java
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

//{{{ imports
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;

//}}}

/**
 * A dockable panel that contains a list of open files.
 *
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent {
	//{{{ instance variables
	private View view;

	private String position;

	private TextAreaFocusHandler textAreaFocusHandler;

	private JTree tree;

	private JScrollPane scrTree;

	private DefaultTreeModel model;

	private BufferListTreeNode rootNode;

	private boolean sortIgnoreCase;

	private boolean flatTree;

	private boolean CreateModelPending = false;

	private Hashtable DistinctDirs = null;

	private Buffer lastBuffer = null;

	private JLabel bufferCountsLabel = new JLabel();

	//}}}

	//{{{ +BufferList(View, String) : <init>
	public BufferList(View view, final String position) {
		super(new BorderLayout(0, 5));

		// <reusage of BufferListTreeNode>
		DistinctDirs = new Hashtable();
		final Object root = new Object() {
			public String toString() {
				return "ROOT";
			}
		};

		rootNode = new BufferListTreeNode(root);

		//
		//
		DistinctDirs = new Hashtable();
		DistinctDirs.put("ROOT", rootNode);
		// </reusage of BufferListTreeNode>

		this.view = view;
		this.position = position;
		this.textAreaFocusHandler = new TextAreaFocusHandler();
		this.sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");

		// tree:
		tree = new JTree();
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

		if (position.equals(DockableWindowManager.FLOATING)) {
			requestFocusOpenFiles();
		}

		currentBufferChanged();

		if (jEdit.getBooleanProperty("bufferlist.startExpanded")) {
			TreeTools.expandAll(tree);
		}
		// move tree scrollbar to the left, ie. show left side of the tree:
		SwingUtilities.invokeLater(new Runnable() {
			//{{{ +run() : void
			public void run() {
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
	public void requestFocusOpenFiles() {
		BufferListTreeNode node = getNode(view.getBuffer());
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
	public Buffer getCurrentBuffer() {
		return view.getBuffer();
	} //}}}

	//{{{ +nextBuffer() : void
	/**
	 * Go to next buffer in open files list.
	 * @since BufferList 0.5
	 */
	public void nextBuffer() {
		Buffer buffer = view.getBuffer();
		Enumeration enum = rootNode.depthFirstEnumeration();

		BufferListTreeNode first = null, next = null, node = null;

		while (enum.hasMoreElements()) {
			node = (BufferListTreeNode) enum.nextElement();
			if (first == null && node.isBuffer()) {
				first = node;
			}
			if (node.getUserObject() == buffer)
				break;
		}

		while (enum.hasMoreElements()) {
			node = (BufferListTreeNode) enum.nextElement();
			if (node.isBuffer()) {
				next = node;
				break;
			}
		}
		if (next == null) {
			next = first;
		}
		if (next != null) {
			Buffer nextBuffer = (Buffer) next.getUserObject();
			view.goToBuffer(nextBuffer);
		}
	} //}}}

	//{{{ +previousBuffer() : void
	/**
	 * Go to previous buffer in open files list.
	 * @since BufferList 0.5
	 */
	public void previousBuffer() {
		Buffer buffer = view.getBuffer();
		Enumeration enum = rootNode.depthFirstEnumeration();

		BufferListTreeNode prev = null, node = null;

		while (enum.hasMoreElements()) {
			node = (BufferListTreeNode) enum.nextElement();
			if (node.getUserObject() == buffer)
				break;
			if (node.isBuffer()) {
				prev = node;
			}
		}

		if (prev == null) {
			while (enum.hasMoreElements()) {
				node = (BufferListTreeNode) enum.nextElement();
				if (node.isBuffer()) {
					prev = node;
				}
			}
		}

		if (prev != null) {
			Buffer prevBuffer = (Buffer) prev.getUserObject();
			view.goToBuffer(prevBuffer);
		}
	} //}}}

	//{{{ +addNotify() : void
	/**
	 * Invoked when the component is created;
	 * adds focus event handlers to all EditPanes of the View
	 * associated with this BufferList.
	 */
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);

		if (view != null) {
			EditPane[] editPanes = view.getEditPanes();
			for (int i = 0; i < editPanes.length; i++)
				editPanes[i].getTextArea().addFocusListener(textAreaFocusHandler);
		}
	} //}}}

	//{{{ +removeNotify() : void
	/**
	 * Invoked when the component is removed;
	 * removes the focus event handlers from all EditPanes.
	 */
	public void removeNotify() {
		super.removeNotify();
		EditBus.removeFromBus(this);

		// removes focus event handlers from all EditPanes of the View
		// associated with this BufferList:
		if (view != null) {
			EditPane[] editPanes = view.getEditPanes();
			for (int i = 0; i < editPanes.length; i++)
				editPanes[i].getTextArea().removeFocusListener(textAreaFocusHandler);
		}
	} //}}}

	//{{{ +handleMessage(EBMessage) : void
	/** Handle jEdit EditBus messages */
	public void handleMessage(EBMessage message) {
		if (message instanceof BufferUpdate) {
			handleBufferUpdate((BufferUpdate) message);
		} else if (message instanceof EditPaneUpdate) {
			handleEditPaneUpdate((EditPaneUpdate) message);
		} else if (message instanceof PropertiesChanged) {
			handlePropertiesChanged();
		}
	} //}}}

	//{{{ -handleBufferUpdate(BufferUpdate) : void
	private void handleBufferUpdate(BufferUpdate bu) {
		if (bu.getWhat() == BufferUpdate.DIRTY_CHANGED) {
			updateNode(bu.getBuffer());
		} else if (bu.getWhat() == BufferUpdate.CREATED
				|| bu.getWhat() == BufferUpdate.CLOSED
				|| bu.getWhat() == BufferUpdate.SAVED) {
			recreateModel();
		}

		updateBufferCounts();
	} //}}}

	//{{{ -handleEditPaneUpdate(EditPaneUpdate) : void
	private void handleEditPaneUpdate(EditPaneUpdate epu) {
		// View v = ((EditPane) epu.getSource()).getView();
		View v = epu.getEditPane().getView();
		if (v != view)
			return; // not for this BufferList instance

		if (epu.getWhat() == EditPaneUpdate.CREATED) {
			epu.getEditPane().getTextArea().addFocusListener(textAreaFocusHandler);
		} else if (epu.getWhat() == EditPaneUpdate.DESTROYED) {
			epu.getEditPane().getTextArea().removeFocusListener(textAreaFocusHandler);
		} else if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
			currentBufferChanged();
		}
	} //}}}

	//{{{ -handlePropertiesChanged() : void
	private void handlePropertiesChanged() {
		boolean modelChanged = false;

		boolean newFlatTree = jEdit.getBooleanProperty("bufferlist.flatTree");
		if (flatTree != newFlatTree) {
			modelChanged = true;
			flatTree = newFlatTree;
			if (flatTree)
				tree.putClientProperty("JTree.lineStyle", "Horizontal");
			else
				tree.putClientProperty("JTree.lineStyle", "Angled");
		}

		// if textClipping at start/end then don't show scrollbar
		if (jEdit.getIntegerProperty("bufferlist.textClipping", 1) == 0) {
			scrTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		} else {
			scrTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		}

		boolean newSortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		if (sortIgnoreCase != newSortIgnoreCase) {
			modelChanged = true;
			sortIgnoreCase = newSortIgnoreCase;
		}

		if (modelChanged) {
			recreateModel();
		}

		// set new cell renderer to change fonts:
		tree.setCellRenderer(new BufferListRenderer(view));
	} //}}}

	//{{{ -updateBufferCounts() : void
	private void updateBufferCounts() {
		int dirtyBuffers = 0;
		Buffer buffers[] = jEdit.getBuffers();
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i].isDirty()) {
				dirtyBuffers++;
			}
		}
		bufferCountsLabel.setText(jEdit.getProperty("bufferlist.openfiles.label") + jEdit.getBufferCount() + " " + jEdit.getProperty("bufferlist.dirtyfiles.label") + dirtyBuffers);
	} //}}}

	//{{{ -getDir(Buffer) : String
	private static String getDir(Buffer buffer) {
		return buffer.getVFS().getParentOfPath(buffer.getPath());
	} //}}}

	//{{{ -createDirectoryNodes(String) : BufferListTreeNode
	private BufferListTreeNode createDirectoryNodes(String path) {
		VFS vfs = VFSManager.getVFSForPath(path);
		String parent = vfs.getParentOfPath(path);
		if (path == parent || path.equals(parent)) {
			return (BufferListTreeNode) DistinctDirs.get("ROOT");
		}

		BufferListTreeNode node = (BufferListTreeNode) DistinctDirs.get(path);
		if (node == null) {
			node = new BufferListTreeNode(path, true);
			DistinctDirs.put(path, node);
		}
		if (!node.isConnected()) {
			BufferListTreeNode parentNode;
			if (flatTree)
				parentNode = (BufferListTreeNode) DistinctDirs.get("ROOT");
			else
				parentNode = createDirectoryNodes(parent);
			parentNode.add(node);
			node.setConnected();
		}
		return node;
	} //}}}

	//{{{ -removeObsoleteDirNodes(BufferListTreeNode) : void
	/**
	 * Removes all intermediate directory nodes that only have one diretory node and no buffer
	 * nodes as children, i.e. removes unneccessary levels from the tree.
	 */
	private void removeObsoleteDirNodes(BufferListTreeNode node) {
		Vector vec = new Vector(node.getChildCount());
		Enumeration children = node.children();

		while (children.hasMoreElements()) {
			vec.add(children.nextElement());
		}

		node.removeAllChildren();

		children = vec.elements();
		while (children.hasMoreElements()) {
			BufferListTreeNode child = (BufferListTreeNode) children.nextElement();
			removeObsoleteDirNodes(child);

			boolean keep = (child.getChildCount() > 1) || (child.getUserObject() instanceof Buffer);

			if (!keep && child.getChildCount() == 1) {
				if (((BufferListTreeNode) child.getFirstChild()).getUserObject() instanceof Buffer) {
					keep = true;
				}
			}

			if (keep) {
				node.add(child);
			} else {
				Enumeration childChildren = child.children();
				while (childChildren.hasMoreElements()) {
					node.add((BufferListTreeNode) childChildren.nextElement());
				}
			}
		}
	} //}}}

	//{{{ -removeDirNodesCommonPrefixes(BufferListTreeNode, String) : void
	/**
	 * Removes the path prefix that is present in the parent node (for each directory node)
	 */
	private void removeDirNodesCommonPrefixes(BufferListTreeNode node, String prefix) {
		Enumeration children = node.children();

		while (children.hasMoreElements()) {
			BufferListTreeNode child = (BufferListTreeNode) children.nextElement();
			if (child.getUserObject() instanceof String) {
				String child_prefix = (String) child.getUserObject();
				if (child_prefix.startsWith(prefix)) {
					child.setUserObject(child_prefix.substring(prefix.length()));
				}
				removeDirNodesCommonPrefixes(child, child_prefix);
			} else {
				removeDirNodesCommonPrefixes(child, prefix);
			}
		}
	} //}}}

	//{{{ -saveExpansionState() : void
	/**
	 * Saves the expansion state of all directory nodes (within each BufferListTreeNode).
	 */
	private void saveExpansionState() {
		Enumeration enum;
		enum = DistinctDirs.elements();
		while (enum.hasMoreElements()) {
			((BufferListTreeNode) enum.nextElement()).reset();
		}

		enum = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		if (enum != null) {
			while (enum.hasMoreElements()) {
				TreePath expPath = (TreePath) enum.nextElement();
				if (expPath.getLastPathComponent() instanceof BufferListTreeNode) {
					BufferListTreeNode node = (BufferListTreeNode) expPath.getLastPathComponent();
					node.setExpanded(true);
				}
			}
		}
	} //}}}

	//{{{ -restoreExpansionState() : void
	/**
	 * Restrores the expansion state of all directory nodes.
	 */
	private void restoreExpansionState() {
		Enumeration enum = DistinctDirs.elements();
		while (enum.hasMoreElements()) {
			BufferListTreeNode node = (BufferListTreeNode) enum.nextElement();
			if (node.isExpanded()) {
				tree.expandPath(new TreePath(node.getPath()));
			}
		}
	} //}}}

	//{{{ -recreateModel() : void
	/**
	 * Schedules a recreation of the tree model (preserving the current expansion state).
	 */
	private void recreateModel() {
		if (!CreateModelPending) {
			CreateModelPending = true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					saveExpansionState();
					createModel();
					restoreExpansionState();
				}
			});
		}
	} //}}}

	//{{{ -createModel() : void
	/**
	 * Sets a new tree model.
	 */
	private void createModel() {
		Buffer[] buffers = jEdit.getBuffers();

		MiscUtilities.quicksort(buffers, new MiscUtilities.Compare() {
			public int compare(Object obj1, Object obj2) {
				if (obj1 == obj2)
					return 0;
				else {
					Buffer buf1 = (Buffer) obj1;
					Buffer buf2 = (Buffer) obj2;
					String dir1 = getDir(buf1);
					String dir2 = getDir(buf2);
					int cmpDir = MiscUtilities.compareStrings(dir1, dir2, sortIgnoreCase);
					if (cmpDir == 0)
						return MiscUtilities.compareStrings(buf1.getName(), buf2.getName(), sortIgnoreCase);
					else
						return cmpDir;
				}
			}
		});

/*
		// <when used here, BufferListTreeNode reuse is ommited>
		final Object root = new Object() {
			public String toString() {
				return "ROOT";
			}
		};

		rootNode = new BufferListTreeNode(root);

		//
		//
		DistinctDirs = new Hashtable();
		DistinctDirs.put("ROOT", rootNode);
		// </when used here, BufferListTreeNode reuse is ommited>
*/

		Enumeration enum = DistinctDirs.elements();
		while (enum.hasMoreElements()) {
			((BufferListTreeNode) enum.nextElement()).removeAllChildren();
		}

		for (int i = 0; i < buffers.length; ++i) {
			Buffer buffer = buffers[i];
			BufferListTreeNode dirNode = createDirectoryNodes(buffer.getVFS().getParentOfPath(buffer.getPath()));
			dirNode.add(new BufferListTreeNode(buffer, false));
		}

		removeObsoleteDirNodes(rootNode);	// NOTE: when ommited, the tree contains all intermediate levels i.e. gets its "full depth"
		removeDirNodesCommonPrefixes(rootNode, "");

		model = new DefaultTreeModel(rootNode);
		tree.setModel(model);
		CreateModelPending = false;
	} //}}}

	//{{{ -getNode(Buffer) : BufferListTreeNode
	/**
	 * @return the tree node for the jEdit buffer, or null if the buffer
	 *  cannot be found in the current tree model.
	 */
	private BufferListTreeNode getNode(Buffer buffer) {
		Enumeration enum = rootNode.depthFirstEnumeration();
		while (enum.hasMoreElements()) {
			BufferListTreeNode node = (BufferListTreeNode) enum.nextElement();
			if (node.getUserObject() == buffer)
				return node;
		}
		return null;
	} //}}}

	//{{{ -updateNode(Buffer) : void
	private void updateNode(Buffer buffer) {
		BufferListTreeNode node = getNode(buffer);
		if (node == null)
			return;

		model.nodeChanged(node);
	} //}}}

	//{{{ -currentBufferChanged() : void
	/**
	 * Called after the current buffer has changed; notifies the cell
	 * renderer and makes sure the current buffer is visible.
	 */
	private void currentBufferChanged() {
		Buffer buffer = view.getBuffer();
		BufferListTreeNode node = getNode(buffer);
		if (node == null)
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
		if (bounds != null) {
			bounds.width = 0;
			bounds.x = 0;
			tree.scrollRectToVisible(bounds);
		}
	} //}}}

	//{{{ -closeWindowAndFocusEditPane() : void
	private void closeWindowAndFocusEditPane() {
		if (position.equals(DockableWindowManager.FLOATING)) {
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
	private class TextAreaFocusHandler extends FocusAdapter {
		//{{{ +focusGained(FocusEvent) : void
		public void focusGained(FocusEvent evt) {
			Component comp = SwingUtilities.getAncestorOfClass(EditPane.class, (Component) evt.getSource());
			if (comp == null)
				return;

			Buffer buffer = ((EditPane) comp).getBuffer();
			if (buffer != lastBuffer)
				currentBufferChanged();
		} //}}}
	} //}}}

	//{{{ -class MouseHandler
	/**
	 * A mouse listener for the buffer list.
	 */
	private class MouseHandler extends MouseAdapter {
		//{{{ +mouseClicked(MouseEvent) : void
		/**
		 * invoked when the mouse button has been clicked (pressed and
		 * released) on the buffer list.
		 */
		public void mouseClicked(MouseEvent e) {
			// first exclude what we don't handle
			if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
				return;
			if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
				return;
			if (e.isAltDown() || e.isAltGraphDown() || e.isMetaDown()
					|| e.isShiftDown() || e.isControlDown())
				return;
			if (e.getClickCount() > 2)
				return;

			e.consume();

			TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;

			BufferListTreeNode node = (BufferListTreeNode) path.getLastPathComponent();
			Object obj = node.getUserObject();
			if (obj instanceof String)
				return;

			Buffer buffer = (Buffer) obj;
			if (e.getClickCount() == 2 && jEdit.getBooleanProperty("bufferlist.closeFilesOnDoubleClick", true)) {
				// left mouse double press: close buffer
				jEdit.closeBuffer(view, buffer);
			} else {
				// left mouse single press: open buffer
				view.goToBuffer(buffer);
			}
		} //}}}

		//{{{ +mousePressed(MouseEvent) : void
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger())
				showPopup(e);
		} //}}}

		//{{{ +mouseReleased(MouseEvent) : void
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger())
				showPopup(e);
		} //}}}

		//{{{ -showPopup(MouseEvent) : void
		private void showPopup(MouseEvent e) {
			e.consume();

			// if user didn't select any buffer, or selected only one buffer,
			// then select entry at mouse position:
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null || paths.length == 1) {
				TreePath locPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				if (locPath != null) {
					Rectangle nodeRect = tree.getPathBounds(locPath);
					if (nodeRect != null && nodeRect.contains(e.getX(), e.getY())) {
						paths = new TreePath[] { locPath };
						tree.setSelectionPath(locPath);
					}
				}
			}

			// check whether user selected a directory node:
			if (paths != null) {
				for (int i = 0; i < paths.length; ++i) {
					BufferListTreeNode node = (BufferListTreeNode) paths[i].getLastPathComponent();
					Object obj = node.getUserObject();
					if (obj != null && obj instanceof String) {
						// user selected directory node; select all entries below it:
						Enumeration children = node.depthFirstEnumeration();
						while (children.hasMoreElements()) {
							BufferListTreeNode childNode = (BufferListTreeNode) children.nextElement();
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
	private class KeyHandler extends KeyAdapter {
		//{{{ +keyPressed(KeyEvent) : void
		public void keyPressed(KeyEvent evt) {
			if (evt.isConsumed())
				return;

			int kc = evt.getKeyCode();
			if (kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_CANCEL) {
				evt.consume();
				tree.clearSelection();
				closeWindowAndFocusEditPane();
			} else if (kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_ACCEPT) {
				evt.consume();
				TreePath[] sel = tree.getSelectionPaths();
				if (sel != null && sel.length > 0) {
					if (sel.length > 1) {
						GUIUtilities.error(BufferList.this, "bufferlist.error.tooMuchSelection", null);
						return;
					} else {
						BufferListTreeNode node = (BufferListTreeNode) sel[0].getLastPathComponent();
						Object obj = node.getUserObject();
						if (obj instanceof Buffer)
							view.setBuffer((Buffer) obj);
					}
				}
				closeWindowAndFocusEditPane();
			}
		} //}}}
	} //}}}
}