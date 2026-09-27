package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Persists user-created levels as simple text files in the user's home directory. */
public final class SavedLevelManager {
    private static final String HEADER = "ROBOT_PATH_FINDING_LEVEL_V2";
    private static final String LEGACY_HEADER = "ROBOT_PATH_FINDING_LEVEL_V1";
    private static final Path DIRECTORY = Paths.get(System.getProperty("user.home"), ".robotpathfinding", "levels");

    private SavedLevelManager() {}

    public static void save(String name, Grid grid) throws IOException {
        Files.createDirectories(DIRECTORY);
        // Keep the filename ASCII-only; the visible level name is stored as UTF-8 Base64.
        // This avoids Windows/locale issues when users enter Vietnamese or emoji.
        String safeFileName = "level_" + System.currentTimeMillis() + "_" + Integer.toUnsignedString(name.hashCode());
        Path file = uniqueFile(DIRECTORY.resolve(safeFileName + ".rpl"));
        StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        out.append(Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8))).append('\n');
        out.append(grid.rows()).append(' ').append(grid.cols()).append('\n');
        out.append(grid.start().row()).append(' ').append(grid.start().col()).append('\n');
        out.append(grid.goal().row()).append(' ').append(grid.goal().col()).append('\n');
        boolean[][] walls = grid.copyWalls();
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) out.append(walls[r][c] ? '1' : '0');
            out.append('\n');
        }
        int[][] costs = grid.copyCosts();
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) out.append(costs[r][c]).append(c + 1 == grid.cols() ? '\n' : ' ');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    public static List<SavedLevel> list() {
        List<SavedLevel> result = new ArrayList<>();
        try {
            if (!Files.exists(DIRECTORY)) return result;
            try (var stream = Files.list(DIRECTORY)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".rpl"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                        .forEach(p -> {
                            try {
                                result.add(new SavedLevel(readName(p), p));
                            } catch (Exception ignored) {
                                // Ignore damaged/old files rather than breaking the app.
                            }
                        });
            }
        } catch (IOException ignored) {}
        return result;
    }

    public static Level load(SavedLevel saved) throws IOException {
        List<String> lines = Files.readAllLines(saved.file(), StandardCharsets.UTF_8);
        if (lines.size() < 5 || (!HEADER.equals(lines.get(0)) && !LEGACY_HEADER.equals(lines.get(0)))) {
            throw new IOException("File level không hợp lệ.");
        }
        String[] size = lines.get(2).trim().split(" ");
        int rows = Integer.parseInt(size[0]);
        int cols = Integer.parseInt(size[1]);
        String[] s = lines.get(3).trim().split(" ");
        String[] g = lines.get(4).trim().split(" ");
        int sr = Integer.parseInt(s[0]), sc = Integer.parseInt(s[1]);
        int gr = Integer.parseInt(g[0]), gc = Integer.parseInt(g[1]);
        if (rows <= 0 || cols <= 0 || lines.size() < 5 + rows) throw new IOException("Dữ liệu level bị thiếu.");

        boolean[][] walls = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            String row = lines.get(5 + r).trim();
            if (row.length() != cols) throw new IOException("Kích thước bản đồ không hợp lệ.");
            for (int c = 0; c < cols; c++) walls[r][c] = row.charAt(c) == '1';
        }
        if (LEGACY_HEADER.equals(lines.get(0))) {
            return new Level(saved.name(), "💾 LEVEL ĐÃ LƯU", sr, sc, gr, gc, walls);
        }
        if (lines.size() < 5 + rows * 2) throw new IOException("Dữ liệu chi phí level bị thiếu.");
        int[][] costs = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            String[] values = lines.get(5 + rows + r).trim().split("\\s+");
            if (values.length != cols) throw new IOException("Ma trận chi phí không hợp lệ.");
            for (int c = 0; c < cols; c++) costs[r][c] = Integer.parseInt(values[c]);
        }
        return new Level(saved.name(), "💾 LEVEL ĐÃ LƯU", sr, sc, gr, gc, walls, costs);
    }

    private static String readName(Path p) throws IOException {
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        if (lines.size() < 2 || (!HEADER.equals(lines.get(0)) && !LEGACY_HEADER.equals(lines.get(0)))) throw new IOException("Invalid level");
        return new String(Base64.getDecoder().decode(lines.get(1)), StandardCharsets.UTF_8);
    }

    private static Path uniqueFile(Path original) throws IOException {
        if (!Files.exists(original)) return original;
        String file = original.getFileName().toString();
        int dot = file.lastIndexOf('.');
        String base = dot > 0 ? file.substring(0, dot) : file;
        String ext = dot > 0 ? file.substring(dot) : "";
        for (int i = 2; i < 10000; i++) {
            Path candidate = original.resolveSibling(base + "_" + i + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        throw new IOException("Không thể tạo tên file level mới.");
    }

    public record SavedLevel(String name, Path file) {}
}
