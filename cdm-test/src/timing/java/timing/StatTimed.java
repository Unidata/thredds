/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package timing;

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