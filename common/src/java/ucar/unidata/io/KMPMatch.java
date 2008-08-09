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
package ucar.unidata.io;

import java.io.InputStream;

/**
 * Knuth-Morris-Pratt Algorithm for Pattern Matching
 * Immutable
 *
 * @author caron
 * @see <a href="http://www.fmi.uni-sofia.bg/fmi/logic/vboutchkova/sources/KMPMatch_java.html">http://www.fmi.uni-sofia.bg/fmi/logic/vboutchkova/sources/KMPMatch_java.html</a>
 * @since May 9, 2008
 */
public class KMPMatch {

  private final byte[] match;
  private final int[] failure;

  /**
   * Constructor
   * @param match search for this byte pattern
   */
  public KMPMatch(byte[] match) {
    this.match = match;
    failure = computeFailure(match);
  }

  public int getMatchLength() { return match.length; }

  /**
   * Finds the first occurrence of match in data.
   * @param data search in this byte block
   * @param start start at data[start]
   * @param max end at data[start+max]
   * @return index into block of first match, else -1 if not found.
   */
  public int indexOf(byte[] data, int start, int max) {
    int j = 0;
    if (data.length == 0) return -1;

    for (int i = start; i < start + max; i++) {
      while (j > 0 && match[j] != data[i])
        j = failure[j - 1];

      if (match[j] == data[i])
        j++;

      if (j == match.length)
        return i - match.length + 1;

    }
    return -1;
  }

  /**
   * Finds the first occurrence of match in data.
   * @param data search in this byte block
   * @param start start at data[start]
   * @param max end at data[start+max]
   * @return index into block of first match, else -1 if not found.
   *
  public int scan(InputStream is, int start, int max) {
    int j = 0;
    if (data.length == 0) return -1;

    for (int i = start; i < start + max; i++) {
      while (j > 0 && match[j] != data[i])
        j = failure[j - 1];

      if (match[j] == data[i])
        j++;

      if (j == match.length)
        return i - match.length + 1;

    }
    return -1;
  } // */


  private int[] computeFailure(byte[] match) {
    int[] result = new int[match.length];

    int j = 0;
    for (int i = 1; i < match.length; i++) {
      while (j > 0 && match[j] != match[i])
        j = result[j - 1];

      if (match[i] == match[i])
        j++;

      result[i] = j;
    }

    return result;
  }
}
