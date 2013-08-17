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

import ucar.nc2.ft.point.bufr.BufrCdmIndex;
import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;
import ucar.nc2.iosp.bufr.Descriptor;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Examine BUFR CdmIndex files
 *
 * @author caron
 * @since 6/29/11
 */
public class BufrCdmIndexPanel extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollectionIndexPanel.class);

  private PreferencesExt prefs;

  private BeanTableSorted stationTable, fldTable;
  private JSplitPane split, split2, split3;

  private TextHistoryPane infoPopup, detailTA;
  private IndependentWindow infoWindow, detailWindow;

  public BufrCdmIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (index == null) return;
        Formatter f = new Formatter();
        index.showIndex(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(infoButton);


    /* AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
    filesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        showFiles(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(filesButton); */

    ////////////////////////////

    PopupMenu varPopup;

    ////////////////
    stationTable = new BeanTableSorted(StationBean.class, (PreferencesExt) prefs.node("StationBean"), false, "stations", "BufrCdmIndexProto.Station", null);
    fldTable = new BeanTableSorted(FieldBean.class, (PreferencesExt) prefs.node("FldBean"), false, "Fields", "BufrCdmIndexProto.Field", null);


    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    detailTA = new TextHistoryPane();
    detailWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), detailTA);
    detailWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stationTable, fldTable);
    split.setDividerLocation(prefs.getInt("splitPos", 800));

    add(split, BorderLayout.CENTER);

  }

  public void save() {
    stationTable.saveState(false);
    fldTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DetailWindowBounds", detailWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  ///////////////////////////////////////////////

  BufrCdmIndex index;
  public void setIndexFile(String indexFile) throws IOException {

    index = BufrCdmIndex.readIndex(indexFile);

    List<StationBean> stations = new ArrayList<StationBean>();
    if (index.stations != null) {
      for (BufrCdmIndexProto.Station s : index.stations)
        stations.add(new StationBean(s));
    }
    stationTable.setBeans(stations);

    List<FieldBean> flds = new ArrayList<FieldBean>();
    if (index.root != null)
      addFields(index.root, flds);
    fldTable.setBeans(flds);
  }

  private void addFields(BufrCdmIndexProto.Field parent, List<FieldBean> flds) {
    if (parent.getFldsList() == null) return;
    for (BufrCdmIndexProto.Field child : parent.getFldsList()) {
      flds.add(new FieldBean(parent, child));
      addFields(child, flds);
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  public class StationBean {
    BufrCdmIndexProto.Station s;

    public StationBean() {
    }

    public StationBean(BufrCdmIndexProto.Station s) {
      this.s = s;
    }

    public String getWmoId() {
      return s.getWmoId();
    }

    public String getName() {
      return s.getId();
    }

    public String getDescription() {
      return s.getDesc();
    }

    public double getLatitude() {
      return s.getLat();
    }

    public double getLongitude() {
      return s.getLon();
    }

    public double getAltitude() {
      return s.getAlt();
    }

    public int getCount() {
      return s.getCount();
    }

  }

    ////////////////////////////////////////////////////////////////////////////

  public class FieldBean {
    BufrCdmIndexProto.Field parent, child;

    public FieldBean() {
    }

    public FieldBean(BufrCdmIndexProto.Field parent, BufrCdmIndexProto.Field child) {
      this.parent = parent;
      this.child = child;
    }

    public String getParent() {
      return parent.getName();
    }

    public String getName() {
      return child.getName();
    }

    public String getFxy() {
      return Descriptor.makeString((short)child.getFxy());
    }

    public String getAction() {
      return child.hasAction() ? child.getAction().toString() : "";
    }

    public String getType() {
      return child.hasType() ? child.getType().toString() : "";
    }

    public int getMin() {
       return child.hasMin() ? child.getMin() : -1;
     }

    public int getMax() {
      return child.hasMax() ? child.getMax() : -1;
    }

  }

}


