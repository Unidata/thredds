package thredds.inventory.partition;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Knows how to read ncx Index files, to decouple eg from GRIB
 *
 * @author caron
 * @since 11/10/13
 */
public interface IndexReader {

  /**
   * Open an ncx file and read children indexes
   * @param indexFile the ncx index file to open
   * @param callback for each child index, call this back
   * @return true if indexFile is a partition collection
   * @throws IOException on bad things
   */
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException;

  public interface AddChildCallback {

    /**
     * Callback for readChildren
     * @param topDir          the directory of the child collection
     * @param filename        the index filename of the child collection
     * @param lastModified    last modified for child collection
     * @throws IOException on bad
     */
    public void addChild(String topDir, String filename, long lastModified) throws IOException;
  }

  /**
   * Open an ncx file and find out what type it is
   * @param indexFile the ncx index file to open
   * @return true if its a partition type index
   * @throws IOException on bad
   */
  public boolean isPartition(Path indexFile) throws IOException;

}
