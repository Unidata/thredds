package ucar.nc2.ui.grib;

import ucar.nc2.grib.GribVariableRenamer;
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
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/6/12
 */
public class GribRenamePanel extends JPanel {

  private PreferencesExt prefs;
  private BeanTable varTable, mapTable;
  private JSplitPane split, split2;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private JComboBox kind;
  private GribVariableRenamer renamer;

  public GribRenamePanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    varTable = new BeanTable(GribVariableRenamer.VariableRenamerBean.class, (PreferencesExt) prefs.node("VariableBean"), false);
    /* varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean csb = (VariableBean) varTable.getSelectedBean();
        showVariable(csb);
      }
    }); */

    mapTable = new BeanTable(MapBean.class, (PreferencesExt) prefs.node("MapBean"), false);
    mapTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MapBean bean = (MapBean) mapTable.getSelectedBean();
        showVariable(bean);
      }
    });
    /* ucar.nc2.ui.widget.PopupMenu varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Compare two tables", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
      }
    }); */

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, mapTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    kind = new JComboBox(new String[] {"GRIB-1", "GRIB-2"});
    buttPanel.add(kind);
    kind.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        renamer = new GribVariableRenamer();
        List<GribVariableRenamer.VariableRenamerBean> vbeans = renamer.readVariableRenamerBeans( (String) kind.getSelectedItem());
        varTable.setBeans(vbeans);
        makeMapBeans();
      }
    });

  }

  public void save() {
    varTable.saveState(false);
    mapTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    // if (fileChooser != null) fileChooser.save();
  }

  public boolean matchNcepName(String oldName) {
    if (renamer == null) return false;
    GribVariableRenamer.VariableRenamerBean bean = (GribVariableRenamer.VariableRenamerBean) varTable.getSelectedBean();
    List<String> result = renamer.matchNcepNames(bean.getDatasetType(), oldName);
    Formatter f = new Formatter();
    f.format("Match '%s'%n", oldName);
    for (String s : result) f.format("  %s%n", s);
    infoTA.setText(f.toString());
    infoTA.gotoTop();
    infoWindow.setVisible(true);
    return true;
  }


  private void showVariable(MapBean bean) {
    infoTA.setText(bean.show());
    infoTA.gotoTop();
    infoWindow.setVisible(true);
  }
  
  //////////////////////////////////////////////////
  
  private void makeMapBeans() {
    HashMap<String, MapBean> map = new HashMap<String, MapBean>(3000);
    List<GribVariableRenamer.VariableRenamerBean> vbeans = varTable.getBeans();
    for (GribVariableRenamer.VariableRenamerBean vbean : vbeans) {
      MapBean mbean = map.get(vbean.getOldName());
      if (mbean == null) {
        mbean = new MapBean(vbean.getOldName());
        map.put(vbean.getOldName(), mbean);
      }
      mbean.add(vbean);
    }

    mapTable.setBeans(new ArrayList(map.values()));
  }

  public class MapBean {
    String oldName;
    List<GribVariableRenamer.VariableRenamerBean> newVars = new ArrayList<GribVariableRenamer.VariableRenamerBean>();
    HashMap<String, GribVariableRenamer.VariableRenamerBean> newVarsMap = new HashMap<String, GribVariableRenamer.VariableRenamerBean>();

    // no-arg constructor
    public MapBean() {
    }
    
    void add(GribVariableRenamer.VariableRenamerBean vbean) {
      newVars.add(vbean);  
      newVarsMap.put(vbean.getNewName(), vbean);
    }

    public MapBean(String oldName) {
      this.oldName = oldName;
    }

    public int getCount() {
      return newVarsMap.size();
    }

    public String getOldName() {
      return oldName;
    }
    
    String show() {
      Formatter f = new Formatter();
      f.format("OldName %s%nNewNames%n", oldName);
      
      List<GribVariableRenamer.VariableRenamerBean> vbeans = new ArrayList<GribVariableRenamer.VariableRenamerBean>(  newVarsMap.values());
      Collections.sort(vbeans);
      for (GribVariableRenamer.VariableRenamerBean vb : vbeans) {
        f.format("  %-70s %-40s %s%n", vb.getNewName(), vb.getVarId(), vb.getDataset());
      }
      f.format("%n");
      for (GribVariableRenamer.VariableRenamerBean vb : newVars) {
        f.format("  %-70s %-40s %s (%s)%n", vb.getNewName(), vb.getVarId(), vb.getDataset(), vb.getDatasetType());
      }

      return f.toString();
    }

  }

}



