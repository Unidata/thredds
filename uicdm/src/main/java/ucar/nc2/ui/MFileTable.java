/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import thredds.inventory.MFile;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/18/13
 */
public class MFileTable extends JPanel {
  private PreferencesExt prefs;
  private final boolean isPopup;

  private BeanTable fileTable;
  private IndependentWindow fileWindow;

  private List<String> gribCollectionFiles = new ArrayList<>();


  public MFileTable(PreferencesExt prefs, boolean isPopup) {
    this.prefs = prefs;
    this.isPopup = isPopup;

    PopupMenu varPopup;

    fileTable = new BeanTable(FileBean.class, (PreferencesExt) prefs.node("FileBean"), false, "Files", "Files", null);
    varPopup = new PopupMenu(fileTable.getJTable(), "Options");
    varPopup.addAction("Open this file in Grib2Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean bean = (FileBean) fileTable.getSelectedBean();
        if (bean == null) return;
        if (MFileTable.this.isPopup) fileWindow.setVisible(false);
        Formatter f = new Formatter();
        f.format("list:");
        f.format(bean.getPath());
        MFileTable.this.firePropertyChange("openGrib2Collection", null, f.toString());
      }
    });
    varPopup.addAction("Add Files to Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<FileBean> beans = (List<FileBean>) fileTable.getSelectedBeans();
        if (beans == null || beans.size() == 0) return;
        for (FileBean bean : beans)
          gribCollectionFiles.add(bean.getPath());
      }
    });
    varPopup.addAction("Clear Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        gribCollectionFiles = new ArrayList<>();
      }
    });
    varPopup.addAction("Open Collection in Grib2Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (gribCollectionFiles.size() == 0) return;
        if (MFileTable.this.isPopup) fileWindow.setVisible(false);
        Formatter f = new Formatter();
        f.format("list:");
        for (String s : gribCollectionFiles) {
          f.format("%s;", s);
        }
        MFileTable.this.firePropertyChange("openGrib2Collection", null, f.toString());
      }
    });

    if (isPopup) {
      fileWindow = new IndependentWindow("Files Used", BAMutil.getImage("nj22/NetcdfUI"), fileTable);
      fileWindow.setBounds((Rectangle) prefs.getBean("FileWindowBounds", new Rectangle(300, 300, 500, 300)));
    } else {
      add(fileTable, BorderLayout.CENTER);
    }
  }

  public void save() {
    fileTable.saveState(false);
    if (fileWindow != null) prefs.putBeanObject("FileWindowBounds", fileWindow.getBounds());
  }

  public void setFiles(File dir, Collection<MFile> files) {
    if (files == null) return;

    int count = 0;
    List<FileBean> beans = new ArrayList<>();
    for (MFile mfile : files)
      beans.add(new FileBean(dir, mfile, count++));

    fileTable.setBeans(beans);
    if (isPopup) fileWindow.show();
  }


  public class FileBean {
    MFile mfile;
    int count;

    public FileBean() {
    }

    public FileBean(File dir, MFile mfile, int count) {
      this.mfile = mfile;
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    public String getName() {
      return mfile.getName();
    }

    public String getPath() {
      return mfile.getPath();
    }

    public long getSize() {
      return mfile.getLength();
    }

    public String getLastModified() {
      return CalendarDateFormatter.toDateTimeString(new Date(mfile.getLastModified()));
    }
  }
}
