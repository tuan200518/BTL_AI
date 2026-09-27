package ui;

import model.CellType;
import model.Grid;
import model.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/** Interactive grid: precise single-cell hover, painting, start/goal editing and panning. */
public class GridPanel extends JPanel {
    public enum EditMode { OBSTACLE, COST, START, GOAL, PAN }

    private final Grid grid;
    private CellType[][] cells;
    private int cellSize = 32;
    private EditMode editMode = EditMode.OBSTACLE;
    private boolean editing = true;
    private boolean mouseDown;
    private int lastRow = -1, lastCol = -1;
    private Point panStart;
    private int startViewportX, startViewportY;
    private int hoverRow = -1, hoverCol = -1;
    private Runnable stateChanged;
    private Runnable panChanged;

    private final Color bg = new Color(10, 15, 24);
    private final Color empty = new Color(26, 35, 48);
    private final Color gridLine = new Color(50, 63, 80);
    private final Color wall = new Color(89, 104, 126);
    private final Color visited = new Color(52, 130, 184);
    private final Color path = new Color(245, 178, 52);
    private final Color startColor = new Color(41, 176, 110);
    private final Color goalColor = new Color(232, 82, 89);
    private final Color robotColor = new Color(167, 139, 250);

    public GridPanel(Grid grid) {
        this.grid = grid;
        setOpaque(true);
        setBackground(bg);
        setFocusable(true);
        setDoubleBuffered(true);
        resetVisuals();

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                mouseDown = true;
                if (editMode == EditMode.PAN || SwingUtilities.isMiddleMouseButton(e)) beginPan(e);
                else if (SwingUtilities.isLeftMouseButton(e)) applyEdit(e.getX(), e.getY());
            }

            @Override public void mouseReleased(MouseEvent e) {
                mouseDown = false;
                panStart = null;
                lastRow = lastCol = -1;
                if (editMode == EditMode.PAN) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                else setCursor(Cursor.getDefaultCursor());
            }

