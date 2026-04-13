# Predator / Prey — Synthetic Mind Demo

A Java Swing simulation of predator/prey pursuit where both agents run Igor Aleksander's Five Axioms of Synthetic Mind, with visible attention cones that shift direction and width based on each agent's live affective state.

![Java](https://img.shields.io/badge/Java-17%2B-orange) ![Swing](https://img.shields.io/badge/UI-Java%20Swing-blue) ![License](https://img.shields.io/badge/license-MIT-green)

---

## What it does

Two agents chase each other around a dark arena. Both run identical cognitive architecture — only their reward structures differ. The predator is rewarded for closing distance; the prey for increasing it. Everything else — divergent attention, opposed movement, contrasting emotional arcs — emerges from that single difference.

The **prey starts faster** but has a stamina pool that drains during the chase. As the stamina arc around the prey fades from cyan to gold to red, the predator gradually closes the gap until the prey is caught.

---

## The Five Axioms

Each agent implements all five axioms every tick:

| # | Axiom | What you see |
|---|-------|--------------|
| 1 | **Presence** | Sensor bars — distance, bearing, wall proximity, social signal |
| 2 | **Attention** | Translucent cone on the arena; direction flips with mood, width narrows under intensity |
| 3 | **Imagination** | Coloured dots showing simulated future states |
| 4 | **Planning** | Action score bars; chosen direction arrow |
| 5 | **Affective State** | Mood gauge + sparkline; pulsing aura around each agent |

> **Note on terminology:** the word *consciousness* is deliberately avoided. Digital systems have no subjectivity. This models the *functional* architecture Aleksander described — a synthetic mind, not a conscious one.

---

## Build & run

Requires Java 17+.

```bash
git clone https://github.com/yourname/predator-prey-synthetic-mind
cd predator-prey-synthetic-mind
chmod +x build.sh && ./build.sh
java -jar PredatorPrey.jar
```

---

## Controls

| Control | Action |
|---------|--------|
| ▶ Run | Start simulation |
| ⏸ Pause | Pause |
| ⏭ Step | Single step |
| ⟳ Reset | Reset with fresh stamina |
| Speed slider | Adjust simulation rate |

---

## Project structure

```
src/
  model/    Agent, NeuralState, NeuralStateSpace, Action
  ui/       ArenaPanel, AgentReadoutPanel, MainFrame, Theme
  main/     PredatorPreyDemo
```

---

## Background

Based on the five axioms proposed by Igor Aleksander in *How to Build a Mind* (2000). The attention cone is the central visual idea: Axiom 2 (Attention) is modulated by Axiom 5 (Affective State), so the cone direction and width change continuously as each agent's mood shifts — making the internal state of each synthetic mind legible to an outside observer without any additional instrumentation.
