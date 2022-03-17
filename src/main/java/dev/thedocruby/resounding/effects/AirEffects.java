package dev.thedocruby.resounding.effects;

import dev.thedocruby.resounding.config.PrecomputedConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

import static dev.thedocruby.resounding.ResoundingEngine.mc;

@Environment(EnvType.CLIENT)
public class AirEffects {
    private AirEffects() {}

    private static final float rainDecayConstant = (float) (Math.log(2.0) / 1200);
    private static float rainAccumulator;
    private static boolean rainHasInitialValue;

    public static float getAbsorptionHF() {
        if(mc == null || mc.world == null || mc.player == null)
            return 1.0f;
        double rain = getRain();
        double rainS = rainAccumulator;
        double biomeHumidity = mc.world.getBiome(mc.player.getBlockPos()).value().getDownfall();
        double biomeTemp = mc.world.getBiome(mc.player.getBlockPos()).value().getTemperature();
        double freq = 10000.0d;

        double relhum = 100.0d * MathHelper.lerp(Math.max(rain, rainS), Math.max(biomeHumidity, 0.2d), 1.0d); // convert biomeHumidity and rain gradients into a dynamic relative humidity value
        double tempK = 25.0d * biomeTemp + 273.15d; // Convert biomeTemp to degrees kelvin

        double hum = relhum*Math.pow(10.0d,4.6151d-6.8346d*Math.pow((273.15d/tempK),1.261d));
        double tempr = tempK/293.15d; // convert tempK to temperature relative to room temp

        double frO = (24+4.04E+4*hum*(0.02d+hum)/(0.391d+hum));
        double frN = Math.pow(tempr,-0.5)*(9+280*hum*Math.exp(-4.17d*(Math.pow(tempr,-1.0f/3.0f)-1)));
        double alpha = 8.686d*freq*freq*(1.84E-11*Math.sqrt(tempr)+Math.pow(tempr,-2.5)*(0.01275d*(Math.exp(-2239.1d/tempK)*1/(frO+freq*freq/frO))+0.1068d*(Math.exp(-3352/tempK)*1/(frN+freq*freq/frN))));

        return (float) Math.pow(10.0d, (alpha * -1.0d * PrecomputedConfig.pC.humAbs)/20.0d); // convert alpha (decibels of attenuation per meter) into airAbsorptionGainHF value and return
    }

    public static float getRain(){
        float tickDelta = 1.0f;
        return (mc==null || mc.world==null) ? 0.0f : mc.world.getRainGradient(tickDelta);
    }

    public static void updateSmoothedRain() {
        if (!rainHasInitialValue) {
            // There is no smoothing on the first value.
            // This is not an optimal approach to choosing the initial value:
            // https://en.wikipedia.org/wiki/Exponential_smoothing#Choosing_the_initial_smoothed_value
            //
            // However, it works well enough for now.
            rainAccumulator = getRain();
            rainHasInitialValue = true;

            return;
        }

        // Implements the basic variant of exponential smoothing
        // https://en.wikipedia.org/wiki/Exponential_smoothing#Basic_(simple)_exponential_smoothing_(Holt_linear)

        // xₜ
        float newValue = getRain();

        // 𝚫t
        float tickDelta = 1.0f;

        // Compute the smoothing factor based on our
        // α = 1 - e^(-𝚫t/τ) = 1 - e^(-k𝚫t)
        float smoothingFactor = (float) (1.0f - Math.exp(-1*rainDecayConstant*tickDelta));

        // sₜ = αxₜ + (1 - α)sₜ₋₁
        rainAccumulator = MathHelper.lerp(smoothingFactor, rainAccumulator, newValue);
    }
}
