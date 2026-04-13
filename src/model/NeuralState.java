package model;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NeuralState {
    private final double[] pattern;
    private final int hash;

    public NeuralState(double[] pattern) {
        this.pattern = pattern.clone();
        this.hash = Arrays.hashCode(discretize(pattern));
    }

    private int[] discretize(double[] p) {
        int[] d = new int[p.length];
        for (int i = 0; i < p.length; i++) d[i] = (int)(p[i] * 10);
        return d;
    }

    public double[] getPattern() { return pattern.clone(); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NeuralState n)) return false;
        return this.hash == n.hash;
    }
    @Override public int hashCode() { return hash; }

    @Override public String toString() {
        return String.format("[%s]",
            Arrays.stream(pattern).limit(3)
                .mapToObj(d -> String.format("%.2f", d))
                .collect(Collectors.joining(", ")));
    }
}
