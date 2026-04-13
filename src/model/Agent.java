package model;

import java.util.*;

/**
 * Synthetic mind agent — Aleksander's Five Axioms.
 *
 * Stamina model:
 *   - Prey starts with a speed advantage (4.2 vs 2.8) but has a stamina
 *     pool that drains while running. As stamina falls, prey speed falls
 *     linearly toward the predator's speed and then below it.
 *   - Predator has effectively infinite stamina — its speed is constant.
 *   - Stamina drains faster when the prey is fleeing hard (high |mood|).
 *   - At zero stamina the prey crawls at 1.4 px/tick — half the predator
 *     speed — so capture is inevitable once exhausted.
 *   - Stamina is exposed on the snapshot so the UI can show an energy bar.
 */
public class Agent {

    public enum Role { PREDATOR, PREY }

    public static class Snapshot {
        public double[] rawSensors;
        public int      stateCount;
        public double[] attentionWeights;
        public double[] focusedSensors;
        public double   attentionDirection;
        public double   attentionWidth;
        public double[] imaginedDistances;
        public Action   chosenAction;
        public Map<Action, Double> actionScores;
        public double   mood;
        public double   lastReward;
        public double   cumulativeReward;
        public double   x, y, facing;
        public double   headingRad;
        public double   stamina;       // 0.0 – 1.0 (prey only; predator always 1.0)
        public double   currentSpeed;  // actual speed this tick
    }

    public final Role   role;
    public final String name;

    private double x, y;
    private double headingRad;

    // Base speeds
    private static final double PREDATOR_SPEED    = 2.2;   // slower predator
    private static final double PREY_SPEED_MAX    = 4.8;   // much faster when fresh
    private static final double PREY_SPEED_MIN    = 1.6;   // exhausted — slower than predator

    // Stamina drains very slowly — prey should run freely for ~800+ steps
    private static final double STAMINA_DRAIN_BASE  = 0.00008; // per tick
    private static final double STAMINA_DRAIN_FEAR  = 0.00010; // extra when frightened
    private static final double STAMINA_RECOVER     = 0.000005;

    private double stamina = 1.0;  // prey starts fully rested

    private final double arenaW, arenaH;
    private final Random rng;

    private final NeuralStateSpace stateSpace = new NeuralStateSpace();
    private double mood = 0.0;
    private double cumulativeReward = 0.0;

    private static final int NUM_IMAGINED = 4;
    private Snapshot lastSnap = new Snapshot();

