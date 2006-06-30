Location {
  double lat, lon, elev;
}

Observation {
  Location loc
  Date time
  StructureData data
}

ObsDataset
  List<Obs> obs;
}

Trajectory extends List<Obs> {
  -> connected in SpaceTime
  -> general case is that each obs has a location and time
  -> time ordered (?)
}

TrajectoryDataset
  List<Trajectory> trajectories
}

---------------------------------
StationDataset specializes by having named, fixed Location

Station extends Location {
  String name
}

StationObs extends Observation {
  Station station
}

StationObsDataset {
  List<Station> getStations
  List<Station> getStations( boundingBox)
  List<StationObs> get(station)
  List<StationObs> get(station, timeRange)
  List<StationObs> get(boundingBox, timeRange)
}

--------------------------------------

Sounding {
  Trajectory
  Station
}

SoundingDataset extends StationObsDataset {
  List<Trajectory> getAllForStation()
}

---------------------------------------------

ShipSoundingDataset {
  List<Trajectory> time-ordered, connected
}


