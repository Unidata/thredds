/* Copyright */
package thredds.server.catalog.tracker;

import java.io.*;

/**
 * Persisted catalog info
 *
 * @author caron
 * @since 6/16/2015
 */
public class CatalogExt {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  private boolean isRoot;
  private long catId;
  private String catRelLocation;
  private long lastRead;

  public CatalogExt() {
  }

  public CatalogExt(long catId, String catRelLocation, boolean isRoot, long lastRead) {
    this.isRoot = isRoot;
    this.catId = catId;
    this.catRelLocation = catRelLocation;
    this.lastRead = lastRead;
  }

  public String getCatRelLocation() {
    return catRelLocation;
  }

  public long getCatId() {
    return catId;
  }

  public void setCatId(long catId) {
    if (this.catId == 0)
      this.catId = catId;
  }

  public long getLastRead() {
    return lastRead;
  }

  public boolean isRoot() {
    return isRoot;
  }

  /*
  message Catalog {
    uint64 catId = 1;    // sequence no
    string catLocation = 2;
    bool isRoot = 3;
    uint64 lastRead = 4;
  }
  */

  public void writeExternal(DataOutputStream out) throws IOException {
    ConfigCatalogExtProto.Catalog.Builder builder = ConfigCatalogExtProto.Catalog.newBuilder();
    builder.setCatId(catId);
    builder.setCatLocation(catRelLocation);
    builder.setIsRoot(isRoot);
    builder.setLastRead(lastRead);

    ConfigCatalogExtProto.Catalog index = builder.build();
    byte[] b = index.toByteArray();
    out.writeInt(b.length);
    out.write(b);

    total_count++;
    total_nbytes += b.length + 4;
  }

  public void readExternal(DataInputStream in) throws IOException {
    int avail = in.available();
    int len = in.readInt();
    byte[] b = new byte[len];
    int n = in.read(b);
    //System.out.printf(" read size = %d%n", b.length);

    //try {
    if (n != len)
      throw new RuntimeException("barf with read size=" + len + " in.available=" + avail);

    ConfigCatalogExtProto.Catalog catp = ConfigCatalogExtProto.Catalog.parseFrom(b);
    this.catId = catp.getCatId();
    this.catRelLocation = catp.getCatLocation();
    this.isRoot = catp.getIsRoot();
    this.lastRead = catp.getLastRead();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CatalogExt that = (CatalogExt) o;

    return catRelLocation.equals(that.catRelLocation);

  }

  @Override
  public int hashCode() {
    return catRelLocation.hashCode();
  }
}
