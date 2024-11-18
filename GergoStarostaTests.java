package org.openmetromaps.maps;

import org.junit.Before;
import org.junit.Test;
import org.openmetromaps.maps.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MapTraversalUnitTests {
    ModelData model;

    Station stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH, stationI, stationJ,
            stationK;
    Line line1, line2, line3;

    /*
     * Setting up a more complex map (as given in the issue):
     * 
     * Line 1: A - B - C - D - E - F - G - H
     * Line 2: A - I - J
     * Line 3: J - K - H
     *
     * The map structure:
     *
        M1  X----X----X----X----X----X----X----X
            A\   B    C    D    E    F    G   /H
              \                              /
            M2 \                            / M3
                X-------------X------------X
                I             J            K     
    */
    @Before
    public void createMap() {
        // Stations
        stationA = new Station(0, "A", new Coordinate(47.4891, 19.0614), new ArrayList<>());
        stationB = new Station(1, "B", new Coordinate(47.4892, 19.0714), new ArrayList<>());
        stationC = new Station(2, "C", new Coordinate(47.4893, 19.0814), new ArrayList<>());
        stationD = new Station(3, "D", new Coordinate(47.4894, 19.0914), new ArrayList<>());
        stationE = new Station(4, "E", new Coordinate(47.4895, 19.1014), new ArrayList<>());
        stationF = new Station(5, "F", new Coordinate(47.4896, 19.1114), new ArrayList<>());
        stationG = new Station(6, "G", new Coordinate(47.4897, 19.1214), new ArrayList<>());
        stationH = new Station(7, "H", new Coordinate(47.4898, 19.1314), new ArrayList<>());
        stationI = new Station(8, "I", new Coordinate(47.4900, 19.0714), new ArrayList<>());
        stationJ = new Station(9, "J", new Coordinate(47.4910, 19.0814), new ArrayList<>());
        stationK = new Station(10, "K", new Coordinate(47.4920, 19.0914), new ArrayList<>());

        // Line 1 (M1)
        line1 = new Line(1, "M1", "#009EE3", false, new ArrayList<>());
        addStop(stationA, line1);
        addStop(stationB, line1);
        addStop(stationC, line1);
        addStop(stationD, line1);
        addStop(stationE, line1);
        addStop(stationF, line1);
        addStop(stationG, line1);
        addStop(stationH, line1);

        // Line 2 (M2)
        line2 = new Line(2, "M2", "#FF5733", false, new ArrayList<>());
        addStop(stationA, line2);
        addStop(stationI, line2);
        addStop(stationJ, line2);

        // Line 3 (M3)
        line3 = new Line(3, "M3", "#33FF57", false, new ArrayList<>());
        addStop(stationJ, line3);
        addStop(stationK, line3);
        addStop(stationH, line3);

        model = new ModelData(new ArrayList<>(List.of(line1, line2, line3)),
                new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH,
                        stationI, stationJ, stationK)));
    }

    private void addStop(Station station, Line line) {
        Stop stop = new Stop(station, line);
        station.getStops().add(stop);
        line.getStops().add(stop);
    }

    /*
     * Starting from:
     *
     * M1: A - B - C - D - E - F - G - H
     * M2: A - I - J
     * M3: J - K - H
     *
     * TRANSFER_LIMIT 0: Only reachable stations on each line without any transfers.
     */
    @Test
    public void testTransferLimitZero() {
        List<Station> result = MapTraversal.traverseMap(model, stationA,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 0);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH,
                stationI, stationJ);
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * TRANSFER_LIMIT 1: Includes reachable stations with a single transfer allowed.
     * Should reach station K via J with one transfer from M2 to M3.
     */
    @Test
    public void testTransferLimitOne() {
        List<Station> result = MapTraversal.traverseMap(model, stationE,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 1);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH,
                stationI, stationJ, stationK);
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * STOP_LIMIT 3: Only three stops away from A, regardless of line or transfers.
     */
    @Test
    public void testStopLimitThree() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.STOP_LIMIT,
                3);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationI, stationJ, stationK);
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * TIME_LIMIT 1: Only the starting station should be reachable within 1 minute.
     * The starting station is not counted in the limit.
     */
    @Test
    public void testTimeLimitOneMinute() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.TIME_LIMIT,
                1);
        Set<Station> expected = Set.of(stationA);
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * TIME_LIMIT 5: Considering 1 minute is spent at the starting station (A),
     * leaving 4 minutes for travel. Should be able to reach the first adjacent
     * station (B on M1 or I on M2).
     */
    @Test
    public void testTimeLimitFiveMinutes() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.TIME_LIMIT,
                5);

        // Since we have only 4 minutes left for travel after the initial 1 minute at A,
        // only B and I should be reachable.
        Set<Station> expected = Set.of(stationA, stationB, stationI); // Elérhető állomások az időkorlát alapján
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * TIME_LIMIT 15: Stations reachable within 15 minutes of travel time.
     * Assuming each station is 3 minutes away, can reach up to station D on M1.
     */
     /*@Test
     public void testTimeLimitFifteenMinutes() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.TIME_LIMIT,
                11);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationI, stationJ, stationK);
        assertEquals(expected, new HashSet<>(result));
    }  */

    /*
     * TIME_LIMIT 30: Allowing up to 30 minutes of travel time, should include more
     * stations.
     * Expected result includes stations up to G on M1 and J on M2.
     */
/*      @Test
    public void testTimeLimitThirtyMinutes() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.TIME_LIMIT,
                30);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationI,
                stationJ);
        assertEquals(expected, new HashSet<>(result));
    }  */

    /*
     * Edge Case: Starting at station K with TRANSFER_LIMIT 0 (no transfers).
     * Should only be able to visit itself and H (as direct neighbors).
     */
    @Test
    public void testEdgeCaseStartingAtKWithZeroTransferLimit() {
        List<Station> result = MapTraversal.traverseMap(model, stationK,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 0);
        Set<Station> expected = Set.of(stationK, stationH, stationJ);
        assertEquals(expected, new HashSet<>(result));
    }

    /*
     * Edge Case: Starting at station A with extremely high STOP_LIMIT (e.g., 10)
     * Should include all stations.
     */
    @Test
    public void testHighStopLimit() {
        List<Station> result = MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.STOP_LIMIT,
                10);
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH,
                stationI, stationJ, stationK);
        assertEquals(expected, new HashSet<>(result));
    }
}
