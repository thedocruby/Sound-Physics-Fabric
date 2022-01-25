package com.thedocruby.soundphysics.config;

@SuppressWarnings("CanBeFinal")
public class MaterialData {
    public String example;
    public double reflectivity;
    public double absorption;

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