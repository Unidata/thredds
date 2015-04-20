package ucar.nc2.ui.grib;

import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

    codeTable = new BeanTable(CodeBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        setEntries(csb.template);
      }
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    /* entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        entryTable.getSelectedBean();
      }
    }); */

    ucar.nc2.ui.widget.PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        csb.showTable(out);
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    setTable(WmoTemplateTable.standard);
  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void setTable(WmoTemplateTable.Version v) {
    try {
      WmoTemplateTable.GribTemplates wmo = WmoTemplateTable.readXml(v);
      List<WmoTemplateTable> codes = wmo.list;
      List<CodeBean> dds = new ArrayList<>(codes.size());
      for (WmoTemplateTable code : codes) {
        dds.add(new CodeBean(code));
      }
      codeTable.setBeans(dds);
      // currTable = v;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void setEntries(WmoTemplateTable template) {
    java.util.List<EntryBean> beans = new ArrayList<>(template.flds.size());
    for (WmoTemplateTable.Field d : template.flds) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  public class CodeBean {
    WmoTemplateTable template;

    // no-arg constructor
    public CodeBean() {
    }

    // create from a dataset
    public CodeBean(WmoTemplateTable template) {
      this.template = template;
    }

    public String getName() {
      return template.name;
    }

    public String getDescription() {
      return template.desc;
    }

    public int getM1() {
      return template.m1;
    }

    public int getM2() {
      return template.m2;
    }

    void showTable(Formatter f) {
      f.format("Template %s (%s)%n", template.name, template.desc);
      for (WmoTemplateTable.Field entry : template.flds) {
        f.format("  %6s (%d): %s", entry.octet, entry.nbytes, entry.content);
        if (entry.note != null)
          f.format(" - %s", entry.note);
        f.format("%n");
      }
    }

  }

  public class EntryBean {
    WmoTemplateTable.Field te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(WmoTemplateTable.Field te) {
      this.te = te;
    }

    public String getOctet() {
      return te.octet;
    }

    public String getContent() {
      return te.content;
    }

    public int getNbytes() {
      return te.nbytes;
    }

    public int getStart() {
      return te.start;
    }

    public String getStatus() {
      return te.status;
    }

    public String getNotes() {
      return te.note;
    }

  }
}
