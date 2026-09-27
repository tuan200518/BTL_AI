package model;

import java.util.*;

/**
 * 12 định nghĩa level cố định.
 *
 * 6 level đầu là mê cung dạng ma trận thực sự, 6 level sau là bản đồ có chướng ngại vật
 * theo dạng vật cản và rào chắn, dễ nhìn khác biệt giữa BFS, Dijkstra và A*.
 */
public final class LevelManager {
    private static final int ROWS = 20;
    private static final int COLS = 28;
    private static final int START_R = 1;
    private static final int START_C = 1;
    private static final int GOAL_R = 17;
    private static final int GOAL_C = 27;

    private LevelManager() {}

    public static Level[] all() {
        Level[] mazeLevels = buildMazeLevels();
        Level[] obstacleLevels = buildObstacleLevels();

        Level[] levels = new Level[mazeLevels.length + obstacleLevels.length];
        System.arraycopy(mazeLevels, 0, levels, 0, mazeLevels.length);
        System.arraycopy(obstacleLevels, 0, levels, mazeLevels.length, obstacleLevels.length);
        return levels;
    }

    private static Level[] buildMazeLevels() {
        String[] names = {
                "Level 1 • Mê cung nhập môn",
                "Level 2 • Những ngã rẽ",
                "Level 3 • Mê cung quanh co",
                "Level 4 • Nhiều ngõ cụt",
                "Level 5 • Mê cung phức tạp",
                "Level 6 • Mê cung thử thách"
        };
        String[] difficulty = {
                "★☆☆☆☆", "★★☆☆☆", "★★★☆☆", "★★★★☆", "★★★★★", "★★★★★+"
        };
        long[] seeds = {37L, 1L, 3L, 10L, 5L, 442L};

        Level[] levels = new Level[names.length];
        for (int i = 0; i < names.length; i++) {
            levels[i] = new Level(names[i], difficulty[i], START_R, START_C, GOAL_R, GOAL_C, createMaze(seeds[i]));
        }
        return levels;
    }

    private static Level[] buildObstacleLevels() {
        String[] names = {
                "Level 7 • Rào chắn hẹp",
                "Level 8 • Khối vật cản lớn",
                "Level 9 • Hàng rào răng cưa",
                "Level 10 • Lưới cản đa tầng",
                "Level 11 • Vách ngăn xoắn ốc",
                "Level 12 • Bãi chướng ngại trọng số"
        };
        String[] difficulty = {
                "★★☆☆☆", "★★★☆☆", "★★★☆☆", "★★★★☆", "★★★★☆", "★★★★★"
        };
        int[] starts = {1, 1, 1, 1, 1, 1};
        int[] startCols = {1, 1, 1, 1, 1, 1};
        int[] goals = {17, 17, 17, 17, 17, 17};
        int[] goalCols = {27, 26, 25, 26, 27, 27};

        Level[] levels = new Level[names.length];
        for (int i = 0; i < names.length; i++) {
            levels[i] = new Level(
                    names[i], difficulty[i],
                    starts[i], startCols[i],
                    goals[i], goalCols[i],
                    createObstaclePattern(i + 1)
            );
        }
        return levels;
    }

    /** Creates a perfect maze using recursive backtracking. */
    private static boolean[][] createMaze(long seed) {
        boolean[][] walls = new boolean[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) Arrays.fill(walls[r], true);

        Random random = new Random(seed);
        boolean[][] visited = new boolean[ROWS][COLS];
        carve(walls, visited, random, START_R, START_C);

        walls[GOAL_R][GOAL_C] = false;
        walls[GOAL_R][GOAL_C - 1] = false;
        walls[START_R][START_C] = false;
        return walls;
    }

