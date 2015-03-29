/* Copyright */
package thredds.server.catalog.proto;

import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Property;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.DatasetBuilder;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 3/28/2015
 */
public class DatasetExt implements Externalizable {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  Dataset ds;

  public DatasetExt() {
  }

  public DatasetExt(Dataset delegate) {
    this.ds = delegate;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogProto.Dataset.Builder builder =  ConfigCatalogProto.Dataset.newBuilder();
    builder.setName(ds.getName());
    if (ds.getUrlPath() != null)
      builder.setPath(ds.getUrlPath());
    if (ds.getId() != null)
      builder.setId(ds.getId());

    for (Access access : ds.getAccess())
      builder.addAccess(buildAccess(access));

    for (Property p : ds.getProperties())
      builder.addProperty( buildProperty(p));

    ConfigCatalogProto.Dataset index = builder.build();
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
    //System.out.printf(" read size = %d%n", b.length);

    //try {
      if (n != len)
        throw new RuntimeException("barf with read size="+len+" in.available=" + avail);

      ConfigCatalogProto.Dataset dsp = ConfigCatalogProto.Dataset.parseFrom(b);
      DatasetBuilder dsb = new DatasetBuilder(null);
      dsb.setName(dsp.getName());
      if (dsp.hasId())
        dsb.put(Dataset.Id, dsp.getId());
      if (dsp.hasPath())
        dsb.put(Dataset.UrlPath, dsp.getPath());

      for (ConfigCatalogProto.Access accessp : dsp.getAccessList())
        dsb.addAccess(parseAccess(dsb, accessp));

      if (dsp.getPropertyCount() > 0)
        dsb.put(Dataset.Properties, parseProperty(dsp.getPropertyList()));

      this.ds = dsb.makeDataset(null);

    //} catch (Throwable e) {
    //  System.out.printf("barf with read size=%d in.available=%d%n", len, avail);
    //  e.printStackTrace();
    //}
  }

  private ConfigCatalogProto.Property buildProperty( Property p) {
    ConfigCatalogProto.Property.Builder builder =  ConfigCatalogProto.Property.newBuilder();
    builder.setName( p.getName());
    builder.setValue(p.getValue());
    return builder.build();
  }

  private List<Property> parseProperty( List<ConfigCatalogProto.Property> ps) {
    List<Property> result = new ArrayList<>();
    for (ConfigCatalogProto.Property p : ps)
      result.add(new Property( p.getName(), p.getValue()));
    return result;
  }

  private ConfigCatalogProto.Access buildAccess( Access a) {
    ConfigCatalogProto.Access.Builder builder =  ConfigCatalogProto.Access.newBuilder();
    builder.setServiceName(a.getService().getName());
    builder.setUrlPath(a.getUrlPath());
    builder.setDataSize(a.getDataSize());
    if (a.getDataFormatName() != null)
      builder.setDataFormatS(a.getDataFormatName());
    return builder.build();
  }

  private AccessBuilder parseAccess( DatasetBuilder dsb, ConfigCatalogProto.Access ap) {
    return new AccessBuilder(dsb, ap.getUrlPath(), null, /* ap.getServiceName(), */ ap.getDataFormatS(), ap.getDataSize());
  }


}
