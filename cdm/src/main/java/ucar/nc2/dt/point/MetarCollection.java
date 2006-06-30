// $Id: MetarCollection.java,v 1.2 2006/06/06 16:07:14 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.Station;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Quick 'n dirty implementation for collection of Metars in UnidatObs format.
 */
public class MetarCollection {
  private ArrayList fileList = new ArrayList();
  private SimpleDateFormat dateFormat;

  public MetarCollection( String dirLocation) {
     dateFormat = new java.text.SimpleDateFormat("yyyyMMdd_HHmm");
     dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    File dir = new File( dirLocation);
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      fileList.add(new DatasetWrap( file.getPath()));
    }
    Collections.sort(fileList);
  }

  public void openStationDataset( String location) throws ParseException {
    fileList.add(new DatasetWrap( location));
  }

  public int extract(List stations, Date startDate, Date endDate, String fldName, StringBuffer sbuff) throws Exception {
    int count = 0;
    for (int i = 0; i < fileList.size(); i++) {
      DatasetWrap dw = (DatasetWrap) fileList.get(i);
      if (!dw.contains(startDate, endDate))
        continue;
      StationObsDataset dataset = acquireStationDataset( dw.location);
      count += extract( dataset, stations, startDate, endDate, fldName, sbuff);
      dataset.close();
    }
    return count;
  }

  public int extract(LatLonRect boundingBox, Date startDate, Date endDate, String fldName, StringBuffer sbuff) throws Exception {
    int count = 0;
    for (int i = 0; i < fileList.size(); i++) {
      DatasetWrap dw = (DatasetWrap) fileList.get(i);
      if (!dw.contains(startDate, endDate))
        continue;
      StationObsDataset dataset = acquireStationDataset( dw.location);
      count += extract( dataset, boundingBox, startDate, endDate, fldName, sbuff);
      dataset.close();
    }
    return count;
  }

  private StationObsDataset acquireStationDataset( String location) throws IOException {
    StringBuffer log = new StringBuffer();
    try {
      StationObsDataset sod = (StationObsDataset) PointObsDatasetFactory.open( location, log);
      System.out.println("open "+location);
      return sod;
    } catch (IOException e) {
      System.out.println("Error= "+log);
      throw e;
    }
  }

  private int extract(StationObsDataset dataset, List stationNames, Date startDate, Date endDate, String fldName, StringBuffer sbuff) throws IOException {
    ArrayList stations = new ArrayList();
    for (int i = 0; i < stationNames.size(); i++) {
      String name = (String) stationNames.get(i);
      Station s = dataset.getStation( name);
      if (s == null)
        System.out.println("No station named "+name);
      else
        stations.add(s);
    }

    int count = 0;
    List data = dataset.getData( stations, startDate, endDate);
    for (int i = 0; i < data.size(); i++) {
      StationObsDatatype obs = (StationObsDatatype) data.get(i);
      ucar.ma2.StructureData sdata = obs.getData();
      String fldData = sdata.getScalarString( fldName);
      sbuff.append(fldData);
      sbuff.append("\n");
      count++;
    }

    return count;
  }

  private int extract(StationObsDataset dataset, LatLonRect boundingBox, Date startDate, Date endDate, String fldName, StringBuffer sbuff) throws IOException {
    int count = 0;
    List data = dataset.getData( boundingBox, startDate, endDate);
    for (int i = 0; i < data.size(); i++) {
      StationObsDatatype obs = (StationObsDatatype) data.get(i);
      ucar.ma2.StructureData sdata = obs.getData();
      String fldData = sdata.getScalarString( fldName);
      sbuff.append(fldData);
      sbuff.append("\n");
      count++;
    }

    return count;
  }

  private class DatasetWrap implements Comparable {
    String location;
    Date start, end;

    DatasetWrap( String location) {
      this.location = location;
      int pos0 = location.lastIndexOf("Surface_METAR_");
      int pos1 = location.lastIndexOf(".");
      String dateString = location.substring(pos0+14, pos1);
      Date nominal = null;
      try {
        nominal = dateFormat.parse( dateString);
      } catch (ParseException e) {
        throw new IllegalStateException(e.getMessage());
      }
      Calendar c = Calendar.getInstance( TimeZone.getTimeZone("GMT"));
      c.setTime( nominal);
      c.add( Calendar.HOUR, -1);
      start = c.getTime();
      c.setTime( nominal);
      c.add( Calendar.HOUR, 24);
      end = c.getTime();
      // System.out.println("  "+dateString+" = "+start+" end= "+end);
    }

    public boolean contains( Date startWant, Date endWant) {
      if (start.after( endWant)) return false;
      if (startWant.after( end)) return false;
      return true;
    }

    public int compareTo(Object o) {
      DatasetWrap dw = (DatasetWrap) o;
      return start.compareTo( dw.start);
    }
  }

  static public void main( String[] args) throws Exception {
    String location = args[0];
    String names = args[1];
    String start = args[2];
    String end = args[3];
    String fldName = args[4];

    DateFormatter formatter = new  DateFormatter();
    Date startDate = formatter.getISODate(start);
    Date endDate = formatter.getISODate(end);
    System.out.println("Extract from "+location);
    System.out.println(" Station= "+names);
    System.out.println(" Start= "+ formatter.toDateTimeString(startDate));
    System.out.println(" End= "+formatter.toDateTimeString(endDate));
    System.out.println(" field= "+fldName);

    ArrayList nameList = new ArrayList();
    StringTokenizer stoke = new StringTokenizer( names, ";");
    while (stoke.hasMoreTokens()) {
      String gridName = StringUtil.unescape( stoke.nextToken());
      nameList.add(gridName);
    }

    MetarCollection extractor = new MetarCollection(location);
    StringBuffer sbuff = new StringBuffer();
    int total = extractor.extract( nameList, startDate, endDate, fldName, sbuff);
    System.out.println(" total= "+total);
    System.out.print(sbuff.toString());
  }
}

/* Change History:
   $Log: MetarCollection.java,v $
   Revision 1.2  2006/06/06 16:07:14  caron
   *** empty log message ***

   Revision 1.1  2006/04/03 22:59:17  caron
   IOSP.readNestedData() remove flatten, handle flatten=false in NetcdfFile.readMemberData(); this allows IOSPs to be simpler
   add metar decoder from Robb's thredds.servlet.ldm package

   Revision 1.2  2006/03/30 21:23:51  caron
   remove DateUnit static methods - not thread safe

   Revision 1.1  2005/11/03 19:30:23  caron
   no message

*/