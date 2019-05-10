package ucar.ui.table;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * A listener that sets the preferred widths of a {@link JTable}'s columns such that they're just big enough to display
 * all of their contents without truncation. Expected usage:
 * <pre>
 * JTable table = new JTable(...);
 * ColumnWidthsResizer listener = new ColumnWidthsResizer(table);
 * table.getModel().addTableModelListener(listener);  // Respond to row and data changes.
 * table.getColumnModel().addColumnModelListener(listener);  // Respond to column changes.
 * </pre>
 */
public class ColumnWidthsResizer implements TableModelListener, TableColumnModelListener {
    private final JTable table;
    private final int fullScanCutoff;

    /**
     * Creates a listener that resizes {@code table}'s column widths when its data and/or structure changes.
     * <p/>
     * If {@code table.getRowCount() <= }{@link #DEFAULT_FULL_SCAN_CUTOFF}, a full scan will be performed. That is,
     * every row will be examined to determine the <b>optimal</b> column widths. Otherwise, a partial scan will be
     * performed. That is, only the header and the first, middle, and last rows will be examined to determine the
     * <b>approximate</b> column widths.
     *
     * @param table  a table.
     */
    public ColumnWidthsResizer(JTable table) {
        this(table, DEFAULT_FULL_SCAN_CUTOFF);
    }

    /**
     * Creates a listener that resizes {@code table}'s column widths when its data and/or structure changes.
     *
     * @param table  a table.
     * @param fullScanCutoff  {@code true} if a full scan should be performed; {@code false} if a partial scan should
     *                        be done instead.
     */
    public ColumnWidthsResizer(JTable table, int fullScanCutoff) {
        this.table = table;
        this.fullScanCutoff = fullScanCutoff;
        ColumnWidthsResizer.resize(table);  // Perform initial resize.
    }

    //////////////////////////////////////////////// TableModelListener ////////////////////////////////////////////////

    @Override public void tableChanged(final TableModelEvent e) {
        if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            return;  // Do not respond to changes in the number of columns here, only row and data changes.
        }

        // ColumnWidthsResizer requires that the internal Swing TableModelListeners that update the JTable view
        // run their updates BEFORE it does its thing. Unfortunately, the order in which listeners are notified is
        // undefined (see https://weblogs.java.net/blog/alexfromsun/archive/2011/06/15/swing-better-world-listeners).
        // As a work-around, we're going to place the resize operation at the end of the event queue. That way, it'll
        // be executed after all TableModelListeners have been notified.
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                // Do not cache the value of doFullScan; we need to reevaluate each time because the number of rows in
                // the table could have changed.
                boolean doFullScan = table.getRowCount() <= fullScanCutoff;

                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    resize(table, doFullScan);  // Resize all columns.
                } else {
                    resize(table, e.getColumn(), doFullScan);  // Resize only the affected column.
                }
            }
        });
    }

    ///////////////////////////////////////////// TableColumnModelListener /////////////////////////////////////////////

    @Override public void columnAdded(TableColumnModelEvent e) {
        boolean doFullScan = table.getRowCount() <= fullScanCutoff;
        resize(table, e.getToIndex(), doFullScan);  // Only resize added column.
    }

    @Override public void columnRemoved(TableColumnModelEvent e) { }

    @Override public void columnMoved(TableColumnModelEvent e) { }

    @Override public void columnMarginChanged(ChangeEvent e) { }

    @Override public void columnSelectionChanged(ListSelectionEvent e) { }

    ////////////////////////////////////////////////////// Static //////////////////////////////////////////////////////

    /**
     * The default maximum number of table rows for which a full scan will be performed. If a table has more rows
     * than this, only a partial scan will be done.
     */
    public static final int DEFAULT_FULL_SCAN_CUTOFF = 10000;

    public static void resize(JTable table) {
        resize(table, doFullScanDefault(table));
    }

    public static void resize(JTable table, boolean doFullScan) {
        for (int col = 0; col < table.getColumnCount(); ++col) {
            resize(table, col, doFullScan);
        }
    }

    public static void resize(JTable table, int colViewIndex) {
        resize(table, colViewIndex, doFullScanDefault(table));
    }

    public static void resize(JTable table, int colViewIndex, boolean doFullScan) {
        int maxWidth = 0;

        // Get header width.
        TableColumn column = table.getColumnModel().getColumn(colViewIndex);
        TableCellRenderer headerRenderer = column.getHeaderRenderer();

        if (headerRenderer == null) {
            headerRenderer = table.getTableHeader().getDefaultRenderer();
        }

        Object headerValue = column.getHeaderValue();
        Component headerRendererComp =
                headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, colViewIndex);

        maxWidth = Math.max(maxWidth, headerRendererComp.getPreferredSize().width);


        // Get cell widths.
        if (doFullScan) {
            for (int row = 0; row < table.getRowCount(); ++row) {
                maxWidth = Math.max(maxWidth, getCellWidth(table, row, colViewIndex));
            }
        } else {
            maxWidth = Math.max(maxWidth, getCellWidth(table, 0, colViewIndex));
            maxWidth = Math.max(maxWidth, getCellWidth(table, table.getRowCount() / 2, colViewIndex));
            maxWidth = Math.max(maxWidth, getCellWidth(table, table.getRowCount() - 1, colViewIndex));
        }

        // For some reason, the calculation above gives a value that is 1 pixel too small.
        // Maybe that's because of the cell divider line?
        ++maxWidth;

        column.setPreferredWidth(maxWidth);
    }

    public static int getCellWidth(JTable table, int rowViewIndex, int colViewIndex) {
        TableCellRenderer cellRenderer = table.getCellRenderer(rowViewIndex, colViewIndex);
        Object value = table.getValueAt(rowViewIndex, colViewIndex);

        Component cellRendererComp =
                cellRenderer.getTableCellRendererComponent(table, value, false, false, rowViewIndex, colViewIndex);
        return cellRendererComp.getPreferredSize().width;
    }

    private static boolean doFullScanDefault(JTable table) {
        return table.getRowCount() <= DEFAULT_FULL_SCAN_CUTOFF;
    }
}
