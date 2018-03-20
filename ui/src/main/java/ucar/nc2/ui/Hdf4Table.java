/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.iosp.hdf4.H4header;
import ucar.nc2.iosp.hdf4.H4iosp;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;

/**
 * ToolsUI/Iosp/Hdf4
 *
 * @author caron
 */
public class Hdf4Table extends JPanel {
  private PreferencesExt prefs;

  private ucar.util.prefs.ui.BeanTable tagTable; //, messTable, attTable;
  private JSplitPane split;

  private TextHistoryPane dumpTA, infoTA;
  private IndependentWindow infoWindow;

  public Hdf4Table(PreferencesExt prefs) {
    this.prefs = prefs;

    tagTable = new BeanTable(TagBean.class, (PreferencesExt) prefs.node("Hdf4Object"), false);
    tagTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TagBean bean = (TagBean) tagTable.getSelectedBean();
        dumpTA.setText("Tag=\n ");
        dumpTA.appendLine(bean.tag.detail());
        dumpTA.appendLine("\nVinfo=");
        dumpTA.appendLine(bean.tag.getVinfo());
      }
    });

    /* messTable = new BeanTable(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false);
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

    attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false);
    attTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        AttributeBean mb = (AttributeBean) attTable.getSelectedBean();
        dumpTA.setText( mb.att.toString());
      }
    }); */

    // the info window
    dumpTA = new TextHistoryPane();

    //splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, objectTable, dumpTA);
    //splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, tagTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    //split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, attTable);
    //split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    tagTable.saveState(false);
    //messTable.saveState(false);
    //attTable.saveState(false);
    // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    //prefs.putInt("splitPosH", splitH.getDividerLocation());
  }

  private H4iosp iosp = null;
  private H4header header;
  private String location;

  public void closeOpenFiles() throws IOException {
    if (iosp != null) iosp.close();
    iosp = null;
  }

  public void setHdf4File(RandomAccessFile raf) throws IOException {
    closeOpenFiles();

    this.location = raf.getLocation();
    java.util.List<TagBean> beanList = new ArrayList<>();

    iosp = new H4iosp();
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(20000);
      t.printStackTrace(new PrintWriter(sw));
      dumpTA.setText(sw.toString());
    }

    header = (H4header) iosp.sendIospMessage("header");
    for (H4header.Tag tag : header.getTags ()) {
      beanList.add(new TagBean(tag));
    }

    tagTable.setBeans(beanList);
  }

  public void getEosInfo(Formatter f) throws IOException {
    header.getEosInfo(f);
  }

  public class TagBean {
    H4header.Tag tag;

    // no-arg constructor
    public TagBean() {
    }

    // create from a dataset
    public TagBean(H4header.Tag tag) {
      this.tag = tag;
    }

    public short getCode(){
      return tag.getCode();
    }

    public String getType() {
      return tag.getType();
    }

    public short getRefno() {
      return tag.getRefno();
    }

    public boolean isExtended() {
      return tag.isExtended();
    }

    public String getVClass() {
      return tag.getVClass();
    }

    public int getOffset() {
      return tag.getOffset();
    }

    public int getLength() {
      return tag.getLength();
    }

    public boolean isUsed() {
      return tag.isUsed();
    }

    public String getVinfo() {
      return tag.getVinfo();
    }

  }

}
