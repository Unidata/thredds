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
    year, month, day, hour, minute, sec, doy, timeIncr, incrS
  }

  private static Map<String, Type> fields = new HashMap<String, Type>(50);
  private static Map<Integer, Map<String, Type>> locals = new HashMap<Integer, Map<String, Type>>(10);

  static {
    fields.put("0-1-1", Type.wmoBlock);
    fields.put("0-1-2", Type.wmoId);
    fields.put("0-1-7", Type.stationId);
    fields.put("0-1-11", Type.stationId);
    fields.put("0-1-18", Type.stationId);

    fields.put("0-4-1", Type.year);
    fields.put("0-4-2", Type.month);
    fields.put("0-4-3", Type.day);
    fields.put("0-4-43", Type.doy);
    fields.put("0-4-4", Type.hour);
    fields.put("0-4-5", Type.minute);
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

    Map<String, Type> ncep = new HashMap<String, Type>(10);
    ncep.put("0-1-198", Type.stationId);
    locals.put(7, ncep);

    Map<String, Type> uu = new HashMap<String, Type>(10);
    ncep.put("0-1-194", Type.stationId);
    locals.put(59, uu);
  }

  public static class Extract {

    Map<Type, List<DataDescriptor>> typeMap = new TreeMap<Type, List<DataDescriptor>>();

    void match(int center, DataDescriptor dds) {
      String name = dds.getFxyName();
      Type type = findField(center, name);
      if (type == null) return;

      // got a match
      List<DataDescriptor> list = typeMap.get(type);
      if (list == null) {
        list = new ArrayList<DataDescriptor>(3);
        typeMap.put(type, list);
      }
      list.add(dds);
    }

    public boolean hasStation() {
      if (typeMap.get(Type.stationId) != null) return true;
      if (typeMap.get(Type.wmoId) != null) return true;
      return false;
    }

    public boolean hasTime() {
      if (typeMap.get(Type.year) == null) return false;
      if (typeMap.get(Type.month) == null) return false;
      if (typeMap.get(Type.day) == null && typeMap.get(Type.doy) == null) return false;
      if (typeMap.get(Type.hour) == null) return false;
      if (typeMap.get(Type.minute) == null) return false;
      return true;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      for (Type type : typeMap.keySet()) {
        f.format(" %20s: ", type);
        List<DataDescriptor> list = typeMap.get(type);
        for (DataDescriptor dds : list) {
          f.format(" %s", dds.name);
          if (dds.desc != null) f.format("=%s", dds.desc);
          f.format(",");
        }
        f.format(" %n");
      }
      return f.toString();
    }
  }

  public static Type findField(int center, String key) {
    Map<String, Type> local = locals.get(center);
    if (local != null)  {
      Type result = local.get(key);
      if (result != null) return result;
    }

    return findStandardField(key);
  }

  static Type findStandardField(String key) {
    return fields.get(key);
  }

  public static Extract extract(Message m) throws IOException {
    Extract result = new Extract();
    extract(m.ids.getCenterId(), m.getRootDataDescriptor(), result);
    return result;
  }

  private static void extract(int center, DataDescriptor dds, Extract extract) {
    for (DataDescriptor subdds : dds.getSubKeys()) {
      extract.match(center, subdds);

      if (subdds.getSubKeys() != null)
       extract(center, subdds, extract);
    }
  }

}
