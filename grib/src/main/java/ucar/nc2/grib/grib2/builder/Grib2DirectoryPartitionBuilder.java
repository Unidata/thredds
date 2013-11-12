package ucar.nc2.grib.grib2.builder;

import thredds.inventory.partition.PartitionManager;

import java.io.File;
import java.nio.file.Path;

/**
 * Grib2 DirectoryPartition Builder
 *
 * @author caron
 * @since 11/10/13
 */
public class Grib2DirectoryPartitionBuilder extends Grib2TimePartitionBuilder {

  public Grib2DirectoryPartitionBuilder(String name, Path directory, PartitionManager tpc, org.slf4j.Logger logger) {
    super(name, directory.toFile(), tpc, logger);
  }

 /*

  public boolean createIndex(Iterator<DirectoryPartition> partIter) {

    while (partIter.hasNext()) {
      DirectoryPartition dirPart = partIter.next();
    }
    return true;
  }

  private void scanForChildren(DirectoryStream.Filter<Path> filter) {
    if (debug) System.out.printf("%n Grib2DirectoryPartitionBuilder %s%n", dir);

    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, filter)) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (attr.isDirectory()) {
        }
        if (debug && (count++ % 100 == 0)) System.out.printf("%d ", count);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class MyFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }  */
}
