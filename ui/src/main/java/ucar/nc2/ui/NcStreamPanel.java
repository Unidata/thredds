/*
 * Copyright 2009-2012 University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.stream.NcStreamIosp;
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
import java.io.IOException;
import java.util.*;

/**
 * Show internal structure of ncstream files.
 *
 * @author caron
 * @since 7/8/12
 */
public class NcStreamPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable messTable;
  private JSplitPane split;

  private TextHistoryPane infoTA, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow2, infoWindow3;

  private RandomAccessFile raf;
  private NetcdfFile ncd;
  private NcStreamIosp iosp;

  public NcStreamPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    messTable = new BeanTable(MessBean.class, (PreferencesExt) prefs.node("NcStreamPanel"), false);
    messTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessBean bean = (MessBean) messTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.getDesc());
      }
    });
    varPopup = new PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show deflate", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessBean bean = (MessBean) messTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.m.showDeflate());
      }
    });

    // the info windows
    infoTA = new TextHistoryPane();

    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    infoPopup3 = new TextHistoryPane();
    infoWindow3 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup3);
    infoWindow3.setBounds((Rectangle) prefs.getBean("InfoWindowBounds3", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messTable, infoTA);
    split.setDividerLocation(prefs.getInt("splitPos", 800));

    add(split, BorderLayout.CENTER);
  }

  public void save() {
    messTable.saveState(false);
    //prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (ncd != null) ncd.close();
    ncd = null;
    raf = null;
    iosp = null;
  }

  public void showInfo(Formatter f) {
    if (ncd == null) return;
    try {
      f.format("%s%n", raf.getLocation());
      f.format(" file length = %d%n", raf.length());
      f.format(" version = %d%n", iosp.getVersion());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    f.format("%n%s", ncd.toString()); // CDL
  }

  public void setNcStreamFile(String filename) throws IOException {
    closeOpenFiles();

    java.util.List<MessBean> messages = new ArrayList<MessBean>();
    ncd = new NetcdfFileSubclass();
    iosp = new NcStreamIosp();
    try {
      raf = new RandomAccessFile(filename, "r");
      java.util.List<NcStreamIosp.NcsMess> ncm = new ArrayList<NcStreamIosp.NcsMess>();
      iosp.openDebug(raf, ncd, ncm);
      for (NcStreamIosp.NcsMess m : ncm) {
        messages.add(new MessBean(m));
      }

    } finally  {
      if (raf != null) raf.close();
    }

    messTable.setBeans(messages);
    //System.out.printf("mess = %d%n", messages.size());
  }

  public class MessBean {
    private NcStreamIosp.NcsMess m;

    MessBean() {
    }

    MessBean(NcStreamIosp.NcsMess m) {
      this.m = m;
    }

    public String getObjClass() {
      return m.what.getClass().toString();
    }

    public String getDesc() {
      return m.what.toString();
    }

    public int getSize() {
      return m.len;
    }

    public int getNelems() {
      return m.nelems;
    }

    public long getFilePos() {
      return m.filePos;
    }

  }

}
