/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

import thredds.inventory.bdb.MetadataManager;
import ucar.unidata.util.StringUtil;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.IOException;

/**
 * Show the info stored in the MetadataManager
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class CollectionTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted collectionNameTable, dataTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public CollectionTable(PreferencesExt prefs) {
    this.prefs = prefs;

    collectionNameTable = new BeanTableSorted(CollectionBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    collectionNameTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CollectionBean bean = (CollectionBean) collectionNameTable.getSelectedBean();
        setCollection(bean.name);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(collectionNameTable.getJTable(), "Options");
    varPopup.addAction("Show Collection Stats", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean bean = (CollectionBean) collectionNameTable.getSelectedBean();
        if (bean == null) return;
        showCollectionInfo(bean.name);
      }
    });
    varPopup.addAction("delete", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean bean = (CollectionBean) collectionNameTable.getSelectedBean();
        if (bean == null) return;
        MetadataManager.deleteCollection(bean.name);
        refresh();
      }
    });

    dataTable = new BeanTableSorted(DataBean.class, (PreferencesExt) prefs.node("DataBean"), false);
    dataTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DataBean bean = (DataBean) dataTable.getSelectedBean();
        showData(bean);
      }
    });
    varPopup = new thredds.ui.PopupMenu(dataTable.getJTable(), "Options");
    varPopup.addAction("delete", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean cbean = (CollectionBean) collectionNameTable.getSelectedBean();
        DataBean bean = (DataBean) dataTable.getSelectedBean();
        if ((cbean == null) || (bean == null)) return;
        MetadataManager.delete(cbean.name, bean.getKey());
        setCollection(cbean.name);
      }
    });

    // the info window
    infoTA = new TextHistoryPane(false, 5000, 50, true, false, 14);
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, collectionNameTable, dataTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
    refresh();
  }

  private void showData(DataBean bean) {
    infoTA.setText(bean.getValue());
    infoWindow.showIfNotIconified();
  }

  // private MetadataManager mm;

  public void save() {
    collectionNameTable.saveState(false);
    dataTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void refresh() {
    java.util.List<CollectionBean> beanList = new ArrayList<CollectionBean>();
    for (String name : MetadataManager.getCollectionNames()) {
      beanList.add(new CollectionBean(name));
    }
    collectionNameTable.setBeans(beanList);
  }

  private void setCollection(String name) {
    java.util.List<DataBean> beans = new ArrayList<DataBean>();
    MetadataManager mm = null;
    try {
      mm = new MetadataManager(name);

      for (MetadataManager.KeyValue data : mm.getContent()) {
        beans.add(new DataBean(data));
      }
      dataTable.setBeans(beans);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (mm != null) mm.close();
    }
  }

  public void showInfo(Formatter result) throws IOException {
    MetadataManager.showEnvStats(result);
  }

  public void showCollectionInfo(String name) {
    MetadataManager mm = null;
    try {
      Formatter f = new Formatter();
      mm = new MetadataManager(name);
      mm.showStats(f);
      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    } catch (IOException e) {
      e.printStackTrace();
    }  finally {
      if (mm != null) mm.close();
    }
  }

  public class CollectionBean {
    String name;

    // no-arg constructor
    public CollectionBean() {
    }

    // create from a dataset
    public CollectionBean(String name) {
      this.name = name;
    }

    public String getName() throws IOException {
      return name;
    }

    public String getNameDecoded() throws IOException {
      return StringUtil.unescape(name);
    }

  }

  public class DataBean {
    MetadataManager.KeyValue data;

    // no-arg constructor
    public DataBean() {
    }

    // create from a dataset
    public DataBean(MetadataManager.KeyValue data) {
      this.data = data;
    }

    public String getKey() {
      return data.key;
    }
    public String getValue() {
      return data.value;
    }
    public int getSize() {
      return data.value.length();
    }
  }

}
