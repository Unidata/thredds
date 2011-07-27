/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import thredds.inventory.DatasetCollectionMFiles;
import thredds.inventory.MFile;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.table.GribTables;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/4/11
 */
public class GribFilesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted filesTable, collectionTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup;
  private IndependentWindow infoWindow;

  public GribFilesPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    ////////////////
    collectionTable = new BeanTableSorted(CollectionBean.class, (PreferencesExt) prefs.node("CollectionBean"), true);
    /* collectionTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        if (pb != null)
          showFilesInCollection(pb);
      }
    }); */

    varPopup = new ucar.nc2.ui.widget.PopupMenu(collectionTable.getJTable(), "Options");
    varPopup.addAction("Show Files in Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        Formatter f = new Formatter();
        showFilesInCollection(pb, f);
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    varPopup.addAction("Read Files", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = collectionTable.getSelectedBeans();
        readFiles(list);
      }
    });

    varPopup.addAction("Open in Grib2n", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        if (pb == null) return;
        GribFilesPanel.this.firePropertyChange("openGrib2n", null, pb.getSpec());
      }
    });

    filesTable = new BeanTableSorted(FileBean.class, (PreferencesExt) prefs.node("FileBean"), false);
    varPopup = new PopupMenu(filesTable.getJTable(), "Options");

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean bean = (FileBean) filesTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showComplete(f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

      varPopup.addAction("Open in Grib2n", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean pb = (FileBean) filesTable.getSelectedBean();
        if (pb == null) return;
        GribFilesPanel.this.firePropertyChange("openGrib2n", null, pb.m.getPath());
      }
    });

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, collectionTable, filesTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    //split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds2Table);
    //split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split2, BorderLayout.CENTER);

  }

  public void save() {
    filesTable.saveState(false);
    collectionTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void showCollection(Formatter f) {

  }

  private void showFilesInCollection(CollectionBean bean, Formatter f) {
    f.format("Collection= %s (%d) %n", bean.spec);
    int count = 0;
    for (MFile mfile : bean.fileList) {
      f.format("  %s%n", mfile.getPath());
      count++;
    }
    f.format(" count = %d %n", count);
  }

  ///////////////////////////////////////////////

  private List<CollectionBean> collections = new ArrayList<CollectionBean>();

  public void setCollection(String spec) throws IOException {
    collections.add(new CollectionBean(spec));
    collectionTable.setBeans(collections);
  }

  private void readFiles(List<CollectionBean> beans) {
    List<FileBean> files = new ArrayList<FileBean>();
    for (CollectionBean bean : beans) {
      for (MFile mfile : bean.fileList) {
        files.add(new FileBean(mfile));
      }
    }
    filesTable.setBeans(files);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class CollectionBean {
    String spec;
    DatasetCollectionMFiles dcm;
    Iterable<MFile> fileList;

    // no-arg constructor

    public CollectionBean() {
    }

    public CollectionBean(String spec) throws IOException {
      this.spec = spec;

      Formatter f = new Formatter();
      DatasetCollectionMFiles dc = null;
      try {
        dc = DatasetCollectionMFiles.open(spec, null, f);
        dc.scan();
        fileList = dc.getFiles();

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
        javax.swing.JOptionPane.showMessageDialog(null, "Collection is null");
      }
    }

    public int getN() {
      return Misc.getSize(fileList);
    }

    public String getSpec() {
      return spec;
    }

  }

  public class FileBean {
    MFile m;
    GribIndex index;
    int nRecords, localCount = 0, gdsCount = 0;
    Grib2Record first;
    GribTables tables;
    boolean bad = false;

    public FileBean() {
    }

    public FileBean(MFile m) {
      this.m = m;

      try {
        index = new GribIndex();
        if (!index.readIndex(m.getPath(), m.getLastModified())) {
          index.makeIndex(m.getPath(), new Formatter());
        }

        Map<Long, Grib2SectionGridDefinition> gdsSet = new HashMap<Long, Grib2SectionGridDefinition>();
        for (Grib2SectionGridDefinition gds : index.getGds()) {
          if (gdsSet.get(gds.calcCRC()) == null)
            gdsSet.put(gds.calcCRC(), gds);
        }
        gdsCount = gdsSet.size();

        nRecords = index.getRecords().size();
        for (Grib2Record gr : index.getRecords()) {
          if (first == null) first = gr;
          if (tables == null) {
            Grib2SectionIdentification ids = gr.getId();
            tables = GribTables.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
          }
          Grib2Pds pds = gr.getPDSsection().getPDS();
          if ((pds.getParameterCategory() > 191) || (pds.getParameterNumber() > 191))
            localCount++;
        }

      } catch (IOException e) {
        System.out.printf("%s%n", e.getMessage());
        bad = true;
      }
    }

    public final String getPath() {
      return (bad) ? "BAD "+ m.getPath() : m.getPath();
    }

    public final String getRefDate() {
      return (bad) ? "" : first.getReferenceDate().toString();
    }

    public String getHeader() {
      return (bad) ? "" : StringUtil.cleanup(first.getHeader());
    }

    public int getMasterTable() {
      return (bad) ? -1 : first.getId().getMaster_table_version();
    }

    public int getLocalTable() {
      return (bad) ? -1 : first.getId().getLocal_table_version();
    }

    public int getCenter() {
      return (bad) ? -1 : first.getId().getCenter_id();
    }

    public int getSubCenter() {
      return (bad) ? -1 : first.getId().getSubcenter_id();
    }

    public String getLocalCount() {
      return (bad) ? "" : localCount+"/"+nRecords;
    }

    public int getGdsCount() {
      return gdsCount;
    }

    void showComplete(Formatter f) {
      try {
        GribNewPanel.showCompleteGribRecord(f, m.getPath(), first, tables);
      } catch (IOException e) {
        e.printStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
      }
    }

  }

}

