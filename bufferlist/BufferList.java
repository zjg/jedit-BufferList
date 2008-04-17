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

// {{{ imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.StandardUtilities;

// }}}
/**
 * A dockable panel that contains a list of open files.
 * 
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent
{
	private static final long serialVersionUID = 1L;

	// {{{ display mode constants
	public static final int DISPLAY_MODE_FLAT_TREE = 1;

	public static final int DISPLAY_MODE_HIERARCHICAL = 2;// }}}

	// {{{ instance variables
	private final View view;

	private final String position;

	private final TextAreaFocusHandler textAreaFocusHandler;

	private final JTree tree;

	private final JScrollPane scrTree;

	private DefaultTreeModel model;

	private final BufferListTreeNode rootNode;

	private boolean sortIgnoreCase;

	private boolean ignoreSelectionChange;

	private int displayMode;

	private boolean CreateModelPending = false;

	private HashMap<String, BufferListTreeNode> DistinctDirs = null;

	private final Buffer lastBuffer = null;

	private final JLabel bufferCountsLabel = new JLabel();// }}}

	// {{{ +BufferList(View, String) : <init>
	public BufferList(View view, final String position)
	{
		super(new BorderLayout(0, 5));
		// <reusage of BufferListTreeNode>
		final Object root = new Object()
		{
			@Override
			public String toString()
			{
				return "ROOT";
			}
		};
		rootNode = new BufferListTreeNode(root);
		//
		//
		DistinctDirs = new HashMap<String, BufferListTreeNode>();
		DistinctDirs.put("ROOT", rootNode);
		// </reusage of BufferListTreeNode>
		this.view = view;
		this.position = position;
		textAreaFocusHandler = new TextAreaFocusHandler();
		sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		// tree:
		ignoreSelectionChange = false;
		tree = new JTree()
		{
			@Override
			protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
				boolean pressed)
			{
				ignoreSelectionChange = true;
				boolean res = super.processKeyBinding(ks, e, condition, pressed);
				ignoreSelectionChange = false;
				return res;
			}
		};
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.addMouseListener(new MouseHandler());
		tree.addKeyListener(new KeyHandler());
		tree.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				if (ignoreSelectionChange || e.getNewLeadSelectionPath() == null)
				{
					return;
				}
				BufferListTreeNode node = (BufferListTreeNode) e.getNewLeadSelectionPath()
					.getLastPathComponent();
				Object obj = node.getUserObject();
				if (obj instanceof Buffer)
				{
					BufferList.this.view.goToBuffer((Buffer)obj);
					return;
				}
			}
		});
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
		setDisplayMode(jEdit.getIntegerProperty("bufferlist.displayMode", DISPLAY_MODE_FLAT_TREE));
		handlePropertiesChanged();
		if (position.equals(DockableWindowManager.FLOATING))
		{
			requestFocusOpenFiles();
		}
		currentBufferChanged();
		if (jEdit.getBooleanProperty("bufferlist.startExpanded"))
		{
			TreeTools.expandAll(tree);
		}
		// move tree scrollbar to the left, ie. show left side of the tree:
		SwingUtilities.invokeLater(new Runnable()
		{
			// {{{ +run() : void
			public void run()
			{
				JScrollBar hbar = scrTree.getHorizontalScrollBar();
				hbar.setValue(hbar.getMinimum());
			} // }}}
		});
	} // }}}

	// {{{ +getInstanceForView(View) : BufferList
	/**
	 * Helper used by various actions in "actions.xml";
	 * 
	 * @since BufferList 1.0.2
	 * @see actions.xml
	 */
	public static BufferList getInstanceForView(View view)
	{
		DockableWindowManager mgr = view.getDockableWindowManager();
		BufferList bufferlist = (BufferList) mgr.getDockable("bufferlist");
		if (bufferlist == null)
		{
			mgr.addDockableWindow("bufferlist");
			bufferlist = (BufferList) mgr.getDockable("bufferlist");
		}
		return bufferlist;
	} // }}}

	// {{{ +requestFocusOpenFiles() : void
	/**
	 * Invoked by action "bufferlist-to-front" only; sets the focus on the table
	 * of open files.
	 * 
	 * @since BufferList 0.5
	 * @see actions.xml
	 */
	public void requestFocusOpenFiles()
	{
		BufferListTreeNode node = getNode(view.getBuffer());
		TreePath path = new TreePath(node.getPath());
		tree.requestFocus();
		tree.expandPath(path);
		tree.setSelectionPath(path);
	} // }}}

	// {{{ +nextBuffer() : void
	/**
	 * Go to next buffer in open files list.
	 * 
	 * @since BufferList 0.5
	 */
	public void nextBuffer()
	{
		Buffer buffer = view.getBuffer();
		Enumeration<BufferListTreeNode> e = rootNode.depthFirstEnumeration();
		BufferListTreeNode first = null, next = null, node = null;
		while (e.hasMoreElements())
		{
			node = e.nextElement();
			if (first == null && node.isBuffer())
			{
				first = node;
			}
			if (node.getUserObject() == buffer)
			{
				break;
			}
		}
		while (e.hasMoreElements())
		{
			node = e.nextElement();
			if (node.isBuffer())
			{
				next = node;
				break;
			}
		}
		if (next == null)
		{
			next = first;
		}
		if (next != null)
		{
			Buffer nextBuffer = (Buffer) next.getUserObject();
			view.goToBuffer(nextBuffer);
		}
	} // }}}

	// {{{ +previousBuffer() : void
	/**
	 * Go to previous buffer in open files list.
	 * 
	 * @since BufferList 0.5
	 */
	public void previousBuffer()
	{
		Buffer buffer = view.getBuffer();
		Enumeration<BufferListTreeNode> e = rootNode.depthFirstEnumeration();
		BufferListTreeNode prev = null, node = null;
		while (e.hasMoreElements())
		{
			node = e.nextElement();
			if (node.getUserObject() == buffer)
			{
				break;
			}
			if (node.isBuffer())
			{
				prev = node;
			}
		}
		if (prev == null)
		{
			while (e.hasMoreElements())
			{
				node = e.nextElement();
				if (node.isBuffer())
				{
					prev = node;
				}
			}
		}
		if (prev != null)
		{
			Buffer prevBuffer = (Buffer) prev.getUserObject();
			view.goToBuffer(prevBuffer);
		}
	} // }}}

	// {{{ +setDisplayMode(int) : void
	/**
	 * Helper function; may be called from "actions.xml"; set the display mode
	 * for this instance.
	 * 
	 * @since BufferList 1.0.2
	 */
	public void setDisplayMode(int pDisplayMode)
	{
		displayMode = pDisplayMode;
		if (displayMode == DISPLAY_MODE_FLAT_TREE)
		{
			tree.putClientProperty("JTree.lineStyle", "Horizontal");
		}
		else
		{
			tree.putClientProperty("JTree.lineStyle", "Angled");
		}
		recreateModel();
	} // }}}

	// {{{ +toggleDisplayMode() : void
	/**
	 * Invoked by action "bufferlist-toggle-display-mode" only; toggles between
	 * DISPLAY_MODE_FLAT_TREE/DISPLAY_MODE_HIERARCHICAL.
	 * 
	 * @since BufferList 1.0.2
	 * @see actions.xml
	 */
	public void toggleDisplayMode()
	{
		if (displayMode == DISPLAY_MODE_FLAT_TREE)
		{
			setDisplayMode(DISPLAY_MODE_HIERARCHICAL);
		}
		else
		{
			setDisplayMode(DISPLAY_MODE_FLAT_TREE);
		}
	} // }}}

	// {{{ +getDisplayMode(View) : int
	/**
	 * Used by "bufferlist-toggle-display-mode:IS_SELECTED"; returns the display
	 * mode for the view's bufferlist or the current default display mode.
	 * 
	 * @since BufferList 1.0.2
	 * @see actions.xml
	 */
	public static int getDisplayMode(View view)
	{
		DockableWindowManager mgr = view.getDockableWindowManager();
		BufferList bufferlist = (BufferList) mgr.getDockable("bufferlist");
		if (bufferlist == null)
		{
			return jEdit.getIntegerProperty("bufferlist.displayMode", DISPLAY_MODE_FLAT_TREE);
		}
		else
		{
			return bufferlist.displayMode;
		}
	} // }}}

	// {{{ +addNotify() : void
	/**
	 * Invoked when the component is created; adds focus event handlers to all
	 * EditPanes of the View associated with this BufferList.
	 */
	@Override
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
		if (view != null)
		{
			EditPane[] editPanes = view.getEditPanes();
			for (EditPane editPane : editPanes)
			{
				editPane.getTextArea().addFocusListener(textAreaFocusHandler);
			}
		}
	} // }}}

	// {{{ +removeNotify() : void
	/**
	 * Invoked when the component is removed; removes the focus event handlers
	 * from all EditPanes.
	 */
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
		// removes focus event handlers from all EditPanes of the View
		// associated with this BufferList:
		if (view != null)
		{
			EditPane[] editPanes = view.getEditPanes();
			for (EditPane editPane : editPanes)
			{
				editPane.getTextArea().removeFocusListener(textAreaFocusHandler);
			}
		}
	} // }}}

	// {{{ +handleMessage(EBMessage) : void
	/** Handle jEdit EditBus messages */
	public void handleMessage(EBMessage message)
	{
		if (message instanceof BufferUpdate)
		{
			handleBufferUpdate((BufferUpdate) message);
		}
		else if (message instanceof EditPaneUpdate)
		{
			handleEditPaneUpdate((EditPaneUpdate) message);
		}
		else if (message instanceof PropertiesChanged)
		{
			handlePropertiesChanged();
		}
	} // }}}

	// {{{ -handleBufferUpdate(BufferUpdate) : void
	private void handleBufferUpdate(BufferUpdate bu)
	{
		if (bu.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			updateNode(bu.getBuffer());
		}
		else if (bu.getWhat() == BufferUpdate.CREATED || bu.getWhat() == BufferUpdate.CLOSED
			|| bu.getWhat() == BufferUpdate.SAVED)
		{
			recreateModel();
		}
		updateBufferCounts();
	} // }}}

	// {{{ -handleEditPaneUpdate(EditPaneUpdate) : void
	private void handleEditPaneUpdate(EditPaneUpdate epu)
	{
		// View v = ((EditPane) epu.getSource()).getView();
		View v = epu.getEditPane().getView();
		if (v != view)
		{
			return; // not for this BufferList instance
		}
		if (epu.getWhat() == EditPaneUpdate.CREATED)
		{
			epu.getEditPane().getTextArea().addFocusListener(textAreaFocusHandler);
		}
		else if (epu.getWhat() == EditPaneUpdate.DESTROYED)
		{
			epu.getEditPane().getTextArea().removeFocusListener(textAreaFocusHandler);
		}
		else if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
		{
			currentBufferChanged();
		}
	} // }}}

	// {{{ -handlePropertiesChanged() : void
	private void handlePropertiesChanged()
	{
		boolean modelChanged = false;
		// if textClipping at start/end then don't show scrollbar
		if (jEdit.getIntegerProperty("bufferlist.textClipping", 1) == 0)
		{
			scrTree
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}
		else
		{
			scrTree.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}
		boolean newSortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		if (sortIgnoreCase != newSortIgnoreCase)
		{
			modelChanged = true;
			sortIgnoreCase = newSortIgnoreCase;
		}
		if (modelChanged)
		{
			recreateModel();
		}
		// set new cell renderer to change fonts:
		tree.setCellRenderer(new BufferListRenderer(view));
	} // }}}

	// {{{ -updateBufferCounts() : void
	private void updateBufferCounts()
	{
		int dirtyBuffers = 0;
		Buffer buffers[] = jEdit.getBuffers();
		for (Buffer buffer : buffers)
		{
			if (buffer.isDirty())
			{
				dirtyBuffers++;
			}
		}
		bufferCountsLabel.setText(jEdit.getProperty("bufferlist.openfiles.label")
			+ jEdit.getBufferCount() + " " + jEdit.getProperty("bufferlist.dirtyfiles.label")
			+ dirtyBuffers);
	} // }}}

	// {{{ -getDir(Buffer) : String
	private static String getDir(Buffer buffer)
	{
		return buffer.getVFS().getParentOfPath(buffer.getPath());
	} // }}}

	// {{{ -createDirectoryNodes(String) : BufferListTreeNode
	private BufferListTreeNode createDirectoryNodes(String path)
	{
		VFS vfs = VFSManager.getVFSForPath(path);
		String parent = vfs.getParentOfPath(path);
		if (path == parent || path.equals(parent))
		{
			return DistinctDirs.get("ROOT");
		}
		BufferListTreeNode node = DistinctDirs.get(path);
		if (node == null)
		{
			node = new BufferListTreeNode(path, true);
			DistinctDirs.put(path, node);
		}
		if (!node.isConnected())
		{
			BufferListTreeNode parentNode;
			if (displayMode == DISPLAY_MODE_FLAT_TREE)
			{
				parentNode = DistinctDirs.get("ROOT");
			}
			else
			{
				parentNode = createDirectoryNodes(parent);
			}
			parentNode.add(node);
			node.setConnected();
		}
		return node;
	} // }}}

	// {{{ -removeObsoleteDirNodes(BufferListTreeNode) : void
	/**
	 * Removes all intermediate directory nodes that only have one diretory node
	 * and no buffer nodes as children, i.e. removes unneccessary levels from
	 * the tree.
	 */
	private void removeObsoleteDirNodes(BufferListTreeNode node)
	{
		Vector<BufferListTreeNode> vec = new Vector<BufferListTreeNode>(node.getChildCount());
		Enumeration<BufferListTreeNode> children = node.children();
		while (children.hasMoreElements())
		{
			vec.add(children.nextElement());
		}
		node.removeAllChildren();
		children = vec.elements();
		while (children.hasMoreElements())
		{
			BufferListTreeNode child = children.nextElement();
			removeObsoleteDirNodes(child);
			boolean keep = child.getChildCount() > 1 || child.getUserObject() instanceof Buffer;
			if (!keep && child.getChildCount() == 1)
			{
				if (((BufferListTreeNode) child.getFirstChild()).getUserObject() instanceof Buffer)
				{
					keep = true;
				}
			}
			if (keep)
			{
				node.add(child);
			}
			else
			{
				Enumeration<BufferListTreeNode> childChildren = child.children();
				while (childChildren.hasMoreElements())
				{
					node.add(childChildren.nextElement());
				}
			}
		}
	} // }}}

	// {{{ -removeDirNodesCommonPrefixes(BufferListTreeNode, String) : void
	/**
	 * Removes the path prefix that is present in the parent node (for each
	 * directory node)
	 */
	private void removeDirNodesCommonPrefixes(BufferListTreeNode node, String prefix)
	{
		Enumeration children = node.children();
		while (children.hasMoreElements())
		{
			BufferListTreeNode child = (BufferListTreeNode) children.nextElement();
			if (child.getUserObject() instanceof String)
			{
				String child_prefix = (String) child.getUserObject();
				if (child_prefix.startsWith(prefix))
				{
					child.setUserObject(child_prefix.substring(prefix.length()));
				}
				removeDirNodesCommonPrefixes(child, child_prefix);
			}
			else
			{
				removeDirNodesCommonPrefixes(child, prefix);
			}
		}
	} // }}}

	// {{{ -saveExpansionState() : void
	/**
	 * Saves the expansion state of all directory nodes (within each
	 * BufferListTreeNode).
	 */
	private void saveExpansionState()
	{
		for (BufferListTreeNode node : DistinctDirs.values())
		{
			node.reset();
		}
		Enumeration<TreePath> e = tree.getExpandedDescendants(new TreePath(tree.getModel()
			.getRoot()));
		if (e != null)
		{
			while (e.hasMoreElements())
			{
				TreePath expPath = e.nextElement();
				if (expPath.getLastPathComponent() instanceof BufferListTreeNode)
				{
					BufferListTreeNode node = (BufferListTreeNode) expPath.getLastPathComponent();
					node.setExpanded(true);
				}
			}
		}
	} // }}}

	// {{{ -restoreExpansionState() : void
	/**
	 * Restrores the expansion state of all directory nodes.
	 */
	private void restoreExpansionState()
	{
		for (BufferListTreeNode node : DistinctDirs.values())
		{
			if (node.isExpanded())
			{
				tree.expandPath(new TreePath(node.getPath()));
			}
		}
	} // }}}

	// {{{ -recreateModel() : void
	/**
	 * Schedules a recreation of the tree model (preserving the current
	 * expansion state).
	 */
	private void recreateModel()
	{
		if (!CreateModelPending)
		{
			CreateModelPending = true;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					saveExpansionState();
					createModel();
					restoreExpansionState();
				}
			});
		}
	} // }}}

	// {{{ -createModel() : void
	/**
	 * Sets a new tree model.
	 */
	private void createModel()
	{
		Buffer[] buffers = jEdit.getBuffers();
		Arrays.sort(buffers, new Comparator<Buffer>()
		{
			public int compare(Buffer buf1, Buffer buf2)
			{
				if (buf1 == buf2)
				{
					return 0;
				}
				else
				{
					String dir1 = getDir(buf1);
					String dir2 = getDir(buf2);
					int cmpDir = StandardUtilities.compareStrings(dir1, dir2, sortIgnoreCase);
					if (cmpDir == 0)
					{
						return StandardUtilities.compareStrings(buf1.getName(), buf2.getName(),
							sortIgnoreCase);
					}
					else
					{
						return cmpDir;
					}
				}
			}
		});
		/*
		 * // <when used here, BufferListTreeNode reuse is ommited> final Object
		 * root = new Object() { public String toString() { return "ROOT"; } };
		 * 
		 * rootNode = new BufferListTreeNode(root); // // DistinctDirs = new
		 * Hashtable(); DistinctDirs.put("ROOT", rootNode); // </when used here,
		 * BufferListTreeNode reuse is ommited>
		 */
		for (BufferListTreeNode node : DistinctDirs.values())
		{
			node.removeAllChildren();
		}
		for (int i = 0; i < buffers.length; ++i)
		{
			Buffer buffer = buffers[i];
			BufferListTreeNode dirNode = createDirectoryNodes(buffer.getVFS().getParentOfPath(
				buffer.getPath()));
			dirNode.add(new BufferListTreeNode(buffer, false));
		}
		removeObsoleteDirNodes(rootNode); // NOTE: when ommited, the tree
		// contains all intermediate levels
		// i.e. gets its "full depth"
		removeDirNodesCommonPrefixes(rootNode, "");
		model = new DefaultTreeModel(rootNode);
		tree.setModel(model);
		CreateModelPending = false;
	} // }}}

	// {{{ -getNode(Buffer) : BufferListTreeNode
	/**
	 * @return the tree node for the jEdit buffer, or null if the buffer cannot
	 *         be found in the current tree model.
	 */
	private BufferListTreeNode getNode(Buffer buffer)
	{
		Enumeration<BufferListTreeNode> e = rootNode.depthFirstEnumeration();
		while (e.hasMoreElements())
		{
			BufferListTreeNode node = e.nextElement();
			if (node.getUserObject() == buffer)
			{
				return node;
			}
		}
		return null;
	} // }}}

	// {{{ -updateNode(Buffer) : void
	private void updateNode(Buffer buffer)
	{
		BufferListTreeNode node = getNode(buffer);
		if (node == null)
		{
			return;
		}
		model.nodeChanged(node);
	} // }}}

	// {{{ -currentBufferChanged() : void
	/**
	 * Called after the current buffer has changed; notifies the cell renderer
	 * and makes sure the current buffer is visible.
	 */
	private void currentBufferChanged()
	{
		Buffer buffer = view.getBuffer();
		BufferListTreeNode node = getNode(buffer);
		if (node == null)
		{
			return;
		}
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
		if (bounds != null)
		{
			bounds.width = 0;
			bounds.x = 0;
			tree.scrollRectToVisible(bounds);
		}
	} // }}}

	// {{{ -closeWindowAndFocusEditPane() : void
	private void closeWindowAndFocusEditPane()
	{
		if (position.equals(DockableWindowManager.FLOATING))
		{
			DockableWindowManager wm = view.getDockableWindowManager();
			wm.removeDockableWindow("bufferlist");
		}
		view.getTextArea().requestFocus();
	} // }}}

	// {{{ -class TextAreaFocusHandler
	/**
	 * Listens for a TextArea to get focus, to make the appropiate buffer in the
	 * BufferList bold.
	 */
	private class TextAreaFocusHandler extends FocusAdapter
	{
		// {{{ +focusGained(FocusEvent) : void
		@Override
		public void focusGained(FocusEvent evt)
		{
			Component comp = SwingUtilities.getAncestorOfClass(EditPane.class, (Component) evt
				.getSource());
			if (comp == null)
			{
				return;
			}
			Buffer buffer = ((EditPane) comp).getBuffer();
			if (buffer != lastBuffer)
			{
				currentBufferChanged();
			}
		} // }}}
	} // }}}

	// {{{ -class MouseHandler
	/**
	 * A mouse listener for the buffer list.
	 */
	private class MouseHandler extends MouseAdapter
	{
		// {{{ +mouseClicked(MouseEvent) : void
		/**
		 * invoked when the mouse button has been clicked (pressed and released)
		 * on the buffer list.
		 */
		@Override
		public void mouseClicked(MouseEvent e)
		{
			// first exclude what we don't handle
			if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0)
			{
				return;
			}
			if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			{
				return;
			}
			if (e.isAltDown() || e.isAltGraphDown() || e.isMetaDown() || e.isShiftDown()
				|| e.isControlDown())
			{
				return;
			}
			if (e.getClickCount() > 2 && e.getClickCount() % 2 != 0)
			{
				return;
			}
			e.consume();
			TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
			if (path == null)
			{
				return;
			}
			BufferListTreeNode node = (BufferListTreeNode) path.getLastPathComponent();
			Object obj = node.getUserObject();
			if (obj instanceof String)
			{
				return;
			}
			Buffer buffer = (Buffer) obj;
			if (e.getClickCount() >= 2
				&& jEdit.getBooleanProperty("bufferlist.closeFilesOnDoubleClick", true))
			{
				// left mouse double press: close buffer
				jEdit.closeBuffer(view, buffer);
			}
			else
			{
				// left mouse single press: open buffer
				// view.goToBuffer(buffer);
			}
		} // }}}

		// {{{ +mousePressed(MouseEvent) : void
		@Override
		public void mousePressed(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				showPopup(e);
			}
		} // }}}

		// {{{ +mouseReleased(MouseEvent) : void
		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				showPopup(e);
			}
		} // }}}

		// {{{ -showPopup(MouseEvent) : void
		private void showPopup(MouseEvent e)
		{
			e.consume();
			// if user didn't select any buffer, or selected only one buffer,
			// then select entry at mouse position:
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null || paths.length == 1)
			{
				TreePath locPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				if (locPath != null)
				{
					Rectangle nodeRect = tree.getPathBounds(locPath);
					if (nodeRect != null && nodeRect.contains(e.getX(), e.getY()))
					{
						paths = new TreePath[] { locPath };
						tree.setSelectionPath(locPath);
					}
				}
			}
			// check whether user selected a directory node:
			if (paths != null)
			{
				for (int i = 0; i < paths.length; ++i)
				{
					BufferListTreeNode node = (BufferListTreeNode) paths[i].getLastPathComponent();
					Object obj = node.getUserObject();
					if (obj != null && obj instanceof String)
					{
						// user selected directory node; select all entries
						// below it:
						Enumeration<BufferListTreeNode> children = node.depthFirstEnumeration();
						while (children.hasMoreElements())
						{
							BufferListTreeNode childNode = children.nextElement();
							TreePath path = new TreePath(childNode.getPath());
							tree.addSelectionPath(path);
						}
					}
				}
			}
			// create & show popup
			paths = tree.getSelectionPaths();
			BufferListPopup popup = new BufferListPopup(view, tree, paths, BufferListPlugin
				.getMenuExtensions());
			popup.show(tree, e.getX() + 1, e.getY() + 1);
		} // }}}
	} // }}}

	// {{{ -class KeyHandler
	/**
	 * A key handler for the buffer list.
	 */
	private class KeyHandler extends KeyAdapter
	{
		// {{{ +keyPressed(KeyEvent) : void
		@Override
		public void keyPressed(KeyEvent evt)
		{
			if (evt.isConsumed())
			{
				return;
			}
			int kc = evt.getKeyCode();
			if (kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_CANCEL)
			{
				evt.consume();
				tree.clearSelection();
				closeWindowAndFocusEditPane();
			}
			else if (kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_ACCEPT)
			{
				evt.consume();
				TreePath[] sel = tree.getSelectionPaths();
				if (sel != null && sel.length > 0)
				{
					if (sel.length > 1)
					{
						GUIUtilities.error(BufferList.this, "bufferlist.error.tooMuchSelection",
							null);
						return;
					}
					else
					{
						BufferListTreeNode node = (BufferListTreeNode) sel[0]
							.getLastPathComponent();
						Object obj = node.getUserObject();
						if (obj instanceof Buffer)
						{
							view.setBuffer((Buffer) obj);
						}
					}
				}
				closeWindowAndFocusEditPane();
			}
		} // }}}
	} // }}}
}