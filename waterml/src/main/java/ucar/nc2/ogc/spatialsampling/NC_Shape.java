package ucar.nc2.ogc.spatialsampling;

import net.opengis.gml.v_3_2_1.PointType;
import net.opengis.spatialsampling.v_2_0_0.Shape;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.gml.NC_PointType;

import javax.xml.bind.JAXBElement;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape
 *
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_Shape {
    private final static net.opengis.spatialsampling.v_2_0_0.ObjectFactory spatialSamplingObjectFactory =
            new net.opengis.spatialsampling.v_2_0_0.ObjectFactory();
    private final static net.opengis.gml.v_3_2_1.ObjectFactory gmlObjectFactory =
            new net.opengis.gml.v_3_2_1.ObjectFactory();

    public static Shape createSpatialSamplingFeatureShape(StationTimeSeriesFeature stationFeat) {
        Shape shape = spatialSamplingObjectFactory.createShape();

        PointType point = NC_PointType.createShapePoint(stationFeat);
        JAXBElement<PointType> pointElem = gmlObjectFactory.createPoint(point);
        shape.setAbstractGeometry(pointElem);

        return shape;
    }

    private NC_Shape() { }
}
