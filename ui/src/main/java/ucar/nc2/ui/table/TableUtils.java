package ucar.nc2.ui.table;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * A utility class offering various operations on JTables.
 *
 * @author cwardgar
 */
public abstract class TableUtils {
    public static void resizeColumnWidths(JTable table, boolean headerOnly) {
        for (int col = 0; col < table.getColumnCount(); ++col) {
            int maxwidth = 0;

            if (!headerOnly) {
                for (int row = 0; row < table.getRowCount(); ++row) {
                    TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
                    Object value = table.getValueAt(row, col);

                    Component cellRendererComp =
                            cellRenderer.getTableCellRendererComponent(table, value, false, false, row, col);
                    maxwidth = Math.max(cellRendererComp.getPreferredSize().width, maxwidth);
                }
            }

            TableColumn column = table.getColumnModel().getColumn(col);
            TableCellRenderer headerRenderer = column.getHeaderRenderer();

            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }

            Object headerValue = column.getHeaderValue();
            Component headerRendererComp =
                    headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, col);

            maxwidth = Math.max(maxwidth, headerRendererComp.getPreferredSize().width);

            // For some reason, the calculation above gives a value that is 1 pixel too small.
            // Maybe that's because of the cell divider line?
            ++maxwidth;

            column.setPreferredWidth(maxwidth);
        }
    }

    /**
     * A listener that sets the preferred widths of a JTable's columns such that they're just big enough to display all
     * of their contents without truncation.
     */
    public static class ColumnWidthResizer implements TableModelListener {
        private final JTable table;
        private final boolean headerOnly;

        public ColumnWidthResizer(JTable table) {
            this(table, false);
        }

        public ColumnWidthResizer(JTable table, boolean headerOnly) {
            this.table = table;
            this.headerOnly = headerOnly;
        }

        @Override public void tableChanged(TableModelEvent e) {
            TableUtils.resizeColumnWidths(table, headerOnly);
        }
    }

    public static void alignTable(JTable table, int alignment) {
        // We don't want to set up completely new cell renderers: rather, we want to use the existing ones but just
        // change their alignment.
        for (int iCol = 0; iCol < table.getColumnCount(); ++iCol) {
            TableColumn tableColumn = table.getColumnModel().getColumn(iCol);

            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            tableColumn.setHeaderRenderer(new TableCellRendererAlignmentDecorator(headerRenderer, alignment));

            TableCellRenderer cellRenderer = tableColumn.getCellRenderer();
            if (cellRenderer == null) {
                cellRenderer = table.getDefaultRenderer(table.getColumnClass(iCol));
            }
            tableColumn.setCellRenderer(new TableCellRendererAlignmentDecorator(cellRenderer, alignment));
        }
    }

    private static class TableCellRendererAlignmentDecorator implements TableCellRenderer {
        private final TableCellRenderer delegate;
        private final int alignment;

        private TableCellRendererAlignmentDecorator(TableCellRenderer delegate, int alignment) {
            this.delegate = delegate;
            this.alignment = alignment;
        }

        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cellRendererComp =
                    delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (cellRendererComp instanceof DefaultTableCellRenderer) {
                JLabel cellLabel = (DefaultTableCellRenderer) cellRendererComp;
                cellLabel.setHorizontalAlignment(alignment);
            }

            return cellRendererComp;
        }
    }

    private TableUtils() { }
}
