/*
 * BufferListOptionPane.java - plugin options pane for BufferList
 * (c) 2000 Dirk Moebius
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. */

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.AbstractOptionPane;


/**
 * This is the option pane that jEdit displays for BufferList's options. 
 */
public class BufferListOptionPane extends AbstractOptionPane {
    private JRadioButton bOneColumn;
    private JRadioButton bTwoColumns;
    private JCheckBox    bVerticalLines;
    private JCheckBox    bHorizontalLines;


    public BufferListOptionPane() {
        super("bufferlist");
    }


    public void _init() {
        bOneColumn = new JRadioButton(jEdit.getProperty(
            "options.bufferlist.oneColumn"));
        bTwoColumns = new JRadioButton(jEdit.getProperty(
            "options.bufferlist.twoColumns"));
            
        ButtonGroup bGroup1 = new ButtonGroup();
        bGroup1.add(bOneColumn);
        bGroup1.add(bTwoColumns);
        
        boolean oneColumn = jEdit.getBooleanProperty(
            "bufferlist.showOneColumn", false);
        if (oneColumn) {
            bOneColumn.setSelected(true);
        } else {
            bTwoColumns.setSelected(true);
        }
        
        bVerticalLines = new JCheckBox(jEdit.getProperty(
            "options.bufferlist.verticalLines"));
        bVerticalLines.setSelected(jEdit.getBooleanProperty(
            "bufferlist.verticalLines", true));

        bHorizontalLines = new JCheckBox(jEdit.getProperty(
            "options.bufferlist.horizontalLines"));
        bHorizontalLines.setSelected(jEdit.getBooleanProperty(
            "bufferlist.horizontalLines", true));

        addComponent(bOneColumn);
        addComponent(bTwoColumns);
        addComponent(bVerticalLines);
        addComponent(bHorizontalLines);
    }


    public void _save() {
        jEdit.setBooleanProperty("bufferlist.showOneColumn", bOneColumn.isSelected());
        jEdit.setBooleanProperty("bufferlist.verticalLines", bVerticalLines.isSelected());
        jEdit.setBooleanProperty("bufferlist.horizontalLines", bHorizontalLines.isSelected());
    }
}

