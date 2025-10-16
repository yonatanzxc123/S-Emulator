package system.core.io;

import java.util.Map;

public final class ArchTierMap {
    private ArchTierMap() {} // prevent instantiation

    /**
     * Minimal architecture tier (1–4) required per instruction class.
     */
    public static final Map<Class<?>, Integer> TIER_MAP = Map.ofEntries(
            // Tier I
            Map.entry(system.core.model.basic.Inc.class, 1),
            Map.entry(system.core.model.basic.Dec.class, 1),
            Map.entry(system.core.model.basic.Nop.class, 1),
            Map.entry(system.core.model.basic.IfGoto.class, 1),

            // Tier II
            Map.entry(system.core.model.synthetic.ZeroVariable.class, 2),
            Map.entry(system.core.model.synthetic.ConstantAssignment.class, 2),
            Map.entry(system.core.model.synthetic.GotoLabel.class, 2),

            // Tier III
            Map.entry(system.core.model.synthetic.Assignment.class, 3),
            Map.entry(system.core.model.synthetic.JumpZero.class, 3),
            Map.entry(system.core.model.synthetic.JumpEqualConstant.class, 3),
            Map.entry(system.core.model.synthetic.JumpEqualVariable.class, 3),

            // Tier IV
            Map.entry(system.core.model.synthetic.advanced.Quote.class, 4),
            Map.entry(system.core.model.synthetic.advanced.JumpEqualFunction.class, 4)
    );

    /**
     * Returns the tier number (1–4) for the given instruction class.
     * Unknown instructions default to Tier IV for safety.
     */
    public static int tierOf(Class<?> cls) {
        return TIER_MAP.getOrDefault(cls, 4);
    }
}