package it.scarpentim.volleycourtmapping.geometry;

import org.opencv.core.Point;

public class GeoUtils {

    public static double squarePointsDistance(Point p1, Point p2) {
        return Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2);
    }
}
