package model;

public class Grid {
    private final int rows;
    private final int cols;
    private final boolean[][] walls;
    private final int[][] costs;
    private Node start;
    private Node goal;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        walls = new boolean[rows][cols];
        costs = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) costs[r][c] = 1;
        }
        start = new Node(1, 1);
        goal = new Node(rows - 2, cols - 2);
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public Node start() { return start; }
    public Node goal() { return goal; }

    public void setStart(int row, int col) {
        if (!inBounds(row, col) || isWall(row, col) || (goal.row() == row && goal.col() == col)) return;
        start = new Node(row, col);
    }

    public void setGoal(int row, int col) {
        if (!inBounds(row, col) || isWall(row, col) || (start.row() == row && start.col() == col)) return;
        goal = new Node(row, col);
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean isWall(int row, int col) { return walls[row][col]; }

    public int costAt(int row, int col) { return costs[row][col]; }

    public void setCost(int row, int col, int cost) {
        if (inBounds(row, col) && cost >= 1) costs[row][col] = cost;
    }

    public void cycleCost(int row, int col) {
        int[] values = {1, 2, 4, 7};
        int current = costAt(row, col);
        int next = values[0];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                next = values[(i + 1) % values.length];
                break;
            }
        }
        setCost(row, col, next);
    }

    public void randomizeCosts(long seed) {
        java.util.Random random = new java.util.Random(seed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) costs[r][c] = random.nextInt(10) + 1;
        }
    }

    public int[][] copyCosts() {
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(costs[r], 0, copy[r], 0, cols);
        return copy;
    }

    public boolean[][] copyWalls() {
        boolean[][] copy = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(walls[r], 0, copy[r], 0, cols);
        return copy;
    }


    public void setWall(int row, int col, boolean wall) {
        if (!inBounds(row, col)) return;
        if ((start.row() == row && start.col() == col) || (goal.row() == row && goal.col() == col)) return;
        walls[row][col] = wall;
    }

    public void toggleWall(int row, int col) {
        if (!inBounds(row, col)) return;
        setWall(row, col, !walls[row][col]);
    }

    public void randomizeWalls(double density, long seed) {
        double clampedDensity = Math.max(0.0, Math.min(0.55, density));
        java.util.Random random = new java.util.Random(seed);
        for (int attempt = 0; attempt < 20; attempt++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) walls[r][c] = random.nextDouble() < clampedDensity;
            }
            walls[start.row()][start.col()] = false;
            walls[goal.row()][goal.col()] = false;
            if (hasPath()) return;
        }
        clearWalls();
    }

    private boolean hasPath() {
        boolean[][] seen = new boolean[rows][cols];
        java.util.ArrayDeque<Node> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        seen[start.row()][start.col()] = true;
        while (!queue.isEmpty()) {
            Node current = queue.remove();
            if (current.equals(goal)) return true;
            for (Node neighbor : neighbors(current)) {
                if (seen[neighbor.row()][neighbor.col()]) continue;
                seen[neighbor.row()][neighbor.col()] = true;
                queue.add(neighbor);
            }
        }
        return false;
    }

    public java.util.List<Node> neighbors(Node node) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        java.util.List<Node> result = new java.util.ArrayList<>(4);
        for (int[] d : dirs) {
            int nr = node.row() + d[0], nc = node.col() + d[1];
            if (inBounds(nr, nc) && !isWall(nr, nc)) result.add(new Node(nr, nc));
        }
        return result;
    }

    public void resetStartGoal() {
        start = new Node(1, 1);
        goal = new Node(rows - 2, cols - 2);
        walls[start.row()][start.col()] = false;
        walls[goal.row()][goal.col()] = false;
    }

    public void clearWalls() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) walls[r][c] = false;
        }
    }

    public void loadLevel(Level level) {
        if (level.walls().length != rows || level.walls()[0].length != cols) {
            throw new IllegalArgumentException("Kích thước level không khớp với Grid.");
        }
        for (int r = 0; r < rows; r++) {
            System.arraycopy(level.walls()[r], 0, walls[r], 0, cols);
            System.arraycopy(level.costs()[r], 0, costs[r], 0, cols);
        }
        start = new Node(level.startRow(), level.startCol());
        goal = new Node(level.goalRow(), level.goalCol());
        walls[start.row()][start.col()] = false;
        walls[goal.row()][goal.col()] = false;
    }
}
