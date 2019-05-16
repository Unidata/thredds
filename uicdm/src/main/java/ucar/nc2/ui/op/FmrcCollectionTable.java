/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import thredds.inventory.bdb.MetadataManager;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Show the info stored in the MetadataManager
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class FmrcCollectionTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTable collectionNameTable, dataTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public FmrcCollectionTable(PreferencesExt prefs) {
    this.prefs = prefs;

    collectionNameTable = new BeanTable(CollectionBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    collectionNameTable.addListSelectionListener(e -> {
        CollectionBean bean = (CollectionBean) collectionNameTable.getSelectedBean();
        setCollection(bean.name);
    });

    PopupMenu varPopup = new PopupMenu(collectionNameTable.getJTable(), "Options");
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
        try {
          MetadataManager.deleteCollection(bean.name);
        } catch (Exception e2) {
          e2.printStackTrace();
        }
        refresh();
      }
    });

    dataTable = new BeanTable(DataBean.class, (PreferencesExt) prefs.node("DataBean"), false);
    dataTable.addListSelectionListener(e -> {
        DataBean bean = (DataBean) dataTable.getSelectedBean();
        showData(bean);
    });
    varPopup = new PopupMenu(dataTable.getJTable(), "Options");
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
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, collectionNameTable, dataTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
    refresh();
  }

  private void showData(DataBean bean) {
    infoTA.setText(bean.getValue());
    infoWindow.show();
  }

  // private MetadataManager mm;

  public void save() {
    collectionNameTable.saveState(false);
    dataTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void refresh() {
    List<CollectionBean> beanList = new ArrayList<>();
    for (String name : MetadataManager.getCollectionNames()) {
      beanList.add(new CollectionBean(name));
    }
    collectionNameTable.setBeans(beanList);
  }

  private void setCollection(String name) {
    List<DataBean> beans = new ArrayList<>();
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
      return StringUtil2.unescape(name);
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
