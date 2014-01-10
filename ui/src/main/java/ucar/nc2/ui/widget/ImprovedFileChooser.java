package ucar.nc2.ui.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ui.table.ColumnWidthsResizer;
import ucar.nc2.ui.table.TableUtils;
import ucar.nc2.ui.util.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * A JFileChooser that displays the "Details" view by default. The table in that view has been tweaked to intelligently
 * resize columns' widths based on their contents and left-align all cells.
 *
 * An improved JFileChooser. It incorporates the following enhancements:
 * <ul>
 *   <li> The "Details" view is displayed by default.
 *   <li> The table in that view has been tweaked to intelligently resize columns' widths based on their contents.
 *   <li> All text in those cells is left-aligned.
 *   <li> {@link #setConfirmOverwrite} can be called to show a confirmation dialog when attempting to overwrite
 *        existing files.
 * </ul>
 *
 * @author cwardgar
 */
public class ImprovedFileChooser extends JFileChooser {
    private static final Logger logger = LoggerFactory.getLogger(ImprovedFileChooser.class);

    // This is a reference to the JDialog created by JFileChooser.createDialog().
    // JFileChooser also has this data member, but it's private. We need our own.
    private JDialog dialog = null;

    public ImprovedFileChooser() {
        this(null, null);
    }

    public ImprovedFileChooser(File currentDirectory) {
        this(currentDirectory, null);
    }

    public ImprovedFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);

        AbstractButton detailsViewButton = getDetailsViewButton(this);
        if (detailsViewButton == null) {
            logger.warn("Couldn't find Details button!");
            return;
        }

        detailsViewButton.doClick();    // Programmatically switch to the Details View.

        List<JTable> tables = SwingUtils.getDescendantsOfType(JTable.class, this);
        final JTable detailsTable;

        if (tables.size() != 1) {
            logger.warn("Expected to find 1 JTable in the file chooser, but found " + tables.size());
            return;
        } else if ((detailsTable = tables.get(0)) == null) {
            logger.warn("The details view table was null!");
            return;
        }

        // Left-align every cell, including header cells.
        detailsTable.addPropertyChangeListener(new AlignTableListener(detailsTable, SwingConstants.LEADING));

        // Set the preferred column widths so that they're just big enough to display all contents without truncation.
        ColumnWidthsResizer.resize(detailsTable);
        ColumnWidthsResizer resizer = new ColumnWidthsResizer(detailsTable);
        detailsTable.getModel().addTableModelListener(resizer);
        detailsTable.getColumnModel().addColumnModelListener(resizer);

        // It's quite likely that the total width of the table is NOT EQUAL to the sum of the preferred column widths
        // that our TableModelListener calculated. In that case, resize all of the columns an equal percentage.
        // This will ensure that the relative ratios of the *actual* widths match those of the *preferred* widths.
        detailsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    private static AbstractButton getDetailsViewButton(JFileChooser fileChooser) {
        AbstractButton detailsButton = SwingUtils.getDescendantOfType(
                AbstractButton.class, fileChooser, "Icon", UIManager.getIcon("FileChooser.detailsViewIcon"));
        if (detailsButton != null) {
            return detailsButton;
        }

        logger.debug("Couldn't find a Details View button in the File Chooser dialog. Searching in popup menu.");

        JComponent componentWithPopupMenu = SwingUtils.getDescendantOfType(
                JComponent.class, fileChooser, "ComponentPopupMenu", SwingUtils.NOT_NULL);
        JPopupMenu popupMenu = componentWithPopupMenu.getComponentPopupMenu();

        for (JMenuItem menuItem : getAllMenuItems(popupMenu)) {
            if (menuItem.getText().equals("Details")) {
                return menuItem;
            }
        }

        return null;
    }

    private static List<JMenuItem> getAllMenuItems(JPopupMenu popupMenu) {
        List<JMenuItem> menuItems = new LinkedList<JMenuItem>();
        getAllMenuItems(popupMenu, menuItems);
        return menuItems;
    }

    private static void getAllMenuItems(MenuElement menuElem, List<JMenuItem> menuItems) {
        if (menuElem instanceof JMenuItem) {
            menuItems.add((JMenuItem) menuElem);
        }

        for (MenuElement subMenuElem : menuElem.getSubElements()) {
            getAllMenuItems(subMenuElem, menuItems);
        }
    }


    private static class AlignTableListener implements PropertyChangeListener {
        private final JTable table;
        private final int alignment;

        private AlignTableListener(JTable table, int alignment) {
            this.table = table;
            this.alignment = alignment;

            TableUtils.installAligners(table, alignment);  // Initial installation of alignment decorators.
        }

        /*
         * Every time the directory is changed in a JFileChooser dialog, a new TableColumnModel is created.
         * This is bad, because it discards the alignment decorators that we installed on the old model.
         * So, we're going to listen for the creation of new TableColumnModels so that we can reinstall the decorators.
         * See http://goo.gl/M6TU9I
         */
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("columnModel")) {
                TableUtils.installAligners(table, alignment);  // Reinstallation of alignment decorators.
            }
        }
    }

    /////////////////////////////////////////////////// JFileChooser ///////////////////////////////////////////////////

    @Override public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        int returnValue = super.showDialog(parent, approveButtonText);
        this.dialog = null;  // dialog was disposed in super-method. Null out so we don't try to use it.
        return returnValue;
    }

    @Override protected JDialog createDialog(Component parent) throws HeadlessException {
        this.dialog = super.createDialog(parent);  // Grab our own local reference to the dialog.
        return this.dialog;
    }
}
