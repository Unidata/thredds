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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.Attribute;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.ui.FileManager;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ToolsUI/Iosp/Hdf5
 *
 * @author caron
 */
public class Hdf5Table extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted objectTable, messTable;
  private JSplitPane split, split2;

  private TextHistoryPane dumpTA;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;

  public Hdf5Table(PreferencesExt prefs) {
    this.prefs = prefs;

    objectTable = new BeanTableSorted(ObjectBean.class, (PreferencesExt) prefs.node("Hdf5Object"), false);
    objectTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        messTable.setBeans(new ArrayList());

        ArrayList beans = new ArrayList();
        ObjectBean ob = (ObjectBean) objectTable.getSelectedBean();
        for ( H5header.Message m : ob.m.getMessages()) {
          beans.add( new MessageBean(m));
        }
        messTable.setBeans(beans);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(objectTable.getJTable(), "Options");
    /* varPopup.addAction("Show DDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ObjectBean vb = (ObjectBean) objectTable.getSelectedBean();
        infoTA.clear();
        Formatter f = new Formatter();
        try {
          if (!vb.m.isTablesComplete()) {
            f.format(" MISSING DATA DESCRIPTORS= ");
            vb.m.showMissingFields(f);
            f.format("%n%n");
          }

          vb.m.dump(f);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(Hdf5Table.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });  */

    messTable = new BeanTableSorted(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false);
    messTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessageBean mb = (MessageBean) messTable.getSelectedBean();
        dumpTA.setText( mb.m.toString());
      }
    });

    // the info window
    dumpTA = new TextHistoryPane();

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, objectTable, messTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, dumpTA);
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
    objectTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private String location;

  public void setHdf5File(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    long start = System.nanoTime();
    java.util.List<ObjectBean> beanList = new ArrayList<ObjectBean>();

    NetcdfFile ncfile = new MyNetcdfFile();
    H5iosp iosp = new H5iosp();
    iosp.open(raf, ncfile, null);

    H5header header = (H5header) iosp.sendIospMessage("header");
    for (H5header.DataObject dataObj : header.getDataObjects()) {
      beanList.add(new ObjectBean(dataObj));
    }

    objectTable.setBeans(beanList);
  }

  private class MyNetcdfFile extends NetcdfFile {

  }

  public class ObjectBean {
    H5header.DataObject m;

    // no-arg constructor
    public ObjectBean() {
    }

    // create from a dataset
    public ObjectBean(H5header.DataObject m) {
      this.m = m;
    }

    public long getAddress(){
      return m.getAddress();
    }

    public String getWho() {
      return m.getWho();
    }
  }

  public class MessageBean {
    H5header.Message m;

    // no-arg constructor
    public MessageBean() {
    }

    // create from a dataset
    public MessageBean(H5header.Message m) {
      this.m = m;
    }

    public String getMessageType(){
      return m.getMtype().toString();
    }

    public short getSize() {
      return m.getSize();
    }

    public byte getFlags() {
      return m.getFlags();
    }

    public long getStart() {
      return m.getStart();
    }

  }


}
