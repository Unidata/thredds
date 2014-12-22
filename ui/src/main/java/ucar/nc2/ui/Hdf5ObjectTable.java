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

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * ToolsUI/Iosp/Hdf5  raw file objects
 *
 * @author caron
 */
public class Hdf5ObjectTable extends JPanel {
  private PreferencesExt prefs;

  private ucar.util.prefs.ui.BeanTable objectTable, messTable, attTable;
  private JSplitPane splitH, split, split2;

  private TextHistoryPane dumpTA, infoTA;
  private IndependentWindow infoWindow;

  public Hdf5ObjectTable(PreferencesExt prefs) {
    this.prefs = prefs;
    PopupMenu varPopup;

    objectTable = new BeanTable(ObjectBean.class, (PreferencesExt) prefs.node("Hdf5Object"), false,
            "H5header.DataObject", "Level 2A data object header", null);
    objectTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        messTable.setBeans(new ArrayList());

        ArrayList<Object> beans = new ArrayList<>();
        ObjectBean ob = (ObjectBean) objectTable.getSelectedBean();
        for ( H5header.HeaderMessage m : ob.m.getMessages()) {
          beans.add( new MessageBean(m));
        }
        messTable.setBeans(beans);

        ArrayList<Object> attBeans = new ArrayList<>();
        for ( H5header.MessageAttribute m : ob.m.getAttributes()) {
          attBeans.add( new AttributeBean(m));
        }
        attTable.setBeans(attBeans);
      }
    });

    varPopup = new ucar.nc2.ui.widget.PopupMenu(objectTable.getJTable(), "Options");
    varPopup.addAction("show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ObjectBean mb = (ObjectBean) objectTable.getSelectedBean();
        if (mb == null) return;
        dumpTA.clear();
        Formatter f = new Formatter();

        try {
          mb.show(f);
        } catch (IOException e1) {
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        dumpTA.appendLine(f.toString());
        dumpTA.gotoTop();
      }
    });


    messTable = new BeanTable(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false,
            "H5header.HeaderMessage", "Level 2A1 and 2A2 (part of Data Object)", null);
    messTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessageBean mb = (MessageBean) messTable.getSelectedBean();
        dumpTA.setText( mb.m.toString());
      }
    });

    varPopup = new ucar.nc2.ui.widget.PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show FractalHeap", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messTable.getSelectedBean();
        if (mb == null) return;
        if (infoTA == null) makeInfoWindow();
        infoTA.clear();
        Formatter f = new Formatter();

        mb.m.showFractalHeap(f);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false,
            "H5header.HeaderAttribute", "Message Type 12/0xC : define an Atribute", null);
    attTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        AttributeBean mb = (AttributeBean) attTable.getSelectedBean();
        Formatter f = new Formatter();
        mb.show(f);
        dumpTA.setText( f.toString());
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

  private void makeInfoWindow() {
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds(new Rectangle(300, 300, 500, 800));
  }

  public void getEosInfo(Formatter f) throws IOException {
    iosp.getEosInfo(f);
  }

  private H5iosp iosp;
  private String location;

  public void closeOpenFiles() throws IOException {
    if (iosp != null) iosp.close();
    iosp = null;
    attTable.clearBeans();
    messTable.clearBeans();
    objectTable.clearBeans();
    dumpTA.clear();
  }

  public void setHdf5File(RandomAccessFile raf) throws IOException {
    closeOpenFiles();

    this.location = raf.getLocation();
    java.util.List<ObjectBean> beanList = new ArrayList<>();

    iosp = new H5iosp();
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    ncfile.sendIospMessage(H5iosp.IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES);

    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(20000);
      PrintWriter s = new PrintWriter(sw);
      t.printStackTrace(s);
      dumpTA.setText(sw.toString());
    }

    H5header header = (H5header) iosp.sendIospMessage("header");
    for (H5header.DataObject dataObj : header.getDataObjects()) {
      beanList.add(new ObjectBean(dataObj));
    }

    objectTable.setBeans(beanList);
  }

  public void showInfo(Formatter f) throws IOException {
    if (iosp == null) return;

    List<Object> objs = objectTable.getBeans();
    for (Object obj : objs) {
      ObjectBean bean = (ObjectBean) obj;
      bean.m.show(f);
    }
  }


  public void showInfo2(Formatter f) throws IOException {
    if (iosp == null) return;

    ByteArrayOutputStream os = new ByteArrayOutputStream(100 * 1000);
    PrintWriter pw = new PrintWriter( new OutputStreamWriter(os, CDM.utf8Charset));
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header H5header/headerDetails H5header/symbolTable H5header/memTracker"));
    H5header headerEmpty = (H5header) iosp.sendIospMessage("headerEmpty");
    headerEmpty.read(pw);
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
    pw.flush();
    f.format("%s", os.toString(CDM.utf8Charset.name()));
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
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

    void show(Formatter f) throws IOException {
      f.format("HDF5 object name '%s'%n", m.getName());
      //for ( H5header.HeaderMessage mess : m.getMessages()) {
      //  if (mess. instanceof  H5header.MessageDatatype)
      //}

      for ( H5header.MessageAttribute mess : m.getAttributes()) {
        Attribute att = mess.getNcAttribute();
        f.format("  %s%n", att);
      }
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

    public int getSize() {
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
      return att.getDataPosAbsolute();
    }

    void show(Formatter f) {
      f.format("hdf5 att = %s%n%n", att);
      try {
        f.format("netcdf attribute%n %s;%n", att.getNcAttribute());
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }

  }


}
