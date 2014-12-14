/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.ma2;

import org.junit.Test;

/**
 * test Range class
 *
 * @author John
 * @since 11/2/11
 */
public class TestRange {

  @Test
  public void testStride() throws InvalidRangeException {
    int nx = 1237;
    Range r = new Range(0,nx-1,9);
    System.out.printf("%s%n",r);
    System.out.printf("%d %d %d %d%n",r.first(),r.last(),r.stride(),r.length());
    assert r.first() == 0;
    assert r.last() == 1233;
    assert r.stride() == 9;
    assert r.length() == 138;

    Section s = new Section( r, r);
    Section.Iterator iter = s.getIterator(new int[] {nx, nx});
    int[] iterResult = new int[2];
    int count = 0;
    for (int y = r.first(); y <= r.last(); y += r.stride()) {
      for (int x = r.first(); x <= r.last(); x += r.stride()) {
        assert iter.hasNext();
        int iterN = iter.next(iterResult);
        assert iterResult[0] == y;
        assert iterResult[1] == x;
        assert iterN == y * nx + x;
        count++;
      }
    }
    assert count == 138 * 138;

  }
}
