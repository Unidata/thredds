/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.grid;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.gis.GridBoundariesExtractor;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Parameter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * A helper class to GridDataset; creates a GridDataset XML document.
 * This is a candidate for the XML representation of the Grid SDT.
 * Used to create form for NetcdfSubsetService for Grids.
 *
 * @author caron
 */
public class GridDatasetInfo {
	private ucar.nc2.dt.GridDataset gds;
	private String path;

	public GridDatasetInfo(ucar.nc2.dt.GridDataset gds, String path) {
		this.gds = gds;
		this.path = path;
	}

	/**
	 * Write the information as an XML document
	 *
	 * @param doc write XML for this Document
	 * @return String output
	 */
	public String writeXML(Document doc) {
		XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
		return fmt.outputString(doc);
	}

	/**
	 * Write the information as an XML document
	 *
	 * @param doc write XML for this Document
	 * @param os  write to this output stream
	 * @throws java.io.IOException on write error
	 */
	public void writeXML(Document doc, OutputStream os) throws IOException {
		XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
		fmt.output(doc, os);
	}

	/**
	 * Create the "Dataset Description" XML document from this GridDataset
	 *
	 * @return a JDOM Document
	 */
	public Document makeDatasetDescription() {
		Element rootElem = new Element("gridDataset");
		Document doc = new Document(rootElem);
		rootElem.setAttribute("location", gds.getLocationURI());
		if (null != path)
			rootElem.setAttribute("path", path);

		/* dimensions
    List dims = getDimensions(gds);
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = (Dimension) dims.get(j);
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeDimension(dim, null));
    } */

		// coordinate axes
		for (CoordinateAxis axis : getCoordAxes(gds)) {
			rootElem.addContent(writeAxis(axis));
		}

		/* grids
    List grids = gds.getGrids();
    Collections.sort(grids, new GridComparator());
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      rootElem.addContent(writeGrid(grid));
    } */

		/* coordinate systems
    List gridSets = gds.getGridsets();
    for (int i = 0; i < gridSets.size(); i++) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) gridSets.get(i);
      rootElem.addContent(writeCoordSys(gridset.getGeoCoordSystem()));
    } */

		// gridSets
		List<GridDataset.Gridset> gridSets = gds.getGridsets();
		Collections.sort(gridSets, new GridSetComparator());
		for (GridDataset.Gridset gridset : gridSets) {
			rootElem.addContent(writeGridSet(gridset));
		}

		// coordinate transforms
		for (CoordinateTransform ct : getCoordTransforms(gds)) {
			rootElem.addContent(writeCoordTransform(ct));
		}

		/* global attributes
    Iterator atts = gds.getGlobalAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    } */

		// add lat/lon bounding box
		LatLonRect bb = gds.getBoundingBox();
		if (bb != null)
			rootElem.addContent(writeBoundingBox(bb));

		// add date range
		CalendarDate start = gds.getCalendarDateStart();
		CalendarDate end = gds.getCalendarDateEnd();
		if ((start != null) && (end != null)) {
			Element dateRange = new Element("TimeSpan");
			dateRange.addContent(new Element("begin").addContent(start.toString()));
			dateRange.addContent(new Element("end").addContent(end.toString()));
			rootElem.addContent(dateRange);
		}

		// add accept list
		addAcceptList(rootElem);

		//    elem.addContent(new Element("accept").addContent("xml"));
		//    elem.addContent(new Element("accept").addContent("csv"));
		//    elem.addContent(new Element("accept").addContent("netcdf"));    
		return doc;
	}

