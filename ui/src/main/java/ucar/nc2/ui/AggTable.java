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

import ucar.ma2.Array;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.CompareNetcdf2;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;

/**
 * ToolsUI/NcML/Aggregation
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class AggTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTable datasetTable;
  private JSplitPane split;

  private TextHistoryPane infoTA, aggTA;
  private IndependentWindow infoWindow;

  private NetcdfDataset current;

  public AggTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    datasetTable = new BeanTable(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    /* datasetTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        datasetTable.getSelectedBean();
      }
    }); */

    PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(datasetTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) datasetTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openNetcdfFile", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) datasetTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openCoordSystems", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) datasetTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openGridDataset", null, dsb.acquireFile());
      }
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check files", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        compare(f);
        checkAggCoordinate(f);

        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    buttPanel.add(compareButton);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    aggTA = new TextHistoryPane();

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, datasetTable, aggTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    datasetTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void setAggDataset(NetcdfDataset ncd) throws IOException {
    current = ncd;

    Aggregation agg = ncd.getAggregation();
    java.util.List<DatasetBean> beanList = new ArrayList<>();
    for (Aggregation.Dataset dataset : agg.getDatasets()) {
      beanList.add(new DatasetBean(dataset));
    }

    datasetTable.setBeans(beanList);

    Formatter f = new Formatter();
    agg.getDetailInfo(f);
    aggTA.setText(f.toString());
  }

  private void checkAggCoordinate(Formatter f) {
    if (null == current) return;

    try {
      Aggregation agg = current.getAggregation();
      String aggDimName = agg.getDimensionName();
      Variable aggCoord = current.findVariable(aggDimName);
      Array data = aggCoord.read();
      f.format("   Aggregated coordinate variable %s%n", aggCoord);
      f.format(NCdumpW.toString(data, aggDimName, null));

      for (Object bean : datasetTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        Aggregation.Dataset ads = dbean.ds;

        try (NetcdfFile aggFile = ads.acquireFile(null)) {
          f.format("   Component file %s%n", aggFile.getLocation());
          Variable aggCoordp = aggFile.findVariable(aggDimName);
          if (aggCoordp == null) {
            f.format("   doesnt have coordinate variable%n");
          } else {
            data = aggCoordp.read();
            f.format(NCdumpW.toString(data, aggCoordp.getNameAndDimensions() + " (" + aggCoordp.getUnitsString() + ")", null));
          }
        }
      }
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(10000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }
  }

  private void compare(Formatter f) {
    try {
      NetcdfFile org = null;
      for (Object bean : datasetTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        Aggregation.Dataset ads = dbean.ds;

        NetcdfFile ncd = ads.acquireFile(null);
        if (org == null)
          org = ncd;
        else {
          CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, false);
          cn.compareVariables(org, ncd);
          ncd.close();
          f.format("--------------------------------%n");
        }
      }
      if (org != null) org.close();
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(10000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }
  }

  public class DatasetBean {
    Aggregation.Dataset ds;

    protected NetcdfFile acquireFile() {
      try {
        return ds.acquireFile(null);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    // create from a dataset
    public DatasetBean(Aggregation.Dataset ds) {
      this.ds = ds;
    }

    public String getLocation() throws IOException {
      return ds.getLocation();
    }

    public String getCacheLocation() {
      return ds.getCacheLocation();
    }

    public String getId() {
      return ds.getId();
    }

  }
}
