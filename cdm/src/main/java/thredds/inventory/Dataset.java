package thredds.inventory;

import ucar.nc2.NetcdfFile;

import java.util.Date;
import java.util.List;
import java.io.File;

/**
 * Class Description
 *
 * @author caron
 * @since May 5, 2009
 */
public interface Dataset {

  public interface Inventory {
    public String getName();
    public Date getDate();
    public NetcdfFile getNetcdfFile();

    // optional - may be null
    public File getFile();
  }

  public Object getKey();

  public List<Inventory> getInventory();

  public NetcdfFile getNetcdfFile(String name);

}
