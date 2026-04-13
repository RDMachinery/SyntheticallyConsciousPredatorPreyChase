package ui;

import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The main arena panel.
 *
 * Visual layers (bottom to top):
 *   1. Arena background with subtle grid scan-lines
 *   2. Trail history for each agent (fading dots)
 *   3. Attention cones — translucent wedges, colour = agent role,
 *      direction shifts with mood, width narrows with intensity
 *   4. Agent bodies with mood-coloured aura
 *   5. Facing arrow and action label
 *   6. Distance line between agents
 */
public class ArenaPanel extends JPanel {

    public static final int W = 560;
    public static final int H = 480;

    private static final int TRAIL_LEN   = 40;
    private static final int AGENT_R     = 14;
    private static final int CONE_LENGTH = 110;

    private Agent.Snapshot predSnap;
    private Agent.Snapshot preySnap;

    private final Deque<Point2D.Float> predTrail = new ArrayDeque<>();
    private final Deque<Point2D.Float> preyTrail = new ArrayDeque<>();

    private float pulse = 0f;

    public ArenaPanel() {
        setBackground(Theme.BG_ARENA);
        setPreferredSize(new Dimension(W, H));
    }

    public void update(Agent.Snapshot pred, Agent.Snapshot prey) {
        this.predSnap = pred;
        this.preySnap = prey;
        pulse = (pulse + 0.15f) % (float)(2 * Math.PI);

        if (pred != null) {
            predTrail.addLast(new Point2D.Float((float)pred.x, (float)pred.y));
            if (predTrail.size() > TRAIL_LEN) predTrail.removeFirst();
        }
        if (prey != null) {
            preyTrail.addLast(new Point2D.Float((float)prey.x, (float)prey.y));
            if (preyTrail.size() > TRAIL_LEN) preyTrail.removeFirst();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2);
        drawDistanceLine(g2);
        drawTrail(g2, predTrail, Theme.PREDATOR_CORE);
        drawTrail(g2, preyTrail, Theme.PREY_CORE);

        if (predSnap != null) {
            drawAttentionCone(g2, predSnap, Agent.Role.PREDATOR);
            drawAgent(g2, predSnap, Agent.Role.PREDATOR);
        }
        if (preySnap != null) {
            drawAttentionCone(g2, preySnap, Agent.Role.PREY);
            drawAgent(g2, preySnap, Agent.Role.PREY);
        }

        drawStamina(g2, preySnap);
        drawArenaFrame(g2);
    }

    // ── Stamina arc around prey ───────────────────────────────────────────────
    private void drawStamina(Graphics2D g2, Agent.Snapshot prey) {
        if (prey == null) return;
        float cx = (float) prey.x, cy = (float) prey.y;
        int r = AGENT_R + 6;

        // Background arc (dim)
        g2.setColor(new Color(30, 50, 60, 120));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval((int)(cx-r), (int)(cy-r), r*2, r*2);

        // Filled arc proportional to stamina
        Color sc = prey.stamina > 0.5
            ? Theme.lerp(new Color(0xFF,0xD5,0x4F), Theme.PREY_CORE, (prey.stamina-0.5)*2)
            : Theme.lerp(Theme.MOOD_NEG, new Color(0xFF,0xD5,0x4F), prey.stamina*2);
        g2.setColor(Theme.withAlpha(sc, 200));
        int arcDeg = (int)(prey.stamina * 360);
        g2.drawArc((int)(cx-r), (int)(cy-r), r*2, r*2, 90, -arcDeg);
    }