    public Agent(Role role, double startX, double startY,
                 double arenaW, double arenaH, long seed) {
        this.role       = role;
        this.name       = role == Role.PREDATOR ? "PREDATOR" : "PREY";
        this.x          = startX;
        this.y          = startY;
        this.arenaW     = arenaW;
        this.arenaH     = arenaH;
        this.rng        = new Random(seed);
        this.headingRad = rng.nextDouble() * Math.PI * 2;
        this.stamina    = 1.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    public Snapshot tick(Agent other) {
        Snapshot snap = new Snapshot();

        // ── STAMINA ───────────────────────────────────────────────────────────
        double currentSpeed;
        if (role == Role.PREDATOR) {
            currentSpeed = PREDATOR_SPEED;
            stamina = 1.0; // predator never tires
        } else {
            // Drain stamina; drain faster under emotional stress (fear)
            double drain = STAMINA_DRAIN_BASE;
            if (Math.abs(mood) > 0.5) drain += STAMINA_DRAIN_FEAR;
            stamina = Math.max(0.0, stamina - drain);
            // Speed falls linearly from max to min as stamina goes 1→0
            currentSpeed = PREY_SPEED_MIN + stamina * (PREY_SPEED_MAX - PREY_SPEED_MIN);
        }
        snap.stamina      = stamina;
        snap.currentSpeed = currentSpeed;

        // ── AXIOM 1: PRESENCE ─────────────────────────────────────────────────
        double[] sensors = buildSensors(other);
        stateSpace.perceive(sensors);
        snap.rawSensors = sensors.clone();
        snap.stateCount = stateSpace.size();

        // ── AXIOM 2: ATTENTION ────────────────────────────────────────────────
        double[] weights = computeAttentionWeights(sensors);
        double[] focused = new double[sensors.length];
        for (int i = 0; i < sensors.length; i++)
            focused[i] = sensors[i] * weights[i];
        snap.attentionWeights = weights.clone();
        snap.focusedSensors   = focused.clone();

        double bearingToOther = Math.atan2(other.y - y, other.x - x);
        snap.attentionDirection = mood >= 0 ? bearingToOther : bearingToOther + Math.PI;
        snap.attentionWidth = (Math.PI / 2.0) * (1.0 - 0.75 * Math.abs(mood));

        // ── AXIOM 3: IMAGINATION ──────────────────────────────────────────────
        double[] imgDists = new double[NUM_IMAGINED];
        NeuralState[] imagined = new NeuralState[NUM_IMAGINED];
        for (int i = 0; i < NUM_IMAGINED; i++) {
            double[] p = sensors.clone();
            for (int j = 0; j < p.length; j++) p[j] += rng.nextGaussian() * 0.1;
            imagined[i] = new NeuralState(p);
            imgDists[i] = Math.max(0, Math.min(1, p[0]));
        }
        snap.imaginedDistances = imgDists;

        // ── AXIOM 4: PLANNING — compute new heading ───────────────────────────
        headingRad = computeHeading(other, sensors, currentSpeed);
        snap.chosenAction  = headingToAction(headingRad);
        snap.actionScores  = buildDisplayScores(snap.chosenAction);
        snap.headingRad    = headingRad;

        // ── AXIOM 5: AFFECTIVE SYSTEM ─────────────────────────────────────────
        double reward = computeReward(other);
        cumulativeReward += reward;
        mood = mood * 0.8 + reward * 0.2;
        mood = Math.max(-1.0, Math.min(1.0, mood));
        stateSpace.learn(sensors, reward);

        snap.mood             = mood;
        snap.lastReward       = reward;
        snap.cumulativeReward = cumulativeReward;

        // ── MOVE ──────────────────────────────────────────────────────────────
        x = clamp(x + Math.cos(headingRad) * currentSpeed, 12, arenaW - 12);
        y = clamp(y + Math.sin(headingRad) * currentSpeed, 12, arenaH - 12);

        snap.x = x; snap.y = y; snap.facing = headingRad;
        lastSnap = snap;
        return snap;
    }

    // ── Heading computation (the core of each role's behaviour) ──────────────
    private double computeHeading(Agent other, double[] sensors, double currentSpeed) {

        double bearingToOther   = Math.atan2(other.y - y, other.x - x);
        double bearingFromOther = bearingToOther + Math.PI;

        if (role == Role.PREDATOR) {
            double wobble = rng.nextGaussian() * 0.22;
            return bearingToOther + wobble;

        } else {
            double escapeHeading = bearingFromOther;

            double wallForceX = 0, wallForceY = 0;
            double margin = 80.0;

            if (x < margin)          wallForceX += (margin - x) / margin;
            if (x > arenaW - margin) wallForceX -= (x - (arenaW - margin)) / margin;
            if (y < margin)          wallForceY += (margin - y) / margin;
            if (y > arenaH - margin) wallForceY -= (y - (arenaH - margin)) / margin;

            double wallMagnitude = Math.sqrt(wallForceX*wallForceX + wallForceY*wallForceY);
            if (wallMagnitude > 0.01) {
                double wallHeading = Math.atan2(wallForceY, wallForceX);
                double wallWeight  = Math.min(1.0, wallMagnitude * 1.5);
                escapeHeading = blendAngles(escapeHeading, wallHeading, wallWeight * 0.6);
            }

            escapeHeading += rng.nextGaussian() * 0.08;
            return escapeHeading;
        }
    }

    // ── Sensors ───────────────────────────────────────────────────────────────
    private double[] buildSensors(Agent other) {
        double dx = other.x - x, dy = other.y - y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        double maxDist = Math.sqrt(arenaW*arenaW + arenaH*arenaH);

        double proximity  = 1.0 - Math.min(1.0, dist / maxDist);
        double absB       = Math.atan2(dy, dx);
        double relBearing = normaliseAngle(absB - headingRad) / Math.PI;
        double wallDist   = Math.min(Math.min(x, arenaW-x), Math.min(y, arenaH-y));
        double wallProx   = 1.0 - Math.min(1.0, wallDist / 120.0);

        return new double[]{proximity, relBearing, wallProx,
                            role == Role.PREDATOR ? PREDATOR_SPEED/5.0 : stamina, other.mood};
    }

    // ── Attention ─────────────────────────────────────────────────────────────
    private double[] computeAttentionWeights(double[] s) {
        double[] w = new double[s.length];
        for (int i = 0; i < s.length; i++) {
            double salience = Math.abs(s[i]);
            double boost = (i == 0 || i == 4) ? Math.abs(mood) * 0.8 : 0;
            w[i] = Math.min(2.0, salience + boost + 0.1);
        }
        return w;
    }

    // ── Reward ────────────────────────────────────────────────────────────────
    private double computeReward(Agent other) {
        double dist = dist(x, y, other.x, other.y);
        double maxDist = Math.sqrt(arenaW*arenaW + arenaH*arenaH);

        if (role == Role.PREDATOR) {
            if (dist < 22) return  1.5;
            if (dist < 60) return  0.4;
            return -(dist / maxDist) * 0.4;
        } else {
            if (dist < 22) return -1.5;
            return (dist / maxDist) * 0.5 - 0.05;
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Blend two angles by weight (0 = fully a, 1 = fully b). */
    private double blendAngles(double a, double b, double weight) {
        double dx = Math.cos(a) * (1-weight) + Math.cos(b) * weight;
        double dy = Math.sin(a) * (1-weight) + Math.sin(b) * weight;
        return Math.atan2(dy, dx);
    }

    /** Convert continuous heading to the nearest cardinal Action for display. */
    private Action headingToAction(double heading) {
        double deg = Math.toDegrees(normaliseAngle(heading));
        if (deg >= -45  && deg <  45)  return Action.EAST;
        if (deg >=  45  && deg < 135)  return Action.SOUTH;
        if (deg >= -135 && deg < -45)  return Action.NORTH;
        return Action.WEST;
    }

    /** Build a simple four-entry score map for the UI display. */
    private Map<Action, Double> buildDisplayScores(Action chosen) {
        Map<Action, Double> m = new LinkedHashMap<>();
        for (Action a : Action.values())
            m.put(a, a == chosen ? 1.0 : rng.nextDouble() * 0.4);
        return m;
    }

    private double dist(double ax, double ay, double bx, double by) {
        double dx = ax-bx, dy = ay-by;
        return Math.sqrt(dx*dx + dy*dy);
    }

    private double normaliseAngle(double a) {
        while (a >  Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public double  getX()          { return x; }
    public double  getY()          { return y; }
    public double  getMood()       { return mood; }
    public Snapshot getLastSnap()  { return lastSnap; }
    public void setPosition(double px, double py) { x = px; y = py; }
}