	/**
	 * Returns a WKT polygon with the dataset boundaries 
	 * 
	 * @return  WKT string
	 */
	public String getDatasetBoundariesWKT(){
		
		return GridBoundariesExtractor.valueOf(gds).getDatasetBoundariesWKT();
	}

  
	/**
	 * Create the "Grid Form" XML document from this GridDataset.
	 * Used to create the Grid HTML form, cause I dont know XSLT
	 *
	 * @return the JDOM Document
	 */
	public Document makeGridForm() {
		Element rootElem = new Element("gridForm");
		Document doc = new Document(rootElem);
		rootElem.setAttribute("location", gds.getLocationURI());
		if (null != path)
			rootElem.setAttribute("path", path);

		// its all about grids
		List<GridDatatype> grids = gds.getGrids();
		Collections.sort(grids, new GridComparator()); // sort by time axis, vert axis, grid name


		CoordinateAxis currentTime = null;
		CoordinateAxis currentVert = null;
		Element timeElem = null;
		Element vertElem = null;
		boolean newTime;

		for (int i = 0; i < grids.size(); i++) {
			GeoGrid grid = (GeoGrid) grids.get(i);
			GridCoordSystem gcs = grid.getCoordinateSystem();


			CoordinateAxis time = gcs.getTimeAxis();
			CoordinateAxis vert = gcs.getVerticalAxis();

			/* System.out.println(" grid "+grid.getName()
              +" time="+(time == null ? " null" : time.hashCode())
              +" vert="+(vert == null ? " null" : vert.hashCode())); */

			//Assuming all variables in dataset has ensemble dim if one has
			if(i==0){
				CoordinateAxis1D ens = gcs.getEnsembleAxis();
				if(ens != null){
					Element ensAxisEl = writeAxis2(ens, "ensemble");    		  
					rootElem.addContent(ensAxisEl);
				}

			}

			if ((i == 0) || !compareAxis(time, currentTime)) {
				timeElem = new Element("timeSet");
				rootElem.addContent(timeElem);
				Element timeAxisElement = writeAxis2(time, "time");
				if (timeAxisElement != null)
					timeElem.addContent(timeAxisElement);
				currentTime = time;
				newTime = true;
			} else {
				newTime = false;
			}

			if (newTime || !compareAxis(vert, currentVert)) {
				vertElem = new Element("vertSet");
				timeElem.addContent(vertElem);
				Element vertAxisElement = writeAxis2(vert, "vert");
				if (vertAxisElement != null)
					vertElem.addContent(vertAxisElement);
				currentVert = vert;
			}

			vertElem.addContent(writeGrid(grid));
		}

		// add lat/lon bounding box
		LatLonRect bb = gds.getBoundingBox();
		if (bb != null)
			rootElem.addContent(writeBoundingBox(bb));

		// add projected bounding box
		//--> Asuming all gridSets have the same coordinates and bbox
		ProjectionRect rect = grids.get(0).getCoordinateSystem().getBoundingBox();

		Element projBBOX =new Element("projectionBox");
		Element minx = new Element("minx");
		minx.addContent( Double.valueOf(rect.getMinX()).toString() );
		projBBOX.addContent(minx);
		Element maxx = new Element("maxx");
		maxx.addContent( Double.valueOf(rect.getMaxX()).toString() );
		projBBOX.addContent(maxx);
		Element miny = new Element("miny");
		miny.addContent( Double.valueOf(rect.getMinY()).toString() );
		projBBOX.addContent(miny);
		Element maxy = new Element("maxy");    
		maxy.addContent( Double.valueOf(rect.getMaxY()).toString() );
		projBBOX.addContent(maxy);    

		rootElem.addContent(projBBOX);

		// add date range
		CalendarDate start = gds.getCalendarDateStart();
		CalendarDate end = gds.getCalendarDateEnd();
		if ((start != null) && (end != null)) {
			Element dateRange = new Element("TimeSpan");
			dateRange.addContent(new Element("begin").addContent(start.toString()));
			dateRange.addContent(new Element("end").addContent(end.toString()));
			rootElem.addContent(dateRange);
		}

		// add accept list
		addAcceptList(rootElem);

		//    Element elem = new Element("AcceptList");
		//    elem.addContent(new Element("accept").addContent("xml"));
		//    elem.addContent(new Element("accept").addContent("csv"));
		//    elem.addContent(new Element("accept").addContent("netcdf"));
		//    rootElem.addContent(elem);
		return doc;
	}

