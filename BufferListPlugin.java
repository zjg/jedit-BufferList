/*
 * BufferListPlugin.java
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Vector;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.browser.HelpfulJList;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.CreateDockableWindow;
import org.gjt.sp.jedit.msg.EditPaneUpdate;


/**
 * The BufferList plugin. 
 *
 * @author Dirk Moebius
 */
public class BufferListPlugin extends EBPlugin {
    
    public static final String NAME = "bufferlist";
    
    private static Icon newStateIcon;
    private static Icon dirtyStateIcon;
    private static Icon savedStateIcon;
    private static Icon newDirtyStateIcon;
    
    static {
        newStateIcon = GUIUtilities.loadIcon("new.gif");
        dirtyStateIcon = GUIUtilities.loadIcon("dirty.gif");
        savedStateIcon = GUIUtilities.loadIcon("normal.gif");
        newDirtyStateIcon = GUIUtilities.loadIcon("new_dirty.gif");
    }
                    
                    
    public void start() {
        EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST, NAME);
        jEdit.addAction(new toggle_bufferlist());
        jEdit.addAction(new bufferlist());
    }


    public void createMenuItems(Vector menuItems) {
        menuItems.addElement(GUIUtilities.loadMenuItem("toggle-bufferlist"));
    }


    public void handleMessage(EBMessage message) {
        if (message instanceof CreateDockableWindow) {
            CreateDockableWindow cmsg = (CreateDockableWindow)message;
            if (cmsg.getDockableWindowName().equals(NAME)) {
                cmsg.setDockableWindow(new BufferListDockable(cmsg.getView(),
                    cmsg.getPosition()));
            }
        }
    }


    /// this action toggles the Buffer List on/off.
    class toggle_bufferlist extends EditAction {
        public toggle_bufferlist() {
            super("toggle-bufferlist");
        }
    
        public void actionPerformed(ActionEvent evt) {
            View view = getView(evt);
            DockableWindowManager wm = view.getDockableWindowManager();
            wm.toggleDockableWindow(NAME);
        }
    
        public boolean isToggle() {
            return true;
        }
    
        public boolean isSelected(Component comp) {
            return getView(comp).getDockableWindowManager()
                .isDockableWindowVisible(NAME);
        }
    }

    
    /// this action brings the Buffer List to the front.
    class bufferlist extends EditAction {
        public bufferlist() {
            super("bufferlist");
        }
        public void actionPerformed(ActionEvent evt) {
            View view = getView(evt);
            DockableWindowManager wm = view.getDockableWindowManager();
            wm.showDockableWindow(NAME);
        }
    }

    
    class BufferListDockable extends JPanel implements EBComponent, DockableWindow {
        private View view;              // the view
        private HelpfulJList list1;     // open buffers
        private HelpfulJList list2;     // recent files
        private ArrayListModel model1;  // model for list1
        private ArrayListModel model2;  // model for list2
        private JSplitPane pane;
        
        public BufferListDockable(View view, String position) {
            super(new BorderLayout());
            this.view = view;
            list1 = new HelpfulJList();
            list1.setCellRenderer(new OpenFilesCellRenderer());
            list1.addMouseListener(new OpenFilesMouseListener());
            list2 = new HelpfulJList();
            list2.setCellRenderer(new RecentFilesCellRenderer());
            list2.addMouseListener(new RecentFilesMouseListener());
            JScrollPane scr1 = new JScrollPane(list1);
            scr1.setColumnHeaderView(new JLabel(jEdit.getProperty(
                "bufferlist.openfiles.label")));
            JScrollPane scr2 = new JScrollPane(list2);
            scr2.setColumnHeaderView(new JLabel(jEdit.getProperty(
                "bufferlist.recentfiles.label")));
            int splitmode = JSplitPane.VERTICAL_SPLIT;
            if (position.equals(DockableWindowManager.TOP) ||
                position.equals(DockableWindowManager.BOTTOM)) {
                splitmode = JSplitPane.HORIZONTAL_SPLIT;
            }                
            pane = new JSplitPane(splitmode, true, scr1, scr2);
            pane.setOneTouchExpandable(true);
            pane.setDividerLocation(Integer.parseInt(
                jEdit.getProperty("bufferlist.divider", "300")));
            add(BorderLayout.CENTER, pane);
            new_models();
        }
        
        public String getName() {
            return NAME;
        }
        
        public Component getComponent() {
            return this;
        }
        
        public void addNotify() {
            super.addNotify();
            EditBus.addToBus(this);
        }

        public void removeNotify() {
            super.removeNotify();
            EditBus.removeFromBus(this);
            view = null;
            jEdit.setProperty("bufferlist.divider", Integer.toString(
                pane.getDividerLocation())); 
        }
        
        public void handleMessage(EBMessage message) {
            if (message instanceof BufferUpdate) {
                BufferUpdate bu = (BufferUpdate) message;
                if ((bu.getWhat() == BufferUpdate.CREATED) ||
                    (bu.getWhat() == BufferUpdate.CLOSED)) {
                    new_models();
                } else if (bu.getWhat() == BufferUpdate.DIRTY_CHANGED) {
                    refresh();
                }
            } else if (message instanceof EditPaneUpdate) {
                EditPaneUpdate epu = (EditPaneUpdate) message;
                if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
                    refresh();
                }
            }
        }

        private void new_models() {
            list1.setModel(model1 = new ArrayListModel(jEdit.getBuffers()));
            list2.setModel(model2 = new ArrayListModel(jEdit.getRecent()));
        }

        private void refresh() {
            model1.fireContentsChanged();
            model2.fireContentsChanged();
        }
        
        class OpenFilesCellRenderer extends JLabel implements ListCellRenderer {
            public Component getListCellRendererComponent(
                    JList list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus)
            {
                Buffer buffer = (Buffer) model1.getElementAt(index);
                this.setText(buffer.getName());
                if (isSelected) {
                    this.setBackground(list.getSelectionBackground());
                    this.setForeground(list.getSelectionForeground());
                } else {
                    this.setBackground(list.getBackground());
                    this.setForeground(list.getForeground());
                }
                if (buffer.isReadOnly()) {
                    this.setForeground(Color.gray);
                }
                if (buffer.isNewFile()) {
                    this.setIcon(buffer.isDirty() ? newDirtyStateIcon : newStateIcon);
                } else {
                    this.setIcon(buffer.isDirty() ? dirtyStateIcon : savedStateIcon);
                }
                // if this is the current buffer, use a bold font
                Font font = list.getFont(); 
                if (view.getBuffer() == buffer) {
                    this.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
                } else {
                    this.setFont(font);
                }
                // Swing needs this, otherwise it doesn't paint the background color:
                this.setOpaque(true);
                return this;
            }
        }
        
        class RecentFilesCellRenderer extends JLabel implements ListCellRenderer {
            public Component getListCellRendererComponent(
                    JList list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus)
            {
                if (isSelected) {
                    this.setBackground(list.getSelectionBackground());
                    this.setForeground(list.getSelectionForeground());
                } else {
                    this.setBackground(list.getBackground());
                    this.setForeground(list.getForeground());
                }
                this.setFont(list.getFont());
                this.setText(model2.getElementAt(index).toString());
                this.setIcon(savedStateIcon);
                this.setOpaque(true);
                return this;
            }
        }
        
        class ArrayListModel extends AbstractListModel {
            Object[] arr;
            public ArrayListModel(Object[] _arr) {
                arr = new Object[_arr.length];
                System.arraycopy(_arr, 0, arr, 0, _arr.length);
            }
            public Object getElementAt(int i) {
                return arr[i];
            }
            public int getSize() {
                return arr.length;
            }
            public void fireContentsChanged() {
                fireContentsChanged(this, 0, arr.length);
            }
        }
        
        class OpenFilesMouseListener extends MouseAdapter {
            public void mouseClicked(MouseEvent e) {
                int index = list1.locationToIndex(e.getPoint());
                if (index == -1) return;
                Buffer buffer = (Buffer) model1.getElementAt(index);
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
        
        class RecentFilesMouseListener extends MouseAdapter {
            public void mouseClicked(MouseEvent e) {
                int index = list2.locationToIndex(e.getPoint());
                if (index == -1) return;
                String filename = (String) model2.getElementAt(index);
                if (e.getClickCount() == 2) {
                    // double-click: jump to buffer
                    jEdit.openFile(view, filename);
                }
            }
        }
    }
}

