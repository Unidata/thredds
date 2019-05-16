package ucar.ui.table;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import java.lang.invoke.MethodHandles;
import ucar.ui.table.HidableTableColumnModel;

/**
 * Created by cwardgar on 1/9/14.
 */
public class HidableTableColumnModelTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test public void testTableModelResize() {
        DefaultTableModel model = new DefaultTableModel(6, 6);
        HidableTableColumnModel tcm = new HidableTableColumnModel(model);

        // At start numAllColumns == numVisibleColumns.
        Assert.assertEquals(tcm.getColumnCount(false), tcm.getColumnCount(true));

        tcm.setColumnVisible(tcm.getColumn(1, false), false);  // Remove column at modelIndex 1.
        tcm.setColumnVisible(tcm.getColumn(4, false), false);  // Remove column at modelIndex 4.

        // We've removed 2 columns.
        Assert.assertEquals(tcm.getColumnCount(false) - 2, tcm.getColumnCount(true));

        model.setColumnCount(10);
        Assert.assertEquals(10, tcm.getColumnCount(true));

        /*
         * This assertion failed in the original source code of XTableColumnModel.
         * From http://www.stephenkelvin.de/XTableColumnModel/:
         *     "There is one gotcha with this design: If you currently have invisible columns and change your table
         *     model the JTable will recreate columns, but will fail to remove any invisible columns."
         */
        Assert.assertEquals(10, tcm.getColumnCount(false));
    }
}
