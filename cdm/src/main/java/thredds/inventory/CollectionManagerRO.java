package thredds.inventory;

import ucar.nc2.time.CalendarDate;

/**
 * CollectionManager Read-Only
 *
 * @author caron
 * @since 11/11/13
 */
public interface CollectionManagerRO {

  public boolean isPartition();

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
   * Get the current collection of MFile.
   * You must call scan() first - this does not call scan().
   * if hasDateExtractor() == true, these will be sorted by Date, otherwise by path.
   *
   * @return current collection of MFile as an Iterable. May be empty, not null.
   */
  public Iterable<MFile> getFiles();

  /**
   * Use the date extractor to extract the date from the filename.
   * Only call if hasDateExtractor() == true.
   *
   * @param mfile extract from here
   * @return Date, or null if none
   */
  public CalendarDate extractRunDate(MFile mfile);

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
   * The latest file in the collection.
   * Only call if hasDateExtractor() == true.
   * @return latest file in the collection
   */
  public MFile getLatestFile();

  /**
   * Close and release any resources. Do not make further calls on this object.
   */
  public void close();


  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the collection manager. kind of a kludge

  public Object getAuxInfo(String key);
  public void putAuxInfo(String key, Object value);

}
