package ui;

import java.awt.*;

/**
 * Thermal night-vision aesthetic.
 * Deep black arena, agents glow like heat signatures.
 * Predator: amber/red (heat of the hunt).
 * Prey: cyan/green (cold flight response).
 * Attention cones: translucent wedges of each agent's colour.
 */
public final class Theme {
    private Theme() {}

    // ── Backgrounds ───────────────────────────────────────────────────────────
    public static final Color BG_VOID    = new Color(0x02040A);
    public static final Color BG_ARENA   = new Color(0x040810);
    public static final Color BG_PANEL   = new Color(0x080F1C);
    public static final Color BG_CARD    = new Color(0x0C1525);
    public static final Color BG_BORDER  = new Color(0x162338);

    // ── Agent colours ─────────────────────────────────────────────────────────
    public static final Color PREDATOR_CORE  = new Color(0xFF6B2A);  // amber-red
    public static final Color PREDATOR_GLOW  = new Color(0xFF3800);
    public static final Color PREDATOR_CONE  = new Color(0xFF6B2A);

    public static final Color PREY_CORE      = new Color(0x00F5C8);  // teal-cyan
    public static final Color PREY_GLOW      = new Color(0x00BFA5);
    public static final Color PREY_CONE      = new Color(0x00E5FF);

    // ── Mood colours (shared with 2D Navigator aesthetic) ─────────────────────
    public static final Color MOOD_NEG = new Color(0xD32F2F);
    public static final Color MOOD_MID = new Color(0x1A2A3A);
    public static final Color MOOD_POS = new Color(0x00BFA5);

    // ── Text ──────────────────────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY   = new Color(0xDCE8F5);
    public static final Color TEXT_SECONDARY = new Color(0x3A5A7A);
    public static final Color TEXT_ACCENT    = new Color(0x00E5FF);

    // ── Axiom accent colours ──────────────────────────────────────────────────
    public static final Color AXIOM_1 = new Color(0x4FC3F7);
    public static final Color AXIOM_2 = new Color(0xFFD54F);
    public static final Color AXIOM_3 = new Color(0xCE93D8);
    public static final Color AXIOM_4 = new Color(0x69F0AE);
    public static final Color AXIOM_5 = new Color(0xFF8A65);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD,  12);
    public static final Font FONT_LABEL  = new Font("Monospaced", Font.PLAIN, 10);
    public static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN,  8);
    public static final Font FONT_HEADER = new Font("Monospaced", Font.BOLD,  10);
    public static final Font FONT_AGENT  = new Font("Monospaced", Font.BOLD,  11);

    // ── Colour utilities ──────────────────────────────────────────────────────
    public static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    public static Color moodColor(double mood) {
        if (mood < 0) return lerp(MOOD_NEG, MOOD_MID, 1 + mood);
        return lerp(MOOD_MID, MOOD_POS, mood);
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Color agentCore(model.Agent.Role role) {
        return role == model.Agent.Role.PREDATOR ? PREDATOR_CORE : PREY_CORE;
    }

    public static Color agentCone(model.Agent.Role role) {
        return role == model.Agent.Role.PREDATOR ? PREDATOR_CONE : PREY_CONE;
    }
}
