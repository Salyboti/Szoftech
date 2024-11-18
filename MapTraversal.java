package org.openmetromaps.maps;

import org.openmetromaps.maps.model.*;
import java.util.*;

public class MapTraversal {
    private static final double AVERAGE_SPEED_KMH = 40.0; // Átlagos utazási sebesség km/h-ban
    private static final double EARTH_RADIUS_KM = 6371.0; // Föld sugara kilométerben
    private static final int MINUTES_PER_HOUR = 60;

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
        Queue<TraversalState> queue = new LinkedList<>();
        Map<Station, Integer> bestTransfers = new HashMap<>();

        queue.add(new TraversalState(src, null, 0));
        bestTransfers.put(src, 0);
        reachableStations.add(src);

        while (!queue.isEmpty()) {
            TraversalState current = queue.poll();
            Station currentStation = current.station;
            Line currentLine = current.line;
            int currentTransfers = current.transfers;

            if (currentTransfers <= maxTransfers) {
                for (Stop stop : currentStation.getStops()) {
                    Line line = stop.getLine();
                    processNeighborStations(line, stop, currentLine, currentTransfers, 
                                         maxTransfers, reachableStations, queue, bestTransfers);
                }
            }
        }

        return new ArrayList<>(reachableStations);
    }

    private static void processNeighborStations(Line line, Stop stop, Line currentLine, 
                                              int currentTransfers, int maxTransfers,
                                              Set<Station> reachableStations, 
                                              Queue<TraversalState> queue,
                                              Map<Station, Integer> bestTransfers) {
        List<Stop> lineStops = line.getStops();
        int stopIndex = lineStops.indexOf(stop);

        // Szomszédos állomások feldolgozása mindkét irányban
        processNeighborInDirection(lineStops, stopIndex - 1, line, currentLine, 
                                 currentTransfers, maxTransfers, reachableStations, 
                                 queue, bestTransfers);
        processNeighborInDirection(lineStops, stopIndex + 1, line, currentLine, 
                                 currentTransfers, maxTransfers, reachableStations, 
                                 queue, bestTransfers);
    }

    private static void processNeighborInDirection(List<Stop> lineStops, int neighborIndex,
                                                 Line line, Line currentLine, int currentTransfers,
                                                 int maxTransfers, Set<Station> reachableStations,
                                                 Queue<TraversalState> queue,
                                                 Map<Station, Integer> bestTransfers) {
        if (neighborIndex >= 0 && neighborIndex < lineStops.size()) {
            Station neighbor = lineStops.get(neighborIndex).getStation();
            int newTransfers = currentTransfers;
            
            // Átszállás számítása csak ha ténylegesen másik vonalra lépünk
            if (currentLine != null && currentLine != line) {
                newTransfers++;
            }

            if (newTransfers <= maxTransfers && 
               (!bestTransfers.containsKey(neighbor) || newTransfers < bestTransfers.get(neighbor))) {
                reachableStations.add(neighbor);
                queue.add(new TraversalState(neighbor, line, newTransfers));
                bestTransfers.put(neighbor, newTransfers);
            }
        }
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

                    processStopNeighbor(lineStops, index - 1, currentStops,
                                      reachableStations, queue, stops);
                    processStopNeighbor(lineStops, index + 1, currentStops,
                                      reachableStations, queue, stops);
                }
            }
        }

        return new ArrayList<>(reachableStations);
    }

    private static void processStopNeighbor(List<Stop> lineStops, int neighborIndex,
                                          int currentStops, Set<Station> reachableStations,
                                          Queue<Station> queue, Map<Station, Integer> stops) {
        if (neighborIndex >= 0 && neighborIndex < lineStops.size()) {
            Station neighborStation = lineStops.get(neighborIndex).getStation();
            if (!stops.containsKey(neighborStation)) {
                reachableStations.add(neighborStation);
                queue.add(neighborStation);
                stops.put(neighborStation, currentStops + 1);
            }
        }
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

                processTimeNeighbor(lineStops, index - 1, current, currentTime,
                                  maxTime, reachableStations, queue, timeSpent);
                processTimeNeighbor(lineStops, index + 1, current, currentTime,
                                  maxTime, reachableStations, queue, timeSpent);
            }
        }
    
        return new ArrayList<>(reachableStations);
    }

    private static void processTimeNeighbor(List<Stop> lineStops, int neighborIndex,
                                          Station current, double currentTime, int maxTime,
                                          Set<Station> reachableStations, Queue<Station> queue,
                                          Map<Station, Double> timeSpent) {
        if (neighborIndex >= 0 && neighborIndex < lineStops.size()) {
            Station neighborStation = lineStops.get(neighborIndex).getStation();
            double timeRequired = calculateTravelTime(current.getLocation(), neighborStation.getLocation());
            
            if (!timeSpent.containsKey(neighborStation) && 
                currentTime + timeRequired <= maxTime) {
                reachableStations.add(neighborStation);
                queue.add(neighborStation);
                timeSpent.put(neighborStation, currentTime + timeRequired);
            }
        }
    }

    private static double calculateTravelTime(Coordinate from, Coordinate to) {
        double lat1 = Math.toRadians(from.getLatitude());
        double lon1 = Math.toRadians(from.getLongitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double lon2 = Math.toRadians(to.getLongitude());
    
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
    
        // Átszámítás percekre az átlagos sebességgel
        return (distance / AVERAGE_SPEED_KMH) * MINUTES_PER_HOUR;
    }

    private static class TraversalState {
        final Station station;
        final Line line;
        final int transfers;

        TraversalState(Station station, Line line, int transfers) {
            this.station = station;
            this.line = line;
            this.transfers = transfers;
        }
    }
}