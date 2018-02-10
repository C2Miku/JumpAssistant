public class RGB {

    int r, g, b;

    public RGB() {
        r = g = b = 0;
    }

    public RGB(int setR, int setG, int setB) {
        r = setR;
        g = setG;
        b = setB;
    }

    public String getRGBString() {
        return Integer.toString(r) + "," + Integer.toString(g) + "," + Integer.toString(b);
    }

    public boolean equals(RGB another) {
        return this.r == another.r && this.g == another.g && this.b == another.b;
    }

}