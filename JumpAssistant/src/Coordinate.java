public class Coordinate {

    int x, y;

    public Coordinate() {
        x = y = 0;
    }

    public Coordinate(int setX, int setY) {
        x = setX;
        y = setY;
    }

    public String getCoordinateString() {
        return Integer.toString(x) + "," + Integer.toString(y);
    }

}