/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.util.memory;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryCounterAgentTest {
  public static void measureSize(Object o) {
    long memShallow = MemoryCounterAgent.sizeOf(o);
    long memDeep = MemoryCounterAgent.deepSizeOf(o);
    System.out.printf("%s, shallow=%d, deep=%d%n",
        o.getClass().getSimpleName(),
        memShallow, memDeep);
  }
  public static void main(String[] args) {
    measureSize(new Object());
    measureSize(new HashMap());
    measureSize(new LinkedHashMap());
    measureSize(new ReentrantReadWriteLock());
    measureSize(new byte[1000]);
    measureSize(new boolean[1000]);
    measureSize(new String("Hello World".toCharArray()));
    measureSize("Hello World");
    measureSize(10);
    measureSize(100);
    measureSize(1000);
    measureSize(new Parent());
    measureSize(new Kid());
    measureSize(Thread.State.TERMINATED);
  }

  private static class Parent {
    private int i;
    private boolean b;
    private long l;
  }

  private static class Kid extends Parent {
    private boolean b;
    private float f;
  }
}
