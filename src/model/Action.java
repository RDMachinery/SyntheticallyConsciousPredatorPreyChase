package model;

public enum Action {
    NORTH, SOUTH, EAST, WEST, WAIT;

    public int dx() {
        return switch (this) { case EAST -> 1; case WEST -> -1; default -> 0; };
    }
    public int dy() {
        return switch (this) { case SOUTH -> 1; case NORTH -> -1; default -> 0; };
    }
}
