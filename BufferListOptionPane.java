/*
 * BufferListOptionPane.java - plugin options pane for BufferList
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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.AbstractOptionPane;


/**
 * This is the option pane that jEdit displays for BufferList's options.
 */
public class BufferListOptionPane extends AbstractOptionPane implements ActionListener {

	public BufferListOptionPane() {
		super("bufferlist");
	}


	public void _init() {
		// BufferList options:

		bAutoShow = new JCheckBox(jEdit.getProperty("options.bufferlist.autoshow"));
		bAutoShow.setSelected(jEdit.getBooleanProperty("bufferlist.autoshow", false));

		bVerticalLines = new JCheckBox(jEdit.getProperty("options.bufferlist.verticalLines"));
		bVerticalLines.setSelected(jEdit.getBooleanProperty("bufferlist.verticalLines", true));

		bHorizontalLines = new JCheckBox(jEdit.getProperty("options.bufferlist.horizontalLines"));
		bHorizontalLines.setSelected(jEdit.getBooleanProperty("bufferlist.horizontalLines", true));

		// NOTE: for historical reasons, the option "Show absolute filename" is named "showOneColumn":
		bShowAbsoluteFilename = new JCheckBox(jEdit.getProperty("options.bufferlist.showAbsoluteFilename"));
		bShowAbsoluteFilename.setSelected(jEdit.getBooleanProperty("bufferlist.showOneColumn", false));

		bHeaders = new JCheckBox(jEdit.getProperty("options.bufferlist.headers"));
		bHeaders.setSelected(jEdit.getBooleanProperty("bufferlist.headers", false));
		bHeaders.addActionListener(this);

		bAutoResize = new JCheckBox(jEdit.getProperty("options.bufferlist.autoresize"));
		bAutoResize.setSelected(jEdit.getBooleanProperty("bufferlist.autoresize", true));
		bAutoResize.setEnabled(bHeaders.isSelected());

		JLabel lbShowColumns = new JLabel(jEdit.getProperty("options.bufferlist.showColumns"));

		bShowStatus = new JCheckBox(jEdit.getProperty("options.bufferlist.showStatus"));
		bShowStatus.setSelected(jEdit.getBooleanProperty("bufferlist.showColumn0", true));

		bShowDir = new JCheckBox(jEdit.getProperty("options.bufferlist.showDir"));
		bShowDir.setSelected(jEdit.getBooleanProperty("bufferlist.showColumn2", true));

		bShowMode = new JCheckBox(jEdit.getProperty("options.bufferlist.showMode"));
		bShowMode.setSelected(jEdit.getBooleanProperty("bufferlist.showColumn3", true));

		JPanel bufferListOptions = new JPanel(new VariableGridLayout(	VariableGridLayout.FIXED_NUM_COLUMNS, 2, 15, 0));
		bufferListOptions.add(bAutoShow);
		bufferListOptions.add(lbShowColumns);
		bufferListOptions.add(bVerticalLines);
		bufferListOptions.add(bShowStatus);
		bufferListOptions.add(bHorizontalLines);
		bufferListOptions.add(bShowDir);
		bufferListOptions.add(bShowAbsoluteFilename);
		bufferListOptions.add(bShowMode);
		bufferListOptions.add(bHeaders);
		bufferListOptions.add(new JPanel()); // empty cell
		bufferListOptions.add(bAutoResize);

		// SessionSwitcher options:

		bShowSwitcher = new JCheckBox(jEdit.getProperty("options.bufferlist.switcher.show"));
		bShowSwitcher.setSelected(jEdit.getBooleanProperty("bufferlist.switcher.show", true));
		bShowSwitcher.addActionListener(this);

		bSwitcherShowTitle = new JCheckBox(jEdit.getProperty("options.bufferlist.switcher.showTitle"));
		bSwitcherShowTitle.setSelected(jEdit.getBooleanProperty("bufferlist.switcher.showTitle", true));
		bSwitcherShowTitle.setEnabled(bShowSwitcher.isSelected());

		bSwitcherAutoSave = new JCheckBox(jEdit.getProperty("options.bufferlist.switcher.autoSave"));
		bSwitcherAutoSave.setSelected(jEdit.getBooleanProperty("bufferlist.switcher.autoSave", true));

		bSwitcherCloseAll = new JCheckBox(jEdit.getProperty("options.bufferlist.switcher.closeAll"));
		bSwitcherCloseAll.setSelected(jEdit.getBooleanProperty("bufferlist.switcher.closeAll", true));

		addSeparator("options.bufferlist.label");
		addComponent(bufferListOptions);
		addSeparator("options.bufferlist.switcher.label");
		addComponent(bShowSwitcher);
		addComponent(bSwitcherShowTitle);
		addComponent(bSwitcherAutoSave);
		addComponent(bSwitcherCloseAll);
	}


