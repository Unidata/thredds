/* Copyright */
package thredds.server.catalog.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.server.catalog.DataRoot;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Externalized DataRoot.
 *
 * @author caron
 * @since 6/16/2015
 */
public class DataRootExt implements Externalizable {
  static private final Logger logger = LoggerFactory.getLogger(DataRootExt.class);

  static public int total_count = 0;
  static public long total_nbytes = 0;

  private String path;
  private String dirLocation;
  private DataRoot.Type type;
  private String catLocation;

  private DataRoot dataRoot;

  public DataRootExt() {
  }

  public DataRootExt(DataRoot dataRoot, String catLocation) {
    this.path = dataRoot.getPath();
    this.dataRoot = dataRoot;
    this.dirLocation = dataRoot.getDirLocation();
    this.type = dataRoot.getType();
    this.catLocation = catLocation;
  }

  public String getPath() {
    return path;
  }

  public DataRoot getDataRoot() {
    if (dataRoot != null) return dataRoot;
    if (type == DataRoot.Type.datasetRoot) {
      dataRoot = new DataRoot(path, dirLocation);
    }
    return dataRoot;
  }

  public void setDataRoot(DataRoot dataRoot) {
    this.dataRoot = dataRoot;
  }

  public String getCatLocation() {
    return catLocation;
  }

  public String getDirLocation() {
    return dirLocation;
  }

  public DataRoot.Type getType() {
    return type;
  }

  /*
      message DataRoot {
        required string urlPath = 1;
        required string dirLocation = 2;  // for simple dataset root
        required string catLocation = 3;
      }
    */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogExtProto.DataRoot.Builder builder = ConfigCatalogExtProto.DataRoot.newBuilder();
    builder.setUrlPath(path);
    builder.setDirLocation(dirLocation);
    builder.setType(convertDataRootType(dataRoot.getType()));
    if (dataRoot.getType() != DataRoot.Type.datasetRoot)
      builder.setCatLocation(catLocation);

    ConfigCatalogExtProto.DataRoot index = builder.build();
    byte[] b = index.toByteArray();
    out.writeInt(b.length);
    out.write(b);

    total_count++;
    total_nbytes += b.length + 4;
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int avail = in.available();
    int len = in.readInt();
    byte[] b = new byte[len];
    int n = in.read(b);
    //System.out.printf(" read size = %d%n", b.length);

    //try {
    if (n != len)
      throw new RuntimeException("barf with read size=" + len + " in.available=" + avail);

    ConfigCatalogExtProto.DataRoot dsp = ConfigCatalogExtProto.DataRoot.parseFrom(b);
    this.path = dsp.getUrlPath();
    this.dirLocation = dsp.getDirLocation();
    this.type = convertDataRootType( dsp.getType());
    if (dsp.hasCatLocation())
      catLocation = dsp.getCatLocation();
  }

  ////////////////////////////
    //  Type {datasetRoot, datasetScan, catalogScan, featureCollection}

  static public DataRoot.Type convertDataRootType(ConfigCatalogExtProto.DataRootType type) {
    switch (type) {
      case datasetRoot:
        return DataRoot.Type.datasetRoot;
      case datasetScan:
        return DataRoot.Type.datasetScan;
      case catalogScan:
        return DataRoot.Type.catalogScan;
      case featureCollection:
        return DataRoot.Type.featureCollection;
    }
    throw new IllegalStateException("illegal DataRootType " + type);
  }

  static public ConfigCatalogExtProto.DataRootType convertDataRootType(DataRoot.Type type) {
     switch (type) {
       case datasetRoot:
         return ConfigCatalogExtProto.DataRootType.datasetRoot;
       case datasetScan:
         return ConfigCatalogExtProto.DataRootType.datasetScan;
       case catalogScan:
         return ConfigCatalogExtProto.DataRootType.catalogScan;
       case featureCollection:
         return ConfigCatalogExtProto.DataRootType.featureCollection;
     }
     throw new IllegalStateException("illegal DataRootType " + type);
   }


}
