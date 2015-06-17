/* Copyright */
package thredds.server.catalog.tracker;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

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
public class DataRootExt implements Externalizable {
  static public int total_count = 0;
  static public long total_nbytes = 0;
  static private final boolean showParsedXML = true;

  private String path, dirPath;
  private long catId;
  private String scanExt, fcExt;

  public DataRootExt() {
  }

  public DataRootExt(long catId, String path, String dirPath, Element scan, Element fc) {
    this.path = path;
    this.catId = catId;
    this.dirPath = dirPath;
    if (scan != null) {
      XMLOutputter xmlOut = new XMLOutputter();
      scanExt = xmlOut.outputString(scan);
    } else if (fc != null) {
      XMLOutputter xmlOut = new XMLOutputter();
      fcExt = xmlOut.outputString(fc);
    }
  }

  /*
  message DataRoot {
    required uint64 catId = 1;
    required string path = 2;
    required string dirLocation = 3;
    optional string datasetScan = 4;
    optional string fcConfig = 5;
  }
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    ConfigCatalogExtProto.DataRoot.Builder builder = ConfigCatalogExtProto.DataRoot.newBuilder();
    builder.setCatId(catId);
    builder.setPath(path);
    builder.setDirLocation(dirPath);
    if (scanExt != null)
      builder.setDatasetScan(scanExt);
    if (fcExt != null)
       builder.setFcConfig(fcExt);

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
    this.catId = dsp.getCatId();
    this.path = dsp.getPath();
    this.dirPath = dsp.getDirLocation();

    if (dsp.hasDatasetScan())
      this.scanExt = dsp.getDatasetScan();
    else if (dsp.hasFcConfig())
      this.fcExt = dsp.getFcConfig();
  }

  public Element getDatasetScanElement() {
    if (scanExt == null) return null;
    org.jdom2.Document doc;
     try {
       SAXBuilder builder = new SAXBuilder();
       doc = builder.build(scanExt);
     } catch (IOException | JDOMException e) {
       throw new RuntimeException(e.getMessage());
     }
     return doc.getRootElement();
  }

  public Element getFeatureCollectionConfigElement() {
    if (fcExt == null) return null;
    org.jdom2.Document doc;
     try {
       SAXBuilder builder = new SAXBuilder();
       doc = builder.build(fcExt);
     } catch (IOException | JDOMException e) {
       throw new RuntimeException(e.getMessage());
     }
     return doc.getRootElement();
  }

}
