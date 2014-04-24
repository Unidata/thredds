package ucar.nc2.ogc2.spatialsampling;

import net.opengis.gml.x32.PointDocument;
import net.opengis.gml.x32.PointType;
import net.opengis.samplingSpatial.x20.ShapeType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc2.gml.NC_PointType;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_Shape {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape
    public static ShapeType createShape(StationTimeSeriesFeature stationFeat) {
        ShapeType shape = ShapeType.Factory.newInstance();

        PointType point = NC_PointType.createPoint(stationFeat);
        shape.setAbstractGeometry(point);

        return shape;
    }

    public static ShapeType initShape(ShapeType shape, StationTimeSeriesFeature stationFeat) {
        PointDocument pointDoc = PointDocument.Factory.newInstance();

        NC_PointType.initPoint(pointDoc.addNewPoint(), stationFeat);

        shape.set(pointDoc);
        return shape;
    }

    private NC_Shape() { }
}
