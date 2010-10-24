package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.iosp.grib.tables.GribTemplate;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since Aug 27, 2010
 */
public class GribWmoTemplatesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted codeTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  private FileManager fileChooser;

  public GribWmoTemplatesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(CodeBean.class, (PreferencesExt) prefs.node("CodeBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        setEntries(csb.code);
      }
    });

    entryTable = new BeanTableSorted(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        EntryBean csb = (EntryBean) entryTable.getSelectedBean();
      }
    });

    /* thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show uses", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        if (usedDds != null) {
          List<Message> list = usedDds.get(csb.getId());
          if (list != null) {
            for (Message use : list)
              use.dumpHeaderShort(out);
          }
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    }); */

    /* AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to current table", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareToCurrent();
      }
    });
    buttPanel.add(compareButton);

    AbstractButton modelsButton = BAMutil.makeButtcon("Select", "Check current models", false);
    modelsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          checkCurrentModels();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    buttPanel.add(modelsButton); */

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    ///

    try {
      java.util.List<GribTemplate> codes = GribTemplate.getWmoStandard();
      java.util.List<CodeBean> dds = new ArrayList<CodeBean>(codes.size());
      for (GribTemplate code : codes) {
        dds.add(new CodeBean(code));
      }
      codeTable.setBeans(dds);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    if (fileChooser != null) fileChooser.save();
  }

  public void setEntries(GribTemplate template) {
    java.util.List<EntryBean> beans = new ArrayList<EntryBean>(template.flds.size());
    for (GribTemplate.Field d : template.flds) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

   public class CodeBean {
    GribTemplate code;

    // no-arg constructor
    public CodeBean() {
    }

    // create from a dataset
    public CodeBean(GribTemplate code) {
      this.code = code;
    }

    public String getName() {
      return code.name;
    }

    public String getDescription() {
      return code.desc;
    }

    public int getM1() {
      return code.m1;
    }

    public int getM2() {
      return code.m2;
    }

  }

  public class EntryBean {
    GribTemplate.Field te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(GribTemplate.Field te) {
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

  }
}
  