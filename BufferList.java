/*
 * BufferList.java
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


// from Java:
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

// from Swing:
import javax.swing.*;
import javax.swing.table.*;

// from jEdit:
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;


/**
 * A dockable panel that contains two tables of open and recent files.
 *
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent, DockableWindow {

	public BufferList(View view, final String position) {
		super(new BorderLayout(0, 5));
		this.view = view;
		this.position = position;
		this.currentBuffer = view.getBuffer();

		iconRenderer = new IconCellRenderer();
		nameRenderer = new NameCellRenderer();
		toolTipRenderer = new ToolTipCellRenderer();

		textAreaFocusHandler = new TextAreaFocusHandler();
		ActionHandler actionhandler = new ActionHandler();
		KeyHandler keyhandler = new KeyHandler();
		FocusHandler focushandler = new FocusHandler();

		table1 = new HelpfulJTable();
		table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table1.setAutoscrolls(false);
		table1.setAutoCreateColumnsFromModel(false);
		try { table1.setSortColumn(Integer.parseInt(jEdit.getProperty("bufferlist.table1.sortColumn", "-1"))); }
		catch (NumberFormatException nfex) {}
		try { table1.setSortOrder(Integer.parseInt(jEdit.getProperty("bufferlist.table1.sortOrder", "-1"))); }
		catch (NumberFormatException nfex) {}
		table1.addMouseListener(new OpenFilesMouseHandler());
		table1.addActionListener(actionhandler);
		table1.addKeyListener(keyhandler);
		table1.addFocusListener(focushandler);
		table1.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals("sortColumn") || prop.equals("sortOrder")) {
					jEdit.setProperty("bufferlist.table1.sortColumn", String.valueOf(table1.getSortColumn()));
					jEdit.setProperty("bufferlist.table1.sortOrder", String.valueOf(table1.getSortOrder()));
					setNewModels();
				}
			}
		});

		table2 = new HelpfulJTable();
		table2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table2.setAutoscrolls(false);
		table2.setAutoCreateColumnsFromModel(false);
		try { table2.setSortColumn(Integer.parseInt(jEdit.getProperty("bufferlist.table2.sortColumn", "-1"))); }
		catch (NumberFormatException nfex) {}
		try { table2.setSortOrder(Integer.parseInt(jEdit.getProperty("bufferlist.table2.sortOrder", "-1"))); }
		catch (NumberFormatException nfex) {}
		table2.addMouseListener(new RecentFilesMouseHandler());
		table2.addActionListener(actionhandler);
		table2.addKeyListener(keyhandler);
		table2.addFocusListener(focushandler);

		table2.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals("sortColumn") || prop.equals("sortOrder")) {
					jEdit.setProperty("bufferlist.table2.sortColumn", String.valueOf(table2.getSortColumn()));
					jEdit.setProperty("bufferlist.table2.sortOrder", String.valueOf(table2.getSortOrder()));
					setNewModels();
				}
			}
		});

		scrTable1 = new JScrollPane(table1);
		scrTable1.getViewport().setBackground(labelBackgrndColor);
		scrTable2 = new JScrollPane(table2);
		scrTable2.getViewport().setBackground(labelBackgrndColor);

		setNewModels();

		header1 = new JLabel(jEdit.getProperty("bufferlist.openfiles.label"));
		header2 = new JLabel(jEdit.getProperty("bufferlist.recentfiles.label"));

		JPanel top = new JPanel(new BorderLayout());
		JPanel bottom = new JPanel(new BorderLayout());
		top.add(BorderLayout.NORTH, header1);
		top.add(BorderLayout.CENTER, scrTable1);
		bottom.add(BorderLayout.NORTH, header2);
		bottom.add(BorderLayout.CENTER, scrTable2);

		int splitmode =
			(position.equals(DockableWindowManager.TOP) ||
			 position.equals(DockableWindowManager.BOTTOM))
			? JSplitPane.HORIZONTAL_SPLIT
			: JSplitPane.VERTICAL_SPLIT;

		pane = new JSplitPane(splitmode, true, top, bottom);
		pane.setOneTouchExpandable(true);
		pane.setDividerLocation(Integer.parseInt(jEdit.getProperty("bufferlist.divider", "300")));

		add(pane, BorderLayout.CENTER);

		propertiesChanged();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (position.equals(DockableWindowManager.FLOATING))
					table1.requestFocus();
			}
		});
	}


	/** DockableWindow implementation */
	public String getName() {
		return BufferListPlugin.NAME;
	}


	/** DockableWindow implementation */
	public Component getComponent() {
		return this;
	}


	public void requestFocusOpenFiles() {
		table1.requestFocus();
	}


	/** go to next buffer in open files list */
	public void nextBuffer() {
		int row = model1.getCurrentBufferRow();
		Buffer next = model1.getBuffer(row == model1.getRowCount()-1 ? 0 : row + 1);
		view.setBuffer(next);
	}


	/** go to previous buffer in open files list */
	public void previousBuffer() {
		int row = model1.getCurrentBufferRow();
		Buffer prev = model1.getBuffer(row == 0 ? model1.getRowCount()-1 : row - 1);
		view.setBuffer(prev);
	}


	/**
	 * Invoked when the component is created; adds focus event handlers to all
	 * EditPanes of the View associated with this BufferList.
	 */
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);

		if (view != null) {
			EditPane[] editPanes = view.getEditPanes();
			for (int i = 0; i < editPanes.length; i++)
				editPanes[i].getTextArea().addFocusListener(textAreaFocusHandler);
		}
	}


	/**
	 * Invoked when the component is removed; saves some properties and removes
	 * the focus event handlers from the EditPanes.
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

		// save divider location:
		jEdit.setProperty("bufferlist.divider", Integer.toString(pane.getDividerLocation()));

		// save column widths and order:
		TableColumnModel tcm1 = table1.getColumnModel();
		TableColumnModel tcm2 = table2.getColumnModel();
		if (tcm1 instanceof PersistentTableColumnModel)
			((PersistentTableColumnModel)tcm1).save();
		if (tcm2 instanceof PersistentTableColumnModel)
			((PersistentTableColumnModel)tcm2).save();

		// save currently selected session:
		if (jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true))
			// FIXME: jEdit bug: list of open buffers is empty if jEdit is exiting
			// Workaround: until this is fixed (jEdit should send an
			// EditorIsExiting message), don't save buffer list to the current
			// session here. Save the currentSession property only.
			SessionManager.getInstance().saveCurrentSessionProperty();
	}


	public void handleMessage(EBMessage message) {
		if (message instanceof BufferUpdate) {
			BufferUpdate bu = (BufferUpdate) message;
			if (bu.getWhat() == BufferUpdate.CREATED ||
				bu.getWhat() == BufferUpdate.CLOSED ||
				bu.getWhat() == BufferUpdate.DIRTY_CHANGED ||
				bu.getWhat() == BufferUpdate.MODE_CHANGED) {
				setNewModels();
			}
		}
		else if (message instanceof EditPaneUpdate) {
			EditPaneUpdate epu = (EditPaneUpdate) message;
			View v = ((EditPane) epu.getSource()).getView();
			if (v == view) {
				if (epu.getWhat() == EditPaneUpdate.CREATED) {
					epu.getEditPane().getTextArea().addFocusListener(textAreaFocusHandler);
				} else if (epu.getWhat() == EditPaneUpdate.DESTROYED) {
					epu.getEditPane().getTextArea().removeFocusListener(textAreaFocusHandler);
				} else if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
					currentBuffer = epu.getEditPane().getBuffer();
					refresh();
				}
			}
		}
		else if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}


	/**
	 * sets new table models and table column models for both tables.
	 */
	private void setNewModels() {
		model1 = new BufferListModel(jEdit.getBuffers());
		model2 = new BufferListModel(BufferHistory.getBufferHistory());
		table1.setModel(model1);
		table2.setModel(model2);
		setNewColumnModel(table1);
		setNewColumnModel(table2);
		currentBuffer = view.getBuffer();
		refresh();
	}


	private void setNewColumnModel(HelpfulJTable table) {
		// save old column sizes and order:
		TableColumnModel tcm = table.getColumnModel();
		if (tcm instanceof PersistentTableColumnModel)
			((PersistentTableColumnModel)tcm).save();
		// make new column model:
		PersistentTableColumnModel ptcm = new PersistentTableColumnModel(table == table1 ? 1 : 2, 4);
		boolean verticalLines = jEdit.getBooleanProperty("bufferlist.verticalLines");
		ptcm.setColumnMargin(verticalLines ? 1 : 0);
		// set column model
		table.setColumnModel(ptcm);
	}


	private void refresh() {
		model1.fireTableDataChanged();
		model2.fireTableDataChanged();

		int row = model1.getCurrentBufferRow();
		int col = table1.getColumnModel().getColumnCount() - 1;
	    Rectangle cellRect = table1.getCellRect(row, col, false);
	    if (cellRect != null) {
			cellRect.x = 0;
			table1.scrollRectToVisible(cellRect);
		}
	}


	private void propertiesChanged() {
		boolean showAbsoluteOld = model1.isShowingAbsoluteFilename();
		boolean showAbsoluteNew = jEdit.getBooleanProperty("bufferlist.showOneColumn");

		// if the property "showOneColumn" has changed, we need to update the models:
		if (showAbsoluteOld != showAbsoluteNew) {
			setNewModels();
		} else {
			// otherwise we only need to update the column models, which is faster:
			setNewColumnModel(table1);
			setNewColumnModel(table2);
		}

		// show/hide table headers:
		if (jEdit.getBooleanProperty("bufferlist.headers", false)) {
			boolean autoresize = jEdit.getBooleanProperty("bufferlist.autoresize", true);
			table1.setAutoResizeColumns(autoresize);
			table2.setAutoResizeColumns(autoresize);
			table1.setTableHeader(new JTableHeader(table1.getColumnModel()));
			table2.setTableHeader(new JTableHeader(table2.getColumnModel()));
		} else {
			table1.setAutoResizeColumns(true);
			table2.setAutoResizeColumns(true);
			table1.setTableHeader(null);
			table2.setTableHeader(null);
		}

		// show/hide vertical & horizontal lines:
		boolean verticalLines = jEdit.getBooleanProperty("bufferlist.verticalLines");
		boolean horizontalLines = jEdit.getBooleanProperty("bufferlist.horizontalLines");
		table1.setShowVerticalLines(verticalLines);
		table2.setShowVerticalLines(verticalLines);
		table1.setShowHorizontalLines(horizontalLines);
		table2.setShowHorizontalLines(horizontalLines);

		// show/hide SessionSwitcher:
		initSessionSwitcher();
	}


	private void closeWindowAndFocusEditPane() {
		if (position.equals(DockableWindowManager.FLOATING)) {
			DockableWindowManager wm = view.getDockableWindowManager();
			wm.removeDockableWindow(BufferListPlugin.NAME);
		}
		view.getTextArea().requestFocus();
	}


	private void initSessionSwitcher() {
		if (jEdit.getBooleanProperty("bufferlist.switcher.show", true)) {
			if (sessionSwitcher != null)
				remove(sessionSwitcher);
			sessionSwitcher = new SessionSwitcher(view);
			add(sessionSwitcher, BorderLayout.NORTH);
		} else {
			if (sessionSwitcher != null)
				remove(sessionSwitcher);
			sessionSwitcher = null;
		}
	}


	// private members
	private View view;
	private Buffer currentBuffer;
	private String position;
	private TextAreaFocusHandler textAreaFocusHandler;
	private HelpfulJTable table1;
	private HelpfulJTable table2;
	private BufferListModel model1;
	private BufferListModel model2;
	private JScrollPane scrTable1;
	private JScrollPane scrTable2;
	private JLabel header1;
	private JLabel header2;
	private JSplitPane pane;
	private SessionSwitcher sessionSwitcher;
	private TableCellRenderer iconRenderer;
	private TableCellRenderer nameRenderer;
	private TableCellRenderer toolTipRenderer;


	// private static members
	private static Font headerNormalFont;
	private static Font headerBoldFont;
	private static Font labelNormalFont;
	private static Font labelBoldFont;
	private static Color labelNormalColor;
	private static Color labelSelectedColor;
	private static Color labelDisabledColor;
	private static Color labelBackgrndColor;


	// initialize static members
	static {
		headerNormalFont = new Font("Dialog", Font.PLAIN, 12);
		headerBoldFont = new Font("Dialog", Font.BOLD, 12);
		labelNormalFont = UIManager.getFont("Table.font");
		labelBoldFont = new Font(labelNormalFont.getName(), Font.BOLD, labelNormalFont.getSize());
		labelNormalColor = UIManager.getColor("Table.foreground");
		labelSelectedColor = UIManager.getColor("Table.selectionForeground");
		labelDisabledColor = UIManager.getColor("TextField.inactiveForeground");
		labelBackgrndColor = UIManager.getColor("Table.background");
	}


	/**
	 * A table model for both open and recent files.
	 * It's a two-dimensional String array. The first dimension carries
	 * the buffer name, the second dimension carries the buffer path.
	 */
	private class BufferListModel extends AbstractTableModel {

		public BufferListModel(Buffer[] bufferarr) {
			super();
			arr = new Object[bufferarr.length][4];

			for (int i=0; i < bufferarr.length; i++) {
				arr[i][0] = bufferarr[i].getName();
				arr[i][1] = bufferarr[i].getVFS().getParentOfPath(bufferarr[i].getPath());
				arr[i][2] = bufferarr[i].getMode().getName();
				arr[i][3] = bufferarr[i];
			}

			sort(table1, 1);
		}


		public BufferListModel(Vector vec) {
			super();
			arr = new Object[vec.size()][4];

			for (int i=0; i < arr.length; i++) {
				String path = ((BufferHistory.Entry)vec.elementAt(i)).path;
				VFS vfs = VFSManager.getVFSForPath(path);
				arr[i][0] = MiscUtilities.getFileName(path);
				arr[i][1] = vfs.getParentOfPath(path);
				arr[i][2] = "(unknown)";
				arr[i][3] = null;
			}

			sort(table2, 2);
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
				// shouldn't occur
				Log.log(Log.WARNING, this, "VFS for path " + dir + name + " not found!");
				return dir + name;
			}
		}


		public int getCurrentBufferRow() {
			for (int i = 0; i < arr.length; i++)
				if (arr[i][3] == currentBuffer)
					return i;
			return -1;
		}


		public Icon getBufferIcon(int row) {
			return getIconForRow(arr[row]);
		}


		private Icon getIconForRow(Object[] rowValues) {
			Icon icon = GUIUtilities.NORMAL_BUFFER_ICON;
			Buffer buffer = (Buffer) rowValues[3];
			if (buffer != null)
				icon = buffer.getIcon();
			else {
				// don't has a buffer (=recent file list), check FS:
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


		private Object[][] arr;
		private boolean showOneColumn = jEdit.getBooleanProperty("bufferlist.showOneColumn");


		private class ColumnSorter implements MiscUtilities.Compare {

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


	/**
	 * A table column model for both tables, that stores order and sizes of the
	 * displayed columns.
	 */
	private class PersistentTableColumnModel extends DefaultTableColumnModel {

		public PersistentTableColumnModel(int tableNum, int modelColumnCount) {
			super();
			this.modelColumnCount = modelColumnCount;
			this.tableNum = tableNum;

			for (int i = 0; i < modelColumnCount; i++) {
				String prefix = "bufferlist.table"+ tableNum + ".column" + i;

				// get model index:
				int modelIndex = i;
				String sModelIndex = jEdit.getProperty(prefix + ".modelIndex", Integer.toString(i));
				try { modelIndex = Integer.parseInt(sModelIndex); } catch (NumberFormatException ex) {}

				if (jEdit.getBooleanProperty("bufferlist.showColumn" + modelIndex, true)) {
					// get column width:
					int width = 75;
					String sWidth = jEdit.getProperty(prefix + ".width", "75");
					try { width = Integer.parseInt(sWidth); } catch (NumberFormatException ex) {}

					// get cell renderer
					TableCellRenderer tcr =
						(modelIndex == 0) ? iconRenderer :
						(modelIndex == 1) ? nameRenderer : toolTipRenderer;

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


	/**
	 * A cell renderer for the first column, displaying the buffer status icon.
	 */
	private class IconCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int col)
		{
			JLabel comp = (JLabel) super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, col
			);

			// determine the icon:
			BufferListModel model = (BufferListModel) table.getModel();
			Icon icon = model.getBufferIcon(row);
			comp.setIcon(icon);

			return comp;
		}

	}


	/**
	 * A cell renderer for the second column, displaying the filename.
	 */
	private class NameCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int col)
		{
			JLabel comp = (JLabel) super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, col
			);

			BufferListModel model = (BufferListModel) table.getModel();
			Buffer buffer = model.getBuffer(row);

			if (buffer != null) {
				comp.setForeground(buffer.isReadOnly() ? labelDisabledColor :
					isSelected ? labelSelectedColor : labelNormalColor);
				comp.setFont(buffer == currentBuffer ? labelBoldFont : labelNormalFont);
			} else {
				comp.setForeground(isSelected ? labelSelectedColor : labelNormalColor);
			}

			return comp;
		}

		public String getToolTipText() {
			return getText();
		}

	}


	/**
	 * A cell renderer that displays the content as tooltip text, too.
	 */
	private class ToolTipCellRenderer extends DefaultTableCellRenderer {

		public String getToolTipText() {
			return getText();
		}

	}


	/**
	 * Listens for a TextArea to get focus, to make the appropiate buffer
	 * in the BufferList bold.
	 */
	private class TextAreaFocusHandler extends FocusAdapter {

		public void focusGained(FocusEvent evt) {
			Component comp = SwingUtilities.getAncestorOfClass(EditPane.class, (Component) evt.getSource());
			if (comp == null)
				return;

			Buffer newBuffer = ((EditPane)comp).getBuffer();
			if (newBuffer != currentBuffer) {
				currentBuffer = newBuffer;
				refresh();
			}
		}

	}


	/**
	 * A mouse listener for the open files table.
	 */
	private class OpenFilesMouseHandler extends MouseAdapter {

		public void mousePressed(MouseEvent e) {
			Point p = e.getPoint();
			int row = table1.rowAtPoint(p);
			if (row == -1)
				return;

			String filename = model1.getFilename(row);
			Buffer buffer = model1.getBuffer(row);

			if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
				if (e.getClickCount() == 1) {
					// left mouse single press: switch to buffer
					if (buffer != null) {
						view.setBuffer(buffer);
						view.toFront();
						view.getEditPane().requestFocus();
					}
					e.consume();
				} else if (e.getClickCount() == 2) {
					// left mouse double press: close buffer
					if (buffer != null)
						jEdit.closeBuffer(view, buffer);
					e.consume();
				}
			}
			else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
				// right mouse press: show popup

				boolean isCurrent = (buffer != null && buffer == currentBuffer);
				table1.getSelectionModel().setSelectionInterval(row, row);
				BufferListPopup popup = new BufferListPopup(view, filename, true, isCurrent);
				popup.show(table1, p.x+1, p.y+1);
				e.consume();
			}
		}

	}


	/**
	 * A mouse listener for the recent files table.
	 */
	private class RecentFilesMouseHandler extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			// only double click allowed here:
			if (e.getClickCount() < 2)
				return;

			// on double-click: jump to buffer
			int row = table2.rowAtPoint(e.getPoint());
			if (row == -1)
				return;

			String filename = model2.getFilename(row);
			jEdit.openFile(view, filename);
			// note: we don't request the focus of the text area here,
			// because the user may want to open a series of files.
		}

		public void mousePressed(MouseEvent e) {
			// only right mb click allowed here:
			if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == 0)
				return;

			// show popup
			Point p = e.getPoint();
			int row = table2.rowAtPoint(p);
			if (row == -1)
				return;

			String filename = model2.getFilename(row);
			table2.getSelectionModel().setSelectionInterval(row, row);
			BufferListPopup popup = new BufferListPopup(view, filename, false, false);
			popup.show(table2, p.x+1, p.y+1);
		}

	}


	/**
	 * A focus handler for both tables.
	 */
	private class FocusHandler extends FocusAdapter {

		public void focusGained(FocusEvent evt) {
			if (evt.getComponent() == table1) {
				header1.setFont(headerBoldFont);
				header2.setFont(headerNormalFont);
			} else {
				header1.setFont(headerNormalFont);
				header2.setFont(headerBoldFont);
			}
		}

	}


	/**
	 * An action handler for both tables.
	 */
	private class ActionHandler implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			if (evt.getActionCommand().equals("enter-pressed")) {
				// <Enter> opens the buffer
				if (evt.getSource() == table1) {
					// open files table
					view.setBuffer(model1.getBuffer(table1.getSelectedRow()));
				} else {
					// recent files table
					String filename = model2.getFilename(table2.getSelectedRow());
					jEdit.openFile(view, filename);
				}
				view.toFront();
				closeWindowAndFocusEditPane();
			}
			else if (evt.getActionCommand().equals("tab-pressed") || evt.getActionCommand().equals("shift-tab-pressed")) {
				// <Tab>/<Shift-Tab> changes between both tables
				if (evt.getSource() == table1)
					table2.requestFocus();
				else
					table1.requestFocus();
			}
		}

	}


	/**
	 * A key handler for the tables.
	 */
	private class KeyHandler extends KeyAdapter {

		public void keyPressed(KeyEvent evt) {
			if (evt.isConsumed())
				return;

			if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
				// <Esc> closes a floating window
				evt.consume();
				closeWindowAndFocusEditPane();
			}
		}

	}

}
