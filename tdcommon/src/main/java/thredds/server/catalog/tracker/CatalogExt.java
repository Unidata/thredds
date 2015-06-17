/* Copyright */
package thredds.server.catalog.tracker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Describe
 *
 * @author caron
 * @since 6/16/2015
 */
public class CatalogExt implements Externalizable {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  private String path;
  private long id;

  public CatalogExt() {
  }

  public CatalogExt(String path, long id) {
    this.path = path;
    this.id = id;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogExtProto.Catalog.Builder builder = ConfigCatalogExtProto.Catalog.newBuilder();
    builder.setPath(path);
    builder.setId(id);

    ConfigCatalogExtProto.Catalog index = builder.build();
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

    ConfigCatalogExtProto.Catalog catp = ConfigCatalogExtProto.Catalog.parseFrom(b);
    this.path = catp.getPath();
    this.id = catp.getId();
  }
}
