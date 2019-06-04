package it.scarpentim.volleycourtmapping.geometry;

import org.opencv.core.Point;

public class LineIntersection {

    private LineFunction lineFunction;
    private Point intersection;

    public LineIntersection(LineFunction lineFunction, Point intersection) {
        this.lineFunction = lineFunction;
        this.intersection = intersection;
    }

    public LineFunction getLineFunction() {
        return lineFunction;
    }

    public Point getIntersection() {
        return intersection;
    }
}
