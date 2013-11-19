package ucar.nc2.grib;

import thredds.inventory.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * superclass for GribCollectionBuilder GRIB1 and GRIB2
 *
 * @author caron
 * @since 5/6/13
 */
public class GribCollectionBuilder {

  // private final List<CollectionManager> collections = new ArrayList<CollectionManager>(); // are there every more than one ?
  protected final CollectionManager dcm; // may be null, when read in from index
  protected final boolean isSingleFile;
  protected final org.slf4j.Logger logger;

  protected GribCollectionBuilder(CollectionManager dcm, boolean isSingleFile, org.slf4j.Logger logger) {
    // assert dcm != null;
    this.dcm = dcm;
    this.isSingleFile = isSingleFile;
    this.logger = logger;
  }

  public static List<GribCollectionBuilder.GcMFile> makeFiles(File directory, List<thredds.inventory.MFile> files) {
    List<GribCollectionBuilder.GcMFile> result = new ArrayList<GcMFile>(files.size());
    String dirPath = StringUtil2.replace(directory.getPath(), '\\', "/");

    for (MFile file : files) {
      String reletiveName;
      if (file.getPath().startsWith(dirPath))
        reletiveName = file.getPath().substring(dirPath.length());
      else
        reletiveName = file.getPath();
      result.add( new GcMFile(directory, reletiveName, file.getLastModified()));
    }
    return result;
  }

  /* needed ?
  // list of files is only changed by setFiles()
  public static class GcCollectionManager extends CollectionManagerAbstract {
    private List<MFile> files;
    private String dirName;

    GcCollectionManager(String collectionNameName) {
      super(collectionNameName);
      setStatic(true);
    }

    public void setFiles(String dirName, List<MFile> files) {
      this.dirName = dirName;
      this.files = files;
    }

    @Override
    public String getRoot() {
      return dirName;
    }

    @Override
    public long getLastScanned() {
      return 0;
    }

    @Override
    public boolean isScanNeeded() {
      return false;
    }

    @Override
    public boolean scanIfNeeded() throws IOException {
      return false;
    }

    @Override
    public boolean scan(boolean sendEvent) throws IOException {
      return false;
    }

    @Override
    public Iterable<MFile> getFiles() {
      return files;
    }

    @Override
    public CalendarDate extractRunDate(MFile mfile) {
      return null;
    }

    @Override
    public boolean hasDateExtractor() {
      return false;
    }

    @Override
    public CalendarDate getStartCollection() {
      return null;
    }
  }  */


  public static class GcMFile implements thredds.inventory.MFile {
    public final File directory;
    public final String name;
    public final long lastModified;

    public GcMFile(File directory, String name, long lastModified) {
      this.directory = directory;
      this.name = name;
      this.lastModified = lastModified;
    }

    public GcMFile(File directory, GribCollectionProto.MFile gcmfile) {
      this.directory = directory;
      this.name = gcmfile.getFilename();
      this.lastModified = gcmfile.getLastModified();
    }

    public GribCollectionProto.MFile makeProto() {
      GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
      b.setFilename(name);
      b.setLastModified(lastModified);
      return b.build();
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    public long getLength() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public String getPath() {
      String path =  new File(directory, name).getPath();
      return StringUtil2.replace(path, '\\', "/");
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int compareTo(thredds.inventory.MFile o) {
      return name.compareTo(o.getName());
    }

    @Override
    public Object getAuxInfo() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuxInfo(Object info) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("GcMFile");
      sb.append("{directory=").append(directory);
      sb.append(", name='").append(name).append('\'');
      sb.append(", lastModified=").append( new Date(lastModified));
      sb.append('}');
      return sb.toString();
    }
  }
}
