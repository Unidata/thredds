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
 * @author cwardgar
 * @since 2014/01/07
 */
public class TableAppearanceAction extends AbstractAction {
    private static final Logger logger = LoggerFactory.getLogger(TableAppearanceAction.class);

    private final JTable table;

    public TableAppearanceAction(JTable table) {
        if (!(table.getColumnModel() instanceof HidableTableColumnModel)) {
            throw new IllegalArgumentException(
                    "table's TableColumnModel must be an instance of HidableTableColumnModel.");
        }

        this.table = table;

        putValue(NAME, "Table appearance");
        putValue(SMALL_ICON, Resource.getIcon(BAMutil.getResourcePath() + "TableAppearance.png", true));
        putValue(SHORT_DESCRIPTION, "Configure the appearance of the table.");
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComponent) {
            JComponent invoker = (JComponent) e.getSource();

            // We must rebuild the popup menu each time this method is called, because columns may have been moved
            // since last time.
            buildPopupMenu().show(invoker, invoker.getWidth() / 2, invoker.getHeight() / 2);
        } else {
            // Without a proper invoker, the popup menu does not disappear when an item is selected or it loses focus.
            // It just stays visible forever.
            logger.error(String.format("JPopupMenu requires a JComponent invoker, but was %s", e.getSource()));
        }
    }

    private JPopupMenu buildPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu(getValue(NAME).toString());
        popupMenu.add(new ResizeColumnWidthsAction());
        popupMenu.addSeparator();
        popupMenu.add(new ShowAllColumnsAction());
        popupMenu.add(new HideAllColumnsAction());
        popupMenu.addSeparator();

        Enumeration<TableColumn> allTableColumns = getTableColumnModel().getColumns(false);
        while (allTableColumns.hasMoreElements()) {
            TableColumn tableColumn = allTableColumns.nextElement();
            ColumnVisibilityAction columnVisibilityAction = new ColumnVisibilityAction(tableColumn);
            JCheckBoxMenuItem columnVisibilityMenuItem = new JCheckBoxMenuItem(columnVisibilityAction);
            popupMenu.add(columnVisibilityMenuItem);
        }

        return popupMenu;
    }

    // We can't cache the table model because a new one may be installed.
    private HidableTableColumnModel getTableColumnModel() {
        return (HidableTableColumnModel) table.getColumnModel();
    }


    private class ResizeColumnWidthsAction extends AbstractAction {
        private ResizeColumnWidthsAction() {
            putValue(NAME, "Resize column widths");
            putValue(SHORT_DESCRIPTION,
                    "Resize widths of the columns so that they're big enough to display all of their contents.");
        }

        @Override public void actionPerformed(ActionEvent e) {
            ColumnWidthsResizer.resize(table);
        }
    }

    private class ShowAllColumnsAction extends AbstractAction {
        private ShowAllColumnsAction() {
            putValue(NAME, "Show all columns");
            putValue(SHORT_DESCRIPTION, "Show all columns in the table.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Enumeration<TableColumn> allTableColumns = getTableColumnModel().getColumns(false);
            while (allTableColumns.hasMoreElements()) {
                TableColumn tableColumn = allTableColumns.nextElement();
                getTableColumnModel().setColumnVisible(tableColumn, true);
            }
        }
    }

    private class HideAllColumnsAction extends AbstractAction {
        private HideAllColumnsAction() {
            putValue(NAME, "Hide all columns");
            putValue(SHORT_DESCRIPTION, "Hide all columns in the table.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Enumeration<TableColumn> allTableColumns = getTableColumnModel().getColumns(false);
            while (allTableColumns.hasMoreElements()) {
                TableColumn tableColumn = allTableColumns.nextElement();
                getTableColumnModel().setColumnVisible(tableColumn, false);
            }
        }
    }

    private class ColumnVisibilityAction extends AbstractAction {
        private final TableColumn column;

        private ColumnVisibilityAction(TableColumn column) {
            this.column = column;
            putValue(NAME, column.getHeaderValue().toString());
            putValue(SHORT_DESCRIPTION, "Check to show this column; uncheck to hide it.");

            // The JCheckBoxMenuItem will read this property to set its initial selected state.
            // In addition, it will write *to* this property when selection events occur.
            putValue(SELECTED_KEY, getTableColumnModel().isColumnVisible(column));
        }

        @Override public void actionPerformed(ActionEvent e) {
            boolean isSelected = (Boolean) getValue(SELECTED_KEY);
            getTableColumnModel().setColumnVisible(column, isSelected);
        }
    }
}
