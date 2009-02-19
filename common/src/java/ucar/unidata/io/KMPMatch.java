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
package ucar.unidata.io;

import net.jcip.annotations.Immutable;

/**
 * Knuth-Morris-Pratt Algorithm for Pattern Matching.
 * Immutable
 *
 * @author caron
 * @see <a href="http://www.fmi.uni-sofia.bg/fmi/logic/vboutchkova/sources/KMPMatch_java.html">http://www.fmi.uni-sofia.bg/fmi/logic/vboutchkova/sources/KMPMatch_java.html</a>
 * @since May 9, 2008
 */
@Immutable
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
   * @return index into data[] of first match, else -1 if not found.
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

  /*
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
