/*
 * BufferListModel.java
 * Copyright (c) 2000,2001 Dirk Moebius
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


package bufferlist.table;


import java.io.File;
import java.util.Vector;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.BufferHistory;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.util.Log;
import common.gui.HelpfulJTable;
import bufferlist.BufferList;


/**
 * A table model for both open and recent files.
 * It's a two-dimensional String array. The first dimension carries
 * the buffer name, the second dimension carries the buffer path.
 */
public class BufferListModel extends AbstractTableModel
{

	/**
	 * Create a new model from an array of Buffer elements.
	 */
	public BufferListModel(BufferList bufferlist, JTable table, Buffer[] bufferarr) {
		super();
		this.bufferlist = bufferlist;

		arr = new Object[bufferarr.length][4];

		for (int i = 0; i < bufferarr.length; i++) {
			arr[i][0] = bufferarr[i].getName();
			arr[i][1] = bufferarr[i].getVFS().getParentOfPath(bufferarr[i].getPath());
			arr[i][2] = bufferarr[i].getMode().getName();
			arr[i][3] = bufferarr[i];
		}

		sort(table, 1);
	}


	/**
	 * Create a new model from a vector of recent files.
	 * Note: the order of elements in the vector is reversed.
	 *
	 * @param  recent  the recent files, containing BufferHistory.Entry elements
	 */
	public BufferListModel(BufferList bufferlist, JTable table, Vector recent) {
		super();
		this.bufferlist = bufferlist;

		int size = recent.size();

		arr = new Object[size][4];

		for (int i = 0; i < size; i++) {
			String path = ((BufferHistory.Entry)recent.elementAt(size - i - 1)).path;
			VFS vfs = VFSManager.getVFSForPath(path);
			arr[i][0] = MiscUtilities.getFileName(path);
			arr[i][1] = vfs.getParentOfPath(path);
			arr[i][2] = "(unknown)";
			arr[i][3] = null;
		}

		sort(table, 2);
	}


	public Object getValueAt(int row, int col) {
		switch (col) {
			case 0: default: return "";
			case 1: return showOneColumn ? getFilename(row) : arr[row][0];
			case 2: return arr[row][1];
			case 3: return arr[row][2];
		}
	}


	public boolean isCellEditable(int row, int col) { return false; }
	public int getRowCount() { return arr.length; }
	public int getColumnCount() { return 4; }
	public String getColumnName(int col) { return jEdit.getProperty("bufferlist.table.column" + col); }
	public Buffer getBuffer(int row) { return (Buffer) arr[row][3]; }
	public Buffer getCurrentBuffer() { return bufferlist.getCurrentBuffer(); }
	public boolean isShowingAbsoluteFilename() { return showOneColumn; }


	public String getFilename(int row) {
		VFS vfs;
		Buffer buf = getBuffer(row);
		String dir = arr[row][1].toString();
		String name = arr[row][0].toString();

		if (buf != null)
			vfs = buf.getVFS();
		else
			vfs = VFSManager.getVFSForPath(dir);

		if (vfs != null)
			return vfs.constructPath(dir, name);
		else {
			// shouldn't happen
			Log.log(Log.WARNING, this, "VFS for path " + dir + name + " not found!");
			return dir + name;
		}
	}


	public int getRowOf(Buffer buffer) {
		for (int i = 0; i < arr.length; i++)
			if (arr[i][3] == buffer)
				return i;
		return -1;
	}


	public Icon getBufferIcon(int row) {
		return getIconForRow(arr[row]);
	}


	private Icon getIconForRow(Object[] rowValues) {
		Icon icon = GUIUtilities.NORMAL_BUFFER_ICON;
		Buffer buffer = (Buffer) rowValues[3];
		if (buffer != null) {
			// has a buffer (ie. it is an open file): use it's icon
			icon = buffer.getIcon();
		} else {
			// doesn't have a buffer (ie. is from recent file list)
			if (jEdit.getBooleanProperty("bufferlist.checkRecentFiles", true)) {
				// check FS for existence of recent files:
				String path = rowValues[1].toString() + rowValues[0].toString();
				VFS vfs = VFSManager.getVFSForPath(path);
				if (vfs != null && vfs instanceof FileVFS) {
					File file = new File(path);
					if (!file.exists())
						icon = GUIUtilities.NEW_BUFFER_ICON;
					else if (!file.canWrite())
						icon = GUIUtilities.READ_ONLY_BUFFER_ICON;
				}
			}
		}
		return icon;
	}


	private void sort(JTable table, int tableNum) {
		String sSortColumn = jEdit.getProperty("bufferlist.table" + tableNum + ".sortColumn", (String)null);
		String sSortOrder = jEdit.getProperty("bufferlist.table" + tableNum + ".sortOrder", (String)null);
		if (sSortColumn != null && sSortOrder != null) {
			try {
				int sortColumn = Integer.parseInt(sSortColumn);
				int sortOrder = Integer.parseInt(sSortOrder);
				int modelColumn = table.getColumnCount() > 0 ? table.convertColumnIndexToModel(sortColumn) : -1;
				if (modelColumn >= 0 && sortOrder != HelpfulJTable.SORT_OFF)
					MiscUtilities.quicksort(arr, new ColumnSorter(modelColumn, sortOrder == HelpfulJTable.SORT_ASCENDING));
			}
			catch (NumberFormatException nfex) {
				Log.log(Log.WARNING, this, "Error getting sort information for table " + tableNum + "; sortColumn: " + sSortColumn + " sortOrder=" + sSortOrder);
			}
		}
	}


	private BufferList bufferlist;
	private Object[][] arr;
	private boolean showOneColumn = jEdit.getBooleanProperty("bufferlist.showOneColumn");


	class ColumnSorter implements MiscUtilities.Compare
	{

		public ColumnSorter(int sortColumn, boolean ascending) {
			this.sortColumn = sortColumn;
			this.ascending = ascending;
		}


		public int compare(Object obj1, Object obj2) {
			// The objects to compare should be two one-dimensional arrays, otherwise it's an error.
			Object[] arr1 = (Object[]) obj1;
			Object[] arr2 = (Object[]) obj2;
			int cmp = 0;

			if (arr1 == arr2)
				return 0;

			if (sortColumn == 0) {
				// special case: sort by status icon
				int val1 = getIconValue(getIconForRow(arr1));
				int val2 = getIconValue(getIconForRow(arr2));
				cmp = val1 - val2;
			} else {
				String s1 = arr1[sortColumn-1].toString().toLowerCase();
				String s2 = arr2[sortColumn-1].toString().toLowerCase();
				cmp = s1.compareTo(s2);
			}

			return ascending ? cmp : -cmp;
		}


		private int getIconValue(Icon icon) {
			if (icon == GUIUtilities.NEW_BUFFER_ICON)
				return 4;
			else if (icon == GUIUtilities.DIRTY_BUFFER_ICON)
				return 3;
			else if (icon == GUIUtilities.NORMAL_BUFFER_ICON)
				return 2;
			else if (icon == GUIUtilities.READ_ONLY_BUFFER_ICON)
				return 1;
			else
				return 0;
		}


		private int sortColumn;
		private boolean ascending;

	}

}

