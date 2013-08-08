package ucar.nc2.iosp.bufr;

import java.io.IOException;
import java.util.*;

/**
 * Extract standard fields from BUFR
 *
 * @author caron
 * @since 8/7/13
 */
public class StandardFields {

  public enum Type {lat, lon, height, heightAboveStation, heightOfStation,
    stationId, wmoId, wmoBlock,
    year, month, day, hour, min, sec, doy, timeIncr, incrS
  }

  private static Map<String, Type> fields = new HashMap<String, Type>(100);

  static {
    fields.put("0-1-1", Type.wmoBlock);
    fields.put("0-1-2", Type.wmoId);
    fields.put("0-1-7", Type.stationId);
    fields.put("0-1-11", Type.stationId);
    fields.put("0-1-18", Type.stationId);   // also "0-1-194" with center = 59

    fields.put("0-4-1", Type.year);
    fields.put("0-4-2", Type.month);
    fields.put("0-4-3", Type.day);
    fields.put("0-4-43", Type.doy);
    fields.put("0-4-4", Type.hour);
    fields.put("0-4-5", Type.min);
    fields.put("0-4-6", Type.sec);
    fields.put("0-4-7", Type.sec);

    fields.put("0-5-1", Type.lat);
    fields.put("0-5-2", Type.lat);
    fields.put("0-6-1", Type.lon);
    fields.put("0-6-2", Type.lon);

    fields.put("0-7-1", Type.heightOfStation);
    fields.put("0-7-2", Type.height);
    fields.put("0-7-7", Type.height);
    fields.put("0-7-10", Type.height);
    fields.put("0-7-30", Type.heightOfStation);

    fields.put("0-7-6", Type.heightAboveStation);
    fields.put("0-7-7", Type.heightAboveStation);
  }

  public static class Extract {

    Map<Type, List<DataDescriptor>> fields = new TreeMap<Type, List<DataDescriptor>>();

    void match(DataDescriptor dds) {
      String name = dds.getFxyName();
      //System.out.printf("  look for %s%n", name);
      Type type = findStandardField(name);
      if (type == null) return;
      List<DataDescriptor> list = fields.get(type);
      if (list == null) {
        list = new ArrayList<DataDescriptor>(3);
        fields.put(type, list);
      }
      list.add(dds);
    }

    public boolean hasStation() {
      if (fields.get(Type.stationId) != null) return true;
      if (fields.get(Type.wmoId) != null) return true;
      return false;
    }

    public boolean hasTime() {
      if (fields.get(Type.year) == null) return false;
      if (fields.get(Type.month) == null) return false;
      if (fields.get(Type.day) == null && fields.get(Type.doy) == null) return false;
      if (fields.get(Type.hour) == null) return false;
      if (fields.get(Type.min) == null) return false;
      return true;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      for (Type type : fields.keySet()) {
        f.format(" %10s: ", type);
        List<DataDescriptor> list = fields.get(type);
        for (DataDescriptor dds : list)
          f.format(" %s=%s, ", dds.name, dds.desc);
        f.format(" %n");
      }
      return f.toString();
    }
  }

  public static Type findStandardField(String key) {
    return fields.get(key);
  }

  public static Extract extract(Message m) throws IOException {
    Extract result = new Extract();
    extract(m.getRootDataDescriptor(), result);
    return result;
  }

  private static void extract(DataDescriptor dds, Extract extract) {
    for (DataDescriptor subdds : dds.getSubKeys()) {
      extract.match(subdds);

      if (subdds.getSubKeys() != null)
       extract(subdds, extract);
    }
  }

}
