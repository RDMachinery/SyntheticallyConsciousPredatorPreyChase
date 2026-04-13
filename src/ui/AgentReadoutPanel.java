package ui;

import model.Agent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Compact five-axiom readout for one agent.
 * Shows sensor bars, attention weights (with cone summary),
 * imagined futures, action scores, and mood gauge + sparkline.
 */
public class AgentReadoutPanel extends JPanel {

    private static final String[] SENSOR_LABELS = {"Prox","Bear","Wall","Spd","Soc"};

    private final Agent.Role role;
    private Agent.Snapshot snap;
    private final Deque<Double> moodHistory = new ArrayDeque<>();
    private static final int MAX_HIST = 60;

    public AgentReadoutPanel(Agent.Role role) {
        this.role = role;
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(200, 440));
        setBorder(BorderFactory.createMatteBorder(0,
            role == Agent.Role.PREDATOR ? 0 : 1,
            0,
            role == Agent.Role.PREDATOR ? 1 : 0,
            Theme.BG_BORDER));
    }

    public void setSnapshot(Agent.Snapshot s) {
        this.snap = s;
        if (s != null) {
            moodHistory.addLast(s.mood);
            if (moodHistory.size() > MAX_HIST) moodHistory.removeFirst();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color agentCol = Theme.agentCore(role);
        String agentName = role == Agent.Role.PREDATOR ? "PREDATOR" : "PREY";

        // ── Header ────────────────────────────────────────────────────────────
        g2.setColor(Theme.withAlpha(agentCol, 25));
        g2.fillRect(0, 0, getWidth(), 24);
        g2.setColor(agentCol);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, 24, getWidth(), 24);
        g2.setFont(Theme.FONT_AGENT);
        g2.setColor(agentCol);
        g2.drawString(agentName, 7, 17);

        if (snap == null) {
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString("Waiting...", 7, 40);
            return;
        }

        int y = 32;

        // ── Stamina bar (prey only, always shown) ─────────────────────────────
        if (role == Agent.Role.PREY) {
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString("STAMINA", 7, y + 7); y += 2;
            Color staminaCol = snap.stamina > 0.5
                ? Theme.lerp(new Color(0xFF,0xD5,0x4F), Theme.PREY_CORE, (snap.stamina - 0.5) * 2)
                : Theme.lerp(Theme.MOOD_NEG, new Color(0xFF,0xD5,0x4F), snap.stamina * 2);
            drawBar(g2, 7, y, getWidth()-14, 9, snap.stamina, staminaCol, null);
            // Speed label on bar
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(String.format("spd %.1f", snap.currentSpeed),
                getWidth() - 52, y + 8);
            y += 14;
        } else {
            // Predator: show constant stamina full bar
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString("STAMINA", 7, y + 7); y += 2;
            drawBar(g2, 7, y, getWidth()-14, 9, 1.0, Theme.PREDATOR_CORE, null);
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString("∞ constant", getWidth() - 64, y + 8);
            y += 14;
        }

        // ── Axiom 1: Presence ─────────────────────────────────────────────────
        y = sectionHeader(g2, "A1 PRESENCE", Theme.AXIOM_1, y);
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("States: " + snap.stateCount, 7, y); y += 11;
        if (snap.rawSensors != null) {
            for (int i = 0; i < snap.rawSensors.length; i++) {
                drawBar(g2, 7, y, getWidth()-14, 6,
                        Math.abs(snap.rawSensors[i]), Theme.AXIOM_1, SENSOR_LABELS[i]);
                y += 9;
            }
        }
        y += 3;

        // ── Axiom 2: Attention ────────────────────────────────────────────────
        y = sectionHeader(g2, "A2 ATTENTION", Theme.AXIOM_2, y);
        if (snap.attentionWeights != null) {
            for (int i = 0; i < snap.attentionWeights.length; i++) {
                // Raw (dim)
                drawBar(g2, 7, y, getWidth()-14, 6,
                        snap.rawSensors != null ? Math.abs(snap.rawSensors[i]) : 0,
                        new Color(50, 80, 110), null);
                // Focused (bright)
                drawBar(g2, 7, y, (int)((getWidth()-14) *
                        Math.min(1, snap.attentionWeights[i] / 2.0)), 6, 1.0,
                        Theme.AXIOM_2, SENSOR_LABELS[i]);
                y += 9;
            }
        }
        // Cone summary
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        String coneDir = snap.mood >= 0 ? "→ toward" : "← away";
        String coneW   = String.format("%.0f°", Math.toDegrees(snap.attentionWidth * 2));
        g2.drawString(String.format("Cone: %s  width:%s", coneDir, coneW), 7, y); y += 12;
        y += 2;

        // ── Axiom 3: Imagination ──────────────────────────────────────────────
        y = sectionHeader(g2, "A3 IMAGINATION", Theme.AXIOM_3, y);
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("Imagined futures:", 7, y); y += 10;
        if (snap.imaginedDistances != null) {
            int dotX = 7;
            for (double d : snap.imaginedDistances) {
                Color dc = role == Agent.Role.PREDATOR
                    ? Theme.lerp(Theme.MOOD_NEG, Theme.PREDATOR_CORE, d)
                    : Theme.lerp(Theme.PREY_CORE, Theme.MOOD_NEG, d);
                g2.setColor(Theme.withAlpha(dc, 200));
                g2.fillOval(dotX, y, 9, 9);
                g2.setColor(Theme.withAlpha(dc, 80));
                g2.drawOval(dotX, y, 9, 9);
                dotX += 14;
            }
            y += 13;
        }
        y += 3;

        // ── Axiom 4: Planning ─────────────────────────────────────────────────
        y = sectionHeader(g2, "A4 PLANNING", Theme.AXIOM_4, y);
        if (snap.actionScores != null) {
            double min = snap.actionScores.values().stream().mapToDouble(v->v).min().orElse(0);
            double max = snap.actionScores.values().stream().mapToDouble(v->v).max().orElse(1);
            double range = Math.max(0.001, max - min);
            for (Map.Entry<model.Action, Double> e : snap.actionScores.entrySet()) {
                boolean chosen = e.getKey() == snap.chosenAction;
                Color fill = chosen ? Theme.AXIOM_4 : new Color(30, 70, 50);
                double norm = (e.getValue() - min) / range;
                drawBar(g2, 7, y, getWidth()-14, 9, norm, fill,
                        e.getKey().name());
                if (chosen) {
                    g2.setFont(Theme.FONT_SMALL);
                    g2.setColor(Theme.AXIOM_4);
                    g2.drawString("◄", getWidth()-14, y+8);
                }
                y += 12;
            }
        }
        y += 3;

        // ── Axiom 5: Affective state ──────────────────────────────────────────
        y = sectionHeader(g2, "A5 AFFECTIVE STATE", Theme.AXIOM_5, y);
        int gaugeH = 28;
        drawMoodGauge(g2, 7, y, getWidth()-14, gaugeH, snap.mood);
        y += gaugeH + 4;
        // Sparkline
        int sparkH = getHeight() - y - 18;
        if (sparkH > 16) {
            drawSparkline(g2, 7, y, getWidth()-14, sparkH, agentCol);
            y += sparkH;
        }
        // Reward summary
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(String.format("R:%+.2f  Σ:%+.1f",
            snap.lastReward, snap.cumulativeReward), 7, getHeight()-5);
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────
    private int sectionHeader(Graphics2D g2, String text, Color col, int y) {
        g2.setColor(Theme.withAlpha(col, 20));
        g2.fillRect(0, y-9, getWidth(), 12);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawLine(0, y+3, getWidth(), y+3);
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(col);
        g2.drawString(text, 5, y+1);
        return y + 13;
    }

    private void drawBar(Graphics2D g2, int x, int y, int w, int h,
                         double fraction, Color fill, String label) {
        g2.setColor(Theme.BG_CARD);
        g2.fillRoundRect(x, y, w, h, 2, 2);
        g2.setColor(fill);
        g2.fillRoundRect(x, y, (int)(w * Math.max(0, Math.min(1, fraction))), h, 2, 2);
        if (label != null) {
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(label, x+2, y+h-1);
        }
    }

    private void drawMoodGauge(Graphics2D g2, int x, int y, int w, int h, double mood) {
        Color mc = Theme.moodColor(mood);
        g2.setColor(Theme.BG_CARD);
        g2.fillArc(x, y, w, h*2, 0, 180);
        g2.setColor(Theme.withAlpha(mc, 160));
        g2.fillArc(x, y, w, h*2, 90, (int)(mood*90));
        double na = Math.toRadians(90 - mood*90);
        int ncx = x+w/2, ncy = y+h;
        int len = h-4;
        g2.setColor(mc);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(ncx, ncy, ncx+(int)(len*Math.cos(na)), ncy-(int)(len*Math.sin(na)));
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(mc);
        g2.drawString(String.format("%+.2f", mood), ncx-16, ncy-3);
    }

    private void drawSparkline(Graphics2D g2, int x, int y, int w, int h, Color col) {
        if (moodHistory.size() < 2) return;
        g2.setColor(Theme.BG_CARD);
        g2.fillRoundRect(x, y, w, h, 3, 3);
        int zeroY = y + h/2;
        g2.setColor(Theme.BG_BORDER);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawLine(x, zeroY, x+w, zeroY);
        Path2D path = new Path2D.Float();
        Double[] vals = moodHistory.toArray(new Double[0]);
        boolean first = true;
        for (int i = 0; i < vals.length; i++) {
            float px = x + (float)i / (MAX_HIST-1) * w;
            float py = (float)(zeroY - vals[i] * (h/2-2));
            if (first) { path.moveTo(px,py); first=false; } else path.lineTo(px,py);
        }
        g2.setColor(Theme.withAlpha(col, 200));
        g2.setStroke(new BasicStroke(1.3f));
        g2.draw(path);
    }
}
