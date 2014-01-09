package ucar.nc2.ui.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.widget.BAMutil;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

/**
 * Displays a popup menu containing several options to configure the appearance of a JTable.
 *
 * Created by cwardgar on 1/7/14.
 */
public class TableAppearanceAction extends AbstractAction {
    private static final Logger logger = LoggerFactory.getLogger(TableAppearanceAction.class);

    private final JTable table;
    private final JPopupMenu popupMenu;

    public TableAppearanceAction(JTable table) {
        this.table = table;

        putValue(NAME, "Table appearance");
        putValue(SMALL_ICON, Resource.getIcon(BAMutil.getResourcePath() + "TableAppearance.png", true));
        putValue(SHORT_DESCRIPTION, "Configure the appearance of the table.");

        this.popupMenu = new JPopupMenu(getValue(NAME).toString());
        popupMenu.add(new ResizeColumnWidthsAction());
        popupMenu.addSeparator();

        Enumeration<TableColumn> tableColumns = table.getColumnModel().getColumns();
        while (tableColumns.hasMoreElements()) {
            TableColumn tableColumn = tableColumns.nextElement();
            ShowColumnAction showColumnAction = new ShowColumnAction(tableColumn);
            JCheckBoxMenuItem showColumnMenuItem = new JCheckBoxMenuItem(showColumnAction);
            popupMenu.add(showColumnMenuItem);
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComponent) {
            JComponent invoker = (JComponent) e.getSource();
            popupMenu.show(invoker, invoker.getWidth() / 2, invoker.getHeight() / 2);
        } else {
            // Without a proper invoker, the popup menu does not disappear when an item is selected or it loses focus.
            // It just stays visible forever.
            logger.error(String.format("JPopupMenu requires a JComponent invoker, but was %s", e.getSource()));
        }
    }


    private class ResizeColumnWidthsAction extends AbstractAction {
        private ResizeColumnWidthsAction() {
            putValue(NAME, "Resize column widths");
            putValue(SHORT_DESCRIPTION,
                    "Resize widths of the columns so that they're big enough to display all of their contents.");
        }

        @Override public void actionPerformed(ActionEvent e) {
            TableUtils.resizeColumnWidths(table, true);
        }
    }

    private class ShowColumnAction extends AbstractAction {
        private final TableColumn column;

        private ShowColumnAction(TableColumn column) {
            this.column = column;
            putValue(NAME, column.getHeaderValue().toString());
            putValue(SHORT_DESCRIPTION, "Check to show this column; uncheck to hide it.");

            // The JCheckBoxMenuItem will read this property to set its initial selected state.
            // In addition, it will write *to* this property when selection events occur.
            putValue(SELECTED_KEY, isShown());
        }

        @Override public void actionPerformed(ActionEvent e) {
            boolean isSelected = (Boolean) getValue(SELECTED_KEY);

            if (isSelected) {
                table.addColumn(column);
            } else {
                table.getColumnModel().removeColumn(column);
            }
        }

        private boolean isShown() {
            return table.getColumnModel().getColumnIndex(column.getHeaderValue()) >= 0;
        }
    }
}
