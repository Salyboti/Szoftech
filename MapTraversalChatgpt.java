package org.openmetromaps.maps;

import org.openmetromaps.maps.model.*;

import java.util.*;

public class MapTraversal {

    public enum MapTraversalLimitType {
        TRANSFER_LIMIT,
        STOP_LIMIT,
        TIME_LIMIT
    }

    public static List<Station> traverseMap(ModelData model, Station src, MapTraversalLimitType limitType, int limit) {
        switch (limitType) {
            case TRANSFER_LIMIT:
                return traverseMapWithTransferLimit(model, src, limit);
            case STOP_LIMIT:
                return traverseMapWithStopLimit(model, src, limit);
            case TIME_LIMIT:
                return traverseMapWithTimeLimit(model, src, limit);
            default:
                throw new IllegalArgumentException("Invalid limit type");
        }
    }

    private static List<Station> traverseMapWithTransferLimit(ModelData model, Station src, int maxTransfers) {
        Set<Station> reachableStations = new LinkedHashSet<>();
        Queue<Station> queue = new LinkedList<>();
        Map<Station, Integer> transfers = new HashMap<>();

        queue.add(src);
        transfers.put(src, 0);
        reachableStations.add(src);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            int currentTransfers = transfers.get(current);

            for (Stop stop : current.getStops()) {
                Line line = stop.getLine();
                for (Stop lineStop : line.getStops()) {
                    Station neighbor = lineStop.getStation();
                    int newTransfers = currentTransfers;

                    // Átszállás számítás, ha más vonalra lépünk
                    if (!stop.getLine().equals(lineStop.getLine())) {
                        newTransfers++;
                    }

                    if (!transfers.containsKey(neighbor) || newTransfers < transfers.get(neighbor)) {
                        if (newTransfers <= maxTransfers) {
                            reachableStations.add(neighbor);
                            queue.add(neighbor);
                            transfers.put(neighbor, newTransfers);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(reachableStations);
    }

    private static List<Station> traverseMapWithStopLimit(ModelData model, Station src, int maxStops) {
        Set<Station> reachableStations = new LinkedHashSet<>();
        Queue<Station> queue = new LinkedList<>();
        Map<Station, Integer> stops = new HashMap<>();

        queue.add(src);
        stops.put(src, 0);
        reachableStations.add(src);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            int currentStops = stops.get(current);

            if (currentStops < maxStops) {
                for (Stop stop : current.getStops()) {
                    List<Stop> lineStops = stop.getLine().getStops();
                    int index = lineStops.indexOf(stop);

                    // Check previous station
                    if (index > 0) {
                        Stop prevStop = lineStops.get(index - 1);
                        Station prevStation = prevStop.getStation();
                        if (!stops.containsKey(prevStation)) {
                            reachableStations.add(prevStation);
                            queue.add(prevStation);
                            stops.put(prevStation, currentStops + 1);
                        }
                    }

                    // Check next station
                    if (index < lineStops.size() - 1) {
                        Stop nextStop = lineStops.get(index + 1);
                        Station nextStation = nextStop.getStation();
                        if (!stops.containsKey(nextStation)) {
                            reachableStations.add(nextStation);
                            queue.add(nextStation);
                            stops.put(nextStation, currentStops + 1);
                        }
                    }
                }
            }
        }
return new ArrayList<>(reachableStations);
    }

    private static List<Station> traverseMapWithTimeLimit(ModelData model, Station src, int maxTime) {
        Set<Station> reachableStations = new LinkedHashSet<>();
        Queue<Station> queue = new LinkedList<>();
        Map<Station, Double> timeSpent = new HashMap<>();
    
        queue.add(src);
        timeSpent.put(src, 0.0);
        reachableStations.add(src);
    
        while (!queue.isEmpty()) {
            Station current = queue.poll();
            double currentTime = timeSpent.get(current);
    
            for (Stop stop : current.getStops()) {
                List<Stop> lineStops = stop.getLine().getStops();
                int index = lineStops.indexOf(stop);

                // Check previous station
                if (index > 0) {
                    Station prevStation = lineStops.get(index - 1).getStation();
                    handleTimeLimitTraversal(current, prevStation, currentTime, maxTime, reachableStations, queue, timeSpent);
                }

                // Check next station
                if (index < lineStops.size() - 1) {
                    Station nextStation = lineStops.get(index + 1).getStation();
                    handleTimeLimitTraversal(current, nextStation, currentTime, maxTime, reachableStations, queue, timeSpent);
                }
            }
        }
    
        return new ArrayList<>(reachableStations);
    }

    private static void handleTimeLimitTraversal(
            Station current, Station neighbor, double currentTime, int maxTime,
            Set<Station> reachableStations, Queue<Station> queue, Map<Station, Double> timeSpent) {
        
        double timeRequired = calculateTravelTime(current.getLocation(), neighbor.getLocation());
        if (!timeSpent.containsKey(neighbor) && currentTime + timeRequired <= maxTime) {
            reachableStations.add(neighbor);
            queue.add(neighbor);
            timeSpent.put(neighbor, currentTime + timeRequired);
        }
    }

    private static double calculateTravelTime(Coordinate from, Coordinate to) {
        double lat1 = from.getLatitude();
        double lon1 = from.getLongitude();
        double lat2 = to.getLatitude();
        double lon2 = to.getLongitude();
    
        // Haversine formula a pontos távolság számításához
        double earthRadius = 6371.0; // Kilométerben
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
    
        // Átalakítás percekké (40 km/h sebesség mellett)
        return (distance / 40) * 60; // idő percben
    }
}
