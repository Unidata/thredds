// $Id: ParamParser.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.servlet;

import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Apr 2, 2006
 * Time: 2:37:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParamParser {

  public StringBuffer errMessage; // if not null, theres an error in the parameters
  public ArrayList nameList;
  public LatLonRect llbb;
  public double time_start = -1.0, time_end = -1.0;
  public Date date_start, date_end;

  public void parseNames(HttpServletRequest req, String paramName) {
    nameList = new ArrayList();

    String[] names = ServletUtil.getParameterValuesIgnoreCase(req, paramName);

    // allowed form: grid=gridName or grid=gridName;gridName;gridName;...
    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        StringTokenizer stoke = new StringTokenizer( names[i], ";");
        while (stoke.hasMoreTokens()) {
          String gridName = StringUtil.unescape( stoke.nextToken());
          nameList.add(gridName);
        }
      }
    }

  }

  private void addMessage(String msg) {
    if (errMessage == null) errMessage = new StringBuffer();
    errMessage.append(msg);
  }


  public void parseBB(HttpServletRequest req, LatLonRect maxBB) {

    boolean hasBB = false;
    double north = 0.0, south = 0.0, west = 0.0, east = 0.0;

    // check for bb = north, south, west, east
    String bb = ServletUtil.getParameterIgnoreCase(req, "bb");
    if (bb != null) {
      StringTokenizer stoke = new StringTokenizer(bb, ",");
      if (stoke.countTokens() != 4)
        addMessage("bb parameter must have 4 values: 'bb=north,south,west,east'");
      else try {
        north = Double.parseDouble(stoke.nextToken());
        south = Double.parseDouble(stoke.nextToken());
        west = Double.parseDouble(stoke.nextToken());
        east = Double.parseDouble(stoke.nextToken());

        if (null == maxBB)
          hasBB = true;
        else {
          hasBB = !ucar.nc2.util.Misc.closeEnough(north, maxBB.getUpperRightPoint().getLatitude()) ||
                  !ucar.nc2.util.Misc.closeEnough(south, maxBB.getLowerLeftPoint().getLatitude()) ||
                  !ucar.nc2.util.Misc.closeEnough(east, maxBB.getUpperRightPoint().getLongitude()) ||
                  !ucar.nc2.util.Misc.closeEnough(west, maxBB.getLowerLeftPoint().getLongitude());
        }

      } catch (NumberFormatException e) {
        addMessage("bb parameter must have valid double values: 'bb=north,south,west,east'");
      }

      // check for north= & south= & etc
    } else {
      String northS = ServletUtil.getParameterIgnoreCase(req, "north");
      String southS = ServletUtil.getParameterIgnoreCase(req, "south");
      String eastS = ServletUtil.getParameterIgnoreCase(req, "west");
      String westS = ServletUtil.getParameterIgnoreCase(req, "east");

      boolean haveSome = ((northS != null) && (northS.trim()).length() > 0) ||
              ((southS != null) && (southS.trim()).length() > 0) ||
              ((eastS != null) && (eastS.trim()).length() > 0) ||
              ((westS != null) && (westS.trim()).length() > 0);

      //if ya have one gotta have em all
      if (haveSome) {
        boolean haveAll = ((northS != null) && (northS.trim()).length() > 0) &&
                ((southS != null) && (southS.trim()).length() > 0) &&
                ((eastS != null) && (eastS.trim()).length() > 0) &&
                ((westS != null) && (westS.trim()).length() > 0);

        if (!haveAll) {
          addMessage("Must have all 4 north, south, west, east parameters");

        } else try {
          north = Double.parseDouble(northS);
          south = Double.parseDouble(southS);
          west = Double.parseDouble(eastS);
          east = Double.parseDouble(westS);

          if (null == maxBB)
            hasBB = true;
          else {
            hasBB = !ucar.nc2.util.Misc.closeEnough(north, maxBB.getUpperRightPoint().getLatitude()) ||
                    !ucar.nc2.util.Misc.closeEnough(south, maxBB.getLowerLeftPoint().getLatitude()) ||
                    !ucar.nc2.util.Misc.closeEnough(east, maxBB.getUpperRightPoint().getLongitude()) ||
                    !ucar.nc2.util.Misc.closeEnough(west, maxBB.getLowerLeftPoint().getLongitude());
          }
        } catch (NumberFormatException e) {
          addMessage("Must have valid (double) north, south, west, east parameters");
        }
      }
    }

    if ((errMessage == null) && hasBB)
      llbb = new LatLonRect(new LatLonPointImpl(south, west), new LatLonPointImpl(north, east));
  }

  public void parseTimeRange(HttpServletRequest req) {

    boolean err = false, hasTime = false;
    String startS = null, endS = null;

    // look for time=start,end
    String time_range = ServletUtil.getParameterIgnoreCase(req, "time");
    if (time_range != null) {
      StringTokenizer stoke = new StringTokenizer( time_range, ",");
      if (stoke.countTokens() != 2)
        err = true;
      else {
        startS = stoke.nextToken();
        endS = stoke.nextToken();
        hasTime = true;
      }

    } else {
      startS = ServletUtil.getParameterIgnoreCase(req, "time_start");
      endS = ServletUtil.getParameterIgnoreCase(req, "time_end");
      boolean hasStart = (startS != null) && (startS.trim().length() > 0);
      boolean hasEnd = (endS != null) && (endS.trim().length() > 0);

      hasTime = hasStart && hasEnd;
      err = hasStart != hasEnd;
    }

    if (hasTime) {
      try {
        time_start = Double.parseDouble(startS);
        time_end = Double.parseDouble(endS);
      } catch (NumberFormatException e) {
        addMessage("Bad format for time_start and time_end parameters; must be doubles (offsets in hours)");
      }
    } else if (err) {
      addMessage("Must have time_start and time_end parameters as offsets in hours");
    }
  }

  public void parseDateRange(HttpServletRequest req, boolean required) {
    boolean err = false, hasTime = false;
    String startS = null, endS = null;

    // look for time=start,end
    String time_range = ServletUtil.getParameterIgnoreCase(req, "dates");
    if (time_range != null) {
      StringTokenizer stoke = new StringTokenizer( time_range, ",");
      if (stoke.countTokens() != 2)
        err = true;
      else {
        startS = stoke.nextToken();
        endS = stoke.nextToken();
        hasTime = true;
      }

    } else {
      startS = ServletUtil.getParameterIgnoreCase(req, "date_start");
      endS = ServletUtil.getParameterIgnoreCase(req, "date_end");
      boolean hasStart = (startS != null) && (startS.trim().length() > 0);
      boolean hasEnd = (endS != null) && (endS.trim().length() > 0);

      hasTime = hasStart && hasEnd;
      err = hasStart != hasEnd;
    }

    if (err || (!hasTime && required)) {
      addMessage("Must have date_start and date_end parameters as valid ISO date strings");

    } else if (hasTime) {
      DateFormatter formatter = new DateFormatter();
      date_start = formatter.getISODate( startS);
      date_end = formatter.getISODate( endS);

      if ((null == date_start) || (null == date_end))
        addMessage("Bad format for date_start and date_end parameters; must be valid ISO date strings");
    }

  }

}
