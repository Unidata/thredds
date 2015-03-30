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

package thredds.server.wms;

import java.io.IOException;
import java.util.List;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import thredds.server.wms.config.LayerSettings;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * Wraps an existing {@link VectorLayer}, ensuring that the layer's {@link #getId() id}
 * and {@link #getName()} name are the same.   In THREDDS there is one
 * Capabilities document per ThreddsDataset, therefore the layer id
 * (unique with a dataset) and the layer name (unique within a Capabilities
 * document) - are always the same.
 * @author Jon
 */
class ThreddsVectorLayer implements VectorLayer, ThreddsLayer{


	private final VectorLayer wrappedLayer;

	// Will be set in ThreddsWmsController.ThreddsLayerFactory
	private LayerSettings layerSettings;

	public ThreddsVectorLayer(VectorLayer wrappedLayer) {
		this.wrappedLayer = wrappedLayer;
	}

	/** The layer's name is also its id in THREDDS */
	public String getName() {
		return this.getId();
	}

	/**
	 * Returns the standard name of this vector layer.  Vector layers are detected
	 * based on the standard names of the underlying scalar layers, and the
	 * id is set to the standard name upon creation.
	 * @todo logic is somewhat brittle and dependent upon ncWMS behaviour, but
	 * this problem will hopefully be solved when we move to a unified underlying
	 * library that provides access to standard names.
	 */
	public String getStandardName() {
		return this.getId();
	}

	public ScalarLayer getXComponent() { 
		return this.wrappedLayer.getXComponent();
	}

	public ScalarLayer getYComponent() {
		return this.wrappedLayer.getYComponent();
	}


	public List<Float>[] readXYComponents(DateTime dateTime, double elevation, RegularGrid grid) throws InvalidDimensionValueException, IOException, FactoryException, TransformException{

		return this.wrappedLayer.readXYComponents(dateTime, elevation, grid);
	}    


	public ThreddsDataset getDataset() {
		return (ThreddsDataset)this.wrappedLayer.getDataset();
	}

	public String getId() {
		return this.wrappedLayer.getId();
	}

	public String getTitle() {
		return this.wrappedLayer.getTitle();
	}

	public String getLayerAbstract() {
		return this.wrappedLayer.getLayerAbstract();
	}

	public String getUnits() {
		return this.wrappedLayer.getUnits();
	}

	public GeographicBoundingBox getGeographicBoundingBox() {
		return this.wrappedLayer.getGeographicBoundingBox();
	}

	public HorizontalGrid getHorizontalGrid() {
		return this.wrappedLayer.getHorizontalGrid();
	}

	public Chronology getChronology() {
		return this.wrappedLayer.getChronology();
	}

	public List<DateTime> getTimeValues() {
		return this.wrappedLayer.getTimeValues();
	}

	public DateTime getCurrentTimeValue() {
		return this.wrappedLayer.getCurrentTimeValue();
	}

	public DateTime getDefaultTimeValue() {
		return this.wrappedLayer.getDefaultTimeValue();
	}

	public List<Double> getElevationValues() {
		return this.wrappedLayer.getElevationValues();
	}

	public double getDefaultElevationValue() {
		return this.wrappedLayer.getDefaultElevationValue();
	}

	public String getElevationUnits() {
		return this.wrappedLayer.getElevationUnits();
	}

	public boolean isElevationPositive() {
		return this.wrappedLayer.isElevationPositive();
	}

	public boolean isElevationPressure() { 
		return this.wrappedLayer.isElevationPressure(); 
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
	public boolean isIntervalTime()
	{
		return this.layerSettings.isIntervalTime();
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
