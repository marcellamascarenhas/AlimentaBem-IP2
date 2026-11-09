package com.alimentabem.model;

// REQ05: calorias e proteínas por 100g (fonte: USDA FoodData Central)
public class ValorNutricional implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final double caloriasPor100g;
    private final double proteinasPor100gEmGramas;
    private final String fonte;

    public ValorNutricional(double caloriasPor100g, double proteinasPor100gEmGramas, String fonte) {
        this.caloriasPor100g = caloriasPor100g;
        this.proteinasPor100gEmGramas = proteinasPor100gEmGramas;
        this.fonte = fonte;
    }

    public double getCaloriasPor100g() { return caloriasPor100g; }
    public double getProteinasPor100gEmGramas() { return proteinasPor100gEmGramas; }
    public String getFonte() { return fonte; }

    @Override
    public String toString() {
        return String.format("%.1f kcal e %.1f g de proteína por 100g (fonte: %s)", caloriasPor100g, proteinasPor100gEmGramas, fonte);
    }
}