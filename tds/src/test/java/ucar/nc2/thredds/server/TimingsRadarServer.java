/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/**
 * User: rkambic
 * Date: Mar 15, 2011
 * Time: 2:00:10 PM
 */

package ucar.nc2.thredds.server;

import ucar.nc2.thredds.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Product;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TimingsRadarServer {

  static String host = "http://motherlode.ucar.edu:";
  static String s8081 = "8081";
  static String s8080 = "9080";
  static String server8081 = host + s8081;
  static String server8080 = host + s8080;
  static String localhost8080 = "http://localhost:" + s8080;
  static String pathStart = "/thredds/radarServer/";
  static String[] datasets = {
      "catalog.xml",
      "nexrad/level2/IDD/dataset.xml",
      "nexrad/level3/IDD/dataset.xml",
      //"terminal/level3/IDD/dataset.xml",
  };

  static String[] stations = {
      "nexrad/level2/IDD/stations.xml",
      "nexrad/level3/IDD/stations.xml",
      "terminal/level3/IDD/stations.xml",
  };
  
  static String[] queries = {
      "nexrad/level2/IDD?stn=KFTG",
      "nexrad/level2/IDD?stn=KGLD",
      "nexrad/level2/IDD?stn=KBYX",
      "nexrad/level3/IDD?var=N0R&stn=FTG",
      "nexrad/level3/IDD?var=N0R&stn=GLD",
      "nexrad/level3/IDD?var=N0R&stn=BYX",
      //"terminal/level3/IDD?var=TR0&stn=MIA", // no local dataset for this test
  };


  public static void main(String args[]) {
    String outputDir;
    File where = new File("C:/data/RadarServer/");
    if (where.exists()) {
      outputDir = where.getPath();
    } else {
      outputDir = "/local/robb/data/radar/RadarServer/";
    }
    System.out.println("Using host "+ host );
    StringBuffer errlog8080      = new StringBuffer();
    StringBuffer errlog8081      = new StringBuffer();
    long  start8080; // = System.currentTimeMillis();
    long  end8080;
    long  start8081; // = System.currentTimeMillis();
    long  end8081;
    long diff;
    TDSRadarDatasetCollection dsc8080;
    TDSRadarDatasetCollection dsc8081;
    for (int i = 0; i < datasets.length; i++) {
      //File file = new File(outputDir + i + ".xml");
      try {
        start8080 = System.currentTimeMillis();
        //IO.readURLtoFileWithExceptions(server8081 + pathStart + datasets[i], file);
        IO.readURLcontentsWithException( server8080 + pathStart + datasets[i] );
        end8080 = System.currentTimeMillis();

        start8081 = System.currentTimeMillis();
        IO.readURLcontentsWithException( server8081 + pathStart + datasets[i] );
        end8081 = System.currentTimeMillis();
        diff = ( end8080 - start8080 ) - ( end8081 - start8081 );
        if ( diff > 0 )
          System.out.println( "Request "+ s8080 +" "+ datasets[i]+" took "+ diff +"ms longer then "+ s8081 );
        else
          System.out.println( "Request "+ s8081 +" "+ datasets[i]+" took "+ -diff +"ms longer then "+ s8080 );
        } catch (IOException ioe) {
        System.out.println("Failed " + datasets[i]);
      }
    }

    Random generator = new Random();
    for (int i = 1; i < datasets.length; i++) {
      if ( i == 2 )
            break;
      try {
        start8080 = System.currentTimeMillis();
        dsc8080 = TDSRadarDatasetCollection.factory("test", server8080 + pathStart + datasets[i], errlog8080);
        //dsc8080 = TDSRadarDatasetCollection.factory("test", localhost8080 + pathStart + datasets[i], errlog8080);
        end8080 = System.currentTimeMillis();

        start8081 = System.currentTimeMillis();
        dsc8081 = TDSRadarDatasetCollection.factory("test", server8081 + pathStart +datasets[i], errlog8081);
        end8081 = System.currentTimeMillis();
        diff = ( end8080 - start8080 ) - ( end8081 - start8081 );
        if ( diff > 0 )
          System.out.println( "Request "+ s8080 +" "+ datasets[i]+" took "+ diff +"ms longer then "+ s8081 );
        else
          System.out.println( "Request "+ s8081 +" "+ datasets[i]+" took "+ -diff +"ms longer then "+ s8080 );


        List<Station> stns8080 = dsc8080.getStations();
        List<Station> stns8081 = dsc8081.getStations();
        List<Product> pd8080  = dsc8080.getRadarProducts();
        List<Product> pd8081  = dsc8081.getRadarProducts();
        String product = null;

        if ( pd8081.size() > 15 ) { // assume level3
          product = pd8081.get( generator.nextInt( pd8081.size() ) ).getID();
        }
        //assert( stns8080.size() == stns8081.size() );
        assert( pd8080.size() == pd8081.size() );

        // 8080 times are not dynamic so use 8081
        List<String> tl8080  = dsc8080.getRadarTimeSpan();
        List<String> tl8081  = dsc8081.getRadarTimeSpan();
        Date ts1 = DateUnit.getStandardOrISO(tl8081.get( 0 ));
        Date ts2 = DateUnit.getStandardOrISO(tl8081.get( 1 ));

        int count = 0;

        for ( int s = 1; s <  stns8081.size(); s++) {
          //if ( count++ > 2 )
          //  break;

          String stnName8080 = stns8081.get( generator.nextInt( stns8081.size() ) ).getName();
          String stnName8081 = stns8081.get( generator.nextInt( stns8081.size() ) ).getName();
          //System.out.println( "stnName8080 ="+ stnName8080 );
          //System.out.println( "stnName8081 ="+ stnName8081 );
          start8080 = System.currentTimeMillis();
          List<Date> tlist8080 = dsc8080.getRadarStationTimes(stnName8080, product, ts1, ts2);
          end8080 = System.currentTimeMillis();
          start8081 = System.currentTimeMillis();
          List<Date> tlist8081 = dsc8081.getRadarStationTimes(stnName8081, product, ts1, ts2);
          end8081 = System.currentTimeMillis();
          diff = ( end8080 - start8080 ) - ( end8081 - start8081 );
          if ( diff > 0 )
            System.out.println( "Request "+ s8080 +" getting "+ stnName8080+" times took "+ diff +"ms longer then "+
                s8081 +" getting "+ stnName8081);
          else
            System.out.println( "Request "+ s8081 +" getting "+ stnName8081+" times took "+ -diff +"ms longer then"+
                s8080 +" getting "+ stnName8080);

        }
      } catch (IOException ioe) {
            System.out.println("Failed " + datasets[i]);
      }

    }
  }
}
