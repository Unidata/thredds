/* Copyright */
package thredds.server.catalog.tracker;

import java.io.*;

/**
 * Describe
 *
 * @author caron
 * @since 6/16/2015
 */
public class CatalogExt {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  private long catId;
  private String catRelLocation;

  public CatalogExt() {
  }

  public CatalogExt(long catId, String catRelLocation) {
    this.catId = catId;
    this.catRelLocation = catRelLocation;
  }

  public String getCatRelLocation() {
    return catRelLocation;
  }

  public long getCatId() {
    return catId;
  }

  /* message Catalog {
    required uint64 catId = 1;    // sequence no
    required string catLocation = 2;
  } */

  public void writeExternal(DataOutputStream out) throws IOException {
    ConfigCatalogExtProto.Catalog.Builder builder = ConfigCatalogExtProto.Catalog.newBuilder();
    builder.setCatId(catId);
    builder.setCatLocation(catRelLocation);

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
  }
}
