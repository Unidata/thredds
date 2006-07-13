package ucar.nc2;

/**
 * A factory for opening a NetcdfFile, used by NetcdfFileCache
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public interface NetcdfFileFactory {

  /**
   * Open a NetcdfFile.
   * @param location location of the NetcdfFile
   * @param cancelTask allows user to cancel, may be null.
   * @return a valid NetcdfFile
   * @throws java.io.IOException
   */
  public NetcdfFile open(String location, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException;
}
