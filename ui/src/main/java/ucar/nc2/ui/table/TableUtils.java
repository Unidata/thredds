package ucar.nc2.ui.table;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * A utility class offering various operations on JTables.
 *
 * @author cwardgar
 */
public abstract class TableUtils {
    /**
     * A listener that sets the preferred widths of a JTable's columns such that they're just big enough to display all
     * of their contents without truncation.
     */
    public static class ResizeColumnWidthsListener implements TableModelListener {
        private final JTable table;
        private final boolean headerOnly;

        /**
         * Creates a listener that resizes {@code table}'s column widths when its data changes.
         * <p>
         * <b>Important:</b> This constructor <i>removes</i> {@code table} as a listener of its own {@link TableModel}.
         * However, all events are <i>forwarded</i> to {@code table} in {@link #tableChanged}. This ensures that
         * {@code table} gets notified of events before {@code this}, which normally can't be guaranteed.
         *
         * @param table  a table.
         * @see <a href="http://goo.gl/RH9thw">Swing in a better world: Listeners</a>
         */
        public ResizeColumnWidthsListener(JTable table) {
            this(table, false);

            // Remove table as a listener. That way, we can control when it gets notified of events.
            table.getModel().removeTableModelListener(table);

            // Perform initial resize.
            TableUtils.resizeColumnWidths(table, headerOnly);
        }

        public ResizeColumnWidthsListener(JTable table, boolean headerOnly) {
            this.table = table;
            this.headerOnly = headerOnly;
        }

        @Override public void tableChanged(TableModelEvent e) {
            table.tableChanged(e);  // table MUST be notified first.
            TableUtils.resizeColumnWidths(table, headerOnly);
        }
    }

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

    ///////////////////////////////////// Test ResizeColumnWidthsListener /////////////////////////////////////

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                int numRows = 3;
                int numCols = 4;

                DefaultTableModel model = new DefaultTableModel(numRows, 0);
                for (int col = 0; col < numCols; ++col) {
                    addColumn(model);
                }

                JTable table = new JTable(model);
                model.addTableModelListener(new ResizeColumnWidthsListener(table));

                JButton minusButton = new JButton(new MinusAction(model));
                JButton moveButton  = new JButton(new MoveAction(table.getColumnModel()));
                JButton plusButton  = new JButton(new PlusAction(model));

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(minusButton, BorderLayout.WEST);
                buttonPanel.add(moveButton,  BorderLayout.CENTER);
                buttonPanel.add(plusButton,  BorderLayout.EAST);

                JFrame frame = new JFrame("Test ResizeColumnWidthsListener");
                frame.add(new JScrollPane(table), BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    public static class MinusAction extends AbstractAction {
        private final DefaultTableModel model;

        public MinusAction(DefaultTableModel model) {
            super("-");
            this.model = model;
        }

        public void actionPerformed(ActionEvent e) {
            if (model.getColumnCount() > 0) {
                model.setColumnCount(model.getColumnCount() - 1);
            }
        }
    }

    public static class MoveAction extends AbstractAction {
        private final TableColumnModel columnModel;

        public MoveAction(TableColumnModel columnModel) {
            super("Move");
            this.columnModel = columnModel;
        }

        public void actionPerformed(ActionEvent e) {
            if (columnModel.getColumnCount() > 0) {
                columnModel.moveColumn(0, columnModel.getColumnCount() - 1);

            }
        }
    }

    public static class PlusAction extends AbstractAction {
        private final DefaultTableModel model;

        public PlusAction(DefaultTableModel model) {
            super("+");
            this.model = model;
        }

        public void actionPerformed(ActionEvent e) {
            addColumn(model);
        }
    }

    private static void addColumn(DefaultTableModel model) {
        Object columnName = Integer.toString(model.getColumnCount());
        Object[] columnData = genColumnData(model);
        model.addColumn(columnName, columnData);
    }

    private static Object[] genColumnData(TableModel model) {
        Object[] data = new Object[model.getRowCount()];
        int cellValLen = (model.getColumnCount() + 1) * 8;
        String cellVal = genString(cellValLen);

        for (int row = 0; row < model.getRowCount(); ++row) {
            data[row] = cellVal;
        }

        return data;
    }

    private static String genString(int len) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; ++i) {
            builder.append((char)('a' + i));
        }
        return builder.toString();
    }

    private TableUtils() { }
}
