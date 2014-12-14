package ucar.nc2.ui.table;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;

/**
 * A row sorter whose ordering can be undone.
 *
 * @see <a href="http://stackoverflow.com/questions/5477965">
 *     How to restore the original row order with JTable's row sorter?</a>
 * @author cwardgar
 * @since 2014/12/10
 */
public class UndoableRowSorter<M extends TableModel> extends TableRowSorter<M> {
    /**
     * Creates an <code>UndoableRowSorter</code> with an empty model.
     */
    public UndoableRowSorter() {
        super(null);
    }

    /**
     * Creates an <code>UndoableRowSorter</code> using <code>model</code>
     * as the underlying <code>TableModel</code>.
     *
     * @param model the underlying <code>TableModel</code> to use,
     *              <code>null</code> is treated as an empty model
     */
    public UndoableRowSorter(M model) {
        super(model);
    }

    @Override
    public void toggleSortOrder(int column) {
        List<? extends SortKey> sortKeys = getSortKeys();
        if (sortKeys.size() > 0) {
            if (sortKeys.get(0).getSortOrder() == SortOrder.DESCENDING) {
                setSortKeys(null);
                return;
            }
        }

        super.toggleSortOrder(column);
    }
}
