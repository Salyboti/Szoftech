package org.openmetromaps.maps;

import org.junit.Before;
import org.junit.Test;
import org.openmetromaps.maps.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class MapTraversalUnitTests {

    ModelData model;

    Station stationA;
    Station stationB;
    Station stationC;
    Station stationD;

    Line line1;
    Line line2; // Új vonal a transfer tesztekhez

    @Before
    public void createMap() {
        // Arrange: Set up a simple map with two lines
        List<Stop> stationAStops = new ArrayList<>();
        stationA = new Station(0, "A", new Coordinate(47.491, 19.061), stationAStops);

        List<Stop> stationBStops = new ArrayList<>();
        stationB = new Station(1, "B", new Coordinate(47.492, 19.062), stationBStops);

        List<Stop> stationCStops = new ArrayList<>();
        stationC = new Station(2, "C", new Coordinate(47.493, 19.063), stationCStops);

        List<Stop> stationDStops = new ArrayList<>();
        stationD = new Station(3, "D", new Coordinate(47.494, 19.064), stationDStops);

        // First line setup
        List<Stop> line1Stops = new ArrayList<>();
        line1 = new Line(4, "1", "#FF0000", false, line1Stops);

        Stop stopA1 = new Stop(stationA, line1);
        stationAStops.add(stopA1);
        line1Stops.add(stopA1);

        Stop stopB1 = new Stop(stationB, line1);
        stationBStops.add(stopB1);
        line1Stops.add(stopB1);

        Stop stopC1 = new Stop(stationC, line1);
        stationCStops.add(stopC1);
        line1Stops.add(stopC1);

        Stop stopD1 = new Stop(stationD, line1);
        stationDStops.add(stopD1);
        line1Stops.add(stopD1);

        // Second line setup (B-C-D) for transfer testing
        List<Stop> line2Stops = new ArrayList<>();
        line2 = new Line(5, "2", "#00FF00", false, line2Stops);

        Stop stopB2 = new Stop(stationB, line2);
        stationBStops.add(stopB2);
        line2Stops.add(stopB2);

        Stop stopC2 = new Stop(stationC, line2);
        stationCStops.add(stopC2);
        line2Stops.add(stopC2);

        Stop stopD2 = new Stop(stationD, line2);
        stationDStops.add(stopD2);
        line2Stops.add(stopD2);

        model = new ModelData(new ArrayList<>(List.of(line1, line2)),
                new ArrayList<>(List.of(stationA, stationB, stationC, stationD)));
    }

    @Test
    public void testTransferLimitZeroTransfers() {
        // Act
        List<Station> result = MapTraversal.traverseMap(model, stationA,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 0);

        // Assert
        Set<Station> expected = new HashSet<>(List.of(stationA, stationB, stationC, stationD));
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testTransferWithMultipleLines() {
        // Ellenőrizzük, hogy a két vonal közötti átszállás helyesen működik
        List<Station> result = MapTraversal.traverseMap(model, stationB,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 1);

        Set<Station> expected = new HashSet<>(List.of(stationA, stationB, stationC, stationD));
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testTransferLimitFromMiddleStation() {
        // Act
        List<Station> result = MapTraversal.traverseMap(model, stationB,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 1);

        // Assert
        Set<Station> expected = new HashSet<>(List.of(stationA, stationB, stationC, stationD));
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testTransferLimitWithCircularLine() {
        // Arrange
        // Körjárat létrehozása
        Station stationG = new Station(7, "G", new Coordinate(47.498, 19.068), new ArrayList<>());

        List<Stop> circleLineStops = new ArrayList<>();
        Line circleLine = new Line(7, "Circle", "#FF00FF", false, circleLineStops);

        // G állomás a körvonalon
        Stop stopG = new Stop(stationG, circleLine);
        stationG.getStops().add(stopG);
        circleLineStops.add(stopG);

        // B és D állomás is része a körvonalnak
        Stop stopB2 = new Stop(stationB, circleLine);
        Stop stopD2 = new Stop(stationD, circleLine);
        stationB.getStops().add(stopB2);
        stationD.getStops().add(stopD2);
        circleLineStops.add(stopB2);
        circleLineStops.add(stopD2);

        ModelData localModel = new ModelData(
                new ArrayList<>(List.of(line1, circleLine)),
                new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationG))
        );

        // Act
        List<Station> result = MapTraversal.traverseMap(localModel, stationB,
                MapTraversal.MapTraversalLimitType.TRANSFER_LIMIT, 1);

        // Assert - egy átszállással elérhető állomások
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD, stationG);
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testStopLimitBidirectional() {
        // Act
        List<Station> result = MapTraversal.traverseMap(model, stationC,
                MapTraversal.MapTraversalLimitType.STOP_LIMIT, 1);

        // Assert
        Set<Station> expected = new HashSet<>(List.of(stationB, stationC, stationD));
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testStopLimitMaxValue() {
        // Act
        List<Station> result = MapTraversal.traverseMap(model, stationA,
                MapTraversal.MapTraversalLimitType.STOP_LIMIT, Integer.MAX_VALUE);

        // Assert - az összes állomásnak elérhetőnek kell lennie
        Set<Station> expected = Set.of(stationA, stationB, stationC, stationD);
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testStopLimitWithDeadEnd() {
        // Arrange
        // Zsákutca állomás hozzáadása
        Station deadEnd = new Station(6, "Z", new Coordinate(47.497, 19.067), new ArrayList<>());

        List<Stop> line3Stops = new ArrayList<>();
        Line line3 = new Line(6, "3", "#0000FF", false, line3Stops);

        Stop stopDeadEnd = new Stop(deadEnd, line3);
        deadEnd.getStops().add(stopDeadEnd);
        line3Stops.add(stopDeadEnd);

        // Kapcsolódás a fővonalhoz
        Stop stopC3 = new Stop(stationC, line3);
        stationC.getStops().add(stopC3);
        line3Stops.add(stopC3);

        ModelData localModel = new ModelData(
                new ArrayList<>(List.of(line1, line3)),
                new ArrayList<>(List.of(stationA, stationB, stationC, stationD, deadEnd))
        );

        // Act
        List<Station> result = MapTraversal.traverseMap(localModel, stationC,
                MapTraversal.MapTraversalLimitType.STOP_LIMIT, 1);

        // Assert
        Set<Station> expected = Set.of(stationB, stationC, stationD, deadEnd);
        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    public void testTimeLimitFiveMinutes() {

    }

    @Test
    public void testTimeLimitTenMinutes() {
        // Act
        List<Station> result = MapTraversal.traverseMap(model, stationA,
                MapTraversal.MapTraversalLimitType.TIME_LIMIT, 10);

        // Assert
        Set<Station> expected = new HashSet<>(List.of(stationA, stationB, stationC));
        assertEquals(expected, new HashSet<>(result));
    }

    // Új tesztek hozzáadása

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLimit() {
        MapTraversal.traverseMap(model, stationA, MapTraversal.MapTraversalLimitType.STOP_LIMIT, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullModel() {
        MapTraversal.traverseMap(null, stationA, MapTraversal.MapTraversalLimitType.STOP_LIMIT, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullStation() {
        MapTraversal.traverseMap(model, null, MapTraversal.MapTraversalLimitType.STOP_LIMIT, 1);
    }

}
