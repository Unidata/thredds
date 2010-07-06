/*
 * Copyright (c) 2010 The University of Reading
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
 */

package thredds.server.wms.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import thredds.server.wms.ThreddsLayer;
import ucar.nc2.units.SimpleUnit;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;

/**
 * Encapsulates the sysadmin's settings of the detailed configuration of the WMS
 * (see wmsConfig.xml).
 */
public class WmsDetailedConfig
{
    private LayerSettings defaultSettings;

    /** Maps standard names to corresponding default settings */
    private Map<String, StandardNameSettings> standardNames =
            new HashMap<String, StandardNameSettings>();

    /** Maps dataset paths to corresponding default settings */
    private Map<String, DatasetPathSettings> datasetPaths =
            new HashMap<String, DatasetPathSettings>();

    /** Private constructor to prevent direct instantiation */
    private WmsDetailedConfig() {}

    /**
     * Parses the XML file from the given file.
     * @return a new WmsDetailedConfig object, if and only if parsing was successful.
     * @throws IOException if there was an io error reading from the file.
     */
    public static WmsDetailedConfig fromFile(File configFile) throws IOException, WmsConfigException
    {
        return fromInputStream(new FileInputStream(configFile));
    }

    /**
     * Parses the XML file from the given input stream, then closes the input stream.
     * @return a new WmsDetailedConfig object, if and only if parsing was successful.
     * @throws IOException if there was an io error reading from the input stream
     */
    public static WmsDetailedConfig fromInputStream(InputStream in) throws IOException, WmsConfigException
    {
        WmsDetailedConfig wmsConfig = new WmsDetailedConfig();

        try
        {
            // Parse the document, with validation
            Document doc = new SAXBuilder(true).build(in);
            in.close();

            // Load the global default settings
            Element defaultSettingsEl =
                (Element)XPath.selectSingleNode(doc, "/wmsConfig/global/defaults");
            // We don't have to check for a null return value since we validated
            // the document against the DTD upon reading.  Similarly we know that
            // all the default settings are non-null: null values would have caused
            // a validation error
            wmsConfig.defaultSettings = new LayerSettings(defaultSettingsEl);

            // Load the overrides for specific standard names
            @SuppressWarnings("unchecked")
            List<Element> standardNamesList =
                (List<Element>)XPath.selectNodes(doc, "/wmsConfig/global/standardNames/standardName");
            for (Element standardNameEl : standardNamesList)
            {
                StandardNameSettings sns = new StandardNameSettings(standardNameEl);
                wmsConfig.standardNames.put(sns.getStandardName(), sns);
            }

            // Load the overrides for specific dataset paths
            @SuppressWarnings("unchecked")
            List<Element> datasetPathsList =
                (List<Element>)XPath.selectNodes(doc, "/wmsConfig/overrides/datasetPath");
            for (Element datasetPathEl : datasetPathsList)
            {
                DatasetPathSettings pathSettings = new DatasetPathSettings(datasetPathEl);
                wmsConfig.datasetPaths.put(pathSettings.getPathSpec(), pathSettings);
            }
        }
        catch(JDOMException jdome)
        {
            throw new WmsConfigException(jdome);
        }

        return wmsConfig;
    }

    /**
     * Gets the settings for the given {@link ThreddsLayer}.  None of the fields
     * will be null in the returned object.
     */
    public LayerSettings getSettings(ThreddsLayer layer)
    {
        LayerSettings settings = new LayerSettings();

        // See if there are specific overrides for this layer's dataset
        String dsPath = layer.getDataset().getDatasetPath();
        DatasetPathSettings dpSettings = this.getBestDatasetPathMatch(dsPath);
        if (dpSettings != null)
        {
            // First we look for the most specific settings, i.e. those for the variable
            LayerSettings varSettings = dpSettings.getSettingsPerVariable().get(layer.getId());
            if (varSettings != null) settings.replaceNullValues(varSettings);

            // Now we look at the default settings for the dataset and use them
            // to insert any currently-unset values
            LayerSettings pathDefaults = dpSettings.getDefaultSettings();
            if (pathDefaults != null) settings.replaceNullValues(pathDefaults);
        }

        // Now look for any per-standard name defaults
        if (layer.getStandardName() != null)
        {
            StandardNameSettings stdNameSettings = this.standardNames.get(layer.getStandardName());
            if (stdNameSettings != null)
            {
                boolean defaultColorScaleRangeUnset = settings.getDefaultColorScaleRange() == null;

                // Set the remaining unset values
                settings.replaceNullValues(stdNameSettings.getSettings());

                // If the default color scale range was previously unset, we
                // must check the units of the new color scale range.
                if (defaultColorScaleRangeUnset &&
                    stdNameSettings.getSettings().getDefaultColorScaleRange() != null)
                {
                    Range<Float> newColorScaleRange = convertUnits(
                        stdNameSettings.getSettings().getDefaultColorScaleRange(),
                        stdNameSettings.getUnits(),
                        layer.getUnits()
                    );
                    // If the units are not convertible, we'll set back to null
                    settings.setDefaultColorScaleRange(newColorScaleRange);
                }
            }
        }

        // Use the global defaults to set any remaining unset values
        settings.replaceNullValues(this.defaultSettings);

        return settings;
    }

