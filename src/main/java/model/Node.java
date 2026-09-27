package model;

import java.util.Objects;

public class Node {
    private final int row;
    private final int col;
    private int gCost = Integer.MAX_VALUE;
    private int hCost;
    private Node parent;

    public Node(int row, int col) { this.row = row; this.col = col; }
    public int row() { return row; }
    public int col() { return col; }
    public int gCost() { return gCost; }
    public void gCost(int value) { gCost = value; }
    public int hCost() { return hCost; }
    public void hCost(int value) { hCost = value; }
    public int fCost() { return gCost + hCost; }
    public Node parent() { return parent; }
    public void parent(Node value) { parent = value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node n)) return false;
        return row == n.row && col == n.col;
    }
    @Override public int hashCode() { return Objects.hash(row, col); }
}
