/*
 * BufferList.java
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


package bufferlist;


import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.table.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
import bufferlist.table.BufferListModel;
import bufferlist.table.PersistentTableColumnModel;


/**
 * A dockable panel that contains two tables of open and recent files.
 *
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent, DockableWindow
{

	public BufferList(View view, final String position) {
		super(new BorderLayout(0, 5));
		this.view = view;
		this.position = position;
		this.currentBuffer = view.getBuffer();

		textAreaFocusHandler = new TextAreaFocusHandler();
		ActionHandler actionhandler = new ActionHandler();
		KeyHandler keyhandler = new KeyHandler();
		FocusHandler focushandler = new FocusHandler();

		// open files table:
		table1 = new HelpfulJTable();
		table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table1.setAutoCreateColumnsFromModel(false);
		table1.addMouseListener(new OpenFilesMouseHandler());
		table1.addActionListener(actionhandler);
		table1.addKeyListener(keyhandler);
		table1.addFocusListener(focushandler);

		try { table1.setSortColumn(Integer.parseInt(jEdit.getProperty("bufferlist.table1.sortColumn", "-1"))); }
		catch (NumberFormatException nfex) {}
		try { table1.setSortOrder(Integer.parseInt(jEdit.getProperty("bufferlist.table1.sortOrder", "-1"))); }
		catch (NumberFormatException nfex) {}

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

		// recent files table:
		table2 = new HelpfulJTable();
		table2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table2.setAutoCreateColumnsFromModel(false);
		table2.addMouseListener(new RecentFilesMouseHandler());
		table2.addActionListener(actionhandler);
		table2.addKeyListener(keyhandler);
		table2.addFocusListener(focushandler);

		try { table2.setSortColumn(Integer.parseInt(jEdit.getProperty("bufferlist.table2.sortColumn", "-1"))); }
		catch (NumberFormatException nfex) {}
		try { table2.setSortOrder(Integer.parseInt(jEdit.getProperty("bufferlist.table2.sortOrder", "-1"))); }
		catch (NumberFormatException nfex) {}

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

		add(pane, BorderLayout.CENTER);

		propertiesChanged();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (position.equals(DockableWindowManager.FLOATING))
					table1.requestFocus();
				pane.setDividerLocation(Integer.parseInt(jEdit.getProperty("bufferlist.divider", "300")));
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


	/**
	 * Invoked by action "bufferlist-to-front" only;
	 * sets the focus on the table of open files.
	 * @see actions.xml
	 */
	public void requestFocusOpenFiles() {
		int row = model1.getRowOf(currentBuffer);
		table1.requestFocus();
		table1.setRowSelectionInterval(row, row);
	}


	/**
	 * @return the current buffer.
	 * @since BufferList 0.6.2
	 */
	public Buffer getCurrentBuffer() {
		return currentBuffer;
	}


	/** Go to next buffer in open files list */
	public void nextBuffer() {
		int row = model1.getRowOf(currentBuffer);
		Buffer next = model1.getBuffer(row == model1.getRowCount() - 1 ? 0 : row + 1);
		view.setBuffer(next);
	}


	/** Go to previous buffer in open files list */
	public void previousBuffer() {
		int row = model1.getRowOf(currentBuffer);
		Buffer prev = model1.getBuffer(row == 0 ? model1.getRowCount() - 1 : row - 1);
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
					currentBufferChanged();
				}
			}
		}
		else if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}


	/**
	 * Overwritten so that changes to the current look and feel take effect.
	 * @see javax.swing.JComponent#updateUI()
	 */
	public void updateUI() {
		initFontsAndColors();
		super.updateUI();
	}


	/**
	 * Sets new table models and table column models for both tables.
	 */
	private void setNewModels() {
		model1 = new BufferListModel(this, table1, jEdit.getBuffers());
		model2 = new BufferListModel(this, table2, BufferHistory.getBufferHistory());
		table1.setModel(model1);
		table2.setModel(model2);
		setNewColumnModel(table1);
		setNewColumnModel(table2);

		// mark new current buffer:
		currentBuffer = view.getBuffer();
		currentBufferChanged();
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

		// bugfix: the table background colors are reverted to default after
		// the models are changed; need to set it again:
		scrTable1.getViewport().setBackground(labelBackgrndColor);
		scrTable2.getViewport().setBackground(labelBackgrndColor);
	}


	/**
	 * Called after the current buffer has changed; notifies the cell
	 * renderers and makes sure the current buffer is visible in the open
	 * files list.
	 */
	private void currentBufferChanged() {
		model1.fireTableDataChanged();
		model2.fireTableDataChanged();

		int row = model1.getRowOf(currentBuffer);
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
		int recentFilesOld = model2.getRowCount();
		int recentFilesNew = BufferHistory.getBufferHistory().size();

		// if the property "showOneColumn" or the number of recent files have
		// changed, we need to update the models:
		if (showAbsoluteOld != showAbsoluteNew || recentFilesOld != recentFilesNew) {
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
	}


	private void closeWindowAndFocusEditPane() {
		if (position.equals(DockableWindowManager.FLOATING)) {
			DockableWindowManager wm = view.getDockableWindowManager();
			wm.removeDockableWindow(BufferListPlugin.NAME);
		}
		view.getTextArea().requestFocus();
	}


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

	private static Font fontHeaderNormal;
	private static Font fontHeaderSelected;
	private static Color labelBackgrndColor;


	private static void initFontsAndColors() {
		Font labelFont = UIManager.getFont("Label.font");
		fontHeaderNormal = new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize());
		fontHeaderSelected = new Font(labelFont.getName(), Font.BOLD, labelFont.getSize());
		labelBackgrndColor = UIManager.getColor("Table.background");
	}


	// initialize static members
	static {
		initFontsAndColors();
	}


	/**
	 * Listens for a TextArea to get focus, to make the appropiate buffer
	 * in the BufferList bold.
	 */
	class TextAreaFocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt) {
			Component comp = SwingUtilities.getAncestorOfClass(EditPane.class, (Component) evt.getSource());
			if (comp == null)
				return;

			Buffer newBuffer = ((EditPane)comp).getBuffer();
			if (newBuffer != currentBuffer) {
				currentBuffer = newBuffer;
				currentBufferChanged();
			}
		}
	}


	/**
	 * A mouse listener for the open files table.
	 */
	class OpenFilesMouseHandler extends MouseAdapter
	{
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
	class RecentFilesMouseHandler extends MouseAdapter
	{
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
	class FocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt) {
			if (evt.getComponent() == table1) {
				header1.setFont(fontHeaderSelected);
				header2.setFont(fontHeaderNormal);
			} else {
				header1.setFont(fontHeaderNormal);
				header2.setFont(fontHeaderSelected);
			}
		}
	}


	/**
	 * An action handler for both tables.
	 */
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt) {
			if (evt.getActionCommand().equals("enter-pressed")) {
				// <Enter> opens the buffer
				if (evt.getSource() == table1) {
					// open files table
					int sel = table1.getSelectedRow();
					if (sel >= 0)
						view.setBuffer(model1.getBuffer(sel));
				} else {
					// recent files table
					int sel =table2.getSelectedRow();
					if (sel >= 0) {
						String filename = model2.getFilename(sel);
						jEdit.openFile(view, filename);
					}
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
	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt) {
			if (evt.isConsumed())
				return;

			if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
				evt.consume();
				closeWindowAndFocusEditPane();
			}
		}
	}

}
