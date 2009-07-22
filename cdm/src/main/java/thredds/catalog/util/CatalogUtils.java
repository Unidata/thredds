/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog.util;

import thredds.catalog.*;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogUtils
{
    private CatalogUtils() {}

    /**
     * Find all catalogRef elements in the dataset tree formed by the given dataset list.
     *
     * @param datasets         the list of datasets from which to find all the catalogRefs
     * @param log              StringBuilder into which any messages will be written
     * @param onlyRelativeUrls only include catalogRefs with relative HREF URLs if true, otherwise include all catalogRef datasets
     * @return the list of catalogRef datasets
     */
    public static List<InvCatalogRef> findAllCatRefsInDatasetTree( List<InvDataset> datasets, StringBuilder log, boolean onlyRelativeUrls )
    {
        List<InvCatalogRef> catRefList = new ArrayList<InvCatalogRef>();
        for ( InvDataset invds : datasets )
        {
            InvDatasetImpl curDs = (InvDatasetImpl) invds;

            if ( curDs instanceof InvDatasetScan || curDs instanceof InvDatasetFmrc )
                continue;

            if ( curDs instanceof InvCatalogRef )
            {
                InvCatalogRef catRef = (InvCatalogRef) curDs;
                String name = catRef.getName();
                String href = catRef.getXlinkHref();
                URI uri;
                try
                {
                    uri = new URI( href );
                }
                catch ( URISyntaxException e )
                {
                    log.append( log.length() > 0 ? "\n" : "" )
                            .append( "***WARN - CatalogRef [").append(name)
                            .append("] with bad HREF [" ).append( href ).append( "] " );
                    continue;
                }
                if ( onlyRelativeUrls && uri.isAbsolute() )
                    continue;

                catRefList.add( catRef );
                continue;
            }

            if ( curDs.hasNestedDatasets() )
                catRefList.addAll( findAllCatRefsInDatasetTree( curDs.getDatasets(), log, onlyRelativeUrls ) );
        }
        return catRefList;
    }

}
