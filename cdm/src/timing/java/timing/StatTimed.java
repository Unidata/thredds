// $Id: StatTimed.java,v 1.3 2004/09/24 03:26:36 caron Exp $
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

package timing;

import timing.Stat;

import java.util.ArrayList;

/**
 */

public class StatTimed extends Stat {
  private ArrayList times = new ArrayList();

  public StatTimed() {
  }

  public StatTimed( String name) {
    super(name, true);
  }

  public StatTimed( Stat s) {
    super(s.getName(), s.getN(), s.average(), s.std(), true);
  }


  /**
   * Add a sample, along with the time it was taken
   * @param s sample value
   * @param time the time when it was taken
   */
  public void sample( double s, java.util.Date time) {
    super.sample(s);
    if (time != null)
      times.add( time);
  }

  /** array list of dates */
  public ArrayList getTimes() { return times; }
  public void setTimes(ArrayList times) { this.times = times; }
}