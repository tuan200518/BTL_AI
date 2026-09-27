package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public final class MazeGenerator {
    private MazeGenerator() {}

    public static Level generate(int rows, int cols, int difficulty, long seed) {
        if (rows < 7 || cols < 7) throw new IllegalArgumentException("Grid quá nhỏ để tạo mê cung.");

        boolean[][] walls = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) walls[r][c] = true;
        }


        Random random = new Random(seed);
        int maxR = rows - 2;
        int maxC = cols - 2;
        int startR = 1, startC = 1;
        walls[startR][startC] = false;
        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(new Node(startR, startC));
        boolean[][] visited = new boolean[rows][cols];
        visited[startR][startC] = true;
        int[][] dirs = {{-2,0},{2,0},{0,-2},{0,2}};

        while (!stack.isEmpty()) {
            Node cur = stack.peek();
            List<int[]> candidates = new ArrayList<>();
            for (int[] d : dirs) {
                int nr = cur.row() + d[0], nc = cur.col() + d[1];
                if (nr >= 1 && nr <= maxR && nc >= 1 && nc <= maxC && !visited[nr][nc]) {
                    candidates.add(new int[]{nr, nc, d[0] / 2, d[1] / 2});
                }
            }
            if (candidates.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = candidates.get(random.nextInt(candidates.size()));
            int nr = next[0], nc = next[1];
            int wallR = cur.row() + next[2], wallC = cur.col() + next[3];
            walls[wallR][wallC] = false;
            walls[nr][nc] = false;
            visited[nr][nc] = true;
            stack.push(new Node(nr, nc));
        }

        int goalR = rows - 2, goalC = cols - 2;
        walls[goalR][goalC] = false;
        walls[goalR][goalC - 1] = false;
        walls[goalR - 1][goalC] = false;

        int openings = switch (difficulty) {
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 11;
            default -> 15;
        };
        for (int i = 0; i < openings; i++) {
            int r = 2 + random.nextInt(Math.max(1, rows - 4));
            int c = 2 + random.nextInt(Math.max(1, cols - 4));
            if (walls[r][c]) {
                int openNeighbors = countOpenNeighbors(walls, r, c);
                if (openNeighbors >= 2) walls[r][c] = false;
            }
        }

        walls[startR][startC] = false;
        walls[goalR][goalC] = false;
        if (!reachable(walls, startR, startC, goalR, goalC)) {
            carveConnector(walls, startR, startC, goalR, goalC, random);
        }

        String name = "Mê cung • Độ khó " + difficulty;
        String stars = "★".repeat(Math.min(5, difficulty)) + "☆".repeat(Math.max(0, 5 - difficulty));
        return new Level(name, stars, startR, startC, goalR, goalC, walls);
    }

    private static int countOpenNeighbors(boolean[][] w, int r, int c) {
        int count = 0;
        int[][] d = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] x : d) {
            int nr=r+x[0], nc=c+x[1];
            if (nr>=0 && nr<w.length && nc>=0 && nc<w[0].length && !w[nr][nc]) count++;
        }
        return count;
    }

    private static boolean reachable(boolean[][] w, int sr, int sc, int gr, int gc) {
        boolean[][] seen = new boolean[w.length][w[0].length];
        ArrayDeque<Node> q = new ArrayDeque<>();
        q.add(new Node(sr,sc)); seen[sr][sc]=true;
        int[][] d={{-1,0},{1,0},{0,-1},{0,1}};
        while(!q.isEmpty()) {
            Node n=q.remove();
            if(n.row()==gr && n.col()==gc) return true;
            for(int[] x:d) {
                int r=n.row()+x[0], c=n.col()+x[1];
                if(r>=0&&r<w.length&&c>=0&&c<w[0].length&&!w[r][c]&&!seen[r][c]) {
                    seen[r][c]=true; q.add(new Node(r,c));
                }
            }
        }
        return false;
    }

    private static void carveConnector(boolean[][] w, int sr, int sc, int gr, int gc, Random random) {
  
        int r=sr, c=sc;
        while (r!=gr || c!=gc) {
            boolean horizontalFirst = random.nextBoolean();
            if (horizontalFirst && c!=gc) c += Integer.compare(gc,c);
            else if (r!=gr) r += Integer.compare(gr,r);
            else c += Integer.compare(gc,c);
            w[r][c]=false;
            if (random.nextDouble()<0.35 && r!=gr) { r += Integer.compare(gr,r); w[r][c]=false; }
            if (random.nextDouble()<0.35 && c!=gc) { c += Integer.compare(gc,c); w[r][c]=false; }
        }
    }
}
