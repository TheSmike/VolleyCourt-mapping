package it.scarpentim.volleycourtmapping;

import org.opencv.core.Point;

class LineFunction {

    private static final double EPSILON = 2;
    private static final double MARGIN = 7;
    public double m,b;
    public double xStart;
    public double yStart;
    public double xEnd;
    public double yEnd;

    private Point startPoint;
    private Point endPoint;


    public LineFunction(double xStart, double yStart, double xEnd, double yEnd) {
        init(xStart, yStart, xEnd, yEnd);
    }

    private void init(double xStart, double yStart, double xEnd, double yEnd) {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;

        startPoint = new Point(xStart, yStart);
        endPoint = new Point(xEnd, yEnd);

//        if ((xEnd - xStart) == 0)
//            throw new UnsupportedOperationException();
        m = (yEnd - yStart) / (xEnd - xStart);
        b = yStart - m * xStart;
    }

    public LineFunction(double[] line) {
        double  xStart = line[0],
                yStart = line[1],
                xEnd = line[2],
                yEnd = line[3];
        init(xStart, yStart, xEnd, yEnd);
    }

    public double compute(double x) {
        return m*x + b;
    }

    @Override
    public String toString() {
        return String.format("y = %fx+%f", m, b);
    }

    public Point intersection(LineFunction f2) {
        if (f2.b - this.b == 0){
            return null;
        }
        double x = (f2.b - this.b) / (this.m - f2.m);
        double y = m*x + b;

        Point point = new Point(x, y);
        return point;
    }

    public boolean segmentContainPoint(Point point) {
        double y = compute(point.x);
        if (y >= point.y - EPSILON && y <= point.y + EPSILON
                && point.x >= Math.min(xStart, xEnd) - MARGIN && point.x <= Math.max(xStart, xEnd) + MARGIN)
            return true;
        else
            return false;
    }

    public double distanceSegmentToPoint(Point point) {
        return findDistanceToSegment(point, this.startPoint, this.endPoint );
    }

    private double findDistanceToSegment(Point pt, Point p1, Point p2) {
        Point closest;
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        if ((dx == 0) && (dy == 0))
        {
            // It's a point not a line segment.
            closest = p1;
            dx = pt.x - p1.x;
            dy = pt.y - p1.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // Calculate the t that minimizes the distance.
        double t = ((pt.x - p1.x) * dx + (pt.y - p1.y) * dy) /
                (dx * dx + dy * dy);

        // See if this represents one of the segment's
        // end points or a point in the middle.
        if (t < 0)
        {
            closest = new Point(p1.x, p1.y);
            dx = pt.x - p1.x;
            dy = pt.y - p1.y;
        }
        else if (t > 1)
        {
            closest = new Point(p2.x, p2.y);
            dx = pt.x - p2.x;
            dy = pt.y - p2.y;
        }
        else
        {
            closest = new Point(p1.x + t * dx, p1.y + t * dy);
            dx = pt.x - closest.x;
            dy = pt.y - closest.y;
        }

        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distanceLineToPoint(Point point) {
        return (point.y - (this.m * point.x + this.b)) / Math.sqrt(1 + this.m * this.m);
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

//    private boolean pointInSegment(Point point) {
//        double y = compute(point.x);
//
//        if (y > Math.min(xStart, xEnd) &&
//    }
}
