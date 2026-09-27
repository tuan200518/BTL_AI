package ui;

import algorithm.*;
import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/** Main application window for the Robot Path Finding simulator. */
public class MainFrame extends JFrame {
    private static final int ROWS = 20;
    private static final int COLS = 28;
    private static final int MAZE_INDEX = 12;
    private static final int RANDOM_WALL_INDEX = 13;
    private static final int CUSTOM_INDEX = 14;
    private static final int SAVED_START_INDEX = 15;

    private final Grid grid = new Grid(ROWS, COLS);
    private final GridPanel panel = new GridPanel(grid);
    private final Level[] levels = LevelManager.all();

    private final JComboBox<String> algorithm = new JComboBox<>(new String[]{"BFS", "Dijkstra", "A*"});
    private final JComboBox<String> tool = new JComboBox<>(new String[]{
            "Chướng ngại vật", "Đổi trọng số ô", "Đặt Start", "Đặt Goal", "Di chuyển khung nhìn"
    });
    private final JComboBox<String> mapChoice = new JComboBox<>();
    private final JComboBox<String> mazeDifficulty = new JComboBox<>(new String[]{
            "1 • Dễ", "2 • Vừa", "3 • Khó", "4 • Rất khó", "5 • Thử thách"
    });

    private final JLabel status = new JLabel("Sẵn sàng.");
    private final JLabel stats = new JLabel("Bước: —    Chi phí: —    Node: —    Thời gian: —");
    private final JLabel zoomLabel = new JLabel("32 px");
    private final JLabel difficultyLabel = new JLabel();
    private final JLabel mazeSectionLabel = new JLabel("MÊ CUNG");
    private final JLabel mazeHint = new JLabel("Mỗi lần NẠP tạo mê cung mới.");

    private JButton startButton, pauseButton, stepButton, compareButton, infoButton, resetButton;
    private JButton loadButton, clearButton, randomCostButton, saveButton;
    private JSlider speedSlider;

    private final List<SavedLevelManager.SavedLevel> savedLevels = new ArrayList<>();
    private volatile boolean running;
    private volatile boolean paused;
    private int currentLevel = 0;
    private long mazeSeed = System.nanoTime();
    private PathFinder.SearchResult currentResult;
    private Timer animationTimer;
    private int animationPhase = 0; // 0=visited, 1=path, 2=robot
    private int animationIndex = 0;
    private int visitedDelay = 18;
    private int pathDelay = 55;
    private int robotDelay = 80;

    private final Color panelBg = new Color(22, 27, 38);
    private final Color text = new Color(232, 236, 243);
    private final Color muted = new Color(157, 168, 187);

    public MainFrame() {
        super("Robot Path Finding • BFS • Dijkstra • A*");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(13, 17, 23));
        setLayout(new BorderLayout(0, 0));
        getRootPane().setBorder(new EmptyBorder(8, 10, 8, 10));

