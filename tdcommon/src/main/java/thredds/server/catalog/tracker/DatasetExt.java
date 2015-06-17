/* Copyright */
package thredds.server.catalog.tracker;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
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
 * TrackedDataset, externalized by ConfigCatalofExtProto
 *
 * @author caron
 * @since 3/28/2015
 */
public class DatasetExt implements Externalizable {
  static public int total_count = 0;
  static public long total_nbytes = 0;

  static private final boolean showParsedXML = true;

  Dataset ds;
  String ncml;

  public String getNcml() {
    return ncml;
  }

  public String getRestrictAccess() {
    return ds == null ? null : ds.getRestrictAccess();
  }

  public DatasetExt() {
  }

  public DatasetExt(Dataset delegate, boolean useNcml) {
    this.ds = delegate;
    // want the string representation
    Element ncmlElem = delegate.getNcmlElement();
    if (useNcml && ncmlElem != null) {
      XMLOutputter xmlOut = new XMLOutputter();
      ncml = xmlOut.outputString(ncmlElem);
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogExtProto.Dataset.Builder builder =  ConfigCatalogExtProto.Dataset.newBuilder();
    builder.setName(ds.getName());
    if (ds.getUrlPath() != null)
      builder.setPath(ds.getUrlPath());
    //if (ds.getId() != null)
    //  builder.setId(ds.getId());
    if (ds.getRestrictAccess() != null)
      builder.setRestrict(ds.getRestrictAccess());
    if (ncml != null)
      builder.setNcml(ncml);

    //for (Access access : ds.getAccess())
    //  builder.addAccess(buildAccess(access));

    //for (Property p : ds.getProperties())
    //  builder.addProperty( buildProperty(p));

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
    //System.out.printf(" read size = %d%n", b.length);

    //try {
      if (n != len)
        throw new RuntimeException("barf with read size="+len+" in.available=" + avail);

    ConfigCatalogExtProto.Dataset dsp = ConfigCatalogExtProto.Dataset.parseFrom(b);
      DatasetBuilder dsb = new DatasetBuilder(null);
      dsb.setName(dsp.getName());
      if (dsp.hasRestrict())
        dsb.put(Dataset.RestrictAccess, dsp.getRestrict());

      if (dsp.hasNcml()) {
        org.jdom2.Document doc;
        try {
          SAXBuilder builder = new SAXBuilder();
          doc = builder.build(dsp.getNcml());
        } catch (JDOMException e) {
          throw new IOException(e.getMessage());
        }
        Element ncmlElem = doc.getRootElement();

        if (showParsedXML) {
          XMLOutputter xmlOut = new XMLOutputter();
          ncml = xmlOut.outputString(ncmlElem);
          System.out.println("*** ConfigCatalogExtProto/ncmlElem = \n" + xmlOut.outputString(ncmlElem) + "\n*******");
        }

        dsb.put(Dataset.Ncml, ncmlElem);
      }

      /* if (dsp.hasId())
        dsb.put(Dataset.Id, dsp.getId());
      if (dsp.hasPath())
        dsb.put(Dataset.UrlPath, dsp.getPath());

      for (ConfigCatalogExtProto.Access accessp : dsp.getAccessList())
        dsb.addAccess(parseAccess(dsb, accessp));

      if (dsp.getPropertyCount() > 0)
        dsb.put(Dataset.Properties, parseProperty(dsp.getPropertyList()));  */

      this.ds = dsb.makeDataset(null);

    //} catch (Throwable e) {
    //  System.out.printf("barf with read size=%d in.available=%d%n", len, avail);
    //  e.printStackTrace();
    //}
  }

  private ConfigCatalogExtProto.Property buildProperty( Property p) {
    ConfigCatalogExtProto.Property.Builder builder =  ConfigCatalogExtProto.Property.newBuilder();
    builder.setName( p.getName());
    builder.setValue(p.getValue());
    return builder.build();
  }

  private List<Property> parseProperty( List<ConfigCatalogExtProto.Property> ps) {
    List<Property> result = new ArrayList<>();
    for (ConfigCatalogExtProto.Property p : ps)
      result.add(new Property( p.getName(), p.getValue()));
    return result;
  }

  private ConfigCatalogExtProto.Access buildAccess( Access a) {
    ConfigCatalogExtProto.Access.Builder builder =  ConfigCatalogExtProto.Access.newBuilder();
    builder.setServiceName(a.getService().getName());
    builder.setUrlPath(a.getUrlPath());
    builder.setDataSize(a.getDataSize());
    if (a.getDataFormatName() != null)
      builder.setDataFormatS(a.getDataFormatName());
    return builder.build();
  }

  private AccessBuilder parseAccess( DatasetBuilder dsb, ConfigCatalogExtProto.Access ap) {
    return new AccessBuilder(dsb, ap.getUrlPath(), null, /* ap.getServiceName(), */ ap.getDataFormatS(), ap.getDataSize());
  }


}
