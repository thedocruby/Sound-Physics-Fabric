package dev.thedocruby.resounding;

/**
 * A material.
 *
 * @param impedance  the impedance of the material
 * @param permeation  the permeation of the material (the inverse of absorption)
 * @param state  the current state of matter
 */
public record Material(double impedance, double permeation, double state) {
    /**
     * The fallback material.
     */
    public static final Material FALLBACK = new Material(350.0, 1.0, 1.0);

    /**
     * Creates a new material.
     * <p>
     * For valid values for the state of matter, see {@code state()}.
     *
     * @param impedance  the impedance of the material
     * @param permeation  the permeation of the material
     * @param state  the current state of matter
     * @see #state()
     */
    public Material {
        if (state < 0.0 || state > 1.0) {
            throw new IllegalArgumentException(
                    "Expected component state to be between zero and one but instead its " + state
            );
        }
    }

    /**
     * The current state of matter. This is a continuous value in the range
     * between zero and one, with each state roughly mapped in the following
     * table:
     * <table>
     *     <tr><th>Value</th> <th>State of Matter</th></tr>
     *     <tr><td>0    </td> <td>absent         </td></tr>
     *     <tr><td>0.25 </td> <td>plasma         </td></tr>
     *     <tr><td>0.5  </td> <td>gas            </td></tr>
     *     <tr><td>0.75 </td> <td>liquid         </td></tr>
     *     <tr><td>1.0  </td> <td>solid          </td></tr>
     * </table>
     *
     * @return the current state of matter
     */
    @Override
    public double state() {
        return this.state;
    }
}
