/*
 * HelpfulJTable.java - a JTable with additional features.
 * Copyright (c) 2000,2001 Dirk Moebius
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


import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import org.gjt.sp.util.Log;


/**
 * An extension of the default Swing JTable, that passes action key events,
 * displays tooltips and autoresizes columns.<p>
 *
 * In detail, the following features are provided:<p>
 *
 * <ul>
 * <li>
 * Fires an <code>ActionEvent</code>, if <b>Enter</b>, <b>Tab</b> or
 * <b>Shift-Tab</b> is pressed.<br>
 * Therefore, <code>addActionListener(...)</code> and
 * <code>removeActionListener(...)</code> methods are provided.
 * </li>
 *
 * <li>
 * Displays tooltips for partially visible text entries.<br>
 * To use this feature, you must use a <code>TableCellRenderer</code> that
 * implements the methods <code>getToolTipText()</code> and/or
 * <code>getToolTipText(MouseEvent)</code>, otherwise you won't see any
 * tooltips.
 * </li>
 *
 * <li>
 * Autoresizes all columns to the length of its longest content string.<br>
 * As a drawback, this <code>HelpfulJTable</code> can only be used to
 * with a String cell renderer, nothing complicated as a JList. (Complex
 * components may be used a CellEditor, however).
 * </li>
 * </ul><p>
 *
 * Only the default constructor of <code>JTable</code> is provided.
 * Please use <code>setModel(TableModel)</code> to set another model.
 *
 * @author Dirk Moebius
 */
public class HelpfulJTable extends JTable {

	public final static int SORT_OFF = -1;
	public final static int SORT_ASCENDING = 1;
	public final static int SORT_DESCENDING = 2;


	public HelpfulJTable() {
		super();
		super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		KeyStroke shifttab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK);

		this.unregisterKeyboardAction(enter);
		this.unregisterKeyboardAction(tab);

		KeyHandler kh = new KeyHandler();

		this.registerKeyboardAction(kh, "enter-pressed", enter, JComponent.WHEN_FOCUSED);
		this.registerKeyboardAction(kh, "tab-pressed", tab, JComponent.WHEN_FOCUSED);
		this.registerKeyboardAction(kh, "shift-tab-pressed", shifttab, JComponent.WHEN_FOCUSED);

		this.addMouseListener(new TooltipMouseHandler());

