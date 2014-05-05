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

package thredds.tds.idd;

import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.util.CatalogUtils;
import ucar.nc2.units.DateType;

import static org.junit.Assert.*;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogValidityTestUtils
{
    private CatalogValidityTestUtils() { }

    public static InvCatalogImpl assertCatalogIsAccessibleAndValid( String catalogUrl )
    {
        assertNotNull( "Null catalog URL not allowed.", catalogUrl );

        InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
        InvCatalogImpl catalog = catFactory.readXML( catalogUrl );

        assertCatalogIsValid( catalog, catalogUrl );

        return catalog;
    }

    public static InvCatalogImpl assertCatalogIsAccessibleValidAndNotExpired( String catalogUrl )
    {
        InvCatalogImpl catalog = assertCatalogIsAccessibleAndValid( catalogUrl );

        String msg = getMessageIfCatalogIsExpired( catalog, catalogUrl );
        assertNull( msg, msg );

        return catalog;
    }

    public static boolean checkIfCatalogTreeIsAccessibleValidAndNotExpired( String catalogUrl,
                                                                            StringBuilder log,
                                                                            boolean onlyRelativeUrls )
    {
        InvCatalogImpl catalog = assertCatalogIsAccessibleValidAndNotExpired( catalogUrl );

        boolean ok = true;
        List<InvCatalogRef> catRefList = CatalogUtils.findAllCatRefsInDatasetTree( catalog.getDatasets(), log, onlyRelativeUrls );

        for ( InvCatalogRef catRef : catRefList )
            ok &= checkIfCatalogTreeIsAccessibleValidAndNotExpired( catRef.getURI().toString(), log, onlyRelativeUrls );

        return ok;
    }


    private static void assertCatalogIsValid( InvCatalogImpl catalog, String catalogUrl )
    {
        assertNotNull( "Null catalog URL not allowed.", catalogUrl );
        assertNotNull( "Null catalog [" + catalogUrl + "].", catalog );

        StringBuilder validationMsg = new StringBuilder();
        boolean isValid = checkIfCatalogIsValid( catalog, catalogUrl, validationMsg );

        assertTrue( "Invalid catalog [" + catalogUrl + "]:" + validationMsg,
                    isValid );
    }

    private static boolean checkIfCatalogIsValid( InvCatalogImpl catalog, String catalogUrl, StringBuilder message )
    {
        if ( catalog == null )
        {
            message.append( "Catalog [" ).append( catalogUrl ).append( "] may not be null." );
            return false;
        }

        return catalog.check( message, false );
    }

    private static String getMessageIfCatalogIsExpired( InvCatalogImpl catalog, String catalogUrl )
    {
        DateType expiresDateType = catalog.getExpires();
        if ( expiresDateType != null )
        {
            if ( expiresDateType.getDate().getTime() < System.currentTimeMillis() )
            {
                return "Expired catalog [" + catalogUrl + "]: " + expiresDateType.toDateTimeStringISO() + ".";
            }
        }
        return null;
    }
}