    // ── Background with scan-line texture ─────────────────────────────────────
    private void drawBackground(Graphics2D g2) {
        g2.setColor(Theme.BG_ARENA);
        g2.fillRect(0, 0, W, H);

        // Subtle horizontal scan lines
        g2.setColor(new Color(0, 20, 40, 18));
        for (int y = 0; y < H; y += 3) g2.drawLine(0, y, W, y);

        // Very faint grid
        g2.setColor(new Color(0, 40, 80, 12));
        for (int x = 0; x < W; x += 40) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 40) g2.drawLine(0, y, W, y);
    }

    // ── Distance line ─────────────────────────────────────────────────────────
    private void drawDistanceLine(Graphics2D g2) {
        if (predSnap == null || preySnap == null) return;
        double dx = preySnap.x - predSnap.x, dy = preySnap.y - predSnap.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        double maxDist = Math.sqrt(W*W + H*H);
        float alpha = (float)(0.3 * (dist / maxDist));

        g2.setColor(new Color(1f, 1f, 1f, Math.min(0.25f, alpha)));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{4, 6}, 0));
        g2.drawLine((int)predSnap.x, (int)predSnap.y, (int)preySnap.x, (int)preySnap.y);

        // Distance label at midpoint
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawString(String.format("%.0fpx", dist),
            (int)((predSnap.x + preySnap.x) / 2) + 4,
            (int)((predSnap.y + preySnap.y) / 2) - 4);
    }

    // ── Trail ─────────────────────────────────────────────────────────────────
    private void drawTrail(Graphics2D g2, Deque<Point2D.Float> trail, Color col) {
        if (trail.isEmpty()) return;
        Object[] pts = trail.toArray();
        int n = pts.length;
        for (int i = 0; i < n; i++) {
            Point2D.Float p = (Point2D.Float) pts[i];
            float alpha = (float)i / n * 0.45f;
            int   r     = Math.max(1, (int)(3 * (float)i / n));
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(),
                                  (int)(alpha * 255)));
            g2.fillOval((int)p.x - r, (int)p.y - r, r*2, r*2);
        }
    }

    // ── Attention cone ────────────────────────────────────────────────────────
    /**
     * The cone visually encodes Axiom 2 (Attention):
     *   - Direction: toward other agent when mood >= 0 (approach/curious),
     *                away from other agent when mood < 0 (flee/frustrated)
     *   - Width:     wide when calm, narrow when emotionally intense
     *   - Colour:    agent's role colour, intensity = |mood|
     *   - Length:    proportional to attention weight on proximity sensor
     */
    private void drawAttentionCone(Graphics2D g2, Agent.Snapshot snap, Agent.Role role) {
        float cx = (float) snap.x;
        float cy = (float) snap.y;

        double dir   = snap.attentionDirection;
        double width = snap.attentionWidth;
        double len   = CONE_LENGTH * (0.5 + Math.abs(snap.mood) * 0.5);

        Color coneCol = Theme.agentCone(role);
        int   baseAlpha = (int)(60 + Math.abs(snap.mood) * 100);

        // Outer glow pass (wider, more transparent)
        drawConeShape(g2, cx, cy, dir, width * 1.4, len * 1.1,
                      Theme.withAlpha(coneCol, baseAlpha / 3));

        // Main cone
        drawConeShape(g2, cx, cy, dir, width, len,
                      Theme.withAlpha(coneCol, baseAlpha));

        // Inner bright beam (narrower, more opaque)
        drawConeShape(g2, cx, cy, dir, width * 0.3, len * 0.8,
                      Theme.withAlpha(coneCol, Math.min(220, baseAlpha * 2)));

        // Cone outline arc
        g2.setColor(Theme.withAlpha(coneCol, 80));
        g2.setStroke(new BasicStroke(1.0f));
        double leftAngle  = dir - width;
        double rightAngle = dir + width;
        g2.drawLine((int)cx, (int)cy,
            (int)(cx + len * Math.cos(leftAngle)),
            (int)(cy + len * Math.sin(leftAngle)));
        g2.drawLine((int)cx, (int)cy,
            (int)(cx + len * Math.cos(rightAngle)),
            (int)(cy + len * Math.sin(rightAngle)));

        // Mood-shift indicator: small arrow showing cone direction change
        drawMoodArrow(g2, cx, cy, dir, snap.mood, coneCol);
    }

    private void drawConeShape(Graphics2D g2, float cx, float cy,
                                double dir, double halfAngle, double len, Color col) {
        Path2D cone = new Path2D.Float();
        cone.moveTo(cx, cy);
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double a = (dir - halfAngle) + (2 * halfAngle) * i / steps;
            cone.lineTo(cx + len * Math.cos(a), cy + len * Math.sin(a));
        }
        cone.closePath();
        g2.setColor(col);
        g2.fill(cone);
    }

    private void drawMoodArrow(Graphics2D g2, float cx, float cy,
                                double dir, double mood, Color col) {
        // Small curved indicator at cone tip showing emotional direction
        int tipX = (int)(cx + 40 * Math.cos(dir));
        int tipY = (int)(cy + 40 * Math.sin(dir));
        g2.setColor(Theme.withAlpha(col, 120));
        g2.setFont(Theme.FONT_SMALL);
        String label = mood > 0.3 ? "▶" : mood < -0.3 ? "◀" : "•";
        g2.drawString(label, tipX - 4, tipY + 3);
    }

    // ── Agent body ────────────────────────────────────────────────────────────
    private void drawAgent(Graphics2D g2, Agent.Snapshot snap, Agent.Role role) {
        float cx = (float) snap.x;
        float cy = (float) snap.y;

        Color core  = Theme.agentCore(role);
        Color mood  = Theme.moodColor(snap.mood);

        // Mood aura
        float pulseR = AGENT_R * 1.8f + (float)(Math.sin(pulse) * 3);
        float[] fracs = {0f, 0.6f, 1f};
        Color[] cols  = {
            Theme.withAlpha(mood, (int)(150 * Math.abs(snap.mood))),
            Theme.withAlpha(mood, (int)(50  * Math.abs(snap.mood))),
            Theme.withAlpha(mood, 0)
        };
        g2.setPaint(new RadialGradientPaint(cx, cy, pulseR, fracs, cols));
        g2.fillOval((int)(cx-pulseR), (int)(cy-pulseR),
                    (int)(pulseR*2),  (int)(pulseR*2));

        // Body fill
        g2.setColor(Theme.withAlpha(core, 200));
        g2.fillOval((int)(cx-AGENT_R), (int)(cy-AGENT_R), AGENT_R*2, AGENT_R*2);

        // Body ring
        g2.setColor(core);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval((int)(cx-AGENT_R), (int)(cy-AGENT_R), AGENT_R*2, AGENT_R*2);

        // Facing arrow — use actual continuous heading
        double arrowAngle = snap.headingRad != 0 ? snap.headingRad : snap.facing;
        float ax = (float)(cx + AGENT_R * 1.5 * Math.cos(arrowAngle));
        float ay = (float)(cy + AGENT_R * 1.5 * Math.sin(arrowAngle));
        g2.setColor(Theme.withAlpha(core, 180));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int)cx, (int)cy, (int)ax, (int)ay);

        // Centre dot
        g2.setColor(Color.WHITE);
        g2.fillOval((int)cx-3, (int)cy-3, 6, 6);

        // Role label
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(core);
        String lbl = role == Agent.Role.PREDATOR ? "P" : "Y";
        g2.drawString(lbl, cx - 4, cy - AGENT_R - 5);

        // Action label
        if (snap.chosenAction != null) {
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.withAlpha(core, 160));
            g2.drawString(snap.chosenAction.name(),
                cx - 14, cy + AGENT_R + 12);
        }
    }

    // ── Arena frame ───────────────────────────────────────────────────────────
    private void drawArenaFrame(Graphics2D g2) {
        g2.setColor(Theme.BG_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(1, 1, W-2, H-2);
        // Corner markers
        int m = 10;
        g2.setColor(new Color(0, 200, 255, 60));
        g2.drawLine(1,   1,   1+m, 1);    g2.drawLine(1, 1, 1, 1+m);
        g2.drawLine(W-2, 1,   W-2-m, 1);  g2.drawLine(W-2, 1, W-2, 1+m);
        g2.drawLine(1,   H-2, 1+m, H-2);  g2.drawLine(1, H-2, 1, H-2-m);
        g2.drawLine(W-2, H-2, W-2-m, H-2);g2.drawLine(W-2, H-2, W-2, H-2-m);
    }
}
