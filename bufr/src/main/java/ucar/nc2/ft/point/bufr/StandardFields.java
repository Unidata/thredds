/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.bufr;

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.nc2.iosp.bufr.DataDescriptor;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.util.*;

/**
 * Extract standard fields from BUFR
 *
 * @author caron
 * @since 8/7/13
 */
public class StandardFields {
  private static int nflds = 50;
  private static Map<BufrCdmIndexProto.FldType, List<String>> type2Flds = new HashMap<BufrCdmIndexProto.FldType, List<String>>(2*nflds);
  private static Map<String, TypeAndOrder> fld2type = new HashMap<String, TypeAndOrder>(2*nflds);
  private static Map<Integer, Map<String, BufrCdmIndexProto.FldType>> locals = new HashMap<Integer, Map<String, BufrCdmIndexProto.FldType>>(10);

  static {
         // first choice
    addField("0-1-1", BufrCdmIndexProto.FldType.wmoBlock);
    addField("0-1-2", BufrCdmIndexProto.FldType.wmoId);
    addField("0-1-18",BufrCdmIndexProto.FldType.stationId);
    addField("0-4-1", BufrCdmIndexProto.FldType.year);
    addField("0-4-2", BufrCdmIndexProto.FldType.month);
    addField("0-4-3", BufrCdmIndexProto.FldType.day);
    addField("0-4-4", BufrCdmIndexProto.FldType.hour);
    addField("0-4-5", BufrCdmIndexProto.FldType.minute);
    addField("0-4-6", BufrCdmIndexProto.FldType.sec);
    addField("0-5-1", BufrCdmIndexProto.FldType.lat);
    addField("0-6-1", BufrCdmIndexProto.FldType.lon);
    addField("0-7-30", BufrCdmIndexProto.FldType.heightOfStation);

     // second choice
    addField("0-1-15", BufrCdmIndexProto.FldType.stationId);
    addField("0-1-19", BufrCdmIndexProto.FldType.stationId);
    addField("0-4-7", BufrCdmIndexProto.FldType.sec);
    addField("0-4-43", BufrCdmIndexProto.FldType.doy);
    addField("0-5-2", BufrCdmIndexProto.FldType.lat);
    addField("0-6-2", BufrCdmIndexProto.FldType.lon);
    addField("0-7-1", BufrCdmIndexProto.FldType.heightOfStation);

     // third choice
    addField("0-1-62", BufrCdmIndexProto.FldType.stationId);
    addField("0-1-63", BufrCdmIndexProto.FldType.stationId);
    addField("0-7-2", BufrCdmIndexProto.FldType.height);
    addField("0-7-10", BufrCdmIndexProto.FldType.height);
    addField("0-7-7", BufrCdmIndexProto.FldType.height);

    // 4th choice  LOOK
    addField("0-1-5", BufrCdmIndexProto.FldType.stationId);
    addField("0-1-6", BufrCdmIndexProto.FldType.stationId);
    // addField("0-1-7", BufrCdmIndexProto.FldType.stationId); satellite id
    addField("0-1-8", BufrCdmIndexProto.FldType.stationId);
    addField("0-1-10", BufrCdmIndexProto.FldType.stationId);
    addField("0-1-11", BufrCdmIndexProto.FldType.stationId);
    addField("0-7-6", BufrCdmIndexProto.FldType.heightAboveStation);
    addField("0-7-7", BufrCdmIndexProto.FldType.heightAboveStation);

    // locals
    /* Map<String, BufrCdmIndexProto.FldType> ncep = new HashMap<String, BufrCdmIndexProto.FldType>(10);
    ncep.put("0-1-198", BufrCdmIndexProto.FldType.stationId);
    locals.put(7, ncep); */

    Map<String, BufrCdmIndexProto.FldType> uu = new HashMap<String, BufrCdmIndexProto.FldType>(10);
    uu.put("0-1-194", BufrCdmIndexProto.FldType.stationId);
    locals.put(59, uu);
  }

