/*
 * PersistentTableColumnModel.java
 * Copyright (c) 2001 Dirk Moebius
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


import java.util.Enumeration;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.gjt.sp.jedit.jEdit;


/**
 * A table column model for both tables, that stores order and sizes of the
 * displayed columns.
 */
public class PersistentTableColumnModel extends DefaultTableColumnModel
{

	private TableCellRenderer iconRenderer = new IconCellRenderer();
	private TableCellRenderer nameRenderer = new NameCellRenderer();
	private TableCellRenderer toolTipRenderer = new ToolTipCellRenderer();


	public PersistentTableColumnModel(int tableNum, int modelColumnCount) {
		super();
		this.modelColumnCount = modelColumnCount;
		this.tableNum = tableNum;

		for (int i = 0; i < modelColumnCount; i++) {
			String prefix = "bufferlist.table"+ tableNum + ".column" + i;

			// get model index:
			int modelIndex = i;
			String sModelIndex = jEdit.getProperty(prefix + ".modelIndex", Integer.toString(i));
			try {
				modelIndex = Integer.parseInt(sModelIndex);
			}
			catch (NumberFormatException ex) {}

			if (jEdit.getBooleanProperty("bufferlist.showColumn" + modelIndex, true)) {
				// get column width:
				int width = 75;
				String sWidth = jEdit.getProperty(prefix + ".width", "75");
				try {
					width = Integer.parseInt(sWidth);
				}
				catch (NumberFormatException ex) {}

				// get cell renderer
				TableCellRenderer tcr =
					(modelIndex == 0) ? iconRenderer :
					(modelIndex == 1) ? nameRenderer :
					toolTipRenderer;

				// add table column
				TableColumn tc = new TableColumn(modelIndex, width, tcr, null);
				tc.setHeaderValue(jEdit.getProperty("bufferlist.table.column" + modelIndex));
				addColumn(tc);
			}
		}
	}


	public void save() {
		int maxCol = getColumnCount();

		for (int modelIndex = 0; modelIndex < modelColumnCount; modelIndex++) {
			int colIndex = findColumnForModelIndex(modelIndex);
			if (colIndex != -1) {
				TableColumn tc = getColumn(colIndex);
				String prefix = "bufferlist.table" + tableNum + ".column" + colIndex;
				jEdit.setProperty(prefix + ".width", Integer.toString(tc.getWidth()));
				jEdit.setProperty(prefix + ".modelIndex", Integer.toString(modelIndex));
			} else {
				String prefix = "bufferlist.table" + tableNum + ".column" + maxCol;
				jEdit.setProperty(prefix + ".modelIndex", Integer.toString(modelIndex));
				maxCol++;
			}
		}
	}


	private int findColumnForModelIndex(int modelIndex) {
		Enumeration e = tableColumns.elements();
		for (int i = 0; e.hasMoreElements(); i++)
			if (((TableColumn)e.nextElement()).getModelIndex() == modelIndex)
				return i;
		return -1;
	}


	private int modelColumnCount;
	private int tableNum;

}

