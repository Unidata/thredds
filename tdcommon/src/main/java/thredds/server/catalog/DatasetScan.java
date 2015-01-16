/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog;

import net.jcip.annotations.Immutable;
import org.jdom2.Element;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.inventory.MFile;

import java.util.List;
import java.util.Map;

/**
 * DatasetScan
 *
 * @author John
 * @since 1/12/2015
 */
@Immutable
public class DatasetScan extends Dataset {
  private final DatasetScanConfig config;
  private final Element ncml;

  public DatasetScan(DatasetNode parent, String name, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders,
          DatasetScanConfig config, Element ncml) {
    super(parent, name, flds, accessBuilders, datasetBuilders);
    this.config = config;
    this.ncml = ncml;
  }

  org.jdom2.Element getNcmlElement() {
    return ncml;
  }

  MFile requestCrawlableDataset(String path) {
    return null;
  }

  private class RegExpNamer {
    private java.util.regex.Pattern pattern;
    private String replaceString;
    private boolean usePath;

    RegExpNamer(String regExp, String replaceString, boolean usePath) {
      this.pattern = java.util.regex.Pattern.compile(regExp);
      this.replaceString = replaceString;
    }

    public String getLabel(MFile mfile) {
      String name = usePath ? mfile.getPath() : mfile.getName();
      java.util.regex.Matcher matcher = this.pattern.matcher(name);
      if (!matcher.find()) return null;

      StringBuffer startTime = new StringBuffer();
      matcher.appendReplacement(startTime, this.replaceString);
      startTime.delete(0, matcher.start());

      if (startTime.length() == 0) return null;

      return startTime.toString();
    }
  }

}
