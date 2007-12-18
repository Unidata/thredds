package ucar.nc2.util.xml;

import org.jdom.input.SAXBuilder;
import org.jdom.*;

import java.io.IOException;
import java.util.List;

import ucar.unidata.util.Format;

public class TimeJdomReading {

  TimeJdomReading(String filename) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(filename);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element root = doc.getRootElement();
    List<Element> metars = root.getChildren("metar");
    for (Element metar : metars) {
      List<Element> data = metar.getChildren("data");
      for (Element datum : data) {
        String name = datum.getAttributeValue("name");
        String val = datum.getText();
        MetarField fld = MetarField.fields.get(name);
        if (null == fld)
          fld = new MetarField(name);
        fld.sum(val);
      }
      count++;
    }

    System.out.println("Read from NetCDF; # metars= " + count);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = " + took + " sec; "+ Format.d(count/took,0)+" metars/sec");
    System.out.println(" memory= "+Runtime.getRuntime().totalMemory());

    for (MetarField f : MetarField.fields.values())
      System.out.println(" "+f.name+ " = "+f.sum);

  }

  public static void main(String args[]) throws IOException {
    String dir = "C:/doc/metarEncoding/save/";
    new TimeJdomReading(dir+"xmlC.xml");
  }
}
