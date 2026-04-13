package ui;

import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Main application window — Predator/Prey Attention Demo.
 *
 * Layout (fits MacBook Pro with Dock visible):
 *
 *  ┌──────────────────────────────────────────────────────┐
 *  │  Header                                              │
 *  ├────────────┬──────────────────────────┬──────────────┤
 *  │  PREDATOR  │       ARENA              │  PREY        │
 *  │  readout   │  (attention cones,       │  readout     │
 *  │  A1-A5     │   trails, agents)        │  A1-A5       │
 *  ├────────────┴──────────────────────────┴──────────────┤
 *  │  Controls + axiom key                                │
 *  └──────────────────────────────────────────────────────┘
 */
public class MainFrame extends JFrame {

    // ── Model ─────────────────────────────────────────────────────────────────
    private Agent predator;
    private Agent prey;
    private int   stepCount = 0;

    // ── UI ────────────────────────────────────────────────────────────────────
    private final ArenaPanel         arenaPanel;
    private final AgentReadoutPanel  predReadout;
    private final AgentReadoutPanel  preyReadout;
    private final JLabel             stepLabel  = label("Step: 0");
    private final JLabel             distLabel  = label("Dist: —");
    private final JLabel             statusLabel= label("Ready");

    private final javax.swing.Timer  simTimer;
    private boolean running = false;

    public MainFrame() {
        setTitle("Predator / Prey — Attention Demo  ·  Aleksander's Five Axioms");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(Theme.BG_VOID);

        arenaPanel   = new ArenaPanel();
        predReadout  = new AgentReadoutPanel(Agent.Role.PREDATOR);
        preyReadout  = new AgentReadoutPanel(Agent.Role.PREY);

        initAgents();
        buildUI();
        pack();
        setLocationRelativeTo(null);

        simTimer = new javax.swing.Timer(120, e -> step());
    }

    private void initAgents() {
        predator = new Agent(Agent.Role.PREDATOR,
            70, 70, ArenaPanel.W, ArenaPanel.H, 42L);
        prey = new Agent(Agent.Role.PREY,
            ArenaPanel.W - 70, ArenaPanel.H - 70,
            ArenaPanel.W, ArenaPanel.H, 137L);
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0,0));
        add(buildHeader(),   BorderLayout.NORTH);
        add(buildCentre(),   BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(0x030611));
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BG_BORDER),
            new EmptyBorder(6,14,6,14)));

        JLabel title = new JLabel("PREDATOR / PREY  ·  ATTENTION DEMO");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_ACCENT);

        JLabel sub = new JLabel(
            "Attention cone direction & width driven by affective state  ·  " +
            "Same architecture, opposed reward structures");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_SECONDARY);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(stepLabel);
        right.add(distLabel);
        right.add(statusLabel);

        h.add(left,  BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildCentre() {
        JPanel c = new JPanel(new BorderLayout(0,0));
        c.setBackground(Theme.BG_VOID);

        c.add(predReadout, BorderLayout.WEST);
        c.add(arenaPanel,  BorderLayout.CENTER);
        c.add(preyReadout, BorderLayout.EAST);
        return c;
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bar.setBackground(new Color(0x030611));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BG_BORDER));

        JLabel speedLbl = label("Speed:");
        JSlider speed = new JSlider(30, 950, 120);
        speed.setOpaque(false);
        speed.setPreferredSize(new Dimension(110, 20));
        speed.addChangeListener(e -> simTimer.setDelay(1000 - speed.getValue()));

        JButton stepBtn  = btn("⏭ Step",   Theme.AXIOM_1);
        JButton runBtn   = btn("▶ Run",    Theme.AXIOM_4);
        JButton pauseBtn = btn("⏸ Pause",  Theme.AXIOM_2);
        JButton resetBtn = btn("⟳ Reset",  Theme.TEXT_SECONDARY);

        stepBtn.addActionListener (e -> { if (!running) step(); });
        runBtn.addActionListener  (e -> { running=true;  simTimer.start();  statusLabel.setText("Running"); });
        pauseBtn.addActionListener(e -> { running=false; simTimer.stop();   statusLabel.setText("Paused"); });
        resetBtn.addActionListener(e -> reset());

        bar.add(speedLbl); bar.add(speed);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(stepBtn); bar.add(runBtn); bar.add(pauseBtn); bar.add(resetBtn);

        // Axiom key
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        addKey(bar, "■ Predator", Theme.PREDATOR_CORE);
        addKey(bar, "■ Prey",     Theme.PREY_CORE);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        addKey(bar, "Cone=Attention", Theme.AXIOM_2);
        addKey(bar, "Aura=Affect",    Theme.AXIOM_5);

        return bar;
    }

    private void addKey(JPanel bar, String text, Color col) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(col);
        bar.add(l);
    }

    // ── Simulation ────────────────────────────────────────────────────────────
    private void step() {
        Agent.Snapshot predSnap = predator.tick(prey);
        Agent.Snapshot preySnap = prey.tick(predator);
        stepCount++;

        arenaPanel.update(predSnap, preySnap);
        predReadout.setSnapshot(predSnap);
        preyReadout.setSnapshot(preySnap);

        stepLabel.setText("Step: " + stepCount);

        double dx = predator.getX() - prey.getX();
        double dy = predator.getY() - prey.getY();
        double dist = Math.sqrt(dx*dx + dy*dy);
        distLabel.setText(String.format("Dist: %.0f", dist));

        if (dist < 16) {
            statusLabel.setText("CAUGHT! after " + stepCount + " steps");
            running = false;
            simTimer.stop();
        } else {
            Agent.Snapshot ps = prey.getLastSnap();
            statusLabel.setText(String.format("Stamina: %.0f%%  Spd: %.1f",
                ps.stamina * 100, ps.currentSpeed));
        }
    }

    private void reset() {
        running = false;
        simTimer.stop();
        stepCount = 0;
        stepLabel.setText("Step: 0");
        distLabel.setText("Dist: —");
        statusLabel.setText("Reset — prey stamina restored");
        initAgents();
        arenaPanel.update(null, null);
        predReadout.setSnapshot(null);
        preyReadout.setSnapshot(null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }

    private static JButton btn(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_LABEL);
        b.setForeground(Theme.TEXT_SECONDARY);
        b.setBackground(Theme.withAlpha(accent, 35));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.withAlpha(accent, 90), 1),
            new EmptyBorder(3,8,3,8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(Theme.withAlpha(accent, 70));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(Theme.withAlpha(accent, 35));
            }
        });
        return b;
    }
}