		if (this.getTableHeader() != null) {
			this.getTableHeader().setResizingAllowed(false);
		}
	}


	/**
	 * If true, columns are autoresized according to the largest display
	 * width of their contents.
	 *
	 * @param  state  whether autoresize is enabled or disabled.
	 */
	public void setAutoResizeColumns(boolean state) {
		autoResizeColumns = state;
		if (tableHeader != null && autoResizeColumns)
			tableHeader.setResizingAllowed(false);
	}


	/**
	 * Return the value of the autoResizeColumns property.
	 * The default is <tt>true</tt>.
	 */
	public boolean getAutoResizeColumns() {
		return autoResizeColumns;
	}


	/**
	 * Set the sort column.
	 * This is a bound bean property.
	 *
	 * @param sortColumn  the new sortColumn value.
	 */
	public void setSortColumn(int sortColumn) {
		int oldSortColumn = this.sortColumn;
		this.sortColumn = sortColumn;
		if (oldSortColumn != this.sortColumn)
			propertySupport.firePropertyChange("sortColumn", new Integer(oldSortColumn), new Integer(this.sortColumn));
	}


	public int getSortColumn() {
		return sortColumn;
	}


	/**
	 * Set whether the current <code>sortColumn</code> should be sorted
	 *  ascending or descending, or not at all. This is a bound bean property.
	 *
	 * @param order  the new sort order, one of SORT_ASCENDING,
	 *               SORT_DESCENDING, SORT_OFF.
	 */
	public void setSortOrder(int order) {
		if (order != SORT_ASCENDING && order != SORT_DESCENDING && order != SORT_OFF)
			throw new IllegalArgumentException("sortOrder must be one of: SORT_ASCENDING, SORT_DESCENDING, SORT_OFF");

		int oldSortOrder = this.sortOrder;
		this.sortOrder = order;
		if (oldSortOrder != this.sortOrder)
			propertySupport.firePropertyChange("sortOrder", new Integer(oldSortOrder), new Integer(this.sortOrder));
	}


	public int getSortOrder() {
		return sortOrder;
	}


	/**
	 * Overridden, so that any attempts to set a mode other than
	 * AUTO_RESIZE_OFF are ignored, if autoResizeColumns is on.
	 */
	public void setAutoResizeMode(int mode) {
		if (autoResizeColumns)
			return;
		super.setAutoResizeMode(mode);
	}


	/**
	 * Overridden, so that any attempts to set a TableHeader with
	 * <tt>resizingAllowed = true</tt> is set back to <tt>false</tt>.
	 */
	public void setTableHeader(JTableHeader th) {
		super.setTableHeader(th);
		if (th != null) {
			// remove resizing allowed, if neccessary:
			if (autoResizeColumns)
				th.setResizingAllowed(false);
			// set mouse listener
			th.addMouseListener(new TableHeaderMouseHandler());
		}
		super.configureEnclosingScrollPane();
	}


	/**
	 * Set a new column model.
	 * This implementation also sets new header renderers that display the
	 * current sort column with a small icon.
	 */
	public void setColumnModel(TableColumnModel tcm) {
		super.setColumnModel(tcm);
		// set header renderer for all columns:
		for (int i = 0, cc = columnModel.getColumnCount(); i < cc; i++)
			columnModel.getColumn(i).setHeaderRenderer(new SortTableHeaderRenderer(i));
	}


	/** Add an action listener to this table instance. */
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}


	/** Remove an action listener from this table instance. */
	public void removeActionListener(ActionListener l) {
		listenerList.remove(ActionListener.class, l);
	}


	/**
	 * Adds a <code>PropertyChangeListener</code> to the listener list.
	 * The listener is registered for all properties.
	 * A <code>PropertyChangeEvent</code> will get fired in response to an
	 * explicit call to <code>setSortColumn</code> and
	 * <code>setSortOrder</code> on the current component.
	 *
	 * @param  listener  the listener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}


	/**
	 * Removes a <code>PropertyChangeListener</code> from the listener list.
	 *
	 * @param  listener  the listener to be removed
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}


	/**
	 * Overridden to return null, if the cell is fully visible, so that
	 * ToolTips are only displayed if the cell is partially hidden.
	 */
	public final String getToolTipText(MouseEvent evt) {
		if (getToolTipLocation(evt) == null)
			return null;
		else
			return super.getToolTipText(evt);
	}


	/**
	 * Overridden to return null, if the cell is fully visible, so that
	 * ToolTips are only displayed if the cell is partially hidden.
	 */
	public final Point getToolTipLocation(MouseEvent evt) {
		int col = columnAtPoint(evt.getPoint());
		int row = rowAtPoint(evt.getPoint());
		if (col < 0 || row < 0) return null;
		Rectangle rect = getCellRect(row, col, true);
		if (cellTextIsFullyVisible(rect, row, col)) {
			return null;
		} else {
			Component comp = getCellRendererComponent(row, col);
			int iconwidth = 0;
			if (comp instanceof JLabel) {
				JLabel label = (JLabel) comp;
				if (label.getIcon() != null) {
					iconwidth = label.getIcon().getIconWidth() + label.getIconTextGap();
				}
			}
			return new Point(rect.x + iconwidth, rect.y);
		}
	}


	/** Autosizes the specified column to the width of its longest cell. */
	public void autosizeColumn(int col) {
		int width = getLongestCellTextWidth(col);
		if (width >= 0) {
			TableColumn tc = columnModel.getColumn(col);
			tc.setPreferredWidth(width);
			resizeAndRepaint();
			if (tableHeader != null)
				tableHeader.resizeAndRepaint();
		}
	}



	/**
	 * Invoked when the table data has changed, this method autoresizes
	 * all columns to its longest content length, if autoResizeColumns is on.
	 */
	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);

		if (!autoResizeColumns)
			return;

		int cc = columnModel.getColumnCount() - 1;
		for (int col = cc; col >= 0; col--) {
			int width = getLongestCellTextWidth(col);
			if (width > 0) {
				TableColumn tc = columnModel.getColumn(col);
				tc.setPreferredWidth(width);
				//tc.setMinWidth(width);
				//tc.setMaxWidth(width);
			}
		}

		resizeAndRepaint();
		if (tableHeader != null)
			tableHeader.resizeAndRepaint();
	}


	/** Overwritten to autoscroll only vertically, not horizontally. */
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		if (getAutoscrolls()) {
			Rectangle cellRect = getCellRect(rowIndex, columnIndex, false);
			if (cellRect != null) {
				cellRect.width = 0;
				scrollRectToVisible(cellRect);
			}
		}

		// Update column selection model:
		// (private void changeSelectionModel(csm, columnIndex, toggle, extend);)
		ListSelectionModel csm = getColumnModel().getSelectionModel();
		if (extend)
			if (toggle)
				csm.setAnchorSelectionIndex(columnIndex);
			else
				csm.setLeadSelectionIndex(columnIndex);
		else
			if (toggle)
				if (csm.isSelectedIndex(columnIndex))
					csm.removeSelectionInterval(columnIndex, columnIndex);
				else
					csm.addSelectionInterval(columnIndex, columnIndex);
			else
				csm.setSelectionInterval(columnIndex, columnIndex);

		// Update row selection model
		// (private void changeSelectionModel(rsm, rowIndex, toggle, extend);)
		ListSelectionModel rsm = getSelectionModel();
		if (extend)
			if (toggle)
				rsm.setAnchorSelectionIndex(rowIndex);
			else
				rsm.setLeadSelectionIndex(rowIndex);
		else
			if (toggle)
				if (rsm.isSelectedIndex(rowIndex))
					rsm.removeSelectionInterval(rowIndex, rowIndex);
				else
					rsm.addSelectionInterval(rowIndex, rowIndex);
			else
				rsm.setSelectionInterval(rowIndex, rowIndex);
	}


	/**
	 * This method is invoked if the user pressed <b>Enter</b>, <b>Tab</b>
	 * or <b>Shift-Tab</b> on the table.
	 */
	protected void fireActionEvent(ActionEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying those that are interested in this event:
		for (int i = listeners.length-2; i >= 0; i -= 2)
			if (listeners[i]==ActionListener.class)
				((ActionListener)listeners[i+1]).actionPerformed(evt);
	}


	/**
	 * Return the cell renderer component for the cell at (row,col).
	 */
	protected Component getCellRendererComponent(int row, int col) {
		String value = getValueAt(row, col).toString();
		TableCellRenderer rend = getCellRenderer(row, col);
		return rend == null ? null
			: rend.getTableCellRendererComponent(this, value, isCellSelected(row,col), hasFocus(), row, col);
	}


	/**
	 * <p>Returns true, if the text of cell (row,col) is fully visible,
	 * i.e. it is not hidden partially by a ScrollPane and it does not
	 * display "...", because the column is too small.</p>
	 */
	private final boolean cellTextIsFullyVisible(Rectangle rect, int row, int col) {
		int textWidth = getCellTextWidth(row, col);
		if (textWidth >= rect.width)
			return false;
		Rectangle vr = getVisibleRect();
		return vr.contains(rect.x, rect.y) && vr.contains(rect.x + textWidth, rect.y);
	}


	/**
	 * Computes the length of the text of cell (row,col), in pixels.
	 */
	private int getCellTextWidth(int row, int col) {
		String value = getValueAt(row, col).toString();
		Component comp = getCellRendererComponent(row, col);
		FontMetrics fm = comp.getFontMetrics(comp.getFont());
		int insetwidth = 0;

		if (comp instanceof JComponent) {
			Insets insets = ((JComponent)comp).getInsets();
			insetwidth += insets.left + insets.right;
		}

		if (comp instanceof JLabel) {
			JLabel label = (JLabel) comp;
			if (label.getIcon() != null)
				insetwidth += label.getIcon().getIconWidth() + label.getIconTextGap();
		}

		return SwingUtilities.computeStringWidth(fm, value) + insetwidth;
	}


	/**
	 * Gets the longest text width of a column, in pixels.
	 */
	private int getLongestCellTextWidth(int col) {
		int max = -1;
		int numRows = dataModel.getRowCount() - 1;

		for (int row = numRows; row >= 0; row--) {
			int width = getCellTextWidth(row, col) + getIntercellSpacing().width + 2;
			if (width > max)
				max = width;
		}

		return max;
	}


	private boolean autoResizeColumns = true;
	private int sortColumn = -1;
	private int sortOrder = SORT_OFF;
	private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);


	private static Icon SORT_UP;
	private static Icon SORT_DOWN;


	static {
		URL urlSortUp = HelpfulJTable.class.getResource("sort_up.gif");
		if (urlSortUp != null)
			SORT_UP = new ImageIcon(urlSortUp);
		else
			Log.log(Log.ERROR, HelpfulJTable.class, "Error fetching image sort_up.gif");

		URL urlSortDown = HelpfulJTable.class.getResource("sort_down.gif");
		if (urlSortDown != null)
			SORT_DOWN = new ImageIcon(urlSortDown);
		else
			Log.log(Log.ERROR, HelpfulJTable.class, "Error fetching image sort_down.gif");
	}


	private class KeyHandler implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			fireActionEvent(evt);
		}

	}


	private class TooltipMouseHandler extends MouseAdapter {

		public void mouseEntered(MouseEvent evt) {
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			toolTipInitialDelay = ttm.getInitialDelay();
			toolTipReshowDelay = ttm.getReshowDelay();
			ttm.setInitialDelay(500);
			ttm.setReshowDelay(0);
		}

		public void mouseExited(MouseEvent evt) {
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setInitialDelay(toolTipInitialDelay);
			ttm.setReshowDelay(toolTipReshowDelay);
		}

		private int toolTipInitialDelay = -1;
		private int toolTipReshowDelay = -1;

	}


	private class TableHeaderMouseHandler extends MouseAdapter {

		public void mouseClicked(MouseEvent evt) {
			int col = getColumnModel().getColumnIndexAtX(evt.getX());
			if (col < 0)
				return;

			if (evt.getClickCount() == 1) {
				// single-click on header: change sort column/order
				evt.consume();
				if (col == sortColumn) {
					switch (sortOrder) {
						case SORT_ASCENDING: setSortOrder(SORT_DESCENDING); break;
						case SORT_DESCENDING: setSortOrder(SORT_OFF); break;
						default: setSortOrder(SORT_ASCENDING); break;
					}
				} else {
					setSortColumn(col);
					setSortOrder(SORT_ASCENDING);
				}
			}
			else if (evt.getClickCount() == 2 && getTableHeader().getResizingAllowed()) {
				// double click on header: autoresize column
				evt.consume();
				Point p = evt.getPoint();
				Rectangle r = getTableHeader().getHeaderRect(col);

				r.grow(-3, 0);
				if (r.contains(p))
					return; // not on the edge

				int midPoint = r.x + r.width/2;
				int resizeCol = (p.x < midPoint) ? col - 1 : col;

				if (resizeCol >= 0)
					autosizeColumn(resizeCol);
			}
		}

	}


	private class SortTableHeaderRenderer extends DefaultTableCellRenderer {

		public SortTableHeaderRenderer(int viewColumn) {
			super();
			this.viewColumn = viewColumn;
			setHorizontalAlignment(SwingConstants.LEADING);
			setHorizontalTextPosition(SwingConstants.LEADING);
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		}

	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
			setText(value == null ? "" : value.toString());
			if (viewColumn == sortColumn)
				switch (sortOrder) {
					case SORT_ASCENDING: 	setIcon(SORT_UP); break;
					case SORT_DESCENDING: setIcon(SORT_DOWN); break;
					default: setIcon(null); break;
				}
			else
				setIcon(null);
			return this;
		}

		private int viewColumn;

	}

}

