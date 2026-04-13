package model;

import java.util.HashMap;
import java.util.Map;

public class NeuralStateSpace {
    private final Map<NeuralState, Double> stateValues = new HashMap<>();

    public NeuralState perceive(double[] sensorInput) {
        NeuralState s = new NeuralState(sensorInput);
        stateValues.putIfAbsent(s, 0.0);
        return s;
    }

    public void learn(double[] input, double reward) {
        NeuralState s = perceive(input);
        double v = stateValues.getOrDefault(s, 0.0);
        stateValues.put(s, v + 0.1 * reward);
    }

    public double getValue(NeuralState s) {
        return stateValues.getOrDefault(s, 0.0);
    }

    public int size() { return stateValues.size(); }
}
