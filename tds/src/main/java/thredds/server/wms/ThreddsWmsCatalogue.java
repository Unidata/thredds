/*******************************************************************************
 * Copyright (c) 2015 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package thredds.server.wms;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.dataset.DataSource;
import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.DiscreteLayeredDataset;
import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.domain.MapDomain;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.feature.DiscreteFeature;
import uk.ac.rdg.resc.edal.graphics.exceptions.EdalLayerNotFoundException;
import uk.ac.rdg.resc.edal.graphics.utils.EnhancedVariableMetadata;
import uk.ac.rdg.resc.edal.graphics.utils.LayerNameMapper;
import uk.ac.rdg.resc.edal.graphics.utils.PlottingDomainParams;
import uk.ac.rdg.resc.edal.graphics.utils.PlottingStyleParameters;
import uk.ac.rdg.resc.edal.graphics.utils.SldTemplateStyleCatalogue;
import uk.ac.rdg.resc.edal.graphics.utils.StyleCatalogue;
import uk.ac.rdg.resc.edal.metadata.DiscreteLayeredVariableMetadata;
import uk.ac.rdg.resc.edal.metadata.VariableMetadata;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.util.ContactInfo;
import uk.ac.rdg.resc.edal.wms.util.ServerInfo;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ucar.nc2.Attribute;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Example of an implementation of a {@link WmsCatalogue} to work with the TDS.
 *
 * This is a working example, but many of the specifics will need to be
 * implemented differently based on how the THREDDS team decide to configure WMS
 * layers.
 *
 * This {@link WmsCatalogue} provides access to a SINGLE dataset. As such, each
 * different dataset requested will have a new instance of this class.
 *
 * NB - No caching is implemented in this {@link WmsCatalogue}. I would
 * recommend a cache which is shared amongst all {@link WmsCatalogue}s, passed
 * in on object construction, and which performs the caching/retrieval in the
 * {@link WmsCatalogue#getFeaturesForLayer(String, PlottingDomainParams)}
 * method. The cache keys will be a pair of the layerName and the
 * {@link PlottingDomainParams}, and the cached values will be
 * {@link Collection}s of {@link DiscreteFeature}s.
 *
 * @author Guy Griffiths
 */
public class ThreddsWmsCatalogue implements WmsCatalogue {
    /*
     * I have declared this as static to be shared between all instances of this
     * class.
     * 
     * However, it *may* be the case that non-gridded datasets need to be
     * supported in TDS, in which case a different DatasetFactory may be needed
     * per dataset type. Currently in EDAL we have:
     * 
     * The CdmGridDatasetFactory which uses the Unidata CDM to load gridded
     * NetCDF data into the EDAL data model
     * 
     * The En3DatasetFactory which uses the Unidata CDM to load the EN3 and EN4
     * NetCDF in-situ datasets into the EDAL data model
     * 
     * If only gridded datasets will be used, then this is fine. However, for
     * maximum flexibility and easy integration of possible future
     * DatasetFactory types, it may be better off being passed into this
     * catalogue.
     */
    static TdsWmsDatasetFactory datasetFactory = new TdsWmsDatasetFactory();

    /*
     * The Dataset associated with this catalogue
     */
    private DiscreteLayeredDataset<? extends DataSource, ? extends DiscreteLayeredVariableMetadata> dataset;

    /*
     * A StyleCatalogue allows us to support different styles for different
     * layer types.
     * 
     * Currently EDAL/ncWMS only have one supported type of StyleCatalogue
     */
    private static final StyleCatalogue styleCatalogue = SldTemplateStyleCatalogue.getStyleCatalogue();

    private String datasetTitle;

    public ThreddsWmsCatalogue(NetcdfDataset ncd, String id) throws IOException, EdalException {
        // in the TDS, we already have a NetcdfFile object, so let's use it to create
        // the edal-java related dataset. To do so, we use our own TdsWmsDatasetFactory, which
        // overrides the getNetcdfDatasetFromLocation method from CdmGridDatasetFactory to take
        // the NetcdfDataset directly. However, createDataset's signature does not take a NetcdfDataset,
        // so we need to make it available to TdsWmsDatasetFactory to use.
        datasetFactory.setNetcdfDataset(ncd);

        // set dataset title
        Attribute datasetTitleAttr;
        datasetTitle = ncd.getTitle();
        if (datasetTitle == null) {
            datasetTitleAttr = ncd.findGlobalAttributeIgnoreCase("title");
            if (datasetTitleAttr != null) {
                datasetTitle = datasetTitleAttr.getStringValue();
            }
        }

        String location = ncd.getLocation();
        dataset = datasetFactory.createDataset(id, location);
    }

