/*{{{ header
 * TreeTools.java - some tools for JTree
 * Copyright (c) 2002 Dirk Moebius
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package bufferlist;

// {{{ imports
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

// }}}

public class TreeTools
{
	// {{{ +expandAll(JTree) : void
	/**
	 * Expand all nodes of a tree.
	 * 
	 * @param tree
	 *            The tree whose nodes to expand.
	 */
	public static void expandAll(JTree tree)
	{
		expandAll(tree, new TreePath(tree.getModel().getRoot()));
	} // }}}

	// {{{ +collapseAll(JTree) : void
	/**
	 * Collapse all nodes of a tree.
	 * 
	 * @param tree
	 *            The tree whose nodes to expand.
	 */
	public static void collapseAll(JTree tree)
	{
		TreePath pathToRoot = new TreePath(tree.getModel().getRoot());
		collapseAll(tree, pathToRoot);
		if (!tree.isRootVisible())
		{
			tree.expandPath(pathToRoot);
		}
	} // }}}

	// {{{ +expandAll(JTree, TreePath) : void
	/**
	 * Expand a tree node and all its child nodes recursively.
	 * 
	 * @param tree
	 *            The tree whose nodes to expand.
	 * @param path
	 *            Path to the node to start at.
	 */
	public static void expandAll(JTree tree, TreePath path)
	{
		Object node = path.getLastPathComponent();
		TreeModel model = tree.getModel();
		if (model.isLeaf(node))
		{
			return;
		}
		tree.expandPath(path);
		int num = model.getChildCount(node);
		for (int i = 0; i < num; i++)
		{
			expandAll(tree, path.pathByAddingChild(model.getChild(node, i)));
		}
	} // }}}

	// {{{ +collapseAll(JTree, TreePath) : void
	/**
	 * Collapse a tree node and all its child nodes recursively.
	 * 
	 * @param tree
	 *            The tree whose nodes to collapse.
	 * @param path
	 *            Path to the node to start at.
	 */
	public static void collapseAll(JTree tree, TreePath path)
	{
		Object node = path.getLastPathComponent();
		TreeModel model = tree.getModel();
		if (model.isLeaf(node))
		{
			return;
		}
		int num = model.getChildCount(node);
		for (int i = 0; i < num; i++)
		{
			collapseAll(tree, path.pathByAddingChild(model.getChild(node, i)));
		}
		tree.collapsePath(path);
	} // }}}
}
