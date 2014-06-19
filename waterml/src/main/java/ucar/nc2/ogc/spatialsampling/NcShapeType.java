package ucar.nc2.ogc.spatialsampling;

import net.opengis.gml.x32.PointDocument;
import net.opengis.samplingSpatial.x20.ShapeType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.gml.NcPointType;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NcShapeType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint/sams:shape
    public static ShapeType initShape(ShapeType shape, StationTimeSeriesFeature stationFeat) {
        // gml:Point
        PointDocument pointDoc = PointDocument.Factory.newInstance();
        NcPointType.initPoint(pointDoc.addNewPoint(), stationFeat);
        shape.set(pointDoc);

        return shape;
    }

    private NcShapeType() { }
}
