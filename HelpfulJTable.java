/*
 * HelpfulJTable.java - a JTable with additional features.
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


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * <p>An extension of the default Swing JTable, that passes action key events,
 * displays tooltips and autoresizes columns.</p>
 *
 * <p>In detail, the following features are provided:<p>
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
 * components may be used as CellEditor, however). 
 * </li>
 * </ul>
 *
 * <p>Only the default constructor of <code>JTable</code> is provided.
 * Please use <code>setModel(TableModel)</code> to set another model.</p>
 *
 * @author Dirk Moebius
 */
public class HelpfulJTable extends JTable {
    
    public HelpfulJTable() {
        super();
        super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); 
        KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0); 
        KeyStroke shifttab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK); 
        this.unregisterKeyboardAction(enter);
        this.unregisterKeyboardAction(tab);
        HelpfulKeyActionListener kh = new HelpfulKeyActionListener();
        this.registerKeyboardAction(kh, "enter-pressed", enter, JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(kh, "tab-pressed", tab, JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(kh, "shift-tab-pressed", shifttab, JComponent.WHEN_FOCUSED);
        this.addMouseListener(new HelpfulTooltipMouseListener());
        if (this.getTableHeader() != null) {
            this.getTableHeader().setReorderingAllowed(false);
            this.getTableHeader().setResizingAllowed(false);
        }
    }

    /** 
     * overridden, so that any attempts to set a mode other than
     * AUTO_RESIZE_OFF are ignored.
     */
    public void setAutoResizeMode(int mode) {
        super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    
    /** 
     * <p>overridden, so that any attempts to set a TableHeader with 
     * <code>reorderingAllowed</code> and <code>resizingAllowed = true</code>
     * are ignored, i.e. these states are set back to <code>false</code>.</p>
     */
    public void setTableHeader(JTableHeader th) {
        super.setTableHeader(th);
        if (th != null) {
            th.setReorderingAllowed(false);
            th.setResizingAllowed(false);
        }
    }
        
    
    /** add an action listener to this table instance. */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    
    /** remove an action listener from this table instance. */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    
    /** 
     * this method is invoked if the user pressed <b>Enter</b>, <b>Tab</b>
     * or <b>Shift-Tab</b> on the table.
     */
    protected void fireActionEvent(ActionEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i >= 0; i -= 2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(evt);
            }
        }
    }

    
    /** 
     * overridden to return null, if the cell is fully visible, so that
     * ToolTips are only displayed if the cell is partially hidden.
     */
    public final String getToolTipText(MouseEvent evt) {
        if (getToolTipLocation(evt) == null) {
            return null;
        } else {
            return super.getToolTipText(evt);
        }
    }

    
    /** 
     * overridden to return null, if the cell is fully visible, so that
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
                    iconwidth = label.getIcon().getIconWidth() +
                                label.getIconTextGap();
                }
            }
            return new Point(rect.x + iconwidth, rect.y);
        }
    }

    
    /** 
     * <p>returns true, if the text of cell (row,col) is fully visible,
     * i.e. it is not hidden partially by a ScrollPane and it does not 
     * display "...", because the column is too small.</p>
     */
    private final boolean cellTextIsFullyVisible(Rectangle rect, 
                                                 int row, 
                                                 int col) {
        int textWidth = getCellTextWidth(row, col);
        if (textWidth >= rect.width) return false;
        Rectangle vr = getVisibleRect();
        return vr.contains(rect.x, rect.y) &&
               vr.contains(rect.x + textWidth, rect.y);
    }

    
    /** return the cell renderer component for the cell at (row,col). */
    protected Component getCellRendererComponent(int row, int col) {
        String value = getValueAt(row, col).toString();
        TableCellRenderer rend = getCellRenderer(row, col);
        if (rend == null) {
            return null;
        } else {
            return rend.getTableCellRendererComponent(this, value, 
                isCellSelected(row,col), hasFocus(), row, col);
        }
    }

    
    /** computes the length of the text of cell (row,col), in pixels. */
    private int getCellTextWidth(int row, int col) {
        String value = getValueAt(row, col).toString();
        Component comp = getCellRendererComponent(row, col);
        Font f = comp.getFont();
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
        int insetwidth = 0;
        if (comp instanceof JComponent) {
            Insets insets = ((JComponent)comp).getInsets();
            insetwidth += insets.left + insets.right;
        }
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            if (label.getIcon() != null) {
                insetwidth += label.getIcon().getIconWidth() +
                              label.getIconTextGap();
            }
        }
        return SwingUtilities.computeStringWidth(fm, value) + insetwidth;
    }

    
    /** gets the longest text width of a column, in pixels. */
    private int getLongestCellTextWidth(int col) {
        int max = -1;
        int numRows = dataModel.getRowCount() - 1;
        for (int row = numRows; row >= 0; row--) {
            int width = getCellTextWidth(row, col) + 
                        getIntercellSpacing().width + 2;
            if (width > max) max = width;
        }
        return max;
    }

    
    /** 
     * invoked when the table data has changed, this method autoresizes
     * all columns to its longest content length.
     */
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        int numCols = columnModel.getColumnCount() - 1;
        for (int col = numCols; col >= 0; col--) {
            int width = getLongestCellTextWidth(col);
            if (width < 0) return;
            TableColumnModel tcm = getColumnModel();
            if (tcm == null) return;
            TableColumn tc = tcm.getColumn(col);
            if (tc == null) return;
            tc.setPreferredWidth(width);
            tc.setMinWidth(width);
            tc.setMaxWidth(width);
        }
        resizeAndRepaint();
        JTableHeader th = getTableHeader();
        if (th != null) {
            th.resizeAndRepaint();
        }
    }

    
    // private members:
    
    private int toolTipInitialDelay = -1;
    private int toolTipReshowDelay = -1;

    // private helper classes:
    
    private class HelpfulKeyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            fireActionEvent(evt);
        }
    }
    
    private class HelpfulTooltipMouseListener extends MouseAdapter {
        public void mouseEntered(MouseEvent evt) {
            ToolTipManager ttm = ToolTipManager.sharedInstance();
            toolTipInitialDelay = ttm.getInitialDelay();
            toolTipReshowDelay = ttm.getReshowDelay();
            ttm.setInitialDelay(200);
            ttm.setReshowDelay(0);
        }
        
        public void mouseExited(MouseEvent evt) {
            ToolTipManager ttm = ToolTipManager.sharedInstance();
            ttm.setInitialDelay(toolTipInitialDelay);
            ttm.setReshowDelay(toolTipReshowDelay);
        }
    }
}

