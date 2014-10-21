package ucar.nc2.ui.table;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * <code>HidableTableColumnModel</code> extends the DefaultTableColumnModel .
 * It provides a comfortable way to hide/show columns.
 * Columns keep their positions when hidden and shown again.
 * <p/>
 * In order to work with JTable it cannot add any events to <code>TableColumnModelListener</code>.
 * Therefore hiding a column will result in <code>columnRemoved</code> event and showing it
 * again will notify listeners of a <code>columnAdded</code>, and possibly a <code>columnMoved</code> event.
 * For the same reason the following methods still deal with visible columns only:
 * getColumnCount(), getColumns(), getColumnIndex(), getColumn()
 * There are overloaded versions of these methods that take a parameter <code>onlyVisible</code> which let's
 * you specify whether you want invisible columns taken into account.
 *
 * @author Stephen Kelvin, mail@StephenKelvin.de
 * @version 0.9 04/03/01
 * @see DefaultTableColumnModel
 * @see <a href="http://www.stephenkelvin.de/XTableColumnModel/">original documentation</a>
 */
public class HidableTableColumnModel extends DefaultTableColumnModel implements TableModelListener {
    /** The model we're currently listening to. */
    private TableModel model;

    /** Array of TableColumn objects in this model. Holds all column objects, regardless of their visibility. */
    protected Vector<TableColumn> allTableColumns = new Vector<>();

    public HidableTableColumnModel(TableModel model) {
        createColumnsFromModel(model);
    }

    /**
     * Sets the visibility of the specified TableColumn.
     * The call is ignored if the TableColumn is not found in this column model
     * or its visibility status did not change.
     * <p/>
     *
     * @param column the column to show/hide
     * @param visible its new visibility status
     */
    // listeners will receive columnAdded()/columnRemoved() event
    public void setColumnVisible(TableColumn column, boolean visible) {
        if (isColumnVisible(column) == visible) {
            return;  // Visibility status did not change.
        }

        if (!visible) {
            super.removeColumn(column);
        } else {
            // find the visible index of the column:
            // iterate through both collections of visible and all columns, counting
            // visible columns up to the one that's about to be shown again
            int noVisibleColumns = tableColumns.size();
            int noInvisibleColumns = allTableColumns.size();
            int visibleIndex = 0;

            for (int invisibleIndex = 0; invisibleIndex < noInvisibleColumns; ++invisibleIndex) {
                TableColumn visibleColumn =
                        (visibleIndex < noVisibleColumns ? tableColumns.get(visibleIndex) : null);
                TableColumn testColumn = allTableColumns.get(invisibleIndex);

                if (testColumn == column) {
                    if (visibleColumn != column) {
                        super.addColumn(column);
                        super.moveColumn(tableColumns.size() - 1, visibleIndex);
                    }
                    return; // ####################
                }
                if (testColumn == visibleColumn) {
                    ++visibleIndex;
                }
            }
        }
    }

    /**
     * Checks whether the specified column is currently visible.
     *
     * @param aColumn column to check
     * @return visibility of specified column (false if there is no such column at all. [It's not visible, right?])
     */
    public boolean isColumnVisible(TableColumn aColumn) {
        return (tableColumns.indexOf(aColumn) >= 0);
    }

    /**
     * Append <code>column</code> to the right of exisiting columns.
     * Posts <code>columnAdded</code> event.
     *
     * @param column The column to be added
     * @throws IllegalArgumentException if <code>column</code> is <code>null</code>
     * @see #removeColumn
     */
    @Override
    public void addColumn(TableColumn column) {
        allTableColumns.addElement(column);
        super.addColumn(column);
    }

    /**
     * Removes <code>column</code> from this column model.
     * Posts <code>columnRemoved</code> event.
     * Will do nothing if the column is not in this model.
     *
     * @param column the column to be added
     * @see #addColumn
     */
    @Override
    public void removeColumn(TableColumn column) {
        int allColumnsIndex = allTableColumns.indexOf(column);
        if (allColumnsIndex != -1) {
            allTableColumns.removeElementAt(allColumnsIndex);
        }
        super.removeColumn(column);
    }