            @Override public void mouseEntered(MouseEvent e) { updateHover(e.getX(), e.getY()); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1, -1); }
            @Override public void mouseMoved(MouseEvent e) { updateHover(e.getX(), e.getY()); }

            @Override public void mouseDragged(MouseEvent e) {
                updateHover(e.getX(), e.getY());
                if (editMode == EditMode.PAN || SwingUtilities.isMiddleMouseButton(e)) panTo(e);
                else if (SwingUtilities.isLeftMouseButton(e)) applyEdit(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                int oldSize = cellSize;
                int newSize = Math.max(18, Math.min(60, oldSize + (e.getWheelRotation() < 0 ? 2 : -2)));
                if (newSize != oldSize) zoomAroundMouse(e.getPoint(), oldSize, newSize);
                e.consume();
            }
        });
    }

    public void setStateChanged(Runnable r) { stateChanged = r; }
    public void setPanChanged(Runnable r) { panChanged = r; }
    public void setEditing(boolean b) { editing = b; }
    public void setEditMode(EditMode mode) {
        editMode = mode;
        setCursor(mode == EditMode.PAN ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }
    public EditMode getEditMode() { return editMode; }
    public int getCellSize() { return cellSize; }

    public void setCellSize(int size) {
        cellSize = Math.max(18, Math.min(60, size));
        revalidate();
        repaint();
    }

    private void zoomAroundMouse(Point mouse, int oldSize, int newSize) {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            Point view = viewport.getViewPosition();
            double worldX = (view.x + mouse.x) / (double) oldSize;
            double worldY = (view.y + mouse.y) / (double) oldSize;
            cellSize = newSize;
            revalidate();
            int newViewX = (int) Math.round(worldX * newSize - mouse.x);
            int newViewY = (int) Math.round(worldY * newSize - mouse.y);
            SwingUtilities.invokeLater(() -> {
                int maxX = Math.max(0, getWidth() - viewport.getWidth());
                int maxY = Math.max(0, getHeight() - viewport.getHeight());
                viewport.setViewPosition(new Point(Math.max(0, Math.min(newViewX, maxX)),
                        Math.max(0, Math.min(newViewY, maxY))));
            });
            repaint();
        } else setCellSize(newSize);
    }

    public void centerView() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            int x = Math.max(0, (getWidth() - viewport.getWidth()) / 2);
            int y = Math.max(0, (getHeight() - viewport.getHeight()) / 2);
            viewport.setViewPosition(new Point(x, y));
        }
    }

    private void beginPan(MouseEvent e) {
        panStart = e.getPoint();
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            Point p = viewport.getViewPosition();
            startViewportX = p.x;
            startViewportY = p.y;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    private void panTo(MouseEvent e) {
        if (panStart == null) return;
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            int dx = e.getX() - panStart.x;
            int dy = e.getY() - panStart.y;
            int maxX = Math.max(0, getWidth() - viewport.getWidth());
            int maxY = Math.max(0, getHeight() - viewport.getHeight());
            int x = Math.max(0, Math.min(startViewportX - dx, maxX));
            int y = Math.max(0, Math.min(startViewportY - dy, maxY));
            viewport.setViewPosition(new Point(x, y));
            if (panChanged != null) panChanged.run();
        }
    }

    /** Converts a screen coordinate to exactly one grid cell. */
    private Point cellAt(int x, int y) {
        if (x < 0 || y < 0) return null;
        int c = x / cellSize;
        int r = y / cellSize;
        return grid.inBounds(r, c) ? new Point(c, r) : null;
    }

    private void updateHover(int x, int y) {
        Point p = cellAt(x, y);
        if (p == null) setHover(-1, -1);
        else setHover(p.y, p.x);
    }

    private void setHover(int row, int col) {
        if (hoverRow == row && hoverCol == col) return;
        hoverRow = row;
        hoverCol = col;
        repaint();
    }

    private void applyEdit(int x, int y) {
        if (!editing) return;
        Point p = cellAt(x, y);
        if (p == null) return;
        int r = p.y, c = p.x;
        if (r == lastRow && c == lastCol && editMode != EditMode.OBSTACLE) return;
        lastRow = r; lastCol = c;

        switch (editMode) {
            case OBSTACLE -> {
                if (r == grid.start().row() && c == grid.start().col()) return;
                if (r == grid.goal().row() && c == grid.goal().col()) return;
                grid.toggleWall(r, c);
            }
            case COST -> {
                if (grid.isWall(r, c)) return;
                grid.cycleCost(r, c);
            }
            case START -> grid.setStart(r, c);
            case GOAL -> grid.setGoal(r, c);
            case PAN -> { return; }
        }
        syncSpecial();
        repaint();
        if (stateChanged != null) stateChanged.run();
    }

    public void resetVisuals() {
        cells = new CellType[grid.rows()][grid.cols()];
        for (int r = 0; r < grid.rows(); r++) Arrays.fill(cells[r], CellType.EMPTY);
        syncSpecial();
        revalidate();
        repaint();
    }

    private void syncSpecial() {
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) cells[r][c] = grid.isWall(r, c) ? CellType.WALL : CellType.EMPTY;
        }
        cells[grid.start().row()][grid.start().col()] = CellType.START;
        cells[grid.goal().row()][grid.goal().col()] = CellType.GOAL;
    }

    public void showVisited(Node n) {
        if (!n.equals(grid.start()) && !n.equals(grid.goal()) && !grid.isWall(n.row(), n.col())) cells[n.row()][n.col()] = CellType.VISITED;
        repaint();
    }

    public void showPath(Node n) {
        if (!n.equals(grid.start()) && !n.equals(grid.goal())) cells[n.row()][n.col()] = CellType.PATH;
        repaint();
    }

    public void showRobot(Node n) {
        for (int r = 0; r < grid.rows(); r++) for (int c = 0; c < grid.cols(); c++)
            if (cells[r][c] == CellType.ROBOT) cells[r][c] = CellType.PATH;
        cells[n.row()][n.col()] = CellType.ROBOT;
        repaint();
    }

    public void clearSearch() { resetVisuals(); }

    @Override public Dimension getPreferredSize() {
        return new Dimension(grid.cols() * cellSize, grid.rows() * cellSize);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D x = (Graphics2D) g.create();
        x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                int px = c * cellSize;
                int py = r * cellSize;
                CellType type = cells[r][c];
                Color color = switch (type) {
                    case WALL -> wall;
                    case START -> startColor;
                    case GOAL -> goalColor;
                    case VISITED -> visited;
                    case PATH -> path;
                    case ROBOT -> robotColor;
                    default -> terrainColor(grid.costAt(r, c));
                };

                x.setColor(color);
                x.fillRoundRect(px + 1, py + 1, cellSize - 2, cellSize - 2, 7, 7);
                x.setColor(gridLine);
                x.setStroke(new BasicStroke(1f));
                x.drawRoundRect(px + 1, py + 1, cellSize - 2, cellSize - 2, 7, 7);

                if (type != CellType.WALL && type != CellType.START && type != CellType.GOAL && type != CellType.ROBOT) {
                    String cost = Integer.toString(grid.costAt(r, c));
                    x.setColor(new Color(210, 220, 235, 220));
                    x.setFont(x.getFont().deriveFont(Font.BOLD, Math.max(10f, cellSize * .28f)));
                    FontMetrics fm = x.getFontMetrics();
                    x.drawString(cost, px + (cellSize - fm.stringWidth(cost)) / 2,
                            py + (cellSize - fm.getHeight()) / 2 + fm.getAscent());
                }

                // IMPORTANT: highlight only the single cell currently under the cursor.
                if (hoverRow == r && hoverCol == c) {
                    x.setColor(new Color(255, 255, 255, 38));
                    x.fillRoundRect(px + 2, py + 2, cellSize - 4, cellSize - 4, 8, 8);
                    x.setColor(new Color(255, 255, 255, 205));
                    x.setStroke(new BasicStroke(2.2f));
                    x.drawRoundRect(px + 2, py + 2, cellSize - 4, cellSize - 4, 8, 8);
                }

                if (type == CellType.START || type == CellType.GOAL || type == CellType.ROBOT) {
                    String s = type == CellType.START ? "S" : type == CellType.GOAL ? "G" : "R";
                    x.setColor(Color.WHITE);
                    x.setFont(x.getFont().deriveFont(Font.BOLD, Math.max(13f, cellSize * .48f)));
                    FontMetrics fm = x.getFontMetrics();
                    x.drawString(s, px + (cellSize - fm.stringWidth(s)) / 2,
                            py + (cellSize - fm.getHeight()) / 2 + fm.getAscent());
                }
            }
        }
        x.dispose();
    }

    private Color terrainColor(int cost) {
        return empty;
    }
}