        refreshSavedLevelChoices();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMapArea(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        panel.setStateChanged(() -> {
            if (!running) {
                clearStats();
                status.setText(isCustomSelection()
                        ? "Level tự xây: hãy chỉnh bản đồ rồi nhấn LƯU LEVEL hoặc START."
                        : "Bản đồ đã thay đổi. Nhấn START để tìm đường.");
                if (saveButton != null) saveButton.setEnabled(isCustomSelection());
            }
        });

        tool.addActionListener(e -> updateTool());
        mapChoice.addActionListener(e -> updateMapSelection());
        loadButton.addActionListener(e -> loadSelectedMap());
        clearButton.addActionListener(e -> clearCustomMap());
        randomCostButton.addActionListener(e -> randomizeCosts());
        saveButton.addActionListener(e -> saveCurrentLevel());
        startButton.addActionListener(e -> runSearch());
        pauseButton.addActionListener(e -> togglePause());
        stepButton.addActionListener(e -> stepAnimation());
        compareButton.addActionListener(e -> compareAlgorithms());
        infoButton.addActionListener(e -> showAlgorithmInfo());
        resetButton.addActionListener(e -> resetCurrentMap());

        installHotkeys();
        mapChoice.setSelectedIndex(CUSTOM_INDEX);
        loadSelectedMap();
        updateTool();
        updateSpeed();
        pauseButton.setEnabled(false);
        stepButton.setEnabled(false);

        setMinimumSize(new Dimension(1050, 700));
        setSize(1480, 920);
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 10));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(53, 63, 80)),
                new EmptyBorder(12, 14, 14, 14)));

        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel h = new JLabel("ROBOT PATH FINDING");
        h.setForeground(new Color(240, 246, 255));
        h.setFont(h.getFont().deriveFont(Font.BOLD, 26f));
        JLabel sub = new JLabel("Mô phỏng BFS • Dijkstra • A* trên bản đồ lưới có trọng số");
        sub.setForeground(new Color(155, 170, 197));
        sub.setFont(sub.getFont().deriveFont(Font.BOLD, 12.5f));
        title.add(h); title.add(Box.createVerticalStrut(3)); title.add(sub);
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        controls.add(label("Bản đồ"));
        mapChoice.setPreferredSize(new Dimension(205, 38));
        mapChoice.setBackground(new Color(20, 28, 40)); mapChoice.setForeground(text);
        controls.add(mapChoice);
        loadButton = button("NẠP", new Color(74, 117, 182)); controls.add(loadButton);
        controls.add(label("Thuật toán"));
        algorithm.setPreferredSize(new Dimension(105, 38));
        algorithm.setBackground(new Color(20, 28, 40)); algorithm.setForeground(text);
        controls.add(algorithm);
        startButton = button("▶ START", new Color(34, 197, 94));
        pauseButton = button("⏸ PAUSE", new Color(199, 133, 45));
        stepButton = button("⏭ BƯỚC", new Color(75, 117, 164));
        resetButton = button("↻ RESET", new Color(82, 92, 108));
        controls.add(startButton); controls.add(pauseButton); controls.add(stepButton); controls.add(resetButton);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMapArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(17, 24, 35));
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(panelBg);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(buildSidePanel(), BorderLayout.EAST);
        return wrapper;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(290, 0));
        side.setBackground(new Color(13, 19, 29));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(43, 54, 71)),
                new EmptyBorder(16, 15, 16, 15)));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        side.add(section("BẢN ĐỒ HIỆN TẠI"));
        side.add(Box.createVerticalStrut(5));
        difficultyLabel.setForeground(new Color(245, 196, 82));
        difficultyLabel.setFont(difficultyLabel.getFont().deriveFont(Font.BOLD, 12f));
        side.add(difficultyLabel);
        side.add(Box.createVerticalStrut(14));

        mazeSectionLabel.setForeground(new Color(109,125,150));
        mazeSectionLabel.setFont(mazeSectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        side.add(mazeSectionLabel); side.add(Box.createVerticalStrut(7));
        mazeDifficulty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34)); side.add(mazeDifficulty);
        side.add(Box.createVerticalStrut(5));
        mazeHint.setForeground(muted); mazeHint.setFont(mazeHint.getFont().deriveFont(10f)); side.add(mazeHint);
        side.add(Box.createVerticalStrut(15));

        side.add(section("CÔNG CỤ CHỈNH SỬA")); side.add(Box.createVerticalStrut(8));
        tool.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); side.add(tool);
        side.add(Box.createVerticalStrut(7));
        clearButton = button("XÓA TƯỜNG", new Color(91, 70, 92)); side.add(clearButton);
        side.add(Box.createVerticalStrut(6));
        randomCostButton = button("GCOST NGẪU NHIÊN", new Color(151, 103, 63)); side.add(randomCostButton);
        side.add(Box.createVerticalStrut(6));
        saveButton = button("💾 LƯU LEVEL", new Color(57, 117, 132)); side.add(saveButton);
        side.add(Box.createVerticalStrut(15));

        compareButton = button("⚖ SO SÁNH 3 THUẬT TOÁN", new Color(79, 91, 143)); side.add(compareButton);
        side.add(Box.createVerticalStrut(6));
        infoButton = button("ℹ GIẢI THÍCH THUẬT TOÁN", new Color(67, 91, 116)); side.add(infoButton);
        side.add(Box.createVerticalStrut(15));

        side.add(section("TỐC ĐỘ MÔ PHỎNG")); side.add(Box.createVerticalStrut(5));
        speedSlider = new JSlider(1, 5, 3); speedSlider.setOpaque(false);
        speedSlider.addChangeListener(e -> updateSpeed()); side.add(speedSlider);
        JPanel si = new JPanel(new BorderLayout()); si.setOpaque(false);
        JLabel slow = new JLabel("Chậm"), fast = new JLabel("Nhanh");
        slow.setForeground(muted); fast.setForeground(muted); zoomLabel.setForeground(muted); zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        si.add(slow, BorderLayout.WEST); si.add(new JLabel("1x  2x  3x  4x  5x", SwingConstants.CENTER), BorderLayout.CENTER); si.add(fast, BorderLayout.EAST);
        side.add(si); side.add(Box.createVerticalStrut(13));

        side.add(section("KÍCH THƯỚC BẢN ĐỒ"));
        JSlider zoom = new JSlider(18, 60, 32); zoom.setOpaque(false);
        zoom.addChangeListener(e -> { panel.setCellSize(zoom.getValue()); zoomLabel.setText(zoom.getValue()+" px"); }); side.add(zoom);
        JPanel zi = new JPanel(new BorderLayout()); zi.setOpaque(false);
        JLabel min = new JLabel("18"), max = new JLabel("60");
        min.setForeground(muted); max.setForeground(muted);
        zi.add(min, BorderLayout.WEST); zi.add(zoomLabel, BorderLayout.CENTER); zi.add(max, BorderLayout.EAST); side.add(zi);
        side.add(Box.createVerticalStrut(13));

        side.add(section("PHÍM TẮT")); side.add(Box.createVerticalStrut(6));
        side.add(info("S", "Đặt Start")); side.add(info("D", "Đặt Goal")); side.add(info("W", "Chướng ngại vật"));
        side.add(info("T", "Đổi trọng số ô")); side.add(info("P", "Pan bản đồ")); side.add(info("SPACE", "Pause / tiếp tục"));
        side.add(info("Ctrl + lăn", "Zoom"));
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 5));
        footer.setOpaque(false); footer.setBorder(new EmptyBorder(10, 4, 2, 4));
        stats.setForeground(text); stats.setFont(stats.getFont().deriveFont(Font.BOLD, 13f));
        status.setForeground(muted); status.setFont(status.getFont().deriveFont(12f));
        footer.add(stats, BorderLayout.NORTH); footer.add(status, BorderLayout.SOUTH); return footer;
    }

    private JLabel section(String s) { JLabel l=new JLabel(s); l.setForeground(new Color(116,139,169)); l.setFont(l.getFont().deriveFont(Font.BOLD,11f)); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l; }
    private JPanel info(String key,String value){ JPanel p=new JPanel(new BorderLayout(5,0)); p.setOpaque(false); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,24)); JLabel a=new JLabel(key),b=new JLabel(value); a.setForeground(text); b.setForeground(muted); a.setFont(a.getFont().deriveFont(Font.BOLD,11f)); b.setFont(b.getFont().deriveFont(10f)); p.add(a,BorderLayout.WEST);p.add(b,BorderLayout.EAST);return p; }
    private JLabel label(String s){ JLabel l=new JLabel(s);l.setForeground(muted);l.setFont(l.getFont().deriveFont(Font.BOLD,11f));return l; }
    private JButton button(String s,Color c){ JButton b=new JButton(s); b.setForeground(Color.WHITE); b.setBackground(c); b.setFocusPainted(false); b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(255,255,255,25)), BorderFactory.createEmptyBorder(7,11,7,11))); b.setFont(b.getFont().deriveFont(Font.BOLD,11.5f)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b; }

    private void installHotkeys() {
        InputMap im=getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); ActionMap am=getRootPane().getActionMap();
        bind(im,am,"pressed S","chooseStart",()->chooseTool(GridPanel.EditMode.START));
        bind(im,am,"pressed D","chooseGoal",()->chooseTool(GridPanel.EditMode.GOAL));
        bind(im,am,"pressed W","chooseWall",()->chooseTool(GridPanel.EditMode.OBSTACLE));
        bind(im,am,"pressed T","chooseCost",()->chooseTool(GridPanel.EditMode.COST));
        bind(im,am,"pressed P","choosePan",()->chooseTool(GridPanel.EditMode.PAN));
        bind(im,am,"pressed SPACE","pauseSearch",this::togglePause);
    }
    private void bind(InputMap im,ActionMap am,String key,String name,Runnable action){ im.put(KeyStroke.getKeyStroke(key),name); am.put(name,new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){action.run();}}); }

    private void chooseTool(GridPanel.EditMode mode) {
        if(running)return;
        int index=switch(mode){case COST->1;case START->2;case GOAL->3;case PAN->4;default->0;};
        tool.setSelectedIndex(index); updateTool(); panel.requestFocusInWindow();
    }
    private void updateTool(){
        int i=tool.getSelectedIndex(); GridPanel.EditMode mode=switch(i){case 1->GridPanel.EditMode.COST;case 2->GridPanel.EditMode.START;case 3->GridPanel.EditMode.GOAL;case 4->GridPanel.EditMode.PAN;default->GridPanel.EditMode.OBSTACLE;};
        panel.setEditMode(mode);
        if (!running) status.setText(switch(mode){
            case OBSTACLE -> "Chế độ chướng ngại vật: click/kéo để đặt hoặc xóa vật cản.";
            case COST -> "Chế độ trọng số: click/kéo ô trống để đổi 1 → 2 → 4 → 7.";
            case START -> "Chế độ Start: chọn vị trí bắt đầu cho robot.";
            case GOAL -> "Chế độ Goal: chọn điểm đích cần đến.";
            case PAN -> "Kéo chuột để di chuyển khung nhìn.";
        });
    }

    private void updateMapSelection(){
        int index=mapChoice.getSelectedIndex(); boolean custom=index==CUSTOM_INDEX, maze=index==MAZE_INDEX, randomWalls=index==RANDOM_WALL_INDEX, saved=index>=SAVED_START_INDEX;
        mazeSectionLabel.setVisible(maze); mazeDifficulty.setVisible(maze); mazeHint.setVisible(maze);
        if(maze){difficultyLabel.setText("MÊ CUNG NGẪU NHIÊN"); mazeDifficulty.setEnabled(!running);}
        else if(randomWalls){difficultyLabel.setText("TƯỜNG NGẪU NHIÊN"); mazeDifficulty.setEnabled(false);}
        else if(custom){difficultyLabel.setText("TỰ XÂY LEVEL"); mazeDifficulty.setEnabled(false);}
        else if(saved){int si=index-SAVED_START_INDEX; difficultyLabel.setText(si>=0&&si<savedLevels.size()?"💾 LEVEL ĐÃ LƯU":"LEVEL ĐÃ LƯU"); mazeDifficulty.setEnabled(false);}
        else if(index>=0&&index<levels.length){difficultyLabel.setText(levels[index].difficulty()); mazeDifficulty.setEnabled(false);}
        if(saveButton!=null) saveButton.setEnabled(custom&&!running);
        if(clearButton!=null) clearButton.setEnabled(!running); if(randomCostButton!=null) randomCostButton.setEnabled(!running);
    }
    private boolean isCustomSelection(){return mapChoice.getSelectedIndex()==CUSTOM_INDEX;}

    private void refreshSavedLevelChoices(){
        savedLevels.clear(); savedLevels.addAll(SavedLevelManager.list());
        mapChoice.removeAllItems(); for(Level l:levels)mapChoice.addItem(l.name());
        mapChoice.addItem("Mê cung ngẫu nhiên"); mapChoice.addItem("Tường ngẫu nhiên"); mapChoice.addItem("Tự xây level");
        for(SavedLevelManager.SavedLevel s:savedLevels)mapChoice.addItem("💾 "+s.name());
    }

    private void loadSelectedMap(){
        if(running)return; stopAnimation(); int index=mapChoice.getSelectedIndex();
        try {
            boolean mapLoaded = false;
            if(index==MAZE_INDEX){
                int diff=mazeDifficulty.getSelectedIndex()+1; mazeSeed=System.nanoTime();
                grid.loadLevel(MazeGenerator.generate(ROWS,COLS,diff,mazeSeed)); mapLoaded = true; panel.clearSearch();
                clearStats(); status.setText("Mê cung mới đã được tạo. Nhấn START để mô phỏng.");
            } else if(index==RANDOM_WALL_INDEX){
                grid.clearWalls(); grid.resetStartGoal(); grid.randomizeWalls(0.28,System.nanoTime()); mapLoaded = true; panel.clearSearch();
                clearStats(); status.setText("Đã tạo bản đồ tường ngẫu nhiên, vẫn bảo đảm có đường tới Goal.");
            } else if(index==CUSTOM_INDEX){
                currentLevel=CUSTOM_INDEX; grid.clearWalls(); grid.resetStartGoal(); mapLoaded = true; panel.clearSearch(); clearStats();
                status.setText("TỰ XÂY LEVEL: dùng W, S, D, T rồi LƯU LEVEL hoặc START.");
            } else if(index>=SAVED_START_INDEX){
                int si=index-SAVED_START_INDEX; if(si>=0&&si<savedLevels.size()){
                    Level saved=SavedLevelManager.load(savedLevels.get(si)); currentLevel=index; grid.loadLevel(saved); mapLoaded = true; panel.clearSearch(); clearStats();
                    status.setText("💾 "+saved.name()+" đã được nạp. Nhấn START để chơi lại.");
                }
            } else if(index>=0&&index<levels.length){
                currentLevel=index; grid.loadLevel(levels[index]); mapLoaded = true; panel.clearSearch(); clearStats();
                status.setText(levels[index].name()+" đã được nạp. Nhấn START để tìm đường.");
            }
            if (mapLoaded) {
                grid.randomizeCosts(System.nanoTime());
                panel.clearSearch();
                status.setText(status.getText()+" GCost đã được random.");
            }
            SwingUtilities.invokeLater(panel::centerView);
        } catch(Exception ex){JOptionPane.showMessageDialog(this,"Không thể nạp level: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}
        updateMapSelection();
    }

    private void clearCustomMap(){if(running)return; grid.clearWalls(); panel.clearSearch(); clearStats(); status.setText("Đã xóa toàn bộ chướng ngại vật. Start và Goal được giữ nguyên.");}
    private void randomizeCosts(){if(running)return; grid.randomizeCosts(System.nanoTime()); panel.clearSearch(); clearStats(); status.setText("Đã random GCost cho các ô trên bản đồ.");}

    private void saveCurrentLevel(){
        if(running||!isCustomSelection())return; String name=JOptionPane.showInputDialog(this,"Đặt tên cho level:","Lưu level",JOptionPane.PLAIN_MESSAGE);
        if(name==null)return; name=name.trim(); if(name.isBlank()){JOptionPane.showMessageDialog(this,"Tên level không được để trống.","Thông báo",JOptionPane.WARNING_MESSAGE);return;}
        try{SavedLevelManager.save(name,grid); refreshSavedLevelChoices(); for(int i=0;i<savedLevels.size();i++)if(savedLevels.get(i).name().equals(name)){mapChoice.setSelectedIndex(SAVED_START_INDEX+i);break;} status.setText("💾 Đã lưu level \""+name+"\".");
            JOptionPane.showMessageDialog(this,"Đã lưu level: "+name+"\n\nBạn có thể chọn lại trong mục Bản đồ và nhấn NẠP → START.","Lưu thành công",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Không thể lưu level: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}
    }

    private void resetCurrentMap(){if(running)return; loadSelectedMap();}
    private PathFinder finder(){return switch((String)algorithm.getSelectedItem()){case "Dijkstra"->new Dijkstra();case "A*"->new AStar();default->new BFS();};}

    private void runSearch(){
        if(running)return;
        stopAnimation(); running=true; paused=false; setControlsForRun(); panel.setEditing(false); panel.clearSearch();
        PathFinder f=finder(); status.setText("Đang tìm đường bằng "+f.name()+"…");
        new SwingWorker<PathFinder.SearchResult,Void>(){
            protected PathFinder.SearchResult doInBackground(){return f.search(grid);}
            protected void done(){try{currentResult=get(); updateStats(currentResult,f.name()); animationPhase=0; animationIndex=0; startAnimation();}catch(InterruptedException|ExecutionException ex){status.setText("Lỗi: "+ex.getMessage());finish();}}
        }.execute();
    }

    private void startAnimation(){
        if(currentResult==null)return;
        if(animationTimer!=null)animationTimer.stop();
        animationTimer=new Timer(delayForPhase(), e->advanceAnimation());
        animationTimer.setCoalesce(true); animationTimer.start();
        status.setText("Mô phỏng "+finder().name()+" • "+phaseText());
    }
    private void advanceAnimation(){
        if(paused||currentResult==null)return;
        if(animationPhase==0){
            if(animationIndex<currentResult.visitedOrder().size()){panel.showVisited(currentResult.visitedOrder().get(animationIndex++));}
            else{animationPhase=1;animationIndex=0;restartTimer();}
        } else if(animationPhase==1){
            if(currentResult.path().isEmpty()){stopAnimation();status.setText("Không tìm thấy đường đi tới Goal!");finish();return;}
            if(animationIndex<currentResult.path().size()){panel.showPath(currentResult.path().get(animationIndex++));}
            else{animationPhase=2;animationIndex=0;restartTimer();}
        } else {
            if(animationIndex<currentResult.path().size()){panel.showRobot(currentResult.path().get(animationIndex++));}
            else{stopAnimation();status.setText("Hoàn thành! Robot đã tới Goal.");finish();return;}
        }
        status.setText((paused?"⏸ ":"")+"Mô phỏng "+phaseText());
    }
    private void restartTimer(){if(animationTimer!=null){animationTimer.setDelay(delayForPhase());}}
    private int delayForPhase(){return animationPhase==0?visitedDelay:animationPhase==1?pathDelay:robotDelay;}
    private String phaseText(){return switch(animationPhase){case 0->"đang mở rộng node";case 1->"đang dựng đường đi";default->"robot đang di chuyển";};}

    private void stepAnimation(){
        if(!running||currentResult==null)return;
        paused=true; if(animationTimer!=null)animationTimer.stop(); advanceAnimationOnce();
        if(running) status.setText("⏸ BƯỚC: "+phaseText()+" • nhấn BƯỚC tiếp hoặc PAUSE/▶ tiếp tục.");
    }

    private void advanceAnimationOnce(){
        if(currentResult==null)return;
        if(animationPhase==0){
            if(animationIndex<currentResult.visitedOrder().size()) panel.showVisited(currentResult.visitedOrder().get(animationIndex++));
            else { animationPhase=1; animationIndex=0; advanceAnimationOnce(); }
        } else if(animationPhase==1){
            if(currentResult.path().isEmpty()){ status.setText("Không tìm thấy đường đi tới Goal!"); finish(); return; }
            if(animationIndex<currentResult.path().size()) panel.showPath(currentResult.path().get(animationIndex++));
            else { animationPhase=2; animationIndex=0; advanceAnimationOnce(); }
        } else {
            if(animationIndex<currentResult.path().size()) panel.showRobot(currentResult.path().get(animationIndex++));
            else { status.setText("Hoàn thành! Robot đã tới Goal."); finish(); }
        }
    }
    private void togglePause(){
        if(!running||currentResult==null)return;
        paused=!paused;
        if(paused){if(animationTimer!=null)animationTimer.stop();pauseButton.setText("▶ TIẾP");status.setText("⏸ Đã tạm dừng • nhấn BƯỚC để đi từng bước.");}
        else{pauseButton.setText("⏸ PAUSE");startAnimation();}
    }
    private void updateSpeed(){
        if(speedSlider==null)return; int s=speedSlider.getValue(); double factor=s;
        visitedDelay=(int)Math.max(5,18/factor); pathDelay=(int)Math.max(12,55/factor); robotDelay=(int)Math.max(18,80/factor);
        if(animationTimer!=null&&animationTimer.isRunning())animationTimer.setDelay(delayForPhase());
    }

    private void compareAlgorithms(){
        if(running)return;
        panel.clearSearch(); setControlsForCompare(); status.setText("Đang chạy BFS, Dijkstra và A* trên cùng bản đồ…");
        new SwingWorker<List<NamedResult>,Void>(){
            protected List<NamedResult> doInBackground(){
                List<NamedResult> out=new ArrayList<>();
                for(PathFinder f:List.of(new BFS(),new Dijkstra(),new AStar())) out.add(new NamedResult(f.name(),f.search(grid)));
                return out;
            }
            protected void done(){try{showComparison(get());}catch(Exception ex){JOptionPane.showMessageDialog(MainFrame.this,"Không thể so sánh: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}finally{finish();}}
        }.execute();
    }
    private record NamedResult(String name, PathFinder.SearchResult result){}

    private void showComparison(List<NamedResult> results){
        String[] cols={"Thuật toán","Trạng thái","Bước","Chi phí","Node duyệt","Thời gian (ms)"};
        Object[][] data=new Object[results.size()][cols.length];
        for(int i=0;i<results.size();i++){NamedResult n=results.get(i);var r=n.result();data[i]=new Object[]{n.name(),r.found()?"Có đường":"Không có đường",r.steps(),r.found()?r.totalCost():"—",r.visitedOrder().size(),String.format("%.3f",r.elapsedNanos()/1_000_000.0)};}
        JTable table=new JTable(data,cols); table.setRowHeight(28); table.setFillsViewportHeight(true);
        JPanel p=new JPanel(new BorderLayout(0,10)); p.setBorder(new EmptyBorder(10,10,10,10));
        JLabel title=new JLabel("SO SÁNH BFS • DIJKSTRA • A*",SwingConstants.CENTER); title.setFont(title.getFont().deriveFont(Font.BOLD,16f));
        JLabel note=new JLabel("Cùng một bản đồ và cùng Start/Goal; thời gian có thể thay đổi theo máy và lần chạy.",SwingConstants.CENTER); note.setForeground(Color.DARK_GRAY);
        p.add(title,BorderLayout.NORTH); p.add(new JScrollPane(table),BorderLayout.CENTER); p.add(note,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this,p,"Kết quả so sánh",JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAlgorithmInfo(){
        String selected=(String)algorithm.getSelectedItem();
        String text=switch(selected){
            case "Dijkstra"->"DIJKSTRA\n\n• Tìm đường có tổng chi phí nhỏ nhất.\n• Không dùng heuristic.\n• Phù hợp với bản đồ có trọng số dương.\n• Ưu tiên node có g(n) nhỏ nhất.\n\nGhi chú: chi phí của ô đích được cộng vào tổng chi phí đường đi.";
            case "A*"->"A*\n\n• Tìm đường bằng f(n) = g(n) + h(n).\n• g(n): chi phí từ Start tới node hiện tại.\n• h(n): Manhattan Distance tới Goal.\n• Với trọng số dương và heuristic phù hợp, A* có thể giảm số node phải xét so với tìm kiếm không định hướng.";
            default->"BFS\n\n• Tìm kiếm theo chiều rộng.\n• Không sử dụng trọng số khi quyết định mở rộng node.\n• Tìm đường ít bước nhất trên lưới không trọng số.\n• Có thể vẫn đi qua các ô có cost cao vì BFS tối ưu số bước, không tối ưu tổng chi phí.";
        };
        JTextArea area=new JTextArea(text); area.setEditable(false); area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14)); area.setBorder(new EmptyBorder(10,10,10,10)); area.setPreferredSize(new Dimension(500,250));
        JOptionPane.showMessageDialog(this,area,"Thông tin thuật toán: "+selected,JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStats(PathFinder.SearchResult r,String name){
        stats.setText(String.format("%s    Bước: %d    Chi phí: %d    Node: %d    Thời gian: %.3f ms",name,r.steps(),r.found()?r.totalCost():0,r.visitedOrder().size(),r.elapsedNanos()/1_000_000.0));
    }
    private void clearStats(){stats.setText("Bước: —    Chi phí: —    Node: —    Thời gian: —"); currentResult=null;}

    private void stopAnimation(){if(animationTimer!=null){animationTimer.stop();animationTimer=null;} paused=false;}
    private void setControlsForRun(){
        startButton.setEnabled(false); resetButton.setEnabled(false); loadButton.setEnabled(false); algorithm.setEnabled(false); tool.setEnabled(false); mapChoice.setEnabled(false); clearButton.setEnabled(false); randomCostButton.setEnabled(false); saveButton.setEnabled(false); mazeDifficulty.setEnabled(false); compareButton.setEnabled(false); infoButton.setEnabled(false);
        pauseButton.setEnabled(true); pauseButton.setText("⏸ PAUSE"); stepButton.setEnabled(true);
    }
    private void setControlsForCompare(){
        running=true; startButton.setEnabled(false); pauseButton.setEnabled(false); stepButton.setEnabled(false); resetButton.setEnabled(false); loadButton.setEnabled(false); algorithm.setEnabled(false); tool.setEnabled(false); mapChoice.setEnabled(false); clearButton.setEnabled(false); randomCostButton.setEnabled(false); saveButton.setEnabled(false); mazeDifficulty.setEnabled(false); compareButton.setEnabled(false); infoButton.setEnabled(false); panel.setEditing(false);
    }
    private void finish(){
        running=false; paused=false; stopAnimation(); startButton.setEnabled(true); pauseButton.setEnabled(false); stepButton.setEnabled(false); resetButton.setEnabled(true); loadButton.setEnabled(true); algorithm.setEnabled(true); tool.setEnabled(true); mapChoice.setEnabled(true); clearButton.setEnabled(true); randomCostButton.setEnabled(true); saveButton.setEnabled(isCustomSelection()); compareButton.setEnabled(true); infoButton.setEnabled(true); mazeDifficulty.setEnabled(isMazeSelection()); panel.setEditing(true); updateMapSelection();
    }
    private boolean isMazeSelection(){return mapChoice.getSelectedIndex()==MAZE_INDEX;}
}
