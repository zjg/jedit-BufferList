/*
 * NameCellRenderer.java
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


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import org.gjt.sp.jedit.Buffer;


/**
 * A table cell renderer for the second column, displaying the filename.
 */
public class NameCellRenderer extends ToolTipCellRenderer
{

	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int col)
	{
		JLabel comp = (JLabel) super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, col
		);

		BufferListModel model = (BufferListModel) table.getModel();
		Buffer buffer = model.getBuffer(row);
		Buffer currentBuffer = model.getCurrentBuffer();

		if (buffer != null) {
			comp.setForeground(buffer.isReadOnly() ? labelDisabledColor :
				isSelected ? labelSelectedColor : labelNormalColor);
			comp.setFont(buffer == currentBuffer ? fontNameSelected : fontNameNormal);
		} else {
			comp.setForeground(isSelected ? labelSelectedColor : labelNormalColor);
		}

		return comp;
	}


	/**
	 * Overwritten so that changes to the current look and feel take effect.
	 * @see javax.swing.JComponent#updateUI()
	 */
	public void updateUI() {
		initFontsAndColors();
		super.updateUI();
	}


	private static Font fontNameNormal;
	private static Font fontNameSelected;
	private static Color labelNormalColor;
	private static Color labelSelectedColor;
	private static Color labelDisabledColor;


	private static void initFontsAndColors() {
		Font tableFont = UIManager.getFont("Table.font");
		fontNameNormal = new Font(tableFont.getName(), Font.PLAIN, tableFont.getSize());
		fontNameSelected = new Font(tableFont.getName(), Font.BOLD, tableFont.getSize());

		labelNormalColor = UIManager.getColor("Table.foreground");
		labelSelectedColor = UIManager.getColor("Table.selectionForeground");
		labelDisabledColor = UIManager.getColor("TextField.inactiveForeground");
	}


	static {
		initFontsAndColors();
	}

}

