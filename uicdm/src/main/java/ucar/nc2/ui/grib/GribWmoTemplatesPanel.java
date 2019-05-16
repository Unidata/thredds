/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grib;

import ucar.nc2.grib.grib2.table.WmoTemplateTables;
import ucar.nc2.grib.grib2.table.WmoTemplateTables.TemplateTable;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * GRIB WMO template UI.
 *
 * @author caron
 * @since Aug 27, 2010
 */
public class GribWmoTemplatesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable codeTable, entryTable;
  private JSplitPane split;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  public GribWmoTemplatesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTable(TemplateBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(e -> {
      TemplateBean csb = (TemplateBean) codeTable.getSelectedBean();
      setEntries(csb.template);
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);

    ucar.ui.widget.PopupMenu varPopup = new ucar.ui.widget.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        TemplateBean csb = (TemplateBean) codeTable.getSelectedBean();
        csb.showTable(out);
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    setTable(WmoTemplateTables.standard);
  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void setTable(WmoTemplateTables.Version v) {
    try {
      WmoTemplateTables tables = WmoTemplateTables.getInstance();
      List<TemplateBean> dds = new ArrayList<>();
      for (TemplateTable templateTable : tables.getTemplateTables()) {
        dds.add(new TemplateBean(templateTable));
      }
      codeTable.setBeans(dds);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setEntries(TemplateTable template) {
    java.util.List<EntryBean> beans = new ArrayList<>();
    for (WmoTemplateTables.Field d : template.getFlds()) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  public class TemplateBean {
    TemplateTable template;

    // no-arg constructor
    public TemplateBean() {
    }

    public TemplateBean(TemplateTable template) {
      this.template = template;
    }

    public String getName() {
      return template.getName();
    }

    public String getDescription() {
      return template.getDesc();
    }

    public int getM1() {
      return template.getM1();
    }

    public int getM2() {
      return template.getM2();
    }

    void showTable(Formatter f) {
      f.format("Template %s (%s)%n", template.getName(), template.getDesc());
      for (WmoTemplateTables.Field entry : template.getFlds()) {
        f.format("  %6s (%s): %s", entry.getOctet(), entry.getNote(), entry.getContent());
        if (entry.getNote() != null)
          f.format(" - %s", entry.getNote());
        f.format("%n");
      }
    }

  }

  public class EntryBean {
    WmoTemplateTables.Field te;

    // no-arg constructor
    public EntryBean() {
    }

    public EntryBean(WmoTemplateTables.Field te) {
      this.te = te;
    }

    public String getOctet() {
      return te.getOctet();
    }

    public String getContent() {
      return te.getContent();
    }

    public int getNbytes() {
      return te.getNbytes();
    }

    public int getStart() {
      return te.getStart();
    }

    public String getStatus() {
      return te.getStatus();
    }

    public String getNotes() {
      return te.getNote();
    }

  }
}
