/* Copyright */
package thredds.server.catalog.tracker;

import thredds.server.catalog.DataRoot;

import java.io.*;

/**
 * Externalized DataRoot.
 *
 * @author caron
 * @since 6/16/2015
 */
public class DataRootExt implements Comparable<DataRootExt> {
  static public int total_count = 0;
  static public long total_nbytes = 0;
  static private final boolean debug = false;

  private String path;
  private String dirLocation;
  private DataRoot.Type type;
  private String catLocation;
  private String name;

  private DataRoot dataRoot;

  public DataRootExt() {
  }

  public DataRootExt(DataRoot dataRoot, String catLocation) {
    this.path = dataRoot.getPath();
    this.dataRoot = dataRoot;
    this.dirLocation = dataRoot.getDirLocation();
    this.type = dataRoot.getType();
    this.name = dataRoot.getName();
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

  public String getName() {
    return name;
  }

  /* message DataRoot {
      required string urlPath = 1;
      required string dirLocation = 2;
      required DataRootType type = 3;
      optional string catLocation = 4;    // omit for simple dataset root
    } */
  public void writeExternal(DataOutputStream out) throws IOException {
    ConfigCatalogExtProto.DataRoot.Builder builder = ConfigCatalogExtProto.DataRoot.newBuilder();
    builder.setUrlPath(path);
    builder.setDirLocation(dirLocation);
    builder.setType(convertDataRootType(type));
    if (type != DataRoot.Type.datasetRoot) {
      builder.setCatLocation(catLocation);
      builder.setName(name);
    }

    ConfigCatalogExtProto.DataRoot index = builder.build();
    byte[] b = index.toByteArray();
    out.writeInt(b.length);
    out.write(b);

    total_count++;
    total_nbytes += b.length + 4;
    if (debug) System.out.printf(" %d: DataRootExt.writeExternal len=%d total=%d%n", total_count, b.length, total_nbytes);
  }

  private static int nrecordsRead = 0;

  public void readExternal(DataInputStream in) throws IOException {
    int avail = in.available();
    int len = in.readInt();
    if (debug) System.out.printf(" %d: DataRootExt.readExternal len=%d%n", nrecordsRead++, len);

    byte[] b = new byte[len];
    int n = in.read(b);
    //System.out.printf(" read size = %d%n", b.length);

    //try {
    if (n != len)
      throw new RuntimeException("DataRootExt.readExternal failed read size=" + len + " in.available=" + avail);

    ConfigCatalogExtProto.DataRoot dsp = ConfigCatalogExtProto.DataRoot.parseFrom(b);
    this.path = dsp.getUrlPath();
    this.dirLocation = dsp.getDirLocation();
    this.type = convertDataRootType( dsp.getType());
    if (dsp.hasCatLocation())
      catLocation = dsp.getCatLocation();
    if (dsp.hasName())
      name = dsp.getName();
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


  @Override
  public int compareTo(DataRootExt o) {
    return path.compareTo( o.getPath());
  }
}
