package it.scarpentim.volleycourtmapping.geometry;

import org.opencv.core.Point;

public class LineFunction {

    private static final double EPSILON = 2;
    private static final double MARGIN = 7;
    private Double m,b;
    public double xStart;
    public double yStart;
    public double xEnd;
    public double yEnd;

    private Point startPoint;
    private Point endPoint;




    private void init(double xStart, double yStart, double xEnd, double yEnd) {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;

        startPoint = new Point(xStart, yStart);
        endPoint = new Point(xEnd, yEnd);

        if ((xEnd - xStart) == 0) {
            m = Double.POSITIVE_INFINITY;
            b = xStart;
        } else {
            m = (yEnd - yStart) / (xEnd - xStart);
            b = yStart - m * xStart;
        }
    }

    public LineFunction(double[] line) {
        double  xStart = line[0],
                yStart = line[1],
                xEnd = line[2],
                yEnd = line[3];
        init(xStart, yStart, xEnd, yEnd);
    }

    public Double computeY(double x) {
        if (m == Double.POSITIVE_INFINITY)
            return Double.NaN;
        else
            return m*x + b;
    }

    public Double computeX(double y) {
        if (m == Double.POSITIVE_INFINITY)
            return b;
        else if (m == 0)
            return Double.NaN;
        else
            return (y-b)/m;
    }

    @Override
    public String toString() {
        if(m == Double.POSITIVE_INFINITY)
            return String.format("x = %f", b);
        else if (m == 0)
            return String.format("y = %f", b);
        else
            return String.format("y = %fx+%f", m, b);
    }

    public Point intersection(LineFunction f2) {
        //se parallele torna null
        if (this.m == Double.POSITIVE_INFINITY && f2.m == Double.POSITIVE_INFINITY) {
            return null;
        } else if (f2.m - this.m == 0) {
            return null;
        } else if (this.m == Double.POSITIVE_INFINITY){
            Double y = f2.computeY(this.xStart);
            return new Point(this.xStart, y);
        } else if (f2.m == Double.POSITIVE_INFINITY){
            Double y = this.computeY(f2.xStart);
            return new Point(f2.xStart, y);
        }

        double x = (f2.b - this.b) / (this.m - f2.m);
        double y = m*x + b;

        Point point = new Point(x, y);
        return point;
    }

    public boolean segmentContainPoint(Point point) {
        double y = computeY(point.x);
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

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void reload(double xStart, double yStart, double xEnd, double yEnd) {
        init(xStart, yStart, xEnd, yEnd);
    }

    public Point getExtremePointFarFromOrigin() {
        if (Math.pow(getStartPoint().x, 2) + Math.pow(getStartPoint().y, 2) >  Math.pow(getEndPoint().x, 2) + Math.pow(getEndPoint().y, 2))
            return getStartPoint();
        else
            return getEndPoint();
    }

    public Point getExtremePointNearTop() {
        if (getStartPoint().y < getEndPoint().y)
            return getStartPoint();
        else
            return getEndPoint();
    }

    public boolean leftTo(Point midPoint) {
        if (m == Double.POSITIVE_INFINITY)
            return xStart < midPoint.x;
        if (m == 0)
            return false;

        double y = computeY(midPoint.x);
        double x = computeX(midPoint.y);

        if (x > midPoint.x) {
            return false;
        }
        else {
            double horizontalDist = GeoUtils.squarePointsDistance(midPoint, new Point(x, midPoint.y));
            double verticalDist = GeoUtils.squarePointsDistance(midPoint, new Point(midPoint.x, y));
            return (horizontalDist < verticalDist);
        }
    }

    public boolean rightTo(Point midPoint) {
        if (m == Double.POSITIVE_INFINITY)
            return xStart > midPoint.x;
        if (m == 0)
            return false;

        double y = computeY(midPoint.x);
        double x = computeX(midPoint.y);

        if (x < midPoint.x) {
            return false;
        }
        else {
            double horizontalDist = GeoUtils.squarePointsDistance(midPoint, new Point(x, midPoint.y));
            double verticalDist = GeoUtils.squarePointsDistance(midPoint, new Point(midPoint.x, y));
            return (horizontalDist < verticalDist);
        }
    }

    public boolean topTo(Point midPoint) {
        if (m == Double.POSITIVE_INFINITY)
            return false;
        if (m == 0)
            return (computeY(midPoint.x) < midPoint.y);

        double y = computeY(midPoint.x);
        double x = computeX(midPoint.y);

        if (y > midPoint.y) {
            return false;
        }
        else {
            double horizontalDist = GeoUtils.squarePointsDistance(midPoint, new Point(x, midPoint.y));
            double verticalDist = GeoUtils.squarePointsDistance(midPoint, new Point(midPoint.x, y));
            return (horizontalDist > verticalDist);
        }
    }

    public boolean bottomTo(Point midPoint) {
        if (m == Double.POSITIVE_INFINITY)
            return false;
        if (m == 0)
            return (computeY(midPoint.x) > midPoint.y);

        double y = computeY(midPoint.x);
        double x = computeX(midPoint.y);

        if (y < midPoint.y) {
            return false;
        }
        else {
            double horizontalDist = GeoUtils.squarePointsDistance(midPoint, new Point(x, midPoint.y));
            double verticalDist = GeoUtils.squarePointsDistance(midPoint, new Point(midPoint.x, y));
            return (horizontalDist > verticalDist);
        }
    }

    public double getSlopeInRadians() {
        return Math.atan(m);
    }


//    private boolean pointInSegment(Point point) {
//        double y = computeY(point.x);
//
//        if (y > Math.min(xStart, xEnd) &&
//    }
}