	private void addAcceptList(Element rootElement){

		// add accept list
		Element elem = new Element("AcceptList");
		//accept list for Grid As Point requests
		Element gridAsPoint = new Element("GridAsPoint");

        // LOOK this is wrong - should be using SupportedOperation class or something
		gridAsPoint.addContent(new Element("accept").addContent("xml").setAttribute("displayName", "xml") );
		gridAsPoint.addContent(new Element("accept").addContent("xml_file").setAttribute("displayName", "xml (file)"));
		gridAsPoint.addContent(new Element("accept").addContent("csv").setAttribute("displayName", "csv"));
		gridAsPoint.addContent(new Element("accept").addContent("csv_file").setAttribute("displayName", "csv (file)"));
		gridAsPoint.addContent(new Element("accept").addContent("geocsv").setAttribute("displayName", "geocsv"));
		gridAsPoint.addContent(new Element("accept").addContent("geocsv_file").setAttribute("displayName", "geocsv (file)"));
		gridAsPoint.addContent(new Element("accept").addContent("netcdf").setAttribute("displayName", "netcdf"));

		//accept list for Grid requests
		Element grids = new Element("Grid");
		grids.addContent(new Element("accept").addContent("netcdf").setAttribute("displayName", "netcdf"));
		//Check if netcdf4 is available
		//	    try{
		//	    	if( Nc4Iosp.isClibraryPresent() ){
		//	    		grids.addContent(new Element("accept").addContent("netcdf4"));	    		
		//	    		gridAsPoint.addContent(new Element("accept").addContent("netcdf4"));
		//	    	}    
		//	    }catch(UnsatisfiedLinkError e){
		//	    	//Log this in threddsServlet.log ??
		//	    }

		elem.addContent(gridAsPoint);
		elem.addContent(grids);	  
		rootElement.addContent(elem);
	}

