/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.grid.GridDefRecord;
import ucar.grib.grib1.Grib1WriteIndex;
import ucar.grib.grib2.Grib2Input;
import ucar.grib.grib2.Grib2WriteIndex;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.ui.FileManager;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * ToolsUI/Iosp/Bufr
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class GribTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted gridRecordTable, gdsTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;

  public GribTable(PreferencesExt prefs) {
    this.prefs = prefs;

    gridRecordTable = new BeanTableSorted(GridRecordBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false);
    /* gridRecordTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ddsTable.setBeans(new ArrayList());
        obsTable.setBeans(new ArrayList());

        GridRecordBean mb = (GridRecordBean) gridRecordTable.getSelectedBean();
        java.util.List<DdsBean> beanList = new ArrayList<DdsBean>();
        try {
          setDataDescriptors(beanList, mb.gr.getRootDataDescriptor(), 0);
          setObs(mb.gr);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(GribTable.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        ddsTable.setBeans(beanList);
      }
    }); */

    gdsTable = new BeanTableSorted(GdsBean.class, (PreferencesExt) prefs.node("GdsBean"), false);
    /* obsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ObsBean csb = (ObsBean) obsTable.getSelectedBean();
      }
    }); */


    /* thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(gridRecordTable.getJTable(), "Options");
    varPopup.addAction("Show DDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridRecordBean vb = (GridRecordBean) gridRecordTable.getSelectedBean();
        infoTA.clear();
        Formatter f = new Formatter();
        try {
          if (!vb.gr.isTablesComplete()) {
            f.format(" MISSING DATA DESCRIPTORS= ");
            vb.gr.showMissingFields(f);
            f.format("%n%n");
          }

          vb.gr.dump(f);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(GribTable.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });  */


    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    //split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    //split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, gridRecordTable, gdsTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }


  public void save() {
    gridRecordTable.saveState(false);
    gdsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private String location;

  public void setGribFile(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();

    File indexFile = File.createTempFile("GribTable", "gbx");
    raf.seek(0);
    Grib2Input g2i = new Grib2Input(raf);
    int edition = g2i.getEdition();
    File gribFile = new File(raf.getLocation());

    GridIndex index = null;
    if (edition == 1) {
      index = new Grib1WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
    } else if (edition == 2) {
      index = new Grib2WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
    }

    java.util.List<GridRecordBean> grList = new ArrayList<GridRecordBean>();
    for (GridRecord gr : index.getGridRecords()) {
      grList.add( new GridRecordBean(gr));
    }

    java.util.List<GdsBean> gdsList = new ArrayList<GdsBean>();
    for (GridDefRecord gds : index.getHorizCoordSys()) {
      gdsList.add( new GdsBean(gds));
    }

    Map<String,String> atts = index.getGlobalAttributes();

    gridRecordTable.setBeans(grList);
    gdsTable.setBeans(gdsList);
  }


  public class GridRecordBean {
    GridRecord gr;

    // no-arg constructor
    public GridRecordBean() {
    }

    public GridRecordBean(GridRecord m) {
      this.gr = m;
    }

    public double getLevelOne() {
      return gr.getLevel1();
    }

    public double getLevelTwo() {
      return gr.getLevel2();
    }

    public int getLevelType1() {
      return gr.getLevelType1();
    }

    public int getLevelType2() {
      return gr.getLevelType2();
    }

    public Date getReferenceTime() {
      return gr.getReferenceTime();
    }

    public Date getValidTime() {
      return gr.getValidTime();
    }

    public int getValidTimeOffset() {
      return gr.getValidTimeOffset();
    }

    public String getParameterName() {
      return gr.getParameterName();
    }

    public String getGridDefRecordId() {
      return gr.getGridDefRecordId();
    }

    public int getDecimalScale() {
      return gr.getDecimalScale();
    }
  }

  public class GdsBean {
     GridDefRecord gds;

     // no-arg constructor
     public GdsBean() {
     }

     public GdsBean(GridDefRecord m) {
       this.gds = m;
     }

     public String getGroupName() {
       return gds.getGroupName();
     }
  }


}
