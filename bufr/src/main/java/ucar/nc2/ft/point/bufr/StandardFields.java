package ucar.nc2.ft.point.bufr;

import ucar.nc2.iosp.bufr.DataDescriptor;
import ucar.nc2.iosp.bufr.Message;

import java.io.IOException;
import java.util.*;

/**
 * Extract standard fields from BUFR
 *
 * @author caron
 * @since 8/7/13
 */
public class StandardFields {

  private static Map<String, BufrCdmIndexProto.FldType> fields = new HashMap<String, BufrCdmIndexProto.FldType>(50);
  private static Map<Integer, Map<String, BufrCdmIndexProto.FldType>> locals = new HashMap<Integer, Map<String, BufrCdmIndexProto.FldType>>(10);

  static {
    fields.put("0-1-1", BufrCdmIndexProto.FldType.wmoBlock);
    fields.put("0-1-2", BufrCdmIndexProto.FldType.wmoId);
    fields.put("0-1-7", BufrCdmIndexProto.FldType.stationId);
    fields.put("0-1-11", BufrCdmIndexProto.FldType.stationId);
    fields.put("0-1-18", BufrCdmIndexProto.FldType.stationId);
    fields.put("0-1-15", BufrCdmIndexProto.FldType.stationId);
    fields.put("0-1-19", BufrCdmIndexProto.FldType.stationId);

    fields.put("0-4-1", BufrCdmIndexProto.FldType.year);
    fields.put("0-4-2", BufrCdmIndexProto.FldType.month);
    fields.put("0-4-3", BufrCdmIndexProto.FldType.day);
    fields.put("0-4-43", BufrCdmIndexProto.FldType.doy);
    fields.put("0-4-4", BufrCdmIndexProto.FldType.hour);
    fields.put("0-4-5", BufrCdmIndexProto.FldType.minute);
    fields.put("0-4-6", BufrCdmIndexProto.FldType.sec);
    fields.put("0-4-7", BufrCdmIndexProto.FldType.sec);

    fields.put("0-5-1", BufrCdmIndexProto.FldType.lat);
    fields.put("0-5-2", BufrCdmIndexProto.FldType.lat);
    fields.put("0-6-1", BufrCdmIndexProto.FldType.lon);
    fields.put("0-6-2", BufrCdmIndexProto.FldType.lon);

    fields.put("0-7-1", BufrCdmIndexProto.FldType.heightOfStation);
    fields.put("0-7-2", BufrCdmIndexProto.FldType.height);
    fields.put("0-7-7", BufrCdmIndexProto.FldType.height);
    fields.put("0-7-10", BufrCdmIndexProto.FldType.height);
    fields.put("0-7-30", BufrCdmIndexProto.FldType.heightOfStation);

    fields.put("0-7-6", BufrCdmIndexProto.FldType.heightAboveStation);
    fields.put("0-7-7", BufrCdmIndexProto.FldType.heightAboveStation);

    Map<String, BufrCdmIndexProto.FldType> ncep = new HashMap<String, BufrCdmIndexProto.FldType>(10);
    ncep.put("0-1-198", BufrCdmIndexProto.FldType.stationId);
    locals.put(7, ncep);

    Map<String, BufrCdmIndexProto.FldType> uu = new HashMap<String, BufrCdmIndexProto.FldType>(10);
    ncep.put("0-1-194", BufrCdmIndexProto.FldType.stationId);
    locals.put(59, uu);
  }

  public static class Extract {

    Map<BufrCdmIndexProto.FldType, List<DataDescriptor>> typeMap = new TreeMap<BufrCdmIndexProto.FldType, List<DataDescriptor>>();

    void match(int center, DataDescriptor dds) {
      String name = dds.getFxyName();
      BufrCdmIndexProto.FldType type = findField(center, name);
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
      if (typeMap.get(BufrCdmIndexProto.FldType.stationId) != null) return true;
      if (typeMap.get(BufrCdmIndexProto.FldType.wmoId) != null) return true;
      return false;
    }

    public boolean hasTime() {
      if (typeMap.get(BufrCdmIndexProto.FldType.year) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.month) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.day) == null && typeMap.get(BufrCdmIndexProto.FldType.doy) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.hour) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.minute) == null) return false;
      return true;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      for (BufrCdmIndexProto.FldType type : typeMap.keySet()) {
        f.format(" %20s: ", type);
        List<DataDescriptor> list = typeMap.get(type);
        for (DataDescriptor dds : list) {
          f.format(" %s", dds.getName());
          if (dds.getDesc() != null) f.format("=%s", dds.getDesc());
          f.format(",");
        }
        f.format(" %n");
      }
      return f.toString();
    }
  }

  public static BufrCdmIndexProto.FldType findField(int center, String key) {
    Map<String, BufrCdmIndexProto.FldType> local = locals.get(center);
    if (local != null)  {
      BufrCdmIndexProto.FldType result = local.get(key);
      if (result != null) return result;
    }

    return findStandardField(key);
  }

  public static BufrCdmIndexProto.FldType findStandardField(String key) {
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
