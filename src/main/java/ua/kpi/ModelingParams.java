package ua.kpi;

import java.util.function.BiFunction;

public enum ModelingParams {
    VARIANT_1(150000, 150000, 50, (time, cost) -> time / 60. / 60),
    VARIANT_2(150000, 200000, 5, (time, cost) -> cost / 100.);

    public int agencyMoney;
    public int agencyTarget;
    public int citizenIncome;
    public BiFunction<Double, Double, Double> dissatisfaction;

    ModelingParams(int agencyMoney, int agencyTarget, int citizenIncome, BiFunction<Double, Double, Double> dissatisfaction) {
        this.agencyMoney = agencyMoney;
        this.agencyTarget = agencyTarget;
        this.citizenIncome = citizenIncome;
        this.dissatisfaction = dissatisfaction;
    }
}
