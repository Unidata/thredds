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
package thredds.server.cataloggen;

import java.io.File;
import java.net.URL;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTaskConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTaskConfig.class );

  private final String name;
  private final String configDocName;
  private final String resultFileName;
  private final int periodInMinutes;
  private final int delayInMinutes;

  private File configDoc = null;
  private URL configDocURL = null;
  private File resultFile = null;

  /**
   * Constructor
   *
   * @param name            - the name of the task.
   * @param configDocName   - the name of the config doc for the task.
   * @param resultFileName  - name of the resulting file
   * @param periodInMinutes - the time in minutes between runs of the task
   * @param delayInMinutes  - the time to wait before the first run of the task
   */
  CatGenTaskConfig( String name,
                   String configDocName,
                   String resultFileName,
                   int periodInMinutes,
                   int delayInMinutes )
  {
    if ( name == null || name.equals( "" ) )
    {
      log.error( "ctor(): The name cannot be null or empty string." );
      throw new IllegalArgumentException( "The name cannot be null or empty string." );
    }
    if ( configDocName == null || configDocName.equals( "" ) )
    {
      log.error( "ctor(): The config doc name cannot be null or empty string." );
      throw new IllegalArgumentException( "The config doc name cannot be null or empty string." );
    }
    if ( resultFileName == null || resultFileName.equals( "" ) )
    {
      log.error( "ctor(): The result file name cannot be null or empty string." );
      throw new IllegalArgumentException( "The result file name cannot be null or empty string." );
    }
    this.name = name;
    this.configDocName = configDocName;
    this.resultFileName = resultFileName;
    this.periodInMinutes = periodInMinutes;
    this.delayInMinutes = delayInMinutes;
  }

  CatGenTaskConfig( CatGenTaskConfig taskConfig )
  {
    this.name = taskConfig.name;
    this.configDocName = taskConfig.configDocName;
    this.resultFileName = taskConfig.resultFileName;
    this.periodInMinutes = taskConfig.periodInMinutes;
    this.delayInMinutes = taskConfig.delayInMinutes;
  }

  public String getName()
  {
    return name;
  }

  public String getConfigDocName()
  {
    return configDocName;
  }

  public String getResultFileName()
  {
    return resultFileName;
  }

  public int getPeriodInMinutes()
  {
    return periodInMinutes;
  }

  public int getDelayInMinutes()
  {
    return delayInMinutes;
  }
}
