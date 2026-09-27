package model;

/** A predefined playable level. */
public record Level(String name, String difficulty, int startRow, int startCol,
                    int goalRow, int goalCol, boolean[][] walls, int[][] costs) {
    public Level {
        walls = copyWalls(walls);
        costs = copyCosts(costs, walls.length, walls[0].length);
    }

    public Level(String name, String difficulty, int startRow, int startCol,
                 int goalRow, int goalCol, boolean[][] walls) {
        this(name, difficulty, startRow, startCol, goalRow, goalCol, walls, defaultCosts(walls));
    }

    private static boolean[][] copyWalls(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int r = 0; r < source.length; r++) copy[r] = source[r].clone();
        return copy;
    }

    private static int[][] copyCosts(int[][] source, int rows, int cols) {
        if (source == null || source.length != rows) throw new IllegalArgumentException("Ma trận chi phí không hợp lệ.");
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            if (source[r] == null || source[r].length != cols) throw new IllegalArgumentException("Ma trận chi phí không hợp lệ.");
            for (int c = 0; c < cols; c++) {
                if (source[r][c] < 1) throw new IllegalArgumentException("Chi phí ô phải lớn hơn 0.");
                copy[r][c] = source[r][c];
            }
        }
        return copy;
    }

    private static int[][] defaultCosts(boolean[][] walls) {
        int[][] costs = new int[walls.length][walls[0].length];
        int[] values = {1, 2, 4, 7};
        for (int r = 0; r < walls.length; r++) {
            for (int c = 0; c < walls[r].length; c++) {
                costs[r][c] = walls[r][c] ? 1 : values[Math.floorMod(r * 31 + c * 17, values.length)];
            }
        }
        return costs;
    }
}