    @Override
    public FeaturesAndMemberName getFeaturesForLayer(String layerName, PlottingDomainParams params)
            throws EdalException {
        /*
         * This uses the method on GriddedDataset to extract the appropriate
         * features.
         * 
         * Caching of individual features (i.e. 2d plottable map features) can
         * go here if caching is desired for the TDS WMS.
         */
        MapDomain mapDomain = new MapDomain(params.getBbox(), params.getWidth(), params.getHeight(),
                params.getTargetZ(), params.getTargetT());
        List<? extends DiscreteFeature<?, ?>> extractedFeatures = dataset.extractMapFeatures(
                CollectionUtils.setOf(layerName), mapDomain);
        return new FeaturesAndMemberName(extractedFeatures, layerName);
    }

    @Override
    public Collection<Dataset> getAllDatasets() {
        /*
         * This should return all datasets for the global capabilities document.
         * Whilst global GetCapabilities are not supported for TDS, this should
         * return the single dataset anyway.
         * 
         * See comment below for more details
         */
        Collection<Dataset> ret = new ArrayList<>();
        ret.add(dataset);
        return ret;
    }

    public boolean allowsGlobalCapabilities() {
        /*-
         * This allows this catalogue to return global capabilities document.
         * 
         * However, since a new catalogue is generated for each dataset, the
         * name is slightly misleading.
         * 
         * It should return true so that we can make requests like:
         * 
         * http://localhost:8080/tds/edalwms/mydata/cci.nc?service=WMS&version=1.3.0&request=GetCapabilities
         * 
         * If it returns false, we need the additional dataset URL parameter:
         * 
         * http://localhost:8080/tds/edalwms/mydata/cci.nc?service=WMS&version=1.3.0&request=GetCapabilities&dataset=mydata/cci.nc
         * 
         * which is superfluous.
         */
        return true;
    }

    @Override
    public Dataset getDatasetFromId(String layerName) {
        /*
         * This catalogue only has one dataset, so we can ignore the ID
         */
        return dataset;
    }

    /**
     *
     * Get the title of the dataset. If the title is null (i.e. not found), return
     * the string value "Untitled Dataset"
     *
     * The title is found by the getTitle() method in NetcdfDataset, or by a global
     * attribute "title" (not case-sensitive)
     *
     * @param layerName name of the layer
     * @return
     */
    @Override
    public String getDatasetTitle(String layerName) {

        return (this.datasetTitle != null) ? this.datasetTitle : "Untitled Dataset";
    }

    @Override
    public boolean isDisabled(String layerName) {
        /*
         * Whether this dataset is disabled. Not sure if this has any meaning
         * within TDS?
         */
        return false;
    }

    @Override
    public boolean isDownloadable(String layerName) {
        /*
         * Whether to allow data to be downloaded using the
         * GetVerticalProfile/GetTimeseries requests with the format as
         * "text/csv"
         */
        return false;
    }

    @Override
    public boolean isQueryable(String layerName) {
        /*
         * Whether this layer accepts GetFeatureInfo requests
         */
        return true;
    }

    @Override
    public DateTime getLastUpdateTime() {
        /*
         * Dummy return method. This is used in GetCapabilities to handle the
         * UPDATESEQUENCE parameter
         */
        return new DateTime();
    }

    @Override
    public ContactInfo getContactInfo() {
        /*
         * Returns the contact information associated with this server. This
         * gets used to populate the appropriate fields in the capabilities
         * document
         */
        return new ContactInfo() {
            @Override
            public String getTelephone() {
                return "x5217";
            }

            @Override
            public String getOrganisation() {
                return "ReSC";
            }

            @Override
            public String getName() {
                return "Guy";
            }

            @Override
            public String getEmail() {
                return "guy@reading";
            }
        };
    }


    public ServerInfo getServerInfo() {
        /*
         * Misc info associated with this WMS server
         */
        return new ServerInfo() {
            @Override
            public String getName() {
                return "THREDDS server";
            }

            @Override
            public int getMaxSimultaneousLayers() {
                /*
                 * Number of layers which may be requested in a single WMS
                 * GetMap request. This should return 1: multiple layers are not
                 * implemented.
                 */
                return 1;
            }

            @Override
            public int getMaxImageWidth() {
                return 1000;
            }

            @Override
            public int getMaxImageHeight() {
                return 1000;
            }

            @Override
            public List<String> getKeywords() {
                return new ArrayList<>();
            }

            @Override
            public String getAbstract() {
                return "This is a THREDDS data server";
            }

            @Override
            public boolean allowsFeatureInfo() {
                return true;
            }
            @Override
            public boolean allowsGlobalCapabilities() {
                return true;
            }
        };
    }

