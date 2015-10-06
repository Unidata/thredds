package ucar.nc2.ui.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ui.table.ColumnWidthsResizer;
import ucar.nc2.ui.table.TableAligner;
import ucar.nc2.ui.util.SwingUtils;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
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
 * </ul>
 *
 * @author cwardgar
 */
public class ImprovedFileChooser extends JFileChooser {
    private static final Logger logger = LoggerFactory.getLogger(ImprovedFileChooser.class);

    private static String osName = System.getProperty("os.name").toLowerCase();
    public static boolean isMacOs = osName.startsWith("mac os x");

    static {
        // Disable editable "Name" column in JFileChooser details table.
        // This setting is honored by sun.swing.FilePane.DetailsTableModel.
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    }

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
            // mac osx does not have a details button. It is recommended to use
            // a java.awt.FileDialog box on MacOSX for the proper look and feel.
            // so, don't warn if on a mac.
            // https://developer.apple.com/library/mac/documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html
            if (!isMacOs) {
                logger.warn("Couldn't find Details button!");
            }
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

        // Set the preferred column widths so that they're big enough to display all data without truncation.
        ColumnWidthsResizer resizer = new ColumnWidthsResizer(detailsTable);
        detailsTable.getModel().addTableModelListener(resizer);
        detailsTable.getColumnModel().addColumnModelListener(resizer);

        // Left-align every cell, including header cells.
        TableAligner aligner = new TableAligner(detailsTable, SwingConstants.LEADING);
        detailsTable.getColumnModel().addColumnModelListener(aligner);

        // Every time the directory is changed in a JFileChooser dialog, a new TableColumnModel is created.
        // This is bad, because it discards the alignment decorators that we installed on the old model.
        // So, we 're going to listen for the creation of new TableColumnModels so that we can reinstall the decorators.
        detailsTable.addPropertyChangeListener(new NewColumnModelListener(detailsTable, SwingConstants.LEADING));

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
        if (componentWithPopupMenu == null) {
            return null;
        }

        JPopupMenu popupMenu = componentWithPopupMenu.getComponentPopupMenu();
        if (popupMenu == null) {
            return null;
        }

        for (JMenuItem menuItem : getAllMenuItems(popupMenu)) {
            if (menuItem.getText().equals("Details")) {
                return menuItem;
            }
        }

        return null;
    }

    private static List<JMenuItem> getAllMenuItems(JPopupMenu popupMenu) {
        List<JMenuItem> menuItems = new LinkedList<>();
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

    private static class NewColumnModelListener implements PropertyChangeListener {
        private final JTable table;
        private final int alignment;

        private NewColumnModelListener(JTable table, int alignment) {
            this.table = table;
            this.alignment = alignment;
        }

        @Override public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("columnModel")) {
                // Left-align every cell, including header cells.
                TableAligner aligner = new TableAligner(table, alignment);
                table.getColumnModel().addColumnModelListener(aligner);
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


    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException,
            InstantiationException, IllegalAccessException {
        // Switch to Nimbus Look and Feel, if it's available.
        if (isMacOs) {
            System.setProperty ("apple.laf.useScreenMenuBar", "true");
        } else {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        ImprovedFileChooser fileChooser = new ImprovedFileChooser();
        fileChooser.setPreferredSize(new Dimension(1000, 750));
        fileChooser.showDialog(null, "Choose");
    }
}
