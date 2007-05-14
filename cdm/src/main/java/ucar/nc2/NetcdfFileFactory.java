package ucar.nc2;

/**
 * A factory for opening a NetcdfFile.
 * Used by NetcdfFileCache, NCML, etc. 
 *
 * @author caron
 */
public interface NetcdfFileFactory {

  /**
   * Open a NetcdfFile.
   *
   * @param location    location of the NetcdfFile
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return a valid NetcdfFile
   * @throws java.io.IOException on error
   */
  public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws java.io.IOException;
}
