package ucar.nc2.ogc.spatialsampling;

import net.opengis.gml.v_3_2_1.PointType;
import net.opengis.spatialsampling.v_2_0_0.Shape;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.gml.NC_PointType;

import javax.xml.bind.JAXBElement;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_Shape {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape
    public static Shape createShape(StationTimeSeriesFeature stationFeat) {
        Shape shape = Factories.SPATIALSAMPLING.createShape();

        PointType point = NC_PointType.createPoint(stationFeat);
        JAXBElement<PointType> pointElem = Factories.GML.createPoint(point);
        shape.setAbstractGeometry(pointElem);

        return shape;
    }

    private NC_Shape() { }
}
