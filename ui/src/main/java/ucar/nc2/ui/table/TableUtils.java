package ucar.nc2.ui.table;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * A utility class offering various operations on JTables.
 *
 * @author cwardgar
 */
public abstract class TableUtils {
    public static void installAligners(JTable table, int alignment) {
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
        try {
            // Switch to Nimbus Look and Feel, if it's available.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {  // TODO: In Java 7, replace this with multi-catch of specific exceptions.
            e.printStackTrace();
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                int numRows = 6;
                int numCols = 6;

                TableModel model = createTableModel(numRows, numCols);
                TableColumnModel tcm = new HidableTableColumnModel(model);
                JTable table = new JTable(model, tcm);

                ColumnWidthsResizer.resize(table);
                ColumnWidthsResizer resizer = new ColumnWidthsResizer(table);
                table.getModel().addTableModelListener(resizer);
                table.getColumnModel().addColumnModelListener(resizer);

                JButton newModelButton = new JButton(new NewModelAction(table));

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(newModelButton,  BorderLayout.CENTER);

                JButton cornerButton = new JButton(new TableAppearanceAction(table));
                cornerButton.setHideActionText(true);
                cornerButton.setContentAreaFilled(false);

                final JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

                JFrame frame = new JFrame("Test ResizeColumnWidthsListener");
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    public static class NewModelAction extends AbstractAction {
        private final JTable table;

        public NewModelAction(JTable table) {
            super("New model");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            table.setModel(createTableModel(table.getModel().getRowCount(), table.getModel().getColumnCount()));
//            ((DefaultTableModel) table.getModel()).setColumnCount(table.getModel().getColumnCount() - 1);
        }
    }

    private static TableModel createTableModel(int numRows, int numCols) {
        DefaultTableModel model = new DefaultTableModel(numRows, 0);
        for (int col = 0; col < numCols; ++col) {
            Object columnName = Integer.toString(model.getColumnCount());
            Object[] columnData = genColumnData(model);
            model.addColumn(columnName, columnData);
        }
        return model;
    }

    private static Object[] genColumnData(TableModel model) {
        Object[] data = new Object[model.getRowCount()];
        int cellValLen = 10;
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