    private static boolean[][] createObstaclePattern(int pattern) {
        boolean[][] walls = new boolean[ROWS][COLS];

        switch (pattern) {
            case 1 -> {
                addVerticalGate(walls, 7, 3, 9);
                addVerticalGate(walls, 16, 10, 16);
                addVerticalGate(walls, 23, 4, 10);
            }
            case 2 -> {
                addVerticalGate(walls, 5, 2, 6);
                addVerticalGate(walls, 11, 8, 12);
                addVerticalGate(walls, 17, 13, 17);
                addVerticalGate(walls, 23, 5, 9);
                addRectangle(walls, 8, 10, 7, 9, true);
                addRectangle(walls, 2, 4, 13, 15, true);
                addRectangle(walls, 14, 16, 19, 21, true);
            }
            case 3 -> {
                addHorizontalGate(walls, 4, 3, 6);
                addHorizontalGate(walls, 7, 20, 23);
                addHorizontalGate(walls, 10, 5, 8);
                addHorizontalGate(walls, 13, 19, 22);
                addHorizontalGate(walls, 16, 7, 10);
            }
            case 4 -> {
                addHorizontalGate(walls, 3, 3, 7);
                addHorizontalGate(walls, 6, 20, 24);
                addHorizontalGate(walls, 9, 4, 8);
                addHorizontalGate(walls, 12, 19, 23);
                addHorizontalGate(walls, 15, 5, 9);
            }
            case 5 -> {
                addHorizontalGate(walls, 3, 2, 4);
                addHorizontalGate(walls, 6, 23, 25);
                addHorizontalGate(walls, 9, 2, 4);
                addHorizontalGate(walls, 12, 23, 25);
                addHorizontalGate(walls, 15, 2, 4);
            }
            case 6 -> {
                addRectangle(walls, 2, 5, 5, 8, true);
                addRectangle(walls, 2, 4, 14, 18, true);
                addRectangle(walls, 3, 6, 22, 24, true);
                addRectangle(walls, 7, 10, 3, 6, true);
                addRectangle(walls, 7, 9, 11, 15, true);
                addRectangle(walls, 6, 10, 20, 23, true);
                addRectangle(walls, 11, 14, 7, 10, true);
                addRectangle(walls, 11, 14, 16, 19, true);
                addRectangle(walls, 11, 15, 24, 26, true);
                addRectangle(walls, 15, 18, 3, 7, true);
                addRectangle(walls, 15, 18, 12, 16, true);
                addRectangle(walls, 15, 18, 20, 23, true);
            }
            default -> {}
        }

        walls[0][0] = false;
        walls[ROWS - 1][COLS - 1] = false;
        walls[START_R][START_C] = false;
        walls[GOAL_R][GOAL_C] = false;
        return walls;
    }

    private static void addVerticalGate(boolean[][] walls, int col, int gapStart, int gapEnd) {
        for (int r = 0; r < ROWS; r++) {
            if (r < gapStart || r > gapEnd) walls[r][col] = true;
        }
    }

    private static void addHorizontalGate(boolean[][] walls, int row, int gapStart, int gapEnd) {
        for (int c = 0; c < COLS; c++) {
            if (c < gapStart || c > gapEnd) walls[row][c] = true;
        }
    }

    private static void addRectangle(boolean[][] walls, int rowStart, int rowEnd, int colStart, int colEnd, boolean fill) {
        for (int r = rowStart; r <= rowEnd; r++) {
            for (int c = colStart; c <= colEnd; c++) {
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) walls[r][c] = fill;
            }
        }
    }

    private static void carve(boolean[][] walls, boolean[][] visited,
                              Random random, int r, int c) {
        visited[r][c] = true;
        walls[r][c] = false;

        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        shuffle(dirs, random);

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr < 1 || nr > 17 || nc < 1 || nc > 25) continue;
            if (visited[nr][nc]) continue;

            walls[r + d[0] / 2][c + d[1] / 2] = false;
            carve(walls, visited, random, nr, nc);
        }
    }

    private static void shuffle(int[][] dirs, Random random) {
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] tmp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = tmp;
        }
    }
}
