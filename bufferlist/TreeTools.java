/*
 * TreeTools.java - some tools for JTree
 * Copyright (c) 2002 Dirk Moebius
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */


package bufferlist;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


public class TreeTools
{

	/**
	 * Expand all nodes of a tree.
	 *
	 * @param tree  The tree whose nodes to expand.
	 */
	public static void expandAll(JTree tree)
	{
		expandAll(tree, new TreePath(tree.getModel().getRoot()));
	}


	/**
	 * Collapse all nodes of a tree.
	 *
	 * @param tree  The tree whose nodes to expand.
	 */
	public static void collapseAll(JTree tree)
	{
		TreePath pathToRoot = new TreePath(tree.getModel().getRoot());
		collapseAll(tree, pathToRoot);
		if(!tree.isRootVisible())
			tree.expandPath(pathToRoot);
	}


	/**
	 * Expand a tree node and all its child nodes recursively.
	 *
	 * @param tree  The tree whose nodes to expand.
	 * @param path  Path to the node to start at.
	 */
	public static void expandAll(JTree tree, TreePath path)
	{
		Object node = path.getLastPathComponent();
		TreeModel model = tree.getModel();

		if(model.isLeaf(node))
			return;

		tree.expandPath(path);

		int num = model.getChildCount(node);
		for(int i = 0; i < num; i++)
			expandAll(tree, path.pathByAddingChild(model.getChild(node, i)));
	}


	/**
	 * Collapse a tree node and all its child nodes recursively.
	 *
	 * @param tree  The tree whose nodes to collapse.
	 * @param path  Path to the node to start at.
	 */
	public static void collapseAll(JTree tree, TreePath path)
	{
		Object node = path.getLastPathComponent();
		TreeModel model = tree.getModel();

		if(model.isLeaf(node))
			return;

		int num = model.getChildCount(node);
		for(int i = 0; i < num; i++)
			collapseAll(tree, path.pathByAddingChild(model.getChild(node, i)));

		tree.collapsePath(path);
	}


	/** Get a copy of the list of expanded tree paths of a tree. */
	public static TreePath[] getExpandedPaths(JTree tree)
	{
		ArrayList expandedPaths = new ArrayList();
		TreePath rootPath = new TreePath(tree.getModel().getRoot());
		Enumeration enum = tree.getExpandedDescendants(rootPath);

		if(enum != null)
			while(enum.hasMoreElements())
				expandedPaths.add(enum.nextElement());

		TreePath[] array = new TreePath[expandedPaths.size()];
		expandedPaths.toArray(array);
		return array;
	}


	/** Expand all the previously remembered expanded paths. */
	public static void setExpandedPaths(JTree tree, TreePath[] expandedPaths)
	{
		if(expandedPaths == null)
			return;

		for(int i = 0; i < expandedPaths.length; ++i)
		{
			TreePath oldPath = expandedPaths[i];
			TreePath newPath = searchPath(tree.getModel(), oldPath);
			if(newPath != null)
				tree.expandPath(newPath);
		}
	}


	/**
	 * Search for a path in the specified tree model, whose nodes have
	 * the same name (compared using <code>equals()</code>)
	 * as the ones specified in the old path.
	 *
	 * @return  a new path for the specified model, or null if no such path
	 *   could be found.
	 */
	public static TreePath searchPath(TreeModel model, TreePath oldPath)
	{
		Object treenode = model.getRoot();
		Object[] oldPathNodes = oldPath.getPath();
		TreePath newPath = new TreePath(treenode);

		for(int i = 0; i < oldPathNodes.length; ++i)
		{
			Object oldPathNode = oldPathNodes[i];

			if(treenode.toString().equals(oldPathNode.toString()))
			{
				if(i == oldPathNodes.length - 1)
					return newPath;
				else
				{
					if(model.isLeaf(treenode))
						return null; // not found
					else
					{
						int count = model.getChildCount(treenode);
						boolean foundChild = false;

						for(int j = 0; j < count; ++j)
						{
							Object child = model.getChild(treenode, j);
							if(child.toString().equals(oldPathNodes[i + 1].toString()))
							{
								newPath = newPath.pathByAddingChild(child);
								treenode = child;
								foundChild = true;
								break;
							}
						}

						if(!foundChild)
							return null; // couldn't find child with same name
					}
				}
			}
		}

		return null;
	}

}