    @Override
    public LayerNameMapper getLayerNameMapper() {
        /*
         * Defines the mapping of layer names to dataset ID / variable ID.
         * 
         * In ncWMS we map WMS layer names to pairs of datasets/variables.
         * 
         * In THREDDS, this is not the case. The dataset ID is essentially
         * encoded in the URL and has already been used to create this
         * ThreddsWmsCatalogue object. The layer name then maps directly to the
         * variable ID within that dataset. This implementation reflects that
         * usage
         */
        return new LayerNameMapper() {
            @Override
            public String getVariableIdFromLayerName(String layerName)
                    throws EdalLayerNotFoundException {
                /*
                 * Variable IDs map directly to layer names
                 */
                return layerName;
            }

            @Override
            public String getLayerName(String datasetId, String varId) {
                /*
                 * Variable IDs map directly to layer names
                 */
                return varId;
            }

            @Override
            public String getDatasetIdFromLayerName(String layerName)
                    throws EdalLayerNotFoundException {
                /*
                 * There is one dataset per catalogue, so we ignore the layer
                 * name and return its ID here
                 */
                return dataset.getId();
            }
        };
    }

    @Override
    public StyleCatalogue getStyleCatalogue() {
        return styleCatalogue;
    }

    @Override
    public EnhancedVariableMetadata getLayerMetadata(final VariableMetadata metadata)
            throws EdalLayerNotFoundException {
        /*
         * This is the method which will need the most modification before being
         * included in a full THREDDS server.
         * 
         * How this works will depend to a certain degree on the mechanism the
         * TDS team choose to configure WMS specific information. It covers a
         * lot of the stuff which currently lives in the LayerSettings class.
         * 
         * Generally speaking it provides default values for plotting etc.
         * (which can be overriden by URL parameters), as well as some metadata.
         * Most methods can safely return null, in which case sensible defaults
         * will be used.
         * 
         * The supplied VariableMetadata can be used to find out more about the
         * variable being plotted.
         */
        return new EnhancedVariableMetadata() {

            /**
             * @return The ID of the variable this {@link EnhancedVariableMetadata} is
             * associated with
             */
            @Override
            public String getId() {
                return metadata.getId();
            }

            /**
             * @return The title of this layer to be displayed in the menu and the
             * Capabilities document
             */
            @Override
            public String getTitle() {
                /*
                 * Should perhaps be more meaningful/configurable?
                 */
                return metadata.getId();
            }

            /**
             * @return A brief description of this layer to be displayed in the
             * Capabilities document
             */
            @Override
            public String getDescription() {
                return null;
            }

            /**
             * @return Copyright information about this layer to be displayed be clients
             */
            @Override
            public String getCopyright() {
                return null;
            }

            /**
             * @return More information about this layer to be displayed be clients
             */
            @Override
            public String getMoreInfo() {
                return null;
            }

            /**
             * @return The default plot settings for this variable - this may not return
             * <code>null</code>, but any of the defined methods within the
             * returned {@link PlottingStyleParameters} object may do.
             */
            @Override
            public PlottingStyleParameters getDefaultPlottingParameters() {
                List<Extent<Float>> scaleRanges = null;
                String palette = null;
                Color aboveMaxColour = null;
                Color belowMinColour = null;
                Color noDataColour = null;
                Boolean logScaling = false;
                Integer numColourBands = null;
                Float opacity = 1.0f;

                return new PlottingStyleParameters(scaleRanges, palette, aboveMaxColour,
                        belowMinColour, noDataColour, logScaling, numColourBands,
                        opacity);

            }

            /**
             * @return Whether or not this layer can be queried with GetFeatureInfo requests
             */
            @Override
            public boolean isQueryable() {
                return false;
            };

            /**
             * @return Whether or not this layer can be downloaded in CSV/CoverageJSON format
             */
            @Override
            public boolean isDownloadable() {
                return false;
            };

            /**
             * @return Whether this layer is disabled
             */
            @Override
            public boolean isDisabled() {
                return false;
            };

        };
    }
}
