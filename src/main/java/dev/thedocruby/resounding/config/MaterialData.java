package dev.thedocruby.resounding.config;

@SuppressWarnings("CanBeFinal")
public class MaterialData {
    public final String example;
    public final double reflectivity;
    public final double absorption;

    public MaterialData(String s, double r, double a){
        reflectivity = r; absorption = a; example = s;
    }

    public MaterialData(double r, double a){
        reflectivity = r; absorption = a; example = null;
    }

    @SuppressWarnings("unused")
    public MaterialData() { reflectivity = 0; absorption = 1; example = ""; }

    public String getExample() {return example;}
    public double getReflectivity() {return reflectivity;}
    public double getAbsorption() {return absorption;}
}