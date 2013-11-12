package thredds.inventory.partition;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Knows how to read and write ncx Index files, to decouple eg from GRIB
 *
 * @author caron
 * @since 11/10/13
 */
public interface IndexReader {

  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException;

  public interface AddChildCallback {
    public void addChild(String topDir, String filename, long lastModified) throws IOException;
  }

}
