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

package ucar.nc2.thredds.monitor;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.filesystem.CacheFile;
import thredds.filesystem.CacheFileProto;
import thredds.filesystem.CacheDirectory;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Date;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;


import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class CacheTable extends JPanel {
  private PreferencesExt prefs;
  private ucar.util.prefs.ui.BeanTableSorted cacheTable, elemTable, cfileTable;

  private CacheManager ehcache;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private JSplitPane split;

  private JTextArea interval, startDate, endDate;

  public CacheTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    cacheTable = new BeanTableSorted(CacheBean.class, (PreferencesExt) prefs.node("Cache"), false);
    cacheTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CacheBean bean = (CacheBean) cacheTable.getSelectedBean();
        if (bean == null) return;
        ArrayList<ElemBean> beans = new ArrayList<ElemBean>();
        for (Object key : bean.cache.getKeys())
          beans.add(new ElemBean((String) key, bean.cache.get(key)));
        elemTable.setBeans(beans);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(cacheTable.getJTable(), "Options");
    varPopup.addAction("Show Stats", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CacheBean bean = (CacheBean) cacheTable.getSelectedBean();
        if (bean == null) return;

        infoTA.clear();
        Formatter f = new Formatter();
        try {
          f.format("%s", bean.cache.getStatistics());
        } catch (Exception e1) {
          JOptionPane.showMessageDialog(CacheTable.this, e1.getMessage());
        }
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    elemTable = new BeanTableSorted(ElemBean.class, (PreferencesExt) prefs.node("Elem"), false);
    elemTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ElemBean bean = (ElemBean) elemTable.getSelectedBean();
        if (bean == null) return;
        CacheDirectory cd = (CacheDirectory) bean.value; // LOOK

        ArrayList<CacheFileBean> beans = new ArrayList<CacheFileBean>();
        for (CacheFile cfile : cd.getChildren())
          beans.add(new CacheFileBean(cfile));
        cfileTable.setBeans(beans);
      }
    });

    /* varPopup = new thredds.ui.PopupMenu(elemTable.getJTable(), "Options");
    varPopup.addAction("Show Info", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ElemBean bean = (ElemBean) elemTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.value.toString());
        infoWindow.showIfNotIconified();
      }
    }); */

    cfileTable = new BeanTableSorted(CacheFileBean.class, (PreferencesExt) prefs.node("CacheFileBean"), false);
    cfileTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CacheFileBean bean = (CacheFileBean) cfileTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.cfile.toString());
        infoWindow.showIfNotIconified();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 100)));

    AbstractButton infoButton = BAMutil.makeButtcon("Info", "Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (ehcache == null) return;
        Formatter f = new Formatter();
        f.format(" Proto count = %d size = %d %n", CacheFileProto.countRead, CacheFileProto.countReadSize);
        int avg = CacheFileProto.countRead == 0 ? 0 : CacheFileProto.countReadSize / CacheFileProto.countRead;
        f.format("       avg = %d %n", avg);
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    buttPanel.add(infoButton);

    setLayout(new BorderLayout());
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cacheTable, elemTable);
    JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, split, cfileTable);
    add(split2, BorderLayout.CENTER);
  }

  public void exit() {
    cacheTable.saveState(false);
    elemTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void setCache(CacheManager ehcache) {
    this.ehcache = ehcache;

    String[] cacheNames = ehcache.getCacheNames();
    ArrayList<CacheBean> beans = new ArrayList<CacheBean>();
    for (String cacheName : cacheNames) {
      Cache cache = ehcache.getCache(cacheName);
      System.out.printf("Open Cache %s%n %s%n", cache, cache.getStatistics().toString());
      beans.add(new CacheBean(cache));
    }
    cacheTable.setBeans(beans);
  }

  ////////////////////////////////////////////////

  public class CacheBean {
    Cache cache;

    public CacheBean() {
    }

    CacheBean(Cache cache) {
      this.cache = cache;
    }

    public String getName() {
      return cache.getName();
    }

    public String getStatus() {
      return cache.getStatus().toString();
    }

    public int getDiskStoreSize() {
      return cache.getDiskStoreSize();
    }

    public int getSize() {
      return cache.getSize();
    }

    public long getMemoryStoreSize() {
      return cache.getMemoryStoreSize();
    }


  }

  public class ElemBean {

    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    String key;
    Object value;

    public ElemBean() {
    }

    ElemBean(String key, Element elem) {
      this.key = key;
      this.value = elem.getObjectValue();
    }

  }

  public class CacheFileBean {
    CacheFile cfile;

    public String getShortName() {
      return cfile.getShortName();
    }

    public Date getLastModified() {
      return new Date(cfile.getLastModified());
    }

    public long getLength() {
      return cfile.getLength();
    }

    public boolean isDirectory() {
      return cfile.isDirectory();
    }

    public CacheFileBean() {
    }

    CacheFileBean(CacheFile cfile) {
      this.cfile = cfile;
    }

  }


}
