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

import ucar.nc2.ui.widget.PopupMenu;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;
import ucar.nc2.dataset.NetcdfDataset;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.BAMutil;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.*;

/**
 * ToolsUI/NcML/Aggregation
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class FmrcTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTable messageTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private CoordSysTable coordSysTable;

  public FmrcTable(PreferencesExt prefs) {
    this.prefs = prefs;

    messageTable = new BeanTable(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    messageTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb != null)
          coordSysTable.setDataset(dsb.ds);
      }
    });

    PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(messageTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        FmrcTable.this.firePropertyChange("openNetcdfFile", null, dsb.ds);
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        FmrcTable.this.firePropertyChange("openCoordSystems", null, dsb.ds);
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        FmrcTable.this.firePropertyChange("openGridDataset", null, dsb.ds);
      }
    });


    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    coordSysTable = new CoordSysTable( (PreferencesExt) prefs.node("CoordSys"), null);

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, coordSysTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    messageTable.saveState(false);
    coordSysTable.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  /* public void setFmrc(FmrcImpl fmrc) throws IOException {
    java.util.List<DatasetBean> beanList = new ArrayList<DatasetBean>();
    DateFormatter df = new DateFormatter();
    beanList.add(new DatasetBean("Fmrc2d", fmrc.getFmrcDataset()));
    beanList.add(new DatasetBean("best", fmrc.getBestTimeSeries()));
    for (Date runDate : fmrc.getRunDates()) {
      beanList.add(new DatasetBean("runDate "+df.toDateTimeString(runDate), fmrc.getRunTimeDataset(runDate)));
    }
    for (Date forecastDate : fmrc.getForecastDates()) {
      beanList.add(new DatasetBean("forecast "+df.toDateTimeString(forecastDate), fmrc.getForecastTimeDataset(forecastDate)));
    }
    for (Double offset : fmrc.getForecastOffsets()) {
      beanList.add(new DatasetBean("offset "+offset, fmrc.getForecastOffsetDataset(offset)));
    }

    messageTable.setBeans(beanList);
    obsTable.setBeans(new ArrayList());
    ddsTable.setBeans(new ArrayList());
  } */

  public class DatasetBean {
    String name;
    NetcdfDataset ds;

    // no-arg constructor
    public DatasetBean() {
    }

    // create from a dataset
    public DatasetBean(String name, NetcdfDataset ds) {
      this.ds = ds;
      this.name = name;
    }

    public String getLocation() throws IOException {
      return ds.getLocation();
    }

    public String getName() {
      return name;
    }

  }

}
