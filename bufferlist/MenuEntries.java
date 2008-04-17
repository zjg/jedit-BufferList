package bufferlist;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.gjt.sp.jedit.View;

/**
 * Interface that must be implemented by any class that wishes to add options to
 * the BufferList right click popup menu.
 */
public interface MenuEntries
{
	/**
	 * The method that will be called to add the entries to the popup menu.
	 * 
	 * @param menu
	 *            The JPopupMenu object to add the entries too.
	 * @param view
	 *            The jEdit View containing the BufferList
	 * @param tree
	 *            The JTree representing the BufferList
	 * @param sel
	 *            An array of TreePath objects - one for each selected entry -
	 *            or null if none selected
	 */
	public void addEntries(JPopupMenu menu, View view, JTree tree, TreePath[] sel);
}
