/* Copyright */
package thredds.server.catalog.tracker;

import java.io.*;
import java.util.Formatter;

/**
 * TrackedDataset, externalized by ConfigCatalogExtProto
 *
 * @author caron
 * @since 3/28/2015
 */
public class DatasetTrackerInfo implements Externalizable {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  static private final boolean showParsedXML = false;

  long catId;
  // Dataset ds;
  String ncml;
  String restrictedAccess;

  public String getNcml() {
    return ncml;
  }

  public String getRestrictAccess() {
    return restrictedAccess;
  }

  public DatasetTrackerInfo() {
  }

  public DatasetTrackerInfo(long catId, String restrictedAccess, String ncml) {
    this.catId = catId;
    this.restrictedAccess = restrictedAccess;
    this.ncml = ncml;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("DatasetTrackerInfo{ catId=%d, restrict=%s", catId, restrictedAccess);
    if (ncml != null)
      f.format("%n%s%n", ncml);
    f.format("}");
    return f.toString();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogExtProto.Dataset.Builder builder = ConfigCatalogExtProto.Dataset.newBuilder();
    builder.setCatId(catId);
    builder.setName("");
    if (restrictedAccess != null)
      builder.setRestrict(restrictedAccess);
    if (ncml != null)
      builder.setNcml(ncml);

    ConfigCatalogExtProto.Dataset index = builder.build();
    byte[] b = index.toByteArray();
    out.writeInt(b.length);
    out.write(b);

    total_count++;
    total_nbytes += b.length + 4;
    //System.out.printf(" write  size = %d%n", b.length);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int avail = in.available();
    int len = in.readInt();
    byte[] b = new byte[len];
    int n = in.read(b);

    if (n != len)
      throw new RuntimeException("barf with read size=" + len + " in.available=" + avail);

    ConfigCatalogExtProto.Dataset pDataset = ConfigCatalogExtProto.Dataset.parseFrom(b);
    this.catId = pDataset.getCatId(); // LOOK not used
    if (pDataset.getRestrict().length() > 0)
      restrictedAccess = pDataset.getRestrict();
    if (pDataset.getNcml().length() > 0)
      ncml = pDataset.getNcml();
  }

  /* private ConfigCatalogExtProto.Property buildProperty(Property p) {
    ConfigCatalogExtProto.Property.Builder builder = ConfigCatalogExtProto.Property.newBuilder();
    builder.setName(p.getName());
    builder.setValue(p.getValue());
    return builder.build();
  }

  private List<Property> parseProperty(List<ConfigCatalogExtProto.Property> ps) {
    List<Property> result = new ArrayList<>();
    for (ConfigCatalogExtProto.Property p : ps)
      result.add(new Property(p.getName(), p.getValue()));
    return result;
  }

  private ConfigCatalogExtProto.Access buildAccess(Access a) {
    ConfigCatalogExtProto.Access.Builder builder = ConfigCatalogExtProto.Access.newBuilder();
    builder.setServiceName(a.getService().getName());
    builder.setUrlPath(a.getUrlPath());
    builder.setDataSize(a.getDataSize());
    if (a.getDataFormatName() != null)
      builder.setDataFormatS(a.getDataFormatName());
    return builder.build();
  }

  private AccessBuilder parseAccess(DatasetBuilder dsb, ConfigCatalogExtProto.Access ap) {
    return new AccessBuilder(dsb, ap.getUrlPath(), null, /* ap.getServiceName(),  ap.getDataFormatS(), ap.getDataSize());
  }   */


}
