package thredds.inventory;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.util.List;

/**
 * Collection of MFiles or Partitions
 *
 * @author caron
 * @since 11/11/13
 */
public interface MCollection extends AutoCloseable {

  /**
   * A leaf is a single file or mfile collection. (note that this may or may not be a partition)
   * A non-leaf is a directory with other directories under it
   * @return if leaf collection
   */
  public boolean isLeaf();

  /**
   * The name of the collection
   * @return name of the collection
   */
  public String getCollectionName();

  /**
   * Get common root directory of all MFiles in the collection - may be null
   *
   * @return root directory name, or null.
   */
  public String getRoot();

  /**
   * Use the date extractor to extract the date from the filename.
   * Only call if hasDateExtractor() == true.
   *
   * @param mfile extract from here
   * @return Date, or null if none
   */
  public CalendarDate extractDate(MFile mfile);

  /**
   * Does this CollectionManager have the ability to extract a date from the MFile ?
   * @return true if CollectionManager has a DateExtractor
   */
  public boolean hasDateExtractor();

  /**
   * The starting date of the collection.
   * Only call if hasDateExtractor() == true.
   * @return starting date of the collection
   */
  public CalendarDate getStartCollection();

  /**
   * Close and release any resources. Do not make further calls on this object.
   */
  public void close();

  /**
   * Choose Proto dataset as index from [0..n-1], based on configuration.
   * @param n size to choose from
   * @return index within range [0..n-1]
   */
  public int getProtoIndex(int n);

  /**
   * last time this collection was modified
   * @return msess since epoch
   */
  public long getLastModified();

  public String getIndexFilename();

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the current collection of MFile.
   * if hasDateExtractor() == true, these will be sorted by Date, otherwise by path.
   *
   * @return current collection of MFile as an Iterable.
   */
  public Iterable<MFile> getFilesSorted() throws IOException;

  /**
   * Sorted filename
   * @return Sorted filename
   * @throws IOException
   */
  public List<String> getFilenames() throws IOException;

  /**
   * The latest file in the collection.
   * Only call if hasDateExtractor() == true.
   * @return latest file in the collection
   */
  public MFile getLatestFile() throws IOException;

  /**
   * Get the current collection of MFile, no guaranteed order.
   * May be faster for large collections, use when order is not important.
   * <pre>
     try (CloseableIterator<MFile> iter = getFileIterator()) {
      while (iter.hasNext()) {
        MFile file = iter.next();
      }
    }
   * </pre>
   * @return current collection of MFile as an CloseableIterator.
   */
  public CloseableIterator<MFile> getFileIterator() throws IOException;


  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the collection manager.

  public Object getAuxInfo(String key);
  public void putAuxInfo(String key, Object value);

}
