package thredds.server.radarServer2;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.unidata.geoloc.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by rmay on 12/9/14.
 */

@XmlRootElement(name = "stationsList")
public class StationList {

    @XmlRootElement(name = "station")
    public static class Station {
        private LatLonPointImmutable location;

        private String stid;
        private String name;
        private String state;
        private String country;
        private double elevation;

        public Station() {
            this.stid = "UNK";
            this.name = "Unnamed";
        }

        public Station(String stid, LatLonPoint loc) {
            this.stid = stid;
            this.location = new LatLonPointImmutable(loc);
            this.name = "Unnamed";
        }

        @XmlAttribute(name = "id")
        @XmlID
        String getStid() {
            return stid;
        }
        void setStid(String stid) {
            this.stid = stid;
        }

        LatLonPoint getLocation() {
            return location;
        }
        void setLocation(LatLonPointImmutable loc) {
            this.location = loc;
        }

        @XmlElement(name = "longitude")
        double getLongitude() {
            return location.getLongitude();
        }

        @XmlElement(name = "latitude")
        double getLatitude() {
            return location.getLatitude();
        }

        @XmlElement(name = "elevation")
        double getElevation() {
            return elevation;
        }
        void setElevation(double elev) {
            this.elevation = elev;
        }

        @XmlElement(name = "name")
        String getName() {
            return name;
        }
        void setName(String name) {
            this.name = name;
        }

        @XmlAttribute(name = "state")
        String getState() {
            return this.state;
        }
        void setState(String state) {
            this.state = state;
        }

        @XmlAttribute(name = "country")
        String getCountry() {
            return this.country;
        }
        void setCountry(String country) {
            this.country = country;
        }
    }

    private Map<String, Station> stations;

    public StationList() {
        stations = new TreeMap<>();
    }

    public void loadFromXmlFile(String filename) {
        SAXBuilder builder = new SAXBuilder();
        File f = new File(filename);
        try {
            Document doc = builder.build(f);
            Element list = doc.getRootElement();
            for (Element station: list.getChildren()) {
                Element loc = station.getChild("location3D");
                String stid = station.getAttributeValue("value");
                char leader = stid.charAt(0);
                if (stid.length() == 4 && (leader == 'K' || leader == 'T')) {
                    stid = stid.substring(1, 4);
                }
                Station newStation = addStation(stid,
                        new LatLonPointImpl(
                                Double.valueOf(loc.getAttributeValue("latitude")),
                                Double.valueOf(loc.getAttributeValue("longitude"))));
                newStation.setElevation(Double.valueOf(loc.getAttributeValue("elevation")));
                newStation.setName(station.getAttributeValue("name"));
                newStation.setState(station.getAttributeValue("state"));
                newStation.setCountry(station.getAttributeValue("country"));
            }
        } catch (IOException|JDOMException e) {
            e.printStackTrace();
        }
    }

    public Station addStation(String stid, LatLonPoint loc) {
        Station added = new Station(stid, new LatLonPointImmutable(loc));
        stations.put(stid, added);
        return added;
    }

    public Station getNearest(double longitude, double latitude) {
        LatLonPointImmutable pt = new LatLonPointImmutable(latitude, longitude);
        Bearing b = new Bearing();
        Station nearest = null;
        double minDist = Double.POSITIVE_INFINITY;
        for (Station s: stations.values()) {
            Bearing.calculateBearing(pt, s.location, b);
            if (b.getDistance() < minDist) {
                minDist = b.getDistance();
                nearest = s;
            }
        }
        return nearest;
    }

    public List<Station> getStations(double east, double west, double north,
                                     double south) {
        LatLonRect rect = new LatLonRect(new LatLonPointImmutable(south, west),
                new LatLonPointImmutable(north, east));

        List<Station> result = new ArrayList<>();

        for (Station s: stations.values())
            if (rect.contains(s.location))
                result.add(s);

        return result;
    }

    @XmlElement(name = "station")
    public Collection<Station> getAll() {
        return stations.values();
    }

    public Station get(String stid) {
        return stations.get(stid);
    }
}