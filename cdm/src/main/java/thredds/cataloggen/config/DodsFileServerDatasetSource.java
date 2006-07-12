// $Id$

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
package thredds.cataloggen.config;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalog;

import java.util.List;

// $Id$

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

public class DodsFileServerDatasetSource  extends DatasetSource
{
  //private static Log log = LogFactory.getLog( DodsFileServerDatasetSource.class);
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DodsFileServerDatasetSource.class);

  public DodsFileServerDatasetSource()
  {
    this.type = DatasetSourceType.getType( "DodsFileServer");
  }

  // @todo Implement the DodsFileServer type of DatasetSource.
  protected InvDataset createDataset( String datasetLocation, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected InvCatalog createSkeletonCatalog( String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected boolean isCollection( InvDataset dataset )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected List expandThisLevel( InvDataset collectionDataset, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

}

/*
 * $Log: DodsFileServerDatasetSource.java,v $
 * Revision 1.9  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.8  2005/11/18 23:51:03  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.7  2005/07/20 22:44:54  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.6  2005/04/05 22:37:01  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.5  2005/01/20 23:13:30  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.4  2004/12/22 22:28:59  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.3  2004/11/30 22:49:12  edavis
 * Start changing DatasetSource into a more usable API.
 *
 * Revision 1.2  2003/08/29 21:41:46  edavis
 * The following changes where made:
 *
 *  1) Added more extensive logging (changed from thredds.util.Log and
 * thredds.util.Debug to using Log4j).
 *
 * 2) Improved existing error handling and added additional error
 * handling where problems could fall through the cracks. Added some
 * catching and throwing of exceptions but also, for problems that aren't
 * fatal, added the inclusion in the resulting catalog of datasets with
 * the error message as its name.
 *
 * 3) Change how the CatGenTimerTask constructor is given the path to the
 * config files and the path to the resulting files so that resulting
 * catalogs are placed in the servlet directory space. Also, add ability
 * for servlet to serve the resulting catalogs.
 *
 * 4) Switch from using java.lang.String to using java.io.File for
 * handling file location information so that path seperators will be
 * correctly handled. Also, switch to java.net.URI rather than
 * java.io.File or java.lang.String where necessary to handle proper
 * URI/URL character encoding.
 *
 * 5) Add handling of requests when no path ("") is given, when the root
 * path ("/") is given, and when the admin path ("/admin") is given.
 *
 * 6) Fix the PUTting of catalogGenConfig files.
 *
 * 7) Start adding GDS DatasetSource capabilities.
 *
 * Revision 1.1  2003/08/20 17:52:37  edavis
 * Initial version.
 *
 */