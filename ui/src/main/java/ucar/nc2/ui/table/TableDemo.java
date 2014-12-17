package ucar.nc2.ui.table;

import org.apache.commons.lang3.RandomStringUtils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Demos features developed for JTables. Mostly used for testing.
 *
 * @author cwardgar
 */
public abstract class TableDemo {
    private final static int numRows = 5;
    private final static int numCols = 5;

    public static void main(String[] args) {
        System.out.println("Testing Travis!");

        try {
            // Switch to Nimbus Look and Feel, if it's available.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {

                    UIManager.setLookAndFeel(info.getClassName());

                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TableModel model = createTableModel(numRows, numCols);
                TableColumnModel tcm = new HidableTableColumnModel(model);
                JTable table = new JTable(model, tcm);
                table.setRowSorter(new UndoableRowSorter<>(model));

                // Set the preferred column widths so that they're big enough to display all data without truncation.
                ColumnWidthsResizer resizer = new ColumnWidthsResizer(table);
                table.getModel().addTableModelListener(resizer);
                table.getColumnModel().addColumnModelListener(resizer);

                // Left-align every cell, including header cells.
                TableAligner aligner = new TableAligner(table, SwingConstants.LEADING);
                table.getColumnModel().addColumnModelListener(aligner);

                JButton removeColumnButton = new JButton(new RemoveColumnAction(table));
                JButton removeRowButton = new JButton(new RemoveRowAction(table));
                JButton addColumnButton = new JButton(new AddColumnAction(table));
                JButton addRowButton = new JButton(new AddRowAction(table));
                JButton resetButton = new JButton(new ResetAction(table));

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(removeRowButton);
                buttonPanel.add(removeColumnButton);
                buttonPanel.add(resetButton);
                buttonPanel.add(addColumnButton);
                buttonPanel.add(addRowButton);

                // Create a button that will popup a menu containing options to configure the appearance of the table.
                JButton cornerButton = new JButton(new TableAppearanceAction(table));
                cornerButton.setHideActionText(true);
                cornerButton.setContentAreaFilled(false);

                // Install the button in the upper-right corner of the table's scroll pane.
                final JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

                // This keeps the corner button visible even when the table is empty (or all columns are hidden).
                scrollPane.setColumnHeaderView(new JViewport());
                scrollPane.getColumnHeader().setPreferredSize(table.getTableHeader().getPreferredSize());

                JFrame frame = new JFrame("Test ResizeColumnWidthsListener");
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    public static class RemoveRowAction extends AbstractAction {
        private final JTable table;

        public RemoveRowAction(JTable table) {
            super("-Row");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (model.getRowCount() > 0) {
                model.setRowCount(model.getRowCount() - 1);
            }
        }
    }

    public static class RemoveColumnAction extends AbstractAction {
        private final JTable table;

        public RemoveColumnAction(JTable table) {
            super("-Col");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (model.getColumnCount() > 0) {
                model.setColumnCount(model.getColumnCount() - 1);
            }
        }
    }

    public static class ResetAction extends AbstractAction {
        private final JTable table;

        public ResetAction(JTable table) {
            super("Reset");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            TableModel model = createTableModel(numRows, numCols);
            TableColumnModel tcm = new HidableTableColumnModel(model);
            table.setModel(model);
            table.setColumnModel(tcm);

            ColumnWidthsResizer resizer = new ColumnWidthsResizer(table);
            table.getModel().addTableModelListener(resizer);
            table.getColumnModel().addColumnModelListener(resizer);

            TableAligner aligner = new TableAligner(table, SwingConstants.LEADING);
            table.getColumnModel().addColumnModelListener(aligner);
        }
    }

    public static class AddColumnAction extends AbstractAction {
        private final JTable table;

        public AddColumnAction(JTable table) {
            super("+Col");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            Object columnName = Integer.toString(model.getColumnCount());
            Object[] columnData = genColumnData(model);
            model.addColumn(columnName, columnData);
        }
    }

    public static class AddRowAction extends AbstractAction {
        private final JTable table;

        public AddRowAction(JTable table) {
            super("+Row");
            this.table = table;
        }

        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            Object[] rowData = new Object[model.getColumnCount()];
            int shift = 1;

            for (int iCol = 0; iCol < model.getColumnCount(); ++iCol) {
                rowData[iCol] = model.getValueAt(model.getRowCount() - 1, (iCol + shift) % model.getColumnCount());
            }

            model.insertRow(model.getRowCount(), rowData);
        }
    }

    private static DefaultTableModel createTableModel(int numRows, int numCols) {
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
        int cellValLen = (model.getColumnCount() + 1) * 5;

        for (int row = 0; row < model.getRowCount(); ++row) {
            data[row] = RandomStringUtils.randomAlphabetic(cellValLen);
        }

        return data;
    }

    private TableDemo() { }
}
