/*
 * BufferList.java
 * Copyright (c) 2000 Dirk Moebius
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
import java.util.Vector;

// from Swing:
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

// from jEdit:
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.CreateDockableWindow;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.util.Log;


/**
 * A dockable panel that contains two tables of open and recent files.
 *
 * @author Dirk Moebius
 */
public class BufferList extends JPanel implements EBComponent, DockableWindow {
    
    private static Font headerNormalFont;
    private static Font headerBoldFont;
    private static Font labelNormalFont;
    private static Font labelBoldFont;
    private static Color labelNormalColor;
    private static Color labelDisabledColor;
    private static Color labelBackgrndColor;
    
    static {
        headerNormalFont   = new Font("Dialog", Font.PLAIN, 12);
        headerBoldFont     = new Font("Dialog", Font.BOLD, 12); 
        labelNormalFont    = UIManager.getFont("EditorPane.font");
        labelBoldFont      = new Font(labelNormalFont.getName(), Font.BOLD, 
                                      labelNormalFont.getSize());
        labelNormalColor   = UIManager.getColor("EditorPane.foreground");
        labelDisabledColor = UIManager.getColor("EditorPane.inactiveForeground");
        labelBackgrndColor = UIManager.getColor("EditorPane.background");
    }
                    
    private View view;                                 // the view
    private Buffer currentBuffer;                      // the buffer the cursor is in
    private String position;                           // the docked position
    private TextAreaFocusHandler textAreaFocusHandler; // focus handler
    private HelpfulJTable table1;                      // open buffers
    private HelpfulJTable table2;                      // recent files
    private StringArrayModel model1;                   // model for table1
    private StringArrayModel model2;                   // model for table2
    private JLabel header1;                            // header for table1
    private JLabel header2;                            // header for table2
    private JSplitPane pane;                           // split pane
    private OpenFilesCellRenderer rendTable1Col1;      // renderer for 
    private OtherCellRenderer rendTable1Col2;          // the table cells
    private RecentFilesCellRenderer rendTable2Col1;
    private OtherCellRenderer rendTable2Col2;
    private boolean showOneColumn;
    
    
    public BufferList(View view, String position) {
        super(new BorderLayout());
        this.view = view;
        this.position = position;
        this.currentBuffer = view.getBuffer();
        
        rendTable1Col1 = new OpenFilesCellRenderer();
        rendTable1Col2 = new OtherCellRenderer();
        rendTable2Col1 = new RecentFilesCellRenderer();
        rendTable2Col2 = new OtherCellRenderer();
        
        textAreaFocusHandler = new TextAreaFocusHandler();
        ActionHandler actionhandler = new ActionHandler();
        KeyHandler keyhandler = new KeyHandler();
        FocusHandler focushandler = new FocusHandler();
        
        table1 = new HelpfulJTable();
        table1.setTableHeader(null);
        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table1.addMouseListener(new OpenFilesMouseHandler());
        table1.addActionListener(actionhandler);
        table1.addKeyListener(keyhandler);
        table1.addFocusListener(focushandler);
        
        table2 = new HelpfulJTable();
        table2.setTableHeader(null);
        table2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table2.addMouseListener(new RecentFilesMouseHandler());
        table2.addActionListener(actionhandler);
        table2.addKeyListener(keyhandler);
        table2.addFocusListener(focushandler);

        setNewModels();

        JScrollPane scrTable1 = new JScrollPane(table1);
        scrTable1.getViewport().setBackground(labelBackgrndColor);
        JScrollPane scrTable2 = new JScrollPane(table2);
        scrTable2.getViewport().setBackground(labelBackgrndColor);
        
        header1 = new JLabel(jEdit.getProperty("bufferlist.openfiles.label"));
        header2 = new JLabel(jEdit.getProperty("bufferlist.recentfiles.label"));
            
        JPanel top = new JPanel(new BorderLayout());
        JPanel bottom = new JPanel(new BorderLayout());
        top.add(BorderLayout.NORTH, header1);
        top.add(BorderLayout.CENTER, scrTable1);
        bottom.add(BorderLayout.NORTH, header2);
        bottom.add(BorderLayout.CENTER, scrTable2);
        
        int splitmode = JSplitPane.VERTICAL_SPLIT;
        if (position.equals(DockableWindowManager.TOP) ||
            position.equals(DockableWindowManager.BOTTOM)) {
            splitmode = JSplitPane.HORIZONTAL_SPLIT;
        }
            
        pane = new JSplitPane(splitmode, true, top, bottom);
        pane.setOneTouchExpandable(true);
        add(BorderLayout.CENTER, pane);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pane.setDividerLocation(Integer.parseInt(
                   jEdit.getProperty("bufferlist.divider", "300")));
                table1.getSelectionModel().setSelectionInterval(0, 0);
                table2.getSelectionModel().setSelectionInterval(0, 0);
                pane.revalidate();
                table1.requestFocus();                
            }
        });
    }

    
    public String getName() {
        return BufferListPlugin.NAME;
    }

    
    public Component getComponent() {
        return this;
    }

    
    public void addNotify() {
        super.addNotify();
        EditBus.addToBus(this);
        addHandlers();
    }

    
    public void removeNotify() {
        super.removeNotify();
        EditBus.removeFromBus(this);
        removeHandlers();
        // save divider location:
        jEdit.setProperty("bufferlist.divider", Integer.toString(
            pane.getDividerLocation())); 
    }
    
    
    public void handleMessage(EBMessage message) {
        if (message instanceof BufferUpdate) {
            BufferUpdate bu = (BufferUpdate) message;
            if ((bu.getWhat() == BufferUpdate.CREATED) ||
                (bu.getWhat() == BufferUpdate.CLOSED)) {
                setNewModels();
            } else if (bu.getWhat() == BufferUpdate.DIRTY_CHANGED) {
                refresh();
            }
        } else if (message instanceof EditPaneUpdate) {
            EditPaneUpdate epu = (EditPaneUpdate) message;
            View v = ((EditPane) epu.getSource()).getView();
            if (v == this.view) {
                if (epu.getWhat() == EditPaneUpdate.CREATED) {
                    // a new split pane was created
                    epu.getEditPane().getTextArea().addFocusListener(
                        textAreaFocusHandler);
                    //this.view.repaint();
                } else if (epu.getWhat() == EditPaneUpdate.DESTROYED) {
                    // a split pane was deleted
                    epu.getEditPane().getTextArea().removeFocusListener(
                        textAreaFocusHandler);
                    //this.view.repaint();
                } else if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
                    currentBuffer = epu.getEditPane().getBuffer();
                    refresh();
                }
            }
        } else if (message instanceof PropertiesChanged) {
            propertiesChanged();
        }
    }

    
    /** 
     * adds focus event handlers to all EditPanes of the View associated
     * with this BufferList.
     */
    private void addHandlers() {
        if (view == null) return;
        EditPane[] editPanes = view.getEditPanes();
        for (int i = 0; i < editPanes.length; i++) {
            editPanes[i].getTextArea().addFocusListener(
                this.textAreaFocusHandler);
        }
    }

    /** 
     * removes focus event handlers from all EditPanes of the View
     * associated with this BufferList. 
     */
    private void removeHandlers() {
        if (view == null) return;
        EditPane[] editPanes = view.getEditPanes();
        for (int i = 0; i < editPanes.length; i++) {
            editPanes[i].getTextArea().removeFocusListener(
                this.textAreaFocusHandler);
        }
    }


    private void refresh() {
        model1.fireTableDataChanged();
        model2.fireTableDataChanged();
    }

    
    private void setNewModels() {
        table1.setModel(model1 = new StringArrayModel(jEdit.getBuffers()));
        table2.setModel(model2 = new StringArrayModel(BufferHistory.getBufferHistory()));
        setNewColumnModel(table1, rendTable1Col1, rendTable1Col2);
        setNewColumnModel(table2, rendTable2Col1, rendTable2Col2);
        currentBuffer = view.getBuffer();
        refresh();
    }
    

    private void setNewColumnModel(JTable table, 
                                   TableCellRenderer rendCol1,
                                   TableCellRenderer rendCol2) {
        // show first column
        DefaultTableColumnModel dtcm = new DefaultTableColumnModel();
        TableColumn col = new TableColumn(0);
        col.setCellRenderer(rendCol1);
        dtcm.addColumn(col);
        
        // show second column (optionally)
        showOneColumn = jEdit.getBooleanProperty("bufferlist.showOneColumn");
        if (!showOneColumn) {
            col = new TableColumn(1);
            col.setCellRenderer(rendCol2);
            dtcm.addColumn(col);
        }
        
        // show vertical/horizontal lines
        boolean verticalLines = jEdit.getBooleanProperty(
            "bufferlist.verticalLines");
        boolean horizontalLines = jEdit.getBooleanProperty(
            "bufferlist.horizontalLines");
        table.setShowVerticalLines(verticalLines);
        table.setShowHorizontalLines(horizontalLines);
        dtcm.setColumnMargin(verticalLines ? 1 : 0);

        // set new column model
        table.setColumnModel(dtcm);
    }
    
    private void closeWindowIfFloating() {
        if (position.equals(DockableWindowManager.FLOATING)) {
            DockableWindowManager wm = view.getDockableWindowManager();
            wm.removeDockableWindow(BufferListPlugin.NAME);
        }
    }

    
    private void propertiesChanged() {
        setNewColumnModel(table1, rendTable1Col1, rendTable1Col2);
        setNewColumnModel(table2, rendTable2Col1, rendTable2Col2);
    }
    

    /** 
     * A table model for both open and recent files. 
     * It's a two-dimensional String array. The first dimension carries
     * the buffer name, the second dimension carries the buffer path.
     */
    private class StringArrayModel extends AbstractTableModel {
        public StringArrayModel(Buffer[] bufferarr) {
            super();
            arr = new String[bufferarr.length][2];
            for (int i=0; i < bufferarr.length; i++) {
                arr[i][0] = bufferarr[i].getName();
                arr[i][1] = bufferarr[i].getVFS().getParentOfPath(
                        bufferarr[i].getPath());
            }
        }
        public StringArrayModel(Vector vec) {
            super();
            arr = new String[vec.size()][2];
            for (int i=0; i < arr.length; i++) {
                String path = ((BufferHistory.Entry)vec.elementAt(i)).path;
                VFS vfs = VFSManager.getVFSForPath(path);
                arr[i][0] = MiscUtilities.getFileName(path);
                arr[i][1] = vfs.getParentOfPath(path);
            }
        }
        public Object getValueAt(int row, int col) { return arr[row][col]; }
        public boolean isCellEditable(int row, int col) { return false; }
        public int getRowCount() { return arr.length; }
        public int getColumnCount() { return 2; } 
        public String getFilename(int row) { 
            String f = arr[row][1] + arr[row][0];
            return f;
        }
        private String[][] arr;
    }

    
    /** A cell renderer for the open files table. */
    private class OpenFilesCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col)
        {
            JLabel comp = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col
            );
            filename = model1.getFilename(row);
            Buffer buffer = jEdit.getBuffer(filename);
            if (buffer == null) return comp;
            comp.setIcon(buffer.getIcon());
            if (buffer.isReadOnly()) {
                comp.setForeground(labelDisabledColor);
            } else {
                comp.setForeground(labelNormalColor);
            }
            if (buffer == currentBuffer) {
                comp.setFont(labelBoldFont);
            } else {
                comp.setFont(labelNormalFont);
            }
            return comp;
        }

        public String getToolTipText() {
            return showOneColumn ? filename : getText();
        }
        
        private String filename;
    }

    
    /** A cell renderer for the recent files table */
    private class RecentFilesCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col)
        {
            JLabel comp = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col
            );
            filename = model2.getFilename(row);
            comp.setIcon(GUIUtilities.NORMAL_BUFFER_ICON);
            return comp;
        }

        public String getToolTipText() {
            return showOneColumn ? filename : getText();
        }
        
        private String filename;
    }
    
    
    /** A cell renderer for the second column in open and recent files */
    private class OtherCellRenderer extends DefaultTableCellRenderer {
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
            Component comp = SwingUtilities.getAncestorOfClass(
                EditPane.class, (Component) evt.getSource());
            if (comp == null) return;
            currentBuffer = ((EditPane)comp).getBuffer();
            refresh();
            if (view != null) {
                view.invalidate();  // redraw
                view.validate();
            }
        }
    }
    
    
    /** a mouse listener for the open files table */
    private class OpenFilesMouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            int row = table1.rowAtPoint(e.getPoint());
            if (row == -1) return;
            String filename = model1.getFilename(row);
            Buffer buffer = jEdit.getBuffer(filename);
            if (e.getClickCount() == 1) {
                // single-click: jump to buffer
                view.setBuffer(buffer);
            }
            else if (e.getClickCount() == 2) {
                // double-click: close buffer
                jEdit.closeBuffer(view, buffer);
            }
        }
    }
    
    
    /** a mouse listener for the recent files table */
    private class RecentFilesMouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            int row = table2.rowAtPoint(e.getPoint());
            if (row == -1) return;
            String filename = model2.getFilename(row);
            if (e.getClickCount() == 2) {
                // double-click: jump to buffer
                jEdit.openFile(view, filename);
            }
        }
    }

    
    /** a focus handler for both tables. */
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
    
    
    /** an action handler for both tables. */
    private class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if (evt.getActionCommand() == "enter-pressed") {
                // <Enter> opens the buffer
                if (evt.getSource() == table1) { 
                    // open files table
                    int row = table1.getSelectedRow();
                    String filename = model1.getFilename(row);
                    view.setBuffer(jEdit.getBuffer(filename));
                    closeWindowIfFloating();
                } else {
                    // recent files table
                    int rows[] = table2.getSelectedRows();
                    for (int i=0; i < rows.length; i++) {
                        String filename = model2.getFilename(rows[i]);
                        jEdit.openFile(view, filename);
                    }
                    closeWindowIfFloating();
                }
            }
            else if (evt.getActionCommand() == "tab-pressed" ||
                     evt.getActionCommand() == "shift-tab-pressed") {
                // <Tab>/<Shift-Tab> changes between both tables
                if (evt.getSource() == table1) {
                    table2.requestFocus();
                } else {
                    table1.requestFocus();
                }
            }
        }
    } // inner class ActionHandler

    
    /** a key handler for the tables */
    private class KeyHandler extends KeyAdapter {
        public void keyPressed(KeyEvent evt) {
            if (evt.isConsumed()) return;
            if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                // <Esc> closes a floating window
                evt.consume();
                closeWindowIfFloating();
            }
        }
    } // inner class KeyHandler

}

