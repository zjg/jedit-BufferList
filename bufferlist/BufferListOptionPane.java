/*
 * BufferListOptionPane.java - plugin options pane for BufferList
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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.AbstractOptionPane;


/**
 * This is the option pane that jEdit displays for BufferList's options.
 */
public class BufferListOptionPane extends AbstractOptionPane implements ActionListener
{

	public BufferListOptionPane()
	{
		super("bufferlist");
	}


	public void _init()
	{
		bAutoShow = new JCheckBox(jEdit.getProperty("options.bufferlist.autoshow"),
			jEdit.getBooleanProperty("bufferlist.autoshow", false));

		bVerticalLines = new JCheckBox(jEdit.getProperty("options.bufferlist.verticalLines"),
			jEdit.getBooleanProperty("bufferlist.verticalLines", false));

		bHorizontalLines = new JCheckBox(jEdit.getProperty("options.bufferlist.horizontalLines"),
			jEdit.getBooleanProperty("bufferlist.horizontalLines", false));

		// NOTE: for historical reasons, the option "Show absolute filename" is named "showOneColumn":
		bShowAbsoluteFilename = new JCheckBox(jEdit.getProperty("options.bufferlist.showAbsoluteFilename"),
			jEdit.getBooleanProperty("bufferlist.showOneColumn", false));

		bHeaders = new JCheckBox(jEdit.getProperty("options.bufferlist.headers"),
			jEdit.getBooleanProperty("bufferlist.headers", false));
		bHeaders.addActionListener(this);

		bAutoResize = new JCheckBox(jEdit.getProperty("options.bufferlist.autoresize"),
			jEdit.getBooleanProperty("bufferlist.autoresize", true));
		bAutoResize.setEnabled(bHeaders.isSelected());

		bCheckRecentFiles = new JCheckBox(jEdit.getProperty("options.bufferlist.checkRecentFiles"),
			jEdit.getBooleanProperty("bufferlist.checkRecentFiles", true));

		bShowStatus = new JCheckBox(jEdit.getProperty("options.bufferlist.showStatus"),
			jEdit.getBooleanProperty("bufferlist.showColumn0", true));

		bShowDir = new JCheckBox(jEdit.getProperty("options.bufferlist.showDir"),
			jEdit.getBooleanProperty("bufferlist.showColumn2", true));

		bShowMode = new JCheckBox(jEdit.getProperty("options.bufferlist.showMode"),
			jEdit.getBooleanProperty("bufferlist.showColumn3", true));

		addSeparator("options.bufferlist.separator.behavior");
		addComponent(bAutoShow);
		addComponent(bCheckRecentFiles);

		addSeparator("options.bufferlist.separator.appearance");
		addComponent(bHeaders);
		addComponent(bAutoResize);
		addComponent(bVerticalLines);
		addComponent(bHorizontalLines);
		addComponent(bShowAbsoluteFilename);

		addSeparator("options.bufferlist.separator.showColumns");
		addComponent(bShowStatus);
		addComponent(bShowDir);
		addComponent(bShowMode);
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
		jEdit.setBooleanProperty("bufferlist.checkRecentFiles", bCheckRecentFiles.isSelected());

		jEdit.setBooleanProperty("bufferlist.showColumn0", bShowStatus.isSelected());
		jEdit.setBooleanProperty("bufferlist.showColumn2", bShowDir.isSelected());
		jEdit.setBooleanProperty("bufferlist.showColumn3", bShowMode.isSelected());

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
			} else
				bAutoResize.setEnabled(true);
		}
	}


	private JCheckBox bAutoShow;
	private JCheckBox bVerticalLines;
	private JCheckBox bHorizontalLines;
	private JCheckBox bShowAbsoluteFilename;
	private JCheckBox bHeaders;
	private JCheckBox bAutoResize;
	private JCheckBox bCheckRecentFiles;
	private JCheckBox bShowStatus;
	private JCheckBox bShowDir;
	private JCheckBox bShowMode;
}

