package thredds.ui.catalog.tools;

import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.crawl.CatalogCrawler;
import thredds.ui.catalog.CatalogChooser;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.IO;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bulk copy of datasets from a catalog. Must have an HTTP access method
 *
 * @author caron
 * @since Dec 14, 2010
 */
public class CatalogCopier extends JPanel {
  private CatalogChooser catalogChooser;
  private TextHistoryPane resultText;
  private JSplitPane splitV;
  private ComboBox filterCB;
  private FileManager fileManager;

  private PreferencesExt prefs;
  private Component parent; // need for popup messages

  /**
   * General Constructor.
   * Create a CatalogChooser and a QueryChooser widget, add them to a JTabbedPane.
   * Optionally write to stdout and/or pop up event messsages.
   *
   * @param prefs persistent storage
   * @param parent parent component
   */
  public CatalogCopier(PreferencesExt prefs, Component parent) {
    this.prefs = prefs;
    this.parent = parent;

    // create the catalog chooser
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("catChooser");
    catalogChooser = new CatalogChooser(node, true, false, false);

    // create result pane;
    JPanel copyPanel = new JPanel(new BorderLayout());
    resultText = new TextHistoryPane( false);
    copyPanel.add( resultText, BorderLayout.CENTER);

    // combo box holds the catalogs
    filterCB = new ComboBox(prefs);

    // top panel buttons
    JButton filterButt = new JButton("Filter");
    filterButt.setToolTipText("apply dataset filter");
    filterButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String filter = (String) filterCB.getSelectedItem();
        doFilter(filter);
      }
    });

    JButton copyButt = new JButton("Copy");
    copyButt.setToolTipText("copy datasets to local drive");
    copyButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String filter = (String) filterCB.getSelectedItem();
        doCopy(filter);
      }
    });

    JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    topButtons.add(filterButt);
    topButtons.add(copyButt);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("Dataset path regexp filter:"), BorderLayout.WEST);
    topPanel.add(filterCB, BorderLayout.CENTER);
    topPanel.add(topButtons, BorderLayout.EAST);

    copyPanel.add( topPanel, BorderLayout.NORTH);

    // layout
    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, catalogChooser, copyPanel);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout( new BorderLayout());
    add( splitV, BorderLayout.CENTER);

    /////////////////////////
    // a file chooser used for catgen on a directory
    fileManager = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
    fileManager.getFileChooser().setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES);
    fileManager.getFileChooser().setDialogTitle( "Choose Directory to write to");
  }

  private void doCopy( String regexp) {
    String dir = fileManager.chooseDirectory(null);
    if (dir == null) return;
    File copyDir = new File(dir);

    Pattern pattern = (regexp == null) ? null : Pattern.compile(regexp);
    InvDataset curr = catalogChooser.getSelectedDataset();
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, new FilterListener(copyDir));

    resultText.clear();
    resultText.appendLine("Read catalog="+curr+" with filter=" +regexp);
    crawler.crawlDirectDatasets(curr, null, null, pattern, false);
    resultText.gotoTop();

    filterCB.addItem(regexp);
  }

  private int count;

  private void doFilter( String regexp) {
    Pattern pattern = (regexp == null) ? null : Pattern.compile(regexp);
    InvDataset curr = catalogChooser.getSelectedDataset();
    if (curr != null) {
      CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, new FilterListener(null));

      count = 0;
      resultText.clear();
      resultText.appendLine("Read catalog=" + curr + " with filter=" + regexp);
      crawler.crawlDirectDatasets(curr, null, null, pattern, false);
      resultText.appendLine("count = " + count);
      resultText.gotoTop();
      filterCB.addItem(regexp);
    }
  }

  private class FilterListener implements CatalogCrawler.Listener {
    File copyDir;

    private FilterListener(File copyDir) {
      this.copyDir = copyDir;
    }

    @Override
    public void getDataset(InvDataset dd, Object context) {
      InvAccess inv = dd.getAccess(thredds.catalog.ServiceType.HTTPServer);
      if (inv == null)
        inv = dd.getAccess(thredds.catalog.ServiceType.HTTP);

      if (inv != null) {
        Pattern pattern = (Pattern) context;
        boolean ok = true;
        if (pattern != null) {
          Matcher m = pattern.matcher(inv.getStandardUrlName());
          ok = m.matches();
        }

        if (ok && (copyDir != null)) {
          boolean copyOk = doOneCopy(inv.getStandardUrlName(), copyDir);
          System.out.printf("%s copyOk=%s%n", inv.getStandardUrlName(), copyOk);
          resultText.appendLine(inv.getStandardUrlName()+" copyOk="+copyOk);
        } else {
          if (ok) System.out.printf("ok match=%s%n", inv.getStandardUrlName());
          resultText.appendLine((ok?"ok ":"no ")+inv.getStandardUrlName());
        }
        if (ok) count++;
      }
    }

    @Override
    public boolean getCatalogRef(InvCatalogRef catRef, Object context) {
      System.out.printf("catRef=%s%n", catRef);
      return true;
    }

    private boolean doOneCopy(String url, File dir) {
      int pos = url.lastIndexOf("/");
      String filename = url.substring(pos+1);
      File f = new File(dir, filename);
      try {
        IO.readURLtoFileWithExceptions(url, f);
        return true;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }
}


  /** save the state */
  public void save() {
    catalogChooser.save();
    filterCB.save();
    prefs.putInt("splitPos", splitV.getDividerLocation());
  }
}