	private Element writeAxis2(CoordinateAxis axis, String name) {
		if (axis == null) return null;

		Element varElem = new Element(name);
		varElem.setAttribute("name", axis.getFullName());
		varElem.setAttribute("shape", getShapeString(axis.getShape())); // axis.getDimensionsString());

		DataType dt = axis.getDataType();
		varElem.setAttribute("type", dt.toString());

		AxisType axisType = axis.getAxisType();
		if (null != axisType)
			varElem.setAttribute("axisType", axisType.toString());

		// attributes
		for (Attribute att : axis.getAttributes())
			varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));

		Element values = ucar.nc2.ncml.NcMLWriter.writeValues(axis, null, false);
		values.setAttribute("npts", Long.toString(axis.getSize()));
		varElem.addContent(values);

		return varElem;
	}

	private boolean compareAxis(CoordinateAxis axis1, CoordinateAxis axis2) {
		if (axis1 == axis2)
			return true;

		if (axis1 == null) return false;
		if (axis2 == null) return false;

		return axis1.equals(axis2);
	}

	// sort by time, then vert, then name
	private static class GridComparator implements Comparator<GridDatatype> {

		// Returns a -1, 0, 1 if the first argument is less than, equal to, or greater than the second.
		public int compare(GridDatatype grid1, GridDatatype grid2) {
			GridCoordSystem gcs1 = grid1.getCoordinateSystem();
			GridCoordSystem gcs2 = grid2.getCoordinateSystem();

			CoordinateAxis time1 = gcs1.getTimeAxis();
			CoordinateAxis time2 = gcs2.getTimeAxis();
			int ret = compareAxis(time1, time2);
			if (ret != 0) return ret;

			CoordinateAxis vert1 = gcs1.getVerticalAxis();
			CoordinateAxis vert2 = gcs2.getVerticalAxis();
			ret = compareAxis(vert1, vert2);
			if (ret != 0) return ret;

			return grid1.getFullName().compareTo(grid2.getFullName());
		}

		private int compareAxis(CoordinateAxis axis1, CoordinateAxis axis2) {
			if (axis1 == axis2)
				return 0;

			if (axis1 == null) return -1;
			if (axis2 == null) return 1;

			return axis1.getFullName().compareTo(axis2.getFullName());
		}

	}

	private List<CoordinateAxis> getCoordAxes(ucar.nc2.dt.GridDataset gds) {
		Set<CoordinateAxis> axesHash = new HashSet<>();

		for (ucar.nc2.dt.GridDataset.Gridset gridset : gds.getGridsets()) {
			GridCoordSystem gcs = gridset.getGeoCoordSystem();
			for (CoordinateAxis axe : gcs.getCoordinateAxes())
				axesHash.add(axe);
		}

		List<CoordinateAxis> list = Arrays.asList((CoordinateAxis[]) axesHash.toArray( new CoordinateAxis[ axesHash.size()]));
		Collections.sort(list);
		return list;
	}

	private List<CoordinateTransform> getCoordTransforms(ucar.nc2.dt.GridDataset gds) {
		Set<CoordinateTransform> ctHash = new HashSet<>();

		for (ucar.nc2.dt.GridDataset.Gridset gridset : gds.getGridsets()) {
			GridCoordSystem gcs = gridset.getGeoCoordSystem();
			for (CoordinateTransform axe : gcs.getCoordinateTransforms())
				ctHash.add(axe);
		}

		List<CoordinateTransform> list = Arrays.asList((CoordinateTransform[]) ctHash.toArray( new CoordinateTransform[ ctHash.size()]));
		Collections.sort(list);
		return list;
	}

	/* private List getDimensions(ucar.nc2.dt.GridDataset gds) {
    HashSet dimHash = new HashSet();
    List grids = gds.getGrids();
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      List dims = grid.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        dimHash.add(dim);
      }
    }
    List list = Arrays.asList(dimHash.toArray());
    Collections.sort(list);
    return list;
  }  */

	private Element writeAxis(CoordinateAxis axis) {

		Element varElem = new Element("axis");
		varElem.setAttribute("name", axis.getFullName());
		varElem.setAttribute("shape", getShapeString(axis.getShape())); // axis.getDimensionsString());

		DataType dt = axis.getDataType();
		varElem.setAttribute("type", dt.toString());

		AxisType axisType = axis.getAxisType();
		if (null != axisType)
			varElem.setAttribute("axisType", axisType.toString());

		// attributes
		for (Attribute att : axis.getAttributes()) {
			varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
		}

		if (axis.getRank() == 1) {
			Element values = ucar.nc2.ncml.NcMLWriter.writeValues(axis, null, true);
			//values.setAttribute("npts", Long.toString(axis.getSize()));
			varElem.addContent(values);
		}

		return varElem;
	}

	// display name plus the dimensions
	private String getShapeString(int[] shape) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < shape.length; i++) {
			if (i != 0) buf.append(" ");
			buf.append(shape[i]);
		}
		return buf.toString();
	}


	private Element writeBoundingBox(LatLonRect bb) {

		Element bbElem = new Element("LatLonBox");
		//LatLonPoint llpt = bb.getLowerLeftPoint();
		//LatLonPoint urpt = bb.getUpperRightPoint();

		//bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(llpt.getLongitude(), 4)));
		bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMin() , 4)));
		//bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(urpt.getLongitude(), 4)));
		bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMax(), 4)));
		//bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(llpt.getLatitude(), 4)));
		bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMin(), 4)));
		//bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(urpt.getLatitude(), 4)));
		bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMax(), 4)));

		return bbElem;

	}

	private Element writeGridSet(GridDataset.Gridset gridset) {
		Element csElem = new Element("gridSet");
		GridCoordSystem cs = gridset.getGeoCoordSystem();          
		csElem.setAttribute("name", cs.getName());

		ProjectionRect rect = cs.getBoundingBox();
		Element projBBOX =new Element("projectionBox");
		Element minx = new Element("minx");
		minx.addContent( Double.valueOf(rect.getMinX()).toString() );
		projBBOX.addContent(minx);
		Element maxx = new Element("maxx");
		maxx.addContent( Double.valueOf(rect.getMaxX()).toString() );
		projBBOX.addContent(maxx);
		Element miny = new Element("miny");
		miny.addContent( Double.valueOf(rect.getMinY()).toString() );
		projBBOX.addContent(miny);
		Element maxy = new Element("maxy");    
		maxy.addContent( Double.valueOf(rect.getMaxY()).toString() );
		projBBOX.addContent(maxy);

		csElem.addContent(projBBOX);

		for (CoordinateAxis axis : cs.getCoordinateAxes()) {
			Element axisElem = new Element("axisRef");
			axisElem.setAttribute("name", axis.getFullName());
			csElem.addContent(axisElem);
		}

		for (CoordinateTransform ct : cs.getCoordinateTransforms()) {
			Element elem = new Element("coordTransRef");
			elem.setAttribute("name", ct.getName());
			csElem.addContent(elem);
		}

		List<GridDatatype> grids = gridset.getGrids();
		Collections.sort(grids);
		for (GridDatatype grid : grids) {
			csElem.addContent(writeGrid(grid));
		}

		return csElem;
	}

	/* private Element writeCoordSys(GridCoordSystem cs) {
    Element csElem = new Element("coordSys");
    csElem.setAttribute("name", cs.getName());
    List axes = cs.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axisRef");
      axisElem.setAttribute("name", axis.getName());
      csElem.addContent(axisElem);
    }
    List cts = cs.getCoordinateTransforms();
    for (int j = 0; j < cts.size(); j++) {
      CoordinateTransform ct = (CoordinateTransform) cts.get(j);
      Element elem = new Element("coordTransRef");
      elem.setAttribute("name", ct.getName());
      csElem.addContent(elem);
    }
    return csElem;
  } */

	private Element writeCoordTransform(CoordinateTransform ct) {
		Element ctElem = new Element("coordTransform");
		ctElem.setAttribute("name", ct.getName());
		ctElem.setAttribute("transformType", ct.getTransformType().toString());
		for (Parameter param : ct.getParameters()) {
			Element pElem = new Element("parameter");
			pElem.setAttribute("name", param.getName());
			pElem.setAttribute("value", param.getStringValue());
			ctElem.addContent(pElem);
		}
		return ctElem;
	}

	private Element writeGrid(GridDatatype grid) {
		Element varElem = new Element("grid");
		varElem.setAttribute("name", grid.getFullName());

        String desc = grid.getDescription() != null ? grid.getDescription() : "No description";
        varElem.setAttribute("desc", desc);

		StringBuilder buff = new StringBuilder();
		List dims = grid.getDimensions();
		for (int i = 0; i < dims.size(); i++) {
			Dimension dim = (Dimension) dims.get(i);
			if (i > 0) buff.append(" ");
			if (dim.isShared())
				buff.append(dim.getShortName());
			else
				buff.append(dim.getLength());
		}
		if (buff.length() > 0)
			varElem.setAttribute("shape", buff.toString());

		DataType dt = grid.getDataType();
		if (dt != null)
			varElem.setAttribute("type", dt.toString());

		//GridCoordSystem cs = grid.getCoordinateSystem();
		//varElem.setAttribute("coordSys", cs.getName());

		// attributes
		for (ucar.nc2.Attribute att : grid.getAttributes()) {
			varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
		}

		return varElem;
	}

	// sort by domain size, then name
	private static class GridSetComparator implements Comparator<GridDataset.Gridset> {

		public int compare(GridDataset.Gridset gridset1, GridDataset.Gridset gridset2) {
			GridCoordSystem cs1 = gridset1.getGeoCoordSystem();
			GridCoordSystem cs2 = gridset2.getGeoCoordSystem();
			if (cs1.getDomain().size() != cs2.getDomain().size())
				return cs1.getDomain().size() - cs2.getDomain().size();
			return cs1.getName().compareTo(cs2.getName());
		}
	}


	/**
	 * debug
	 *
	 * @param args ignored
	 * @throws java.io.IOException on io error
	 */
	public static void main(String args[]) throws IOException {
		String url = "cdmremote:http://localhost:8080/thredds/cdmremote/grib/NCDC/CFSR/NCDC-CFSR/PGB-LatLon0p5";

		GridDataset ncd = ucar.nc2.dt.grid.GridDataset.open(url);
		GridDatasetInfo info = new GridDatasetInfo(ncd, null);
		FileOutputStream fos2 = new FileOutputStream("C:/tmp2/gridInfo.xml");
		info.writeXML(info.makeGridForm(), fos2);
		fos2.close();

		String infoString = info.writeXML(info.makeGridForm());
		System.out.println(infoString);
	}

}

