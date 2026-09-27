package algorithm;

import model.Grid;
import model.Node;
import java.util.List;

public interface PathFinder {
    SearchResult search(Grid grid);
    String name();

    record SearchResult(List<Node> path, List<Node> visitedOrder, long elapsedNanos, int totalCost) {
        public boolean found() { return !path.isEmpty(); }
        public int steps() { return Math.max(0, path.size() - 1); }
    }
}
