package it.scarpentim.volleycourtmapping;

class LineFunction {

    public double m,b;
    public double xStart;
    public double yStart;
    public double xEnd;
    public double yEnd;


    public LineFunction(double xStart, double yStart, double xEnd, double yEnd) {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;

        if ((xEnd - xStart) == 0)
            throw new UnsupportedOperationException();
        m = (yEnd - yStart) / (xEnd - xStart);
        b = yStart - m * xStart;
    }

    public double compute(double xStart) {
        return m*xStart + b;
    }
}
