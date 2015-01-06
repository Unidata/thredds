/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.ui;

import thredds.inventory.MFile;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
      fileWindow = new IndependentWindow("Files Used", BAMutil.getImage("netcdfUI"), fileTable);
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