    /**
     * Converts the given range of values to a new unit.
     * @param floatRange The range of values to convert
     * @param oldUnits The units of {@code floatRange}
     * @param newUnits The units into which the range is to be converted
     * @return a new Range object containing the same values as {@code floatRange}
     * but in the new units.  If the units are not convertible, this method shall
     * return null.
     */
    private static Range<Float> convertUnits(Range<Float> floatRange, String oldUnits, String newUnits)
    {
        SimpleUnit oldUnit = SimpleUnit.factory(oldUnits);
        SimpleUnit newUnit = SimpleUnit.factory(newUnits);
        if (oldUnit == null || newUnit == null) return null;

        try
        {
            return Ranges.newRange(
                (float)oldUnit.convertTo(floatRange.getMinimum(), newUnit),
                (float)oldUnit.convertTo(floatRange.getMaximum(), newUnit)
            );
        }
        catch(Exception e)
        {
            return null;
        }
    }

    /**
     * Find the dataset path settings that best match the given url path, or null
     * if there is no match.  If multiple settings match the url path, an exact
     * match will "win".  If there is no exact match the longest
     * pattern in the config file "wins" (crudely, this is probably the most
     * precise match).
     */
    private DatasetPathSettings getBestDatasetPathMatch(String urlPath)
    {
        // First look for an exact match (small optimization)
        DatasetPathSettings settings = this.datasetPaths.get(urlPath);
        if (settings != null) return settings;

        // Now look through all the settings for a pattern match, retaining the
        // match with the longest pattern.
        int longestPatternMatchLength = 0;
        DatasetPathSettings bestMatch = null;
        for (DatasetPathSettings dpSettings : this.datasetPaths.values())
        {
            if (dpSettings.pathSpecMatches(urlPath))
            {
                if (dpSettings.getPathSpec().length() > longestPatternMatchLength)
                {
                    longestPatternMatchLength = dpSettings.getPathSpec().length();
                    bestMatch = dpSettings;
                }
            }
        }
        return bestMatch;
    }

    public static void main(String[] args) throws Exception
    {
        Range<Float> newRange = convertUnits(Ranges.newRange(268.0f, 305.0f), "K", "Celsius");
        System.out.println(newRange);

        InputStream in = new FileInputStream(
            "C:\\Documents and Settings\\Jon\\My Documents\\projects\\THREDDS\\" +
            "svn\\tds\\src\\main\\webapp\\WEB-INF\\altContent\\startup\\wmsConfig.xml");
        WmsDetailedConfig wmsConfig = WmsDetailedConfig.fromInputStream(in);
        System.out.println(wmsConfig.defaultSettings);

        System.out.println();
        System.out.println("Dataset paths:");
        for (Map.Entry<String, DatasetPathSettings> entry : wmsConfig.datasetPaths.entrySet())
        {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue().getDefaultSettings());
            for (Map.Entry<String, LayerSettings> varEntry : entry.getValue().getSettingsPerVariable().entrySet())
            {
                System.out.println("Variable " + varEntry.getKey() + ":");
                System.out.println("    " + varEntry.getValue());
            }
        }

        System.out.println();
        System.out.println("Standard names:");
        for (Map.Entry<String, StandardNameSettings> entry : wmsConfig.standardNames.entrySet())
        {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue().getUnits());
            System.out.println(entry.getValue().getSettings());
        }
    }

}
