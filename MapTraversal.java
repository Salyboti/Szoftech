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
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be non-negative");
        }
        if (model == null || src == null) {
            throw new IllegalArgumentException("Model and source station must not be null");
        }

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
        Queue<Stop> queue = new LinkedList<>();
        Map<Stop, Integer> transferCount = new HashMap<>();
        Set<Stop> visitedStops = new HashSet<>();
    
        // Az induló állomás összes stopját hozzáadjuk a bejáráshoz
        for (Stop stop : src.getStops()) {
            queue.add(stop);
            transferCount.put(stop, 0);
            reachableStations.add(stop.getStation());
        }
    
        while (!queue.isEmpty()) {
            Stop currentStop = queue.poll();
            Line currentLine = currentStop.getLine();
            int currentTransfers = transferCount.get(currentStop);
    
            for (Stop lineStop : currentLine.getStops()) {
                if (!visitedStops.contains(lineStop)) {
                    visitedStops.add(lineStop);
                    reachableStations.add(lineStop.getStation());
    
                    if (lineStop.getLine().equals(currentLine)) {
                        queue.add(lineStop);
                        transferCount.put(lineStop, currentTransfers);
                    }
                }
            }
    
            // Átszállások kezelése
            if (currentTransfers < maxTransfers) {
                for (Stop neighborStop : currentStop.getStation().getStops()) {
                    if (!neighborStop.getLine().equals(currentLine) && 
                        (!transferCount.containsKey(neighborStop) || currentTransfers + 1 < transferCount.get(neighborStop))) {
                        queue.add(neighborStop);
                        transferCount.put(neighborStop, currentTransfers + 1);
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

                    if (index > 0) {
                        Stop prevStop = lineStops.get(index - 1);
                        Station prevStation = prevStop.getStation();
                        if (!stops.containsKey(prevStation)) {
                            reachableStations.add(prevStation);
                            queue.add(prevStation);
                            stops.put(prevStation, currentStops + 1);
                        }
                    }

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
        timeSpent.put(src, 1.0); // Starting station takes 1 minute to visit
        reachableStations.add(src);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            double currentTime = timeSpent.get(current);

            for (Stop stop : current.getStops()) {
                List<Stop> lineStops = stop.getLine().getStops();
                int index = lineStops.indexOf(stop);

                if (index > 0) {
                    Station prevStation = lineStops.get(index - 1).getStation();
                    handleTimeLimitTraversal(current, prevStation, currentTime, maxTime, reachableStations, queue, timeSpent);
                }

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

        double timeRequired = calculateTravelTime(current.getLocation(), neighbor.getLocation()) + 1.0; // Travel + 1 min stop
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

        double deltaLat = Math.abs(lat1 - lat2) * 110.574;
        double deltaLon = Math.abs(lon1 - lon2) * 111.320 * Math.cos(Math.toRadians(lat1));
        double distance = Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon);
        return (distance / 40) * 60; // 40 km/h in minutes
    }
}