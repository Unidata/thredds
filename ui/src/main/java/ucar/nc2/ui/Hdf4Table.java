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
import ucar.nc2.iosp.hdf4.H4iosp;
import ucar.nc2.iosp.hdf4.H4header;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * ToolsUI/Iosp/Hdf4
 *
 * @author caron
 */
public class Hdf4Table extends JPanel {
  private PreferencesExt prefs;

  private ucar.util.prefs.ui.BeanTableSorted tagTable; //, messTable, attTable;
  private JSplitPane split;

  private TextHistoryPane dumpTA, infoTA;
  private IndependentWindow infoWindow;

  public Hdf4Table(PreferencesExt prefs) {
    this.prefs = prefs;

    tagTable = new BeanTableSorted(TagBean.class, (PreferencesExt) prefs.node("Hdf4Object"), false);
    tagTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TagBean bean = (TagBean) tagTable.getSelectedBean();
        dumpTA.setText("Tag=\n ");
        dumpTA.appendLine(bean.tag.detail());
        dumpTA.appendLine("\nVinfo=");
        dumpTA.appendLine(bean.tag.getVinfo());
      }
    });

    /* messTable = new BeanTableSorted(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false);
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

    attTable = new BeanTableSorted(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false);
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

  private H4header header;
  private String location;

  public void setHdf4File(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    long start = System.nanoTime();
    java.util.List<TagBean> beanList = new ArrayList<TagBean>();

    H4iosp iosp = new H4iosp();
    NetcdfFile ncfile = new MyNetcdfFile(iosp);
    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(20000);
      PrintStream s = new PrintStream(bos);
      t.printStackTrace(s);
      dumpTA.setText( bos.toString());      
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

  // need  acccess to protected constructor: iosp.open(raf, ncfile, null);
  private class MyNetcdfFile extends NetcdfFile {
    public MyNetcdfFile(H4iosp iosp) {
       this.spi = iosp; // iosp must be set during open
    }
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
