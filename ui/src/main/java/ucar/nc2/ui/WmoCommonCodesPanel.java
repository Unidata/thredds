package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * WMO Common Codes
 *
 * @author caron
 * @since Aug 25, 2010
 */
public class WmoCommonCodesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted codeTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  private FileManager fileChooser;

  public WmoCommonCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        setEntries(cct);
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
        CodeTableBean csb = (CodeTableBean) codeTable.getSelectedBean();
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
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to current table", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareToCurrent();
      }
    });
    buttPanel.add(compareButton);

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in WMO table", false);
    dupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lookForProblems();
      }
    });
    buttPanel.add(dupButton);

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
    buttPanel.add(modelsButton);   */

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
      List<TableBean> tables = new ArrayList<TableBean>();
      for (CommonCodeTable.Table t : CommonCodeTable.Table.values()) {
        tables.add(new TableBean(t));
      }
      codeTable.setBeans(tables);

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

  public void setEntries(CommonCodeTable codeTable) {
    List<EntryBean> beans = new ArrayList<EntryBean>(codeTable.entries.size());
    for (CommonCodeTable.TableEntry d : codeTable.entries) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  public class TableBean {
    CommonCodeTable.Table t;

    // no-arg constructor
    public TableBean() {
    }

    // create from a dataset
    public TableBean(CommonCodeTable.Table t) {
      this.t = t;
    }

    public String getName() {
      return t.getName();
    }

    public String getEnumName() {
      return t.name();
    }

    public int getType() {
      return t.getTableType();
    }

    public String getResource() {
      return t.getResourceName();
    }

  }

  public class EntryBean {
    CommonCodeTable.TableEntry te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(CommonCodeTable.TableEntry te) {
      this.te = te;
    }

    public String getValue() {
      return te.value;
    }

    public String getComment() {
      return te.comment;
    }

    public String getStatus() {
      return te.status;
    }

    public int getCode() {
      return te.code;
    }

    public int getCode2() {
      return te.code2;
    }

    public int getLine() {
      return te.line;
    }
  }
}