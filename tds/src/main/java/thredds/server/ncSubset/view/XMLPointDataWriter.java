package thredds.server.ncSubset.view;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

class XMLPointDataWriter implements PointDataWriter {

	static private Logger log = LoggerFactory.getLogger(XMLPointDataWriter.class);

	private XMLStreamWriter xmlStreamWriter;

	private XMLPointDataWriter(OutputStream os) {

		xmlStreamWriter = createXMLStreamWriter(os);
	}	
	
	public static XMLPointDataWriter createXMLPointDataWriter(OutputStream os){
		return new XMLPointDataWriter(os);
	}
	
	//@Override
	//public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point, CoordinateAxis1D zAxis) {
	public boolean header(Map<String, List<String>> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point) {
		boolean headerWritten = false;
		try {
			xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
			xmlStreamWriter.writeStartElement("grid");
			xmlStreamWriter.writeAttribute("dataset",
					gridDataset.getLocationURI());
			headerWritten = true;
		} catch (XMLStreamException xse) {
			log.error("Error writting xml header", xse);
		}

		return headerWritten;
	}
	
	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel) {
		
		boolean allDone = true;
		
		List<String> keys =new ArrayList<String>(groupedVars.keySet());
		//loop over variable groups
		for(String key : keys){
			//get wanted vertCoords for group (all if vertCoord==null just one otherwise)
			List<String> varsGroup = groupedVars.get(key);
			GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset,	varsGroup);			
			CoordinateAxis1D verticalAxisForGroup = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getVerticalAxis();
			if(verticalAxisForGroup ==null){
				//Read and write vars--> time, point
				allDone = allDone && write(varsGroup, gridDataset, gap, date, point);
			}else{
				//read and write time, verCoord for each variable in group
				if(targetLevel != null){
					Double vertCoord = NcssRequestUtils.getTargetLevelForVertCoord(verticalAxisForGroup, targetLevel);
					allDone = write(varsGroup, gridDataset, gap, date, point, vertCoord, verticalAxisForGroup.getUnitsString() );
				}else{//All levels
					for(Double vertCoord : verticalAxisForGroup.getCoordValues() ){
						/////Fix axis!!!!
						if(verticalAxisForGroup.getCoordValues().length ==1  )
							vertCoord =NcssRequestUtils.getTargetLevelForVertCoord(verticalAxisForGroup, vertCoord);
						
						allDone = allDone && write(varsGroup, gridDataset, gap, date, point, vertCoord, verticalAxisForGroup.getUnitsString() );
						
					}
				}				
				
			}			
			
		}
		return allDone;
	}	
	

	private boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point, Double targetLevel, String zUnits) {

		Iterator<String> itVars = vars.iterator();
		boolean pointDone=false;
		try {
			xmlStreamWriter.writeStartElement("point");
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("name", "date");
			writeDataTag( xmlStreamWriter, attributes, date.toString() );
			attributes.clear();
			int contVars = 0;
			while (itVars.hasNext()) {
				String varName = itVars.next();
				GridDatatype grid = gridDataset.findGridDatatype(varName);
								
				if (gap.hasTime(grid, date) && gap.hasVert(grid, targetLevel)) {
					GridAsPointDataset.Point p = gap.readData(grid, date, targetLevel, point.getLatitude(),	point.getLongitude());
					if (contVars == 0) {
						writeCoordinates(xmlStreamWriter, Double.valueOf(p.lat), Double.valueOf(p.lon));
						attributes.put("name", "vertCoord");
						attributes.put("units", zUnits);
						writeDataTag(xmlStreamWriter, attributes, Double.valueOf(p.z).toString());
						attributes.clear();
					}
					attributes.put("name", varName);
					attributes.put("units", grid.getUnitsString());
					writeDataTag(xmlStreamWriter, attributes, Double.valueOf(p.dataValue).toString());
					attributes.clear();

				} else {
					// write missingvalues!!!
					if (contVars == 0) {
						writeCoordinates(xmlStreamWriter, Double.valueOf(point.getLatitude()), Double.valueOf(point.getLongitude()));
					}
					attributes.put("name", varName);
					attributes.put("units", grid.getUnitsString());
					writeDataTag(xmlStreamWriter, attributes, Double.valueOf(gap.getMissingValue(grid)).toString());
					attributes.clear();
				}
				contVars++;
			}
			xmlStreamWriter.writeEndElement(); //Closes point
			pointDone = true;
		} catch (XMLStreamException xse) {
			log.error("Error writting tag point", xse);
		} catch (IOException ioe){
			log.error("Error reading point data", ioe);
		}

		return pointDone;
	}
	
	private boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point){
		
		Iterator<String> itVars = vars.iterator();
		boolean pointDone=false;
		try {
			xmlStreamWriter.writeStartElement("point");
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("name", "date");
			writeDataTag( xmlStreamWriter, attributes, date.toString() );
			attributes.clear();
			int contVars = 0;
			while (itVars.hasNext()) {
				String varName = itVars.next();
				GridDatatype grid = gridDataset.findGridDatatype(varName);

				if (gap.hasTime(grid, date) ) {
					GridAsPointDataset.Point p = gap.readData(grid, date, point.getLatitude(),	point.getLongitude());
					if (contVars == 0) {
						writeCoordinates(xmlStreamWriter, Double.valueOf(p.lat), Double.valueOf(p.lon));
						attributes.clear();
					}
					attributes.put("name", varName);
					attributes.put("units", grid.getUnitsString());
					writeDataTag(xmlStreamWriter, attributes, Double.valueOf(p.dataValue).toString());
					attributes.clear();

				} else {
					// write missingvalues!!!
					if (contVars == 0) {
						writeCoordinates(xmlStreamWriter, Double.valueOf(point.getLatitude()), Double.valueOf(point.getLongitude()));
					}
					attributes.put("name", varName);
					attributes.put("units", grid.getUnitsString());
					writeDataTag(xmlStreamWriter, attributes, Double.valueOf(gap.getMissingValue(grid)).toString());
					attributes.clear();
				}
				contVars++;
			}
			xmlStreamWriter.writeEndElement(); //Closes point
			pointDone = true;
		} catch (XMLStreamException xse) {
			log.error("Error writting tag point", xse);
		} catch (IOException ioe){
			log.error("Error reading point data", ioe);
		}

		return pointDone;
		
	}	

	@Override
	public boolean trailer() {
		boolean endDocument = false;
		try {
			xmlStreamWriter.writeEndElement(); // Closes tag grid
			xmlStreamWriter.writeEndDocument();
			endDocument = true;
		} catch (XMLStreamException xse) {
			log.error("Error writing end document", xse);
		}

		return endDocument;
	}
	
	@Override
	public HttpHeaders getResponseHeaders(){
		return new HttpHeaders();
	}	

	private XMLStreamWriter createXMLStreamWriter(OutputStream os) {

		XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
		XMLStreamWriter writer = null;
	
		try {
			writer = outputFactory.createXMLStreamWriter(os, "UTF-8");
		} catch (XMLStreamException xse) {
			log.error(xse.getMessage());
		}

		return writer;

	}

	private void writeDataTag(XMLStreamWriter writer,
			Map<String, String> attributes, String content)
			throws XMLStreamException {

		writer.writeStartElement("data");
		Set<String> attNames = attributes.keySet();

		Iterator<String> it = attNames.iterator();
		while (it.hasNext()) {
			String attName = it.next();
			writer.writeAttribute(attName, attributes.get(attName));
		}

		writer.writeCharacters(content);
		writer.writeEndElement();
	}

	private void writeCoordinates(XMLStreamWriter writer, Double lat, Double lon)
			throws XMLStreamException {

		Map<String, String> attributes = new HashMap<String, String>();
		// tag data for lat
		attributes.put("name", "lat");
		attributes.put("units", "degrees_north");
		writeDataTag(writer, attributes, lat.toString());
		attributes.clear();
		// tag data for lon
		attributes.put("name", "lon");
		attributes.put("units", "degrees_east");
		writeDataTag(writer, attributes, lon.toString());
		attributes.clear();

	}



}
