package ucar.nc2.ogc;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class Factories {
    public final static net.opengis.gml.v_3_2_1.ObjectFactory GML =
            new net.opengis.gml.v_3_2_1.ObjectFactory();

    public static final net.opengis.om.v_2_0_0.ObjectFactory OM =
            new net.opengis.om.v_2_0_0.ObjectFactory();

    public static final net.opengis.swe.v_2_0_0.ObjectFactory SWE =
            new net.opengis.swe.v_2_0_0.ObjectFactory();

    public static final net.opengis.spatialsampling.v_2_0_0.ObjectFactory SPATIALSAMPLING =
            new net.opengis.spatialsampling.v_2_0_0.ObjectFactory();

    public final static net.opengis.waterml.v_2_0_1.ObjectFactory WATERML =
            new net.opengis.waterml.v_2_0_1.ObjectFactory();

    private Factories() { }
}
