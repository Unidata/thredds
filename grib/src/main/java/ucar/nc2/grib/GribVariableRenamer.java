package ucar.nc2.grib;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Try to figure out what GRIB name in 4.2 maps to in 4.3 NCEP IDD datasets.
 * Not guaranteed to be correct.
 * See ToolsUI IOSP/GRIB2/GRIB-RENAME
 *
 * @author caron
 * @since 4/7/12
 */
public class GribVariableRenamer {
  static private boolean debug = false;
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribVariableRenamer.class);

  private static HashMap<String, Renamer> map1;
  private static HashMap<String, Renamer> map2;
  
  private void initMap1() {
    List<VariableRenamerBean> beans = readVariableRenameFile("resources/grib1/grib1VarMap.xml");
    map1 = makeMapBeans(beans);
  }

  private void initMap2() {
    List<VariableRenamerBean> beans = readVariableRenameFile("resources/grib2/grib2VarMap.xml");
    map2 = makeMapBeans(beans);
  }

  public List<String> getMappedNamesGrib2(String oldName) {
    List<String> result = new ArrayList<String>();
    Renamer mbean = map2.get(oldName);
    if (mbean == null) return null;
    for (VariableRenamerBean r : mbean.newVars) {
      result.add(r.newName);
    }
    return result;
  }

  /**
   * Look for possible matches of old (4.2) grib names in new (4.3) dataset.
   * 
   * @param gds check existence in this dataset. Must be from a GRIB1 or GRIB2 dataset.
   * @param oldName old name from 4.2 dataset
   * @return list of possible matches (as grid short name), each exists in the dataset
   */
  public List<String> matchNcepNames(GridDataset gds, String oldName) {
    List<String> result = new ArrayList<String>();
    
    // look for exact match
    if (contains(gds, oldName))  {
      result.add(oldName); 
      return result;
    }
    
    Attribute att = gds.findGlobalAttributeIgnoreCase(CDM.FILE_FORMAT);
    boolean isGrib1 = (att != null) && att.getStringValue().startsWith("GRIB-1");
    boolean isGrib2 = (att != null) && att.getStringValue().startsWith("GRIB-2");
    HashMap<String, Renamer> map;
    if (isGrib1) {
      if (map1 == null) initMap1();
      map = map1;

    } else if (isGrib2) {
      if (map2 == null) initMap2();
      map = map2;

    } else {
      return result; // empty list
    }
    
    // look in our renamer map
    Renamer mbean = map.get(oldName);
    if (mbean != null && mbean.newName != null && contains(gds, mbean.newName))  {
      result.add(mbean.newName); // if its unique, then we are done
      return result;
    }

    // not unique - match against NCEP dataset
    if (mbean != null) {
      String dataset = extractDatasetFromLocation(gds.getLocationURI());
      if (dataset != null) {
        for (VariableRenamerBean r : mbean.newVars) {
          if (r.getDatasetType().equals(dataset) && contains(gds, r.newName)) result.add(r.newName);
        }
        if (result.size() == 1) return result; // return if unique
      }
    }

    // not unique, no unique match against dataset - check existence in the dataset
    if (mbean != null) {
      for (VariableRenamerBean r : mbean.newVarsMap.values()) {
        if (contains(gds, r.newName)) result.add(r.newName);
      }
      if (result.size() > 0) return result;
    }
    
    // try to map oldName -> new prefix
    String oldMunged = munge(oldName);
    for (GridDatatype grid : gds.getGrids()) {
      String newMunged = munge(grid.getShortName());
      if (newMunged.startsWith(oldMunged))
        result.add(grid.getShortName());
    }
    if (result.size() > 0) return result;

    // return empty list
    return result;
  }
  
  private String munge(String old) {
    StringBuilder oldLower = new StringBuilder( old.toLowerCase());
    StringUtil2.remove(oldLower, "_-");
    return oldLower.toString();
  }
  
  private boolean contains(GridDataset gds, String name) {
    return gds.findGridByShortName(name) != null;
  }
  
  private String getNewName(HashMap<String, Renamer> map, String datasetLocation, String oldName) {
    Renamer mbean = map.get(oldName);
    if (mbean == null) return null; // ??
    if (mbean.newName != null) return mbean.newName; // if its unique, then we are done
    String dataset = extractDatasetFromLocation(datasetLocation);
    for (VariableRenamerBean r : mbean.newVars) {
      if (r.getDatasetType().equals(dataset)) return r.getNewName();
    }
    return null; // ??
  }

  public static String extractDatasetFromLocation(String location) {
    int pos = location.lastIndexOf("/");
    if (pos > 0) location = location.substring(pos+1);
    int posSuffix = location.lastIndexOf(".");
    if (posSuffix-14 > 0)
      return location.substring(0, posSuffix-14) + location.substring(posSuffix);
    return "";
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

  // debugging only
  public List<VariableRenamerBean> readVariableRenamerBeans(String which) {
    if (which.equals("GRIB1"))
      return readVariableRenameFile("resources/grib1/grib1VarMap.xml");
    else
      return readVariableRenameFile("resources/grib2/grib2VarMap.xml");
  }  
  
  private List<VariableRenamerBean> readVariableRenameFile(String path) {
    java.util.List<VariableRenamerBean> beans = new ArrayList<VariableRenamerBean>(1000);
    if (debug) System.out.printf("reading table %s%n", path);
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.warn("Cant read file " + path);
        return null;
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
          beans.add(new VariableRenamerBean(dsName, oldName, newName, varId));
        }
      }
      return beans;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;

    } catch (JDOMException e) {
      e.printStackTrace();
      return null;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  public static class VariableRenamerBean implements Comparable<VariableRenamerBean> {
    String dsName, dsType, oldName, newName, varId;

    // no-arg constructor
    public VariableRenamerBean() {
    }

    public VariableRenamerBean(String dsName, String oldName, String newName, String varId) {
      this.dsName = dsName;
      this.dsType = extractDatasetFromLocation(dsName);
      this.oldName = oldName;
      this.newName = newName;
      this.varId = varId;
    }

    public String getDataset() {
      return dsName;
    }

    public String getDatasetType() {
      return dsType;
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
    public int compareTo(VariableRenamerBean o) {
      return newName.compareTo(o.getNewName());
    }
  }

  //////////////////////////////////////////////////

  private HashMap<String, Renamer> makeMapBeans(List<VariableRenamerBean> vbeans) {
    HashMap<String, Renamer> map = new HashMap<String, Renamer>(3000);
    for (VariableRenamerBean vbean : vbeans) {
      Renamer mbean = map.get(vbean.getOldName());
      if (mbean == null) {
        mbean = new Renamer(vbean.getOldName());
        map.put(vbean.getOldName(), mbean);
      }
      mbean.add(vbean);
    }
    
    for (Renamer rmap : map.values()) {
      rmap.finish();
    }

    return map;
  }

  private class Renamer {
    String oldName, newName;
    List<VariableRenamerBean> newVars = new ArrayList<VariableRenamerBean>();
    HashMap<String, VariableRenamerBean> newVarsMap = new HashMap<String, VariableRenamerBean>();

    // no-arg constructor
    public Renamer() {
    }

    public Renamer(String oldName) {
      this.oldName = oldName;
    }

    void add(VariableRenamerBean vbean) {
      newVarsMap.put(vbean.getNewName(), vbean);
      newVars.add(vbean);
    }
    
    void finish() {
      if (newVarsMap.values().size() == 1) {
        newName = newVars.get(0).getNewName();
        // newVars = null; // GC
      }
    }

    public int getCount() {
      return newVars.size();
    }

    public String getOldName() {
      return oldName;
    }

  }


}