	public void _save() {
		boolean displayedColumnsChanged =
			jEdit.getBooleanProperty("bufferlist.showColumn0") != bShowStatus.isSelected() ||
			jEdit.getBooleanProperty("bufferlist.showColumn2") != bShowDir.isSelected() ||
			jEdit.getBooleanProperty("bufferlist.showColumn3") != bShowMode.isSelected();


		jEdit.setBooleanProperty("bufferlist.autoshow", bAutoShow.isSelected());
		jEdit.setBooleanProperty("bufferlist.verticalLines", bVerticalLines.isSelected());
		jEdit.setBooleanProperty("bufferlist.horizontalLines", bHorizontalLines.isSelected());
		jEdit.setBooleanProperty("bufferlist.showOneColumn", bShowAbsoluteFilename.isSelected());
		jEdit.setBooleanProperty("bufferlist.headers", bHeaders.isSelected());
		jEdit.setBooleanProperty("bufferlist.autoresize", bAutoResize.isSelected());

		jEdit.setBooleanProperty("bufferlist.showColumn0", bShowStatus.isSelected());
		jEdit.setBooleanProperty("bufferlist.showColumn2", bShowDir.isSelected());
		jEdit.setBooleanProperty("bufferlist.showColumn3", bShowMode.isSelected());

		jEdit.setBooleanProperty("bufferlist.switcher.show", bShowSwitcher.isSelected());
		jEdit.setBooleanProperty("bufferlist.switcher.showTitle", bSwitcherShowTitle.isSelected());
		jEdit.setBooleanProperty("bufferlist.switcher.autoSave", bSwitcherAutoSave.isSelected());
		jEdit.setBooleanProperty("bufferlist.switcher.closeAll", bSwitcherCloseAll.isSelected());

		if (displayedColumnsChanged) {
			// discard column order:
			jEdit.unsetProperty("bufferlist.table1.column0.modelIndex");
			jEdit.unsetProperty("bufferlist.table1.column1.modelIndex");
			jEdit.unsetProperty("bufferlist.table1.column2.modelIndex");
			jEdit.unsetProperty("bufferlist.table1.column3.modelIndex");
			jEdit.unsetProperty("bufferlist.table2.column0.modelIndex");
			jEdit.unsetProperty("bufferlist.table2.column1.modelIndex");
			jEdit.unsetProperty("bufferlist.table2.column2.modelIndex");
			jEdit.unsetProperty("bufferlist.table2.column3.modelIndex");
		}
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bHeaders) {
			// auto-resize is set to on and may not be changed if no headers are shown:
			if (!bHeaders.isSelected()) {
				bAutoResize.setSelected(true);
				bAutoResize.setEnabled(false);
			} else {
				bAutoResize.setEnabled(true);
			}
		}
		else if (e.getSource() == bShowSwitcher) {
			bSwitcherShowTitle.setEnabled(bShowSwitcher.isSelected());
		}
	}


	private JCheckBox bAutoShow;
	private JCheckBox bVerticalLines;
	private JCheckBox bHorizontalLines;
	private JCheckBox bShowAbsoluteFilename;
	private JCheckBox bHeaders;
	private JCheckBox bAutoResize;
	private JCheckBox bShowStatus;
	private JCheckBox bShowDir;
	private JCheckBox bShowMode;
	private JCheckBox bShowSwitcher;
	private JCheckBox bSwitcherShowTitle;
	private JCheckBox bSwitcherAutoSave;
	private JCheckBox bSwitcherCloseAll;

}

