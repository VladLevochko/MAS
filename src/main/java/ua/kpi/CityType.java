package ua.kpi;

public enum CityType {
    KYIV(29000, 30000, 3000, 150),
    LUHANSK(14000, 17000, 470, 50),
    ZHYTOMYR(9000, 7200, 280, 15),
    KERCH(20000, 5000, 150, 5),
    UZHHOROD(7900, 8100, 115, 4),
    BILA_TSERKVA(200, 200, 5, 2),
    CHABANY(100, 100, 2, 1);

    public int width;
    public int height;
    public int population;
    public int drivers;

    CityType(int width, int height, int population, int drivers) {
        this.width = width;
        this.height = height;
        this.population = population;
        this.drivers = drivers;
    }
}
