// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.iosp.misc;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;

import java.io.IOException;

/*

USPLN-LIGHTNING,2006-10-23T18:00:00,2006-10-23T18:00:00
USPLN-LIGHTNING,2006-10-23T18:01:00,2006-10-23T18:01:00
2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1
2006-10-23T17:59:40,5.4274766,-71.2189314,-31.7,1
2006-10-23T17:59:44,9.3568365,-76.8001513,-34.3,1
2006-10-23T17:59:46,17.5633074,-94.0887814,-23.2,1
2006-10-23T17:59:48,12.9961308,-87.1277356,-33.9,1
2006-10-23T17:59:48,18.2779193,-93.8292358,-15.0,1
....

USPLN data format:

Each 1 minute packet sent has an ASCII header, followed by a record for
each lightning detection during the past 1 minute.

Header
The ASCII header provides information on the creation time of the one
minute packet and ending date and time of the file.

Sample Header:
USPLN-LIGHTNING,2004-10-11T20:45:02,2004-10-11T20:45:02
Description:
Name of Product: USPLN-LIGHTNING
Creation of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02
Ending of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02

NOTE: All times in UTC

Strike Record Following the header, an individual record is provided for
each lightning strike in a comma delimited format.

Sample Strike Records:
2004-10-11T20:44:02,32.6785331,-105.4344587,-96.1,1
2004-10-11T20:44:05,21.2628231,-86.9596634,53.1,1
2004-10-11T20:44:05,21.2967119,-86.9702106,50.3,1
2004-10-11T20:44:06,19.9044769,-100.7082608,43.1,1
2004-10-11T20:44:11,21.4523434,-82.5202274,-62.8,1
2004-10-11T20:44:11,21.8155306,-82.6708778,80.9,1

Description:

Strike Date/Time (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:44:02

Strike Latitude (deg): 32.6785331
Strike Longitude (deg): -105.4344587
Strike Amplitude (kAmps, see note below): -96.1
Stroke Count (number of strokes per flash): 1

Note: At the present time USPLN data are only provided in stroke format,
so the stroke count will always be 1.

Notes about Decoding Strike Amplitude
The amplitude field is utilized to indicate the amplitude of strokes and
polarity of strokes for all Cloud-to- Ground Strokes.

For other types of detections this field is utilized to provide
information on the type of stroke detected.

The amplitude number for Cloud-to-Ground strokes provides the amplitude
of the stroke and the sign (+/-) provides the polarity of the stroke.

An amplitude of 0 indicates USPLN Cloud Flash Detections rather than
Cloud-to-Ground detections.

Cloud flash detections include cloud-to-cloud, cloud-to-air, and
intra-cloud flashes.

An amplitude of -999 or 999 indicates a valid cloud-to-ground stroke
detection in which an amplitude was not able to be determined. Typically
these are long-range detections.
*/


public class UspL2 {

  public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException {
    NetcdfFile.registerIOProvider(UspL2.class);
    NetcdfFile ncfile = NetcdfFile.open( TestAll.getUpcSharePath() + "/testdata/lightning/uspln/uspln_20061023.18");
    System.out.println("ncfile = \n"+ncfile);
  }

}
