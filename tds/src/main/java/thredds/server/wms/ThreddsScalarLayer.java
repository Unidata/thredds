/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package thredds.server.wms;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;

import java.io.IOException;
import java.util.List;
import thredds.server.wms.config.LayerSettings;
import ucar.nc2.Attribute;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;

/**
 * Wraps a GridDatatype as a ScalarLayer object
 * @todo Implement more efficient getTimeseries()
 * @author Jon
 */
class ThreddsScalarLayer extends AbstractScalarLayer implements ThreddsLayer
{
    private GridDatatype grid;
    private ThreddsDataset dataset;
    private List<DateTime> times;
    private DataReadingStrategy dataReadingStrategy;

    // Will be set in ThreddsWmsController.ThreddsLayerFactory
    private LayerSettings layerSettings;

    public ThreddsScalarLayer(String id) {
        super(id);
    }

  @Override
  public String getName()
  {
    return this.getId();
  }

  @Override
    public ThreddsDataset getDataset() { return this.dataset; }
    public void setDataset( ThreddsDataset dataset) { this.dataset = dataset; }

    public void setGridDatatype(GridDatatype grid) { this.grid = grid; }

  @Override
  public Chronology getChronology()
  {
     if (this.times == null || this.times.isEmpty()) return null;
     return this.times.get(0).getChronology();
  }

  @Override
    public List<DateTime> getTimeValues() {
        return this.times;
    }

    public void setTimeValues(List<DateTime> timeValues) {
        this.times = timeValues;
    }

    public void setDataReadingStrategy(DataReadingStrategy dataReadingStrategy) {
        this.dataReadingStrategy = dataReadingStrategy;
    }

    @Override
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
            throws InvalidDimensionValueException, IOException
    {
        PointList singlePoint = PointList.fromPoint(xy);
        return this.readPointList(time, elevation, singlePoint).get(0);
    }

    @Override
    public List<Float> readPointList(DateTime time, double elevation, PointList pointList)
            throws InvalidDimensionValueException, IOException
    {
        int tIndex = this.findAndCheckTimeIndex(time);
        int zIndex = this.findAndCheckElevationIndex(elevation);
        return CdmUtils.readPointList(
            this.grid,
            this.getHorizontalCoordSys(),
            tIndex,
            zIndex,
            pointList,
            this.dataReadingStrategy
        );
    }

    public String getStandardName() {
        if (this.grid == null) return null;
        Attribute stdNameAtt = this.grid.findAttributeIgnoreCase("standard_name");
        if (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
        {
            return null;
        }
        return stdNameAtt.getStringValue();
    }

    public void setLayerSettings(LayerSettings layerSettings) {
        this.layerSettings = layerSettings;
    }

    /// The properties below are taken from the LayerSettings

    @Override
    public boolean isQueryable() {
        return this.layerSettings.isAllowFeatureInfo();
    }

    @Override
    public Range<Float> getApproxValueRange() {
        return this.layerSettings.getDefaultColorScaleRange();
    }

    @Override
    public boolean isLogScaling() {
        return this.layerSettings.isLogScaling();
    }

    @Override
    public ColorPalette getDefaultColorPalette() {
        return ColorPalette.get(this.layerSettings.getDefaultPaletteName());
    }

    @Override
    public int getDefaultNumColorBands() {
        return this.layerSettings.getDefaultNumColorBands();
    }

}