  private static class TypeAndOrder {
    BufrCdmIndexProto.FldType type;
    int order;

    private TypeAndOrder(BufrCdmIndexProto.FldType type, int order) {
      this.type = type;
      this.order = order;
    }
  }

  private static void addField(String fld, BufrCdmIndexProto.FldType type) {
    List<String> list = type2Flds.get(type);
    if (list == null) {
      list = new ArrayList<String>();
      type2Flds.put(type, list);
    }
    list.add(fld); // keep in order

    TypeAndOrder tao = new TypeAndOrder(type, list.size()-1);
    fld2type.put(fld, tao);
  }

  //////////////////////////////////////////////////////

  private static TypeAndOrder findTao(int center, String key) {
    Map<String, BufrCdmIndexProto.FldType> local = locals.get(center);
    if (local != null)  {
      BufrCdmIndexProto.FldType result = local.get(key);
      if (result != null) return new TypeAndOrder(result, -1);
    }

    return fld2type.get(key);
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
    TypeAndOrder tao =  fld2type.get(key);
    return (tao == null) ? null : tao.type;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  public static StandardFieldsFromMessage extract(Message m) throws IOException {
    StandardFieldsFromMessage result = new StandardFieldsFromMessage();
    extract(m.ids.getCenterId(), m.getRootDataDescriptor(), result);
    return result;
  }

  private static void extract(int center, DataDescriptor dds, StandardFieldsFromMessage extract) {
    for (DataDescriptor subdds : dds.getSubKeys()) {
      extract.match(center, subdds);

      if (subdds.getSubKeys() != null)
       extract(center, subdds, extract);
    }
  }

  public static class StandardFieldsFromMessage {

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
      if (typeMap.get(BufrCdmIndexProto.FldType.lat) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.lon) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.stationId) != null) return true;
      if (typeMap.get(BufrCdmIndexProto.FldType.wmoId) != null) return true;
      return false;
    }

    public boolean hasTime() {
      if (typeMap.get(BufrCdmIndexProto.FldType.year) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.month) == null) return false;
      if (typeMap.get(BufrCdmIndexProto.FldType.day) == null && typeMap.get(BufrCdmIndexProto.FldType.doy) == null) return false;
      //if (typeMap.get(BufrCdmIndexProto.FldType.hour) == null) return false;  // LOOK could assume 0:0 ??
      //if (typeMap.get(BufrCdmIndexProto.FldType.minute) == null) return false;
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

  public static class StandardFieldsFromStructure {

    private static class Field {
      TypeAndOrder tao;
      String memberName;
      String valueS;
      int value = -1;
      double valueD = Double.NaN;
      double scale = 1.0;
      double offset = 0.0;
      boolean hasScale;

      private Field(TypeAndOrder tao, Variable v) {
        this.tao = tao;
        this.memberName = v.getShortName();
        Attribute att = v.findAttribute("scale_factor");
        if (att != null && !att.isString()) {
          scale = att.getNumericValue().doubleValue();
          hasScale = true;
        }
        att = v.findAttribute("add_offset");
        if (att != null && !att.isString()) {
          offset = att.getNumericValue().doubleValue();
          hasScale = true;
        }
      }
    }

    private Map<BufrCdmIndexProto.FldType, Field> map = new HashMap<BufrCdmIndexProto.FldType, Field>();

    public StandardFieldsFromStructure(int center, Structure obs) {
      // run through all available fields - LOOK we are not recursing into sub sequences
      for (Variable v : obs.getVariables()) {
        Attribute att = v.findAttribute(BufrIosp2.fxyAttName);
        if (att == null) continue;
        String key = att.getStringValue();
        TypeAndOrder tao = findTao(center, key);
        if (tao == null) continue;

        Field oldFld = map.get(tao.type);
        if (oldFld == null) {
          Field fld = new Field(tao, v);
          map.put(tao.type, fld);
        } else {
          if (oldFld.tao.order < tao.order) {  // replace old one
            Field fld = new Field(tao, v);
            map.put(tao.type, fld);
          }
        }
      }
    }

    // extract standard fields values from specific StructureData
    public void extract(StructureData sdata) {
      StructureMembers sm = sdata.getStructureMembers();
      for (Field fld : map.values()) {
        StructureMembers.Member m = sm.findMember(fld.memberName);
        DataType dtype = m.getDataType();
        if (dtype.isString())
          fld.valueS = sdata.getScalarString(m).trim();
        else if (dtype.isIntegral()) {
          fld.value = sdata.convertScalarInt(m);
          fld.valueD = fld.value;
        } else if (dtype.isNumeric())
          fld.valueD = sdata.convertScalarDouble(m);
      }
    }

    public boolean hasField(BufrCdmIndexProto.FldType type) {
      return null != map.get(type);
    }

    public String getFieldName(BufrCdmIndexProto.FldType type) {
      Field fld = map.get(type);
      return (fld == null) ? null : fld.memberName;
    }

    public String getFieldValueS(BufrCdmIndexProto.FldType type) {
      Field fld = map.get(type);
      if (fld == null) return null;
      if (fld.valueS != null) return fld.valueS;
      if (fld.value != -1) return Integer.toString(fld.value);
      if (!Double.isNaN(fld.valueD)) return Double.toString(fld.valueD);
      return null;
    }

    public int getFieldValue(BufrCdmIndexProto.FldType type) {
      Field fld = map.get(type);
      return (fld == null) ? -1 : fld.value;
    }

    public double getFieldValueD(BufrCdmIndexProto.FldType type) {
      Field fld = map.get(type);
      if  (fld == null) return Double.NaN;
      if (fld.hasScale)
        return fld.valueD * fld.scale + fld.offset;
      return fld.valueD;
    }

    public String getStationId() {
      if (hasField(BufrCdmIndexProto.FldType.stationId))
        return getFieldValueS(BufrCdmIndexProto.FldType.stationId);
      if (hasField(BufrCdmIndexProto.FldType.wmoBlock) && hasField(BufrCdmIndexProto.FldType.wmoId))
         return getFieldValue(BufrCdmIndexProto.FldType.wmoBlock)+"/"+getFieldValue(BufrCdmIndexProto.FldType.wmoId);
      if (hasField(BufrCdmIndexProto.FldType.wmoId))
         return Integer.toString(getFieldValue(BufrCdmIndexProto.FldType.wmoId));
      return null;
     }

    public CalendarDate makeCalendarDate() {
      if (!hasField(BufrCdmIndexProto.FldType.year)) return null;
      int year = getFieldValue(BufrCdmIndexProto.FldType.year);

      int hour = !hasField(BufrCdmIndexProto.FldType.hour) ? 0 : getFieldValue(BufrCdmIndexProto.FldType.hour);
      int minute = !hasField(BufrCdmIndexProto.FldType.minute) ? 0 : getFieldValue(BufrCdmIndexProto.FldType.minute);
      int sec = !hasField(BufrCdmIndexProto.FldType.sec) ? 0 : getFieldValue(BufrCdmIndexProto.FldType.sec);
      if (sec < 0) {
        sec = 0;
      } else if (sec > 0){
        Field fld = map.get(BufrCdmIndexProto.FldType.sec);
        if (fld.scale != 0) {
          sec = (int) (sec * fld.scale);  // throw away msecs
        }
        if (sec < 0 || sec > 59) sec = 0;
      }

      if (hasField(BufrCdmIndexProto.FldType.month) && hasField(BufrCdmIndexProto.FldType.day)) {
        int month = getFieldValue(BufrCdmIndexProto.FldType.month);
        int day = getFieldValue(BufrCdmIndexProto.FldType.day);
        return CalendarDate.of(null, year, month, day, hour, minute, sec);

      } else if (hasField(BufrCdmIndexProto.FldType.doy)) {
        int doy = getFieldValue(BufrCdmIndexProto.FldType.doy);
        return CalendarDate.withDoy(null, year, doy, hour, minute, sec);
      }

      return null;
    }

  }

}
