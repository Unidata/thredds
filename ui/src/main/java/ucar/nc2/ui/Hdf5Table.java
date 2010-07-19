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
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.iosp.hdf5.H5header;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;

/**
 * ToolsUI/Iosp/Hdf5
 *
 * @author caron
 */
public class Hdf5Table extends JPanel {
  private PreferencesExt prefs;

  private ucar.util.prefs.ui.BeanTableSorted objectTable, messTable, attTable;
  private JSplitPane splitH, split, split2;

  private TextHistoryPane dumpTA, infoTA;
  private IndependentWindow infoWindow;

  public Hdf5Table(PreferencesExt prefs) {
    this.prefs = prefs;

    objectTable = new BeanTableSorted(ObjectBean.class, (PreferencesExt) prefs.node("Hdf5Object"), false, "H5header.DataObject", "Level 2A data object header");
    objectTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        messTable.setBeans(new ArrayList());

        ArrayList beans = new ArrayList();
        ObjectBean ob = (ObjectBean) objectTable.getSelectedBean();
        for ( H5header.HeaderMessage m : ob.m.getMessages()) {
          beans.add( new MessageBean(m));
        }
        messTable.setBeans(beans);

        ArrayList attBeans = new ArrayList();
        for ( H5header.MessageAttribute m : ob.m.getAttributes()) {
          attBeans.add( new AttributeBean(m));
        }
        attTable.setBeans(attBeans);
      }
    });

    messTable = new BeanTableSorted(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false, "H5header.HeaderMessage", "Level 2A1 and 2A2 (part of Data Object)");
    messTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessageBean mb = (MessageBean) messTable.getSelectedBean();
        dumpTA.setText( mb.m.toString());
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show FractalHeap", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messTable.getSelectedBean();

        if (infoTA == null) {
          infoTA = new TextHistoryPane();
          infoWindow = new IndependentWindow("Extra", BAMutil.getImage("netcdfUI"), infoTA);
          infoWindow.setBounds(new Rectangle(300, 300, 500, 800));
        }
        infoTA.clear();
        Formatter f = new Formatter();
        mb.m.showFractalHeap(f);

        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    attTable = new BeanTableSorted(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false, "H5header.HeaderAttribute", "Message Type 12/0xC : define an Atribute");
    attTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        AttributeBean mb = (AttributeBean) attTable.getSelectedBean();
        dumpTA.setText( mb.att.toString());
      }
    });

    // the info window
    dumpTA = new TextHistoryPane();

    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, objectTable, dumpTA);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, messTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, attTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    objectTable.saveState(false);
    messTable.saveState(false);
    attTable.saveState(false);
    // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putInt("splitPosH", splitH.getDividerLocation());
  }

  private H5iosp iosp;
  private String location;

  public void setHdf5File(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    long start = System.nanoTime();
    java.util.List<ObjectBean> beanList = new ArrayList<ObjectBean>();

    iosp = new H5iosp();
    NetcdfFile ncfile = new MyNetcdfFile(iosp);
    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(20000);
      PrintStream s = new PrintStream(bos);
      t.printStackTrace(s);
      dumpTA.setText( bos.toString());      
    }

    H5header header = (H5header) iosp.sendIospMessage("header");
    for (H5header.DataObject dataObj : header.getDataObjects()) {
      beanList.add(new ObjectBean(dataObj));
    }

    objectTable.setBeans(beanList);
  }

  public void showInfo(Formatter f) throws IOException {
    if (iosp == null) return;

    ByteArrayOutputStream ff = new ByteArrayOutputStream(100 * 1000);
    PrintStream ps = new PrintStream(ff);
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header H5header/headerDetails H5header/symbolTable H5header/memTracker"));
    H5header headerEmpty = (H5header) iosp.sendIospMessage("headerEmpty");
    headerEmpty.read(ps);
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
    ps.flush();
    f.format("%s", ff.toString());
  }

  private class MyNetcdfFile extends NetcdfFile {
    private MyNetcdfFile(H5iosp iosp) {
      super();
      spi = iosp;
    }
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

    public String getName() {
      return m.getName();
    }
  }

  public class MessageBean {
    H5header.HeaderMessage m;

    // no-arg constructor
    public MessageBean() {
    }

    // create from a dataset
    public MessageBean(H5header.HeaderMessage m) {
      this.m = m;
    }

    public String getMessageType(){
      return m.getMtype().toString();
    }

    public String getName(){
      return m.getName();
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

  public class AttributeBean {
    H5header.MessageAttribute att;

    // no-arg constructor
    public AttributeBean() {
    }

    // create from a dataset
    public AttributeBean(H5header.MessageAttribute att) {
      this.att = att;
    }

    public byte getVersion() {
      return att.getVersion();
    }

    public String getAttributeName() {
      return att.getName();
    }

    public String getMdt() {
      return att.getMdt().toString();
    }

    public String getMds() {
      return att.getMds().toString();
    }

    public long getDataPos() {
      return att.getDataPos();
    }

  }


}
