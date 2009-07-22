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
package thredds.server.wms.responses;

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.VectorLayerImpl;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import ucar.nc2.dt.GridDataset;

import java.util.Map;
import java.util.HashMap;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: Aug 11, 2008
 * Time: 11:39:48 PM
 * To change this template use File | Settings | File Templates.
 */
abstract public class FileBasedResponse
{
    private static org.slf4j.Logger log =
              org.slf4j.LoggerFactory.getLogger( FileBasedResponse.class );

    protected RequestParams params;
    protected GridDataset dataset;
    protected Map<String, LayerImpl> layers;
    protected LayerImpl layer;
    protected DataReader reader;
    protected UsageLogEntry usageLogEntry;

    public FileBasedResponse(RequestParams _params, GridDataset _dataset, UsageLogEntry _usageLogEntry) throws Exception
    {
       params = _params;
       dataset = _dataset;
       usageLogEntry = _usageLogEntry;
       reader = DataReader.getDataReader("uk.ac.rdg.resc.ncwms.datareader.DefaultDataReader", dataset);

       layers = reader.getAllLayers(dataset);

       findVectorQuantities( dataset, layers );
    }

    /**
     * Searches through the collection of Layer objects, looking for
     * pairs of quantities that represent the components of a vector, e.g.
     * northward/eastward_sea_water_velocity.  Modifies the given Hashtable
     * in-place.
     *
     * @todo Only works for northward/eastward and x/y components so far
     */
    private static void findVectorQuantities( GridDataset ds, Map<String, LayerImpl> layers )
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, LayerImpl[]> components = new HashMap<String, LayerImpl[]>();
        for ( LayerImpl layer : layers.values() )
        {
            if ( layer.getTitle().contains( "eastward" ) )
            {
                String vectorKey = layer.getTitle().replaceFirst( "eastward_", "" );
                // Look to see if we've already found the northward component
                if ( !components.containsKey( vectorKey ) )
                {
                    // We haven't found the northward component yet
                    components.put( vectorKey, new LayerImpl[2] );
                }
                components.get( vectorKey )[0] = layer;
            }
            else if ( layer.getTitle().contains( "northward" ) )
            {
                String vectorKey = layer.getTitle().replaceFirst( "northward_", "" );
                // Look to see if we've already found the eastward component
                if ( !components.containsKey( vectorKey ) )
                {
                    // We haven't found the eastward component yet
                    components.put( vectorKey, new LayerImpl[2] );
                }
                components.get( vectorKey )[1] = layer;
            }
        }

        // Now add the vector quantities to the collection of Layer objects
        for ( String key : components.keySet() )
        {
            LayerImpl[] comps = components.get( key );

            if ( comps[0] != null && comps[1] != null )
            {
                comps[0].setDataset( ds );
                comps[1].setDataset( ds );
                // We've found both components.  Create a new Layer object
                LayerImpl vec = new VectorLayerImpl( key, comps[0], comps[1] );
                // Use the title as the unique ID for this variable
                vec.setId( key );
                layers.put( key, vec );
            }
        }
    }

    abstract public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception;

}