    /**
     * Moves the column from <code>oldIndex</code> to <code>newIndex</code>.
     * Posts  <code>columnMoved</code> event.
     * Will not move any columns if <code>oldIndex</code> equals <code>newIndex</code>.
     *
     * @throws IllegalArgumentException if either <code>oldIndex</code> or
     *                                  <code>newIndex</code>
     *                                  are not in [0, getColumnCount() - 1]
     * @param    oldIndex            index of column to be moved
     * @param    newIndex            new index of the column
     */
    @Override
    public void moveColumn(int oldIndex, int newIndex) {
        if ((oldIndex < 0) || (oldIndex >= getColumnCount()) ||
                (newIndex < 0) || (newIndex >= getColumnCount()))
            throw new IllegalArgumentException("moveColumn() - Index out of range");

        TableColumn fromColumn = tableColumns.get(oldIndex);
        TableColumn toColumn = tableColumns.get(newIndex);

        int allColumnsOldIndex = allTableColumns.indexOf(fromColumn);
        int allColumnsNewIndex = allTableColumns.indexOf(toColumn);

        if (oldIndex != newIndex) {
            allTableColumns.removeElementAt(allColumnsOldIndex);
            allTableColumns.insertElementAt(fromColumn, allColumnsNewIndex);
        }

        super.moveColumn(oldIndex, newIndex);
    }

    /**
     * Returns the total number of columns in this model.
     *
     * @param onlyVisible if set only visible columns will be counted
     * @return the number of columns in the <code>tableColumns</code> array
     * @see    #getColumns
     */
    public int getColumnCount(boolean onlyVisible) {
        Vector columns = (onlyVisible ? tableColumns : allTableColumns);
        return columns.size();
    }

    /**
     * Returns an <code>Enumeration</code> of all the columns in the model.
     *
     * @param onlyVisible if set all invisible columns will be missing from the enumeration.
     * @return an <code>Enumeration</code> of the columns in the model
     */
    public Enumeration<TableColumn> getColumns(boolean onlyVisible) {
        Vector columns = (onlyVisible ? tableColumns : allTableColumns);

        return columns.elements();
    }

    /**
     * Returns the <code>TableColumn</code> object for the column
     * at <code>columnIndex</code>.
     *
     * @param    columnIndex    the index of the column desired
     * @param    onlyVisible    if set columnIndex is meant to be relative to all visible columns only
     * else it is the index in all columns
     * @return the <code>TableColumn</code> object for the column
     * at <code>columnIndex</code>
     */
    public TableColumn getColumn(int columnIndex, boolean onlyVisible) {
        if (onlyVisible) {
            return tableColumns.elementAt(columnIndex);
        } else {
            return allTableColumns.elementAt(columnIndex);
        }

    }


    @Override public void tableChanged(TableModelEvent e) {
        if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {  // We only care about TableStructureChanged events.
            assert e.getSource() instanceof TableModel :
                    String.format("Why is %s firing a %s?", e.getSource(), e.getClass());
            createColumnsFromModel((TableModel) e.getSource());
        }
    }

    // Adapted from JTable.createDefaultColumnsFromModel().
    public void createColumnsFromModel(TableModel newModel) {
        if (newModel != model) {
            if (model != null) {
                model.removeTableModelListener(this);  // Stop listening to old model.
            }

            newModel.addTableModelListener(this);  // Start listening to new one.
            model = newModel;
        }

        // Removes all current columns including the hidden ones.
        // For visible columns that are removed, TableColumnModelEvents get fired.
        while (!allTableColumns.isEmpty()) {
            removeColumn(allTableColumns.elementAt(0));
        }

        // Create new columns from the data model info
        for (int modelColumnIndex = 0; modelColumnIndex < newModel.getColumnCount(); modelColumnIndex++) {
            TableColumn newColumn = new TableColumn(modelColumnIndex);
            String columnName = newModel.getColumnName(modelColumnIndex);
            newColumn.setHeaderValue(columnName);
            addColumn(newColumn);
        }
    }
}
