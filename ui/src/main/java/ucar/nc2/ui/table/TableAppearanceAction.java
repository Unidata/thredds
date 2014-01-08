package ucar.nc2.ui.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.widget.BAMutil;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
        public ResizeColumnWidthsAction() {
            putValue(NAME, "Resize column widths");
            putValue(SHORT_DESCRIPTION,
                    "Resize widths of the columns so that they're big enough to display all of their contents.");
        }

        @Override public void actionPerformed(ActionEvent e) {
            TableUtils.resizeColumnWidths(table, true);
        }
    }
}
