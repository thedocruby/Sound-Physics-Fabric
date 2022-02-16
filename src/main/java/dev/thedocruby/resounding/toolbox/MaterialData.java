package dev.thedocruby.resounding.toolbox;

public class MaterialData {
    public String example;
    public double reflectivity;
    public double absorption;

    @SuppressWarnings("unused")
    public MaterialData(){}

    public MaterialData(String example, double reflectivity, double absorption){
        this.example = example;
        this.reflectivity = reflectivity;
        this.absorption = absorption;
    }
}

// TODO: Figure out how to make this a record
/*
public record MaterialData(
        String example,
        double reflectivity,
        double absorption
) {
    public MaterialData(){
        // Requires constructor!
    }
}
 */