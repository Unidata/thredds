package ucar.nc2.ui;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.ui.dialog.Grib1TableDialog;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/6/12
 */
public class GribVariableRename extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribVariableRename.class);
  static private boolean debug = true;

  private PreferencesExt prefs;

  private BeanTableSorted varTable, mapTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private Grib1TableDialog dialog;

  public GribVariableRename(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(VariableBean.class, (PreferencesExt) prefs.node("VariableBean"), false);
    /* varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean csb = (VariableBean) varTable.getSelectedBean();
        showVariable(csb);
      }
    }); */

    mapTable = new BeanTableSorted(MapBean.class, (PreferencesExt) prefs.node("MapBean"), false);
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

    /* AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Table Used", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
          dialog = new Grib1TableDialog((Frame) null);
          dialog.pack();
        }
        dialog.setVisible(true);
      }
    });
    buttPanel.add(infoButton); */

    readVariableRenameFile("resources/grib2/gribVarMap.xml");
    makeMapBeans();
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

  /*
  <dataset name="DGEX_Alaska_12km_20100524_0000.grib2">
    <param oldName="Geopotential_height" newName="Geopotential_height_pressure" varId="VAR_0-3-5_L100" />
    <param oldName="Geopotential_height_surface" newName="Geopotential_height_surface" varId="VAR_0-3-5_L1" />
    <param oldName="MSLP_Eta_Reduction" newName="MSLP_Eta_model_reduction_msl" varId="VAR_0-3-192_L101" />
    <param oldName="Maximum_temperature" newName="Maximum_temperature_height_above_ground" varId="VAR_0-0-4_L103" />
    <param oldName="Minimum_temperature" newName="Minimum_temperature_height_above_ground" varId="VAR_0-0-5_L103" />
  </dataset>

   */
  public boolean readVariableRenameFile(String path) {
    java.util.List<VariableBean> beans = new ArrayList<VariableBean>(1000);
    if (debug) System.out.printf("reading table %s%n", path);
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.warn("Cant read file "+path);
        return false;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();
      List<Element> dsElems = root.getChildren("dataset");
      for (Element dsElem : dsElems) {
        String dsName = dsElem.getAttributeValue("name");
        List<Element> params = dsElem.getChildren("param");
        for (Element elem : params) {
          String oldName = elem.getAttributeValue("oldName");
          String newName = elem.getAttributeValue("newName");
          String varId = elem.getAttributeValue("varId");
          beans.add( new VariableBean(dsName, oldName, newName, varId));
        }
      }
      varTable.setBeans(beans);
      return true;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;

    } catch (JDOMException e) {
      e.printStackTrace();
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  private void showVariable(MapBean bean) {
    infoTA.setText(bean.show());
    infoTA.gotoTop();
    infoWindow.setVisible(true);
  }

  public class VariableBean implements Comparable<VariableBean> {
    String dsName, oldName, newName, varId;

    // no-arg constructor
    public VariableBean() {
    }

    public VariableBean(String dsName, String oldName, String newName, String varId) {
      this.dsName = dsName;
      this.oldName = oldName;
      this.newName = newName;
      this.varId = varId;
    }

    public String getDataset() {
      return dsName;
    }

    public String getVarId() {
      return varId;
    }

    public String getOldName() {
      return oldName;
    }
    
    public String getNewName() {
      return newName;
    }

    public String getStatus() {
      if (oldName.equals(newName)) return "*";
      if (oldName.equalsIgnoreCase(newName)) return "**";
      return "";
    }

    @Override
    public int compareTo(VariableBean o) {
      return newName.compareTo(o.getNewName());
    }
  }
  
  //////////////////////////////////////////////////
  
  private void makeMapBeans() {
    HashMap<String, MapBean> map = new HashMap<String, MapBean>(3000);
    List<VariableBean> vbeans = varTable.getBeans();
    for (VariableBean vbean : vbeans) {
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
    List<VariableBean> newVars = new ArrayList<VariableBean>();
    HashMap<String, VariableBean> newVarsMap = new HashMap<String, VariableBean>();

    // no-arg constructor
    public MapBean() {
    }
    
    void add(VariableBean vbean) {
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
      
      List<VariableBean> vbeans = new ArrayList<VariableBean>(  newVarsMap.values());
      Collections.sort(vbeans);
      for (VariableBean vb : vbeans) {
        f.format("  %-70s %-40s %s%n", vb.getNewName(), vb.getVarId(), vb.getDataset());
      }
      f.format("%n");
      for (VariableBean vb : newVars) {
        f.format("  %-70s %-40s %s%n", vb.getNewName(), vb.getVarId(), vb.getDataset());
      }

      return f.toString();
    }

  }

}



