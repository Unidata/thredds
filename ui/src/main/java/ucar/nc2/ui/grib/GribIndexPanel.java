package ucar.nc2.ui.grib;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

/**
 * Examine GRIB index (gbx9) files
 *
 * @author caron
 * @since 12/10/12
 */
public class GribIndexPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable recordTable;

  private TextHistoryPane infoPopup, detailTA;
  private IndependentWindow infoWindow, detailWindow;

  public GribIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    /* AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        gc.showIndex(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(infoButton);


    AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
    filesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        showFiles(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(filesButton);    */

    ////////////////////////////

    PopupMenu popup;

    recordTable = new BeanTable(RecordBean.class, (PreferencesExt) prefs.node("Grib2RecordBean"), false);

    popup = new PopupMenu(recordTable.getJTable(), "Options");
    popup.addAction("Show Record", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) recordTable.getSelectedBean();
        if (bean != null) {
          Formatter f= new Formatter();
          bean.show(f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });


    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    detailTA = new TextHistoryPane();
    detailWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), detailTA);
    detailWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    /* split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, groupTable, varTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 800));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split3, vertCoordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, timeCoordTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500)); */

    add(recordTable, BorderLayout.CENTER);

  }

  Object gc = null;

  public void save() {
    recordTable.saveState(false);
    /* prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DetailWindowBounds", detailWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation()); */
  }

  public void closeOpenFiles() throws IOException {
    // if (gc != null) gc.close();
    gc = null;
  }

  ///////////////////////////////////////////////
  public void setIndexFile(String indexFile) throws IOException {
    closeOpenFiles();

    try (RandomAccessFile raf = new RandomAccessFile(indexFile, "r")) {
      raf.seek(0);
      String magic = raf.readString(Grib2Index.MAGIC_START
              .getBytes(CDM.utf8Charset).length);
      if (magic.equals(Grib2Index.MAGIC_START)) {
        readIndex2(indexFile);
      } else if (magic.equals(Grib1Index.MAGIC_START)) {
        readIndex1(indexFile);
      } else
        throw new IOException("Not a grib index file =" + magic);
    }

  }

  public void readIndex1(String filename) throws IOException {
    Grib1Index g1idx =  new Grib1Index();
    g1idx.readIndex(filename, 0, thredds.inventory.CollectionUpdateType.nocheck);

    java.util.List<RecordBean> records = new ArrayList<>();
     for (Grib1Record gr : g1idx.getRecords())
       records.add(new RecordBean(gr));
     recordTable.setBeans(records);
  }

  public void readIndex2(String filename) throws IOException {
    Grib2Index g2idx =  new Grib2Index();
    g2idx.readIndex(filename, 0, thredds.inventory.CollectionUpdateType.nocheck);

    java.util.List<RecordBean> records = new ArrayList<>();
     for (Grib2Record gr : g2idx.getRecords())
       records.add(new RecordBean(gr));
     recordTable.setBeans(records);
  }


  ////////////////////////////////////////////////////////////////////////////


  public class RecordBean {
    Grib1Record gr1;
    Grib2Record gr2;

    public RecordBean() {
    }

    public RecordBean(Grib2Record gr) {
      this.gr2 = gr;
    }

    public RecordBean(Grib1Record gr) {
      this.gr1 = gr;
    }

    public int getFile() {
      return (gr2 == null) ? gr1.getFile() : gr2.getFile();
    }

    public String getReferenceDate() {
      return (gr2 == null) ? gr1.getReferenceDate().toString() : gr2.getReferenceDate().toString();
    }

    public long getStart() {
      return (gr2 == null) ? gr1.getIs().getStartPos() : gr2.getIs().getStartPos();
    }

    public long getLength() {
      return (gr2 == null) ? gr1.getIs().getMessageLength() : gr2.getIs().getMessageLength();
    }

    private void show(Formatter f) {
      if (gr2 == null) show(gr1, f); else show(gr2, f);
    }

    private void show(Grib1Record gr1, Formatter f) {
      Grib1SectionProductDefinition pds = gr1.getPDSsection();
      Grib1Customizer cust = Grib1Customizer.factory(gr1, null);
      pds.showPds(cust, f);
    }

    private void show(Grib2Record gr2, Formatter f) {
      gr2.show(f);
    }
  }

}


