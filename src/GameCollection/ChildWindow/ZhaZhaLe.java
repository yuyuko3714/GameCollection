package GameCollection.ChildWindow;

import javax.swing.*;
import javax.swing.Timer;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * 炸炸乐 - 休闲小游戏 融合扫雷棋盘规则与连锁爆炸消除玩法 Java 21 LTS + Swing 单文件实现 (兼容Java 11)
 * 
 * @author 炸炸乐开发团队
 */
public class ZhaZhaLe extends JFrame {
	// ==================== 常量定义 ====================
	private static final int ROWS = 16; // 棋盘行数
	private static final int COLS = 30; // 棋盘列数
	private static final int BASE_CELL_SIZE = 20; // 基础格子像素大小
	private static final int BASE_BOARD_WIDTH = COLS * BASE_CELL_SIZE;
	private static final int BASE_BOARD_HEIGHT = ROWS * BASE_CELL_SIZE;
	private static final int FPS = 60; // 动画帧率
	private static final int FRAME_DELAY = 1000 / FPS;

	// 游戏模式
	public enum GameMode {
		ENTERTAINMENT, STRATEGY
	}

	// 难度级别
	public enum Difficulty {
		EASY(70), NORMAL(50), HARD(40), EXTREME(30);

		final int mineCount;

		Difficulty(int mineCount) {
			this.mineCount = mineCount;
		}
	}

	// 格子状态
	private enum CellState {
		HIDDEN, REVEALED, EMPTY, EXPLODING
	}

	// ==================== 内部类：格子数据 ====================
	private static class Cell {
		boolean isMine;
		int adjacentMines;
		CellState state;
		float animAlpha; // 动画透明度 0~1
		int animOffsetY; // 下落动画Y偏移
		int flashCount; // 闪烁计数

		Cell() {
			this.isMine = false;
			this.adjacentMines = 0;
			this.state = CellState.HIDDEN;
			this.animAlpha = 1.0f;
			this.animOffsetY = 0;
			this.flashCount = 0;
		}

		void reset() {
			isMine = false;
			adjacentMines = 0;
			state = CellState.HIDDEN;
			animAlpha = 1.0f;
			animOffsetY = 0;
			flashCount = 0;
		}
	}

	// ==================== 游戏状态 ====================
	private enum GameScreen {
		MENU, PLAYING, PAUSED, GAME_OVER
	}

	private GameScreen currentScreen;
	private GameMode gameMode;
	private Difficulty difficulty;
	private Cell[][] board;
	private int score;
	private int remainingTime; // 娱乐模式：秒
	private int remainingClicks; // 策略模式：点击次数
	private boolean gameWon;
	private boolean firstClick; // 标记是否是第一次点击

	// 缩放相关
	private float scaleFactor = 1.0f; // 当前缩放比例
	private int cellSize = BASE_CELL_SIZE; // 当前格子大小

	// 线程池 (Java 11兼容替代虚拟线程)
	private ExecutorService executorService;
	private Future<?> timerFuture; // 计时器线程Future，用于停止旧计时器

	// 动画相关
	private Timer animationTimer;
	private Set<Point> explodingCells;
	private Map<Point, Integer> fallingCells; // 位置 -> 目标下落距离
	private boolean isAnimating;

	// UI组件
	private JPanel cardPanel;
	private JPanel menuPanel;
	private JPanel gamePanel;
	private BoardPanel boardPanel;
	private JLabel scoreLabel;
	private JLabel statusLabel;
	private JLabel difficultyLabel;
	private JLabel finalScoreLabel;
	private JLabel resultLabel;
	// ==================== 颜色和字体定义 ====================
	private static final Color[] NUMBER_COLORS = { new Color(0, 0, 255), // 1 - 蓝
			new Color(0, 128, 0), // 2 - 绿
			new Color(255, 0, 0), // 3 - 红
			new Color(0, 0, 128), // 4 - 深蓝
			new Color(128, 0, 0), // 5 - 深红
			new Color(0, 128, 128), // 6 - 青
			new Color(0, 0, 0), // 7 - 黑
			new Color(128, 128, 128) // 8 - 灰
	};

	private static final Color BG_COLOR = new Color(240, 240, 240);
	private static final Color HIDDEN_COLOR = new Color(192, 192, 192);
	private static final Color REVEALED_COLOR = new Color(224, 224, 224);
	private static final Color MINE_COLOR = new Color(255, 100, 100);
	private static final Color EMPTY_COLOR = BG_COLOR;

	// ==================== 构造函数 ====================
	public ZhaZhaLe() {
		super("炸炸乐");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(true); // 允许窗口缩放、最大化、最小化

		// 初始化线程池
		executorService = Executors.newCachedThreadPool();

		// 初始化游戏状态
		currentScreen = GameScreen.MENU;
		gameMode = GameMode.ENTERTAINMENT;
		difficulty = Difficulty.NORMAL;
		board = new Cell[ROWS][COLS];
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				board[r][c] = new Cell();
			}
		}
		explodingCells = ConcurrentHashMap.newKeySet();
		fallingCells = new ConcurrentHashMap<>();

		// 初始化UI
		initUI();

		// 启动动画主循环
		initAnimationTimer();

		pack();
		setLocationRelativeTo(null);
	}

	// ==================== UI初始化 ====================
	private void initUI() {
		cardPanel = new JPanel(new CardLayout());

		createMenuPanel();
		createGamePanel();
		createPausePanel();
		createGameOverPanel();

		add(cardPanel);
	}

	private void createMenuPanel() {
		menuPanel = new JPanel(new GridBagLayout());
		menuPanel.setBackground(BG_COLOR);
		menuPanel.setPreferredSize(new Dimension(BASE_BOARD_WIDTH + 40, BASE_BOARD_HEIGHT + 150));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// 标题
		JLabel titleLabel = new JLabel("炸炸乐", SwingConstants.CENTER);
		titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 48));
		titleLabel.setForeground(new Color(220, 50, 50));
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon3.png")));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		menuPanel.add(titleLabel, gbc);

		// 模式选择
		gbc.gridwidth = 1;
		gbc.gridy = 1;
		JLabel modeLabel = new JLabel("游戏模式:");
		modeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		menuPanel.add(modeLabel, gbc);

		JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JRadioButton entertainmentBtn = new JRadioButton("娱乐模式 (3分钟)", true);
		JRadioButton strategyBtn = new JRadioButton("策略模式 (50次点击)");
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(entertainmentBtn);
		modeGroup.add(strategyBtn);
		entertainmentBtn.addActionListener(e -> gameMode = GameMode.ENTERTAINMENT);
		strategyBtn.addActionListener(e -> gameMode = GameMode.STRATEGY);
		modePanel.add(entertainmentBtn);
		modePanel.add(strategyBtn);
		gbc.gridx = 1;
		menuPanel.add(modePanel, gbc);

		// 难度选择
		gbc.gridx = 0;
		gbc.gridy = 2;
		JLabel diffLabel = new JLabel("难度级别:");
		diffLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		menuPanel.add(diffLabel, gbc);

		String[] diffNames = { "简单", "普通", "困难", "极难" };
		JComboBox<String> diffCombo = new JComboBox<>(diffNames);
		diffCombo.setSelectedIndex(1);
		diffCombo.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		diffCombo.addActionListener(e -> {
			int idx = diffCombo.getSelectedIndex();
			difficulty = Difficulty.values()[idx];
		});
		gbc.gridx = 1;
		menuPanel.add(diffCombo, gbc);

		// 开始按钮
		JButton startBtn = new JButton("开始游戏");
		startBtn.setFont(new Font("微软雅黑", Font.BOLD, 20));
		startBtn.setPreferredSize(new Dimension(200, 50));
		startBtn.setBackground(new Color(100, 180, 100));
		startBtn.setForeground(Color.BLACK);
		startBtn.addActionListener(e -> startGame());
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		menuPanel.add(startBtn, gbc);

		// 退出按钮
		JButton exitBtn = new JButton("退出游戏");
		exitBtn.setFont(new Font("微软雅黑", Font.BOLD, 18));
		exitBtn.setPreferredSize(new Dimension(200, 45));
		exitBtn.setBackground(new Color(200, 80, 80));
		exitBtn.setForeground(Color.BLACK);
		exitBtn.addActionListener(e -> dispose());
		gbc.gridy = 4;
		menuPanel.add(exitBtn, gbc);

		cardPanel.add(menuPanel, "MENU");
	}

	private void createGamePanel() {
		gamePanel = new JPanel(new BorderLayout());
		gamePanel.setBackground(BG_COLOR);

		// 顶部状态栏
		JPanel statusBar = new JPanel(new BorderLayout(20, 0));
		statusBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		statusBar.setBackground(new Color(220, 220, 220));

		scoreLabel = new JLabel("分数: 0");
		scoreLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
		statusBar.add(scoreLabel, BorderLayout.WEST);

		statusLabel = new JLabel("剩余时间: 03:00");
		statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
		statusBar.add(statusLabel, BorderLayout.CENTER);

		difficultyLabel = new JLabel("难度: 普通");
		difficultyLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
		statusBar.add(difficultyLabel, BorderLayout.EAST);

		gamePanel.add(statusBar, BorderLayout.NORTH);

		// 棋盘区域
		boardPanel = new BoardPanel();
		boardPanel.setPreferredSize(new Dimension(BASE_BOARD_WIDTH, BASE_BOARD_HEIGHT));
		gamePanel.add(boardPanel, BorderLayout.CENTER);

		// 底部控制栏
		JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		controlBar.setBackground(new Color(220, 220, 220));

		JButton pauseBtn = new JButton("暂停游戏");
		pauseBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		pauseBtn.addActionListener(e -> pauseGame());
		controlBar.add(pauseBtn);

		JButton restartBtn = new JButton("重新开始");
		restartBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		restartBtn.addActionListener(e -> startGame());
		controlBar.add(restartBtn);

		JButton menuBtn = new JButton("返回主菜单");
		menuBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		menuBtn.addActionListener(e -> showMenu());
		controlBar.add(menuBtn);

		JButton exitBtn = new JButton("退出游戏");
		exitBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		exitBtn.addActionListener(e -> dispose());
		controlBar.add(exitBtn);

		gamePanel.add(controlBar, BorderLayout.SOUTH);

		cardPanel.add(gamePanel, "GAME");
	}

	private void createPausePanel() {
		JPanel pausePanel = new JPanel(new BorderLayout());
		pausePanel.setBackground(BG_COLOR);
		pausePanel.setPreferredSize(new Dimension(BASE_BOARD_WIDTH + 40, BASE_BOARD_HEIGHT + 200));

		// 标题
		JLabel titleLabel = new JLabel("游戏暂停", SwingConstants.CENTER);
		titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
		pausePanel.add(titleLabel, BorderLayout.NORTH);

		// 游戏规则文本区域
		JTextArea rulesArea = new JTextArea();
		rulesArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		rulesArea.setEditable(false);
		rulesArea.setBackground(BG_COLOR);
		rulesArea.setLineWrap(true);
		rulesArea.setWrapStyleWord(true);
		rulesArea.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

		String rules = "【游戏规则 - 炸炸乐】\n\n" + "一、基本操作\n" + "- 左键点击隐藏格子打开它\n" + "- 点击到数字格或空白格：仅打开当前格子\n"
				+ "- 点击到地雷：触发连锁爆炸消除（核心玩法）\n" + "- 第一下点击保证不是地雷\n\n" + "二、连锁爆炸机制\n" + "当点击到地雷时：\n"
				+ "1. 消除被点击地雷的9宫格范围内所有格子\n" + "2. 扫描所有已打开的数字格，找出它们周围的隐藏地雷\n" + "3. 消除每个隐藏地雷的9宫格范围内所有格子\n"
				+ "4. 消除所有已打开的空白格\n\n" + "三、分数计算\n" + "单次得分 = 消除格子总数 × (1 + 连锁地雷数)\n" + "- 消除越多，分数越高\n"
				+ "- 连锁地雷越多，倍率越高\n\n" + "四、游戏模式\n" + "- 娱乐模式: 3分钟倒计时，目标≥10000分\n" + "- 策略模式: 50次点击限制，目标≥1000分\n\n"
				+ "五、难度设置\n" + "- 简单：70颗地雷\n" + "- 普通：50颗地雷\n" + "- 困难：40颗地雷\n" + "- 极难：30颗地雷\n\n" + "六、格子下落与重生成\n"
				+ "爆炸消除后：\n" + "1. 上方格子垂直下落填补空位\n" + "2. 顶部生成新的隐藏格子\n" + "3. 整个棋盘重新随机排布，地雷总数不变";

		rulesArea.setText(rules);
		JScrollPane scrollPane = new JScrollPane(rulesArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
		pausePanel.add(scrollPane, BorderLayout.CENTER);

		// 按钮区域
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
		btnPanel.setBackground(new Color(220, 220, 220));

		JButton resumeBtn = new JButton("继续游戏");
		resumeBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		resumeBtn.setPreferredSize(new Dimension(120, 40));
		resumeBtn.addActionListener(e -> resumeGame());
		btnPanel.add(resumeBtn);

		JButton menuBtn = new JButton("返回主菜单");
		menuBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		menuBtn.setPreferredSize(new Dimension(120, 40));
		menuBtn.addActionListener(e -> showMenu());
		btnPanel.add(menuBtn);

		JButton exitBtn = new JButton("退出游戏");
		exitBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		exitBtn.setPreferredSize(new Dimension(120, 40));
		exitBtn.addActionListener(e -> dispose());
		btnPanel.add(exitBtn);

		pausePanel.add(btnPanel, BorderLayout.SOUTH);

		cardPanel.add(pausePanel, "PAUSED");
	}

	private void pauseGame() {
		currentScreen = GameScreen.PAUSED;
		// 娱乐模式暂停时停止计时器线程
		if (gameMode == GameMode.ENTERTAINMENT && timerFuture != null && !timerFuture.isDone()) {
			timerFuture.cancel(true);
		}
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, "PAUSED");
	}

	private void resumeGame() {
		currentScreen = GameScreen.PLAYING;
		// 娱乐模式继续时重新启动计时器线程
		if (gameMode == GameMode.ENTERTAINMENT) {
			startTimerThread();
		}
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, "GAME");
	}

	private void createGameOverPanel() {
		JPanel overPanel = new JPanel(new GridBagLayout());
		overPanel.setBackground(BG_COLOR);
		overPanel.setPreferredSize(new Dimension(BASE_BOARD_WIDTH + 40, BASE_BOARD_HEIGHT + 150));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(15, 10, 15, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel titleLabel = new JLabel("游戏结束", SwingConstants.CENTER);
		titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		overPanel.add(titleLabel, gbc);

		finalScoreLabel = new JLabel("最终得分: 0", SwingConstants.CENTER);
		finalScoreLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
		gbc.gridy = 1;
		overPanel.add(finalScoreLabel, gbc);

		resultLabel = new JLabel("", SwingConstants.CENTER);
		resultLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
		gbc.gridy = 2;
		overPanel.add(resultLabel, gbc);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		JButton againBtn = new JButton("再来一局");
		againBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		againBtn.setPreferredSize(new Dimension(120, 40));
		againBtn.addActionListener(e -> startGame());

		JButton menuBtn = new JButton("返回主菜单");
		menuBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		menuBtn.setPreferredSize(new Dimension(120, 40));
		menuBtn.addActionListener(e -> showMenu());

		btnPanel.add(againBtn);
		btnPanel.add(menuBtn);
		gbc.gridy = 3;
		overPanel.add(btnPanel, gbc);

		cardPanel.add(overPanel, "OVER");
	}

	// ==================== 棋盘绘制面板 ====================
	private class BoardPanel extends JPanel {
		BoardPanel() {
			setBackground(EMPTY_COLOR);

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (isAnimating || currentScreen != GameScreen.PLAYING)
						return;
					if (SwingUtilities.isLeftMouseButton(e)) {
						// 使用当前缩放后的格子大小计算点击位置
						int col = e.getX() / cellSize;
						int row = e.getY() / cellSize;
						if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
							handleCellClick(row, col);
						}
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// 计算当前面板大小对应的缩放比例
			int panelWidth = getWidth();
			int panelHeight = getHeight();
			float scaleX = (float) panelWidth / (COLS * BASE_CELL_SIZE);
			float scaleY = (float) panelHeight / (ROWS * BASE_CELL_SIZE);
			scaleFactor = Math.min(scaleX, scaleY);
			scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f)); // 限制缩放范围 0.5x ~ 3x
			cellSize = Math.round(BASE_CELL_SIZE * scaleFactor);

			for (int r = 0; r < ROWS; r++) {
				for (int c = 0; c < COLS; c++) {
					drawCell(g2d, r, c);
				}
			}
		}

		private void drawCell(Graphics2D g, int r, int c) {
			Cell cell = board[r][c];
			int x = c * cellSize;
			int y = r * cellSize + Math.round(cell.animOffsetY * scaleFactor);

			// 透明度处理
			Composite oldComp = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cell.animAlpha));

			// 绘制格子背景
			switch (cell.state) {
			case HIDDEN:
				g.setColor(HIDDEN_COLOR);
				g.fill3DRect(x, y, cellSize - 1, cellSize - 1, true);
				break;
			case REVEALED:
				g.setColor(REVEALED_COLOR);
				g.fillRect(x, y, cellSize - 1, cellSize - 1);

				if (cell.isMine) {
					// 绘制地雷（按比例缩放）
					int padding = Math.round(4 * scaleFactor);
					g.setColor(MINE_COLOR);
					g.fillOval(x + padding, y + padding, cellSize - 2 * padding, cellSize - 2 * padding);
					g.setColor(Color.BLACK);
					g.drawOval(x + padding, y + padding, cellSize - 2 * padding, cellSize - 2 * padding);
				} else if (cell.adjacentMines > 0) {
					// 绘制数字（按比例缩放字体）
					int fontSize = Math.round(12 * scaleFactor);
					g.setColor(NUMBER_COLORS[cell.adjacentMines - 1]);
					g.setFont(new Font("Arial", Font.BOLD, fontSize));
					FontMetrics fm = g.getFontMetrics();
					String num = String.valueOf(cell.adjacentMines);
					int tx = x + (cellSize - fm.stringWidth(num)) / 2;
					int ty = y + (cellSize + fm.getAscent()) / 2 - Math.round(2 * scaleFactor);
					g.drawString(num, tx, ty);
				}
				break;
			case EMPTY:
				g.setColor(EMPTY_COLOR);
				g.fillRect(x, y, cellSize - 1, cellSize - 1);
				break;
			case EXPLODING:
				// 爆炸闪烁效果
				Color flashColor = (cell.flashCount % 2 == 0) ? new Color(255, 200, 100) : new Color(255, 100, 50);
				g.setColor(flashColor);
				g.fillRect(x, y, cellSize - 1, cellSize - 1);
				break;
			}

			// 绘制边框
			g.setColor(Color.GRAY);
			g.drawRect(x, y, cellSize - 1, cellSize - 1);

			g.setComposite(oldComp);
		}
	}

	// ==================== 动画系统 ====================
	private void initAnimationTimer() {
		animationTimer = new Timer(FRAME_DELAY, e -> {
			try {
				updateAnimations();
				boardPanel.repaint();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		animationTimer.start();
	}

	private void updateAnimations() {
		// 更新爆炸动画
		if (!explodingCells.isEmpty()) {
			for (Point p : explodingCells) {
				Cell cell = board[p.x][p.y];
				cell.flashCount++;
				if (cell.flashCount >= 6) { // 闪烁3次后消失
					cell.state = CellState.EMPTY;
					cell.animAlpha = 0f;
				}
			}
			// 爆炸动画完成
			boolean allDone = true;
			for (Point p : explodingCells) {
				if (board[p.x][p.y].flashCount < 6) {
					allDone = false;
					break;
				}
			}
			if (allDone) {
				explodingCells.clear();
				startFallingAnimation();
			}
		}

		// 更新下落动画
		if (!fallingCells.isEmpty()) {
			Iterator<Map.Entry<Point, Integer>> it = fallingCells.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Point, Integer> entry = it.next();
				Point p = entry.getKey();
				Cell cell = board[p.x][p.y];
				int targetDist = entry.getValue();

				if (cell.animOffsetY < targetDist) {
					cell.animOffsetY = Math.min(cell.animOffsetY + 10, targetDist);
				} else {
					it.remove();
				}
			}

			// 下落完成
			if (fallingCells.isEmpty()) {
				isAnimating = false;
				regenerateBoard();
			}
		}
	}

	// ==================== 游戏核心逻辑 ====================
	private void startGame() {
		// 重置棋盘
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				board[r][c].reset();
			}
		}

		// 生成地雷
		generateBoard();

		// 重置游戏状态
		score = 0;
		firstClick = true; // 重置第一次点击标志
		explodingCells.clear();
		fallingCells.clear();
		isAnimating = false;

		// 更新UI
		updateStatusLabels();
		difficultyLabel.setText("难度: " + getDifficultyName(difficulty));

		// 切换到游戏界面（必须先设置状态，再启动计时器）
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, "GAME");
		currentScreen = GameScreen.PLAYING;

		// 最后启动计时器（确保currentScreen已设置为PLAYING）
		if (gameMode == GameMode.ENTERTAINMENT) {
			remainingTime = 180; // 3分钟
			// 先停止旧的计时器线程（如果存在）
			if (timerFuture != null && !timerFuture.isDone()) {
				timerFuture.cancel(true);
			}
			startTimerThread();
		} else {
			remainingClicks = 50;
		}
	}

	private void generateBoard() {
		int totalMines = difficulty.mineCount;
		Random rand = new Random();

		// 随机放置地雷
		int placed = 0;
		while (placed < totalMines) {
			int r = rand.nextInt(ROWS);
			int c = rand.nextInt(COLS);
			if (!board[r][c].isMine) {
				board[r][c].isMine = true;
				placed++;
			}
		}

		// 计算每个格子周围地雷数
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				if (!board[r][c].isMine) {
					board[r][c].adjacentMines = countAdjacentMines(r, c);
				}
			}
		}
	}

	private int countAdjacentMines(int row, int col) {
		int count = 0;
		for (int dr = -1; dr <= 1; dr++) {
			for (int dc = -1; dc <= 1; dc++) {
				if (dr == 0 && dc == 0)
					continue;
				int nr = row + dr;
				int nc = col + dc;
				if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS) {
					if (board[nr][nc].isMine)
						count++;
				}
			}
		}
		return count;
	}

	private void handleCellClick(int row, int col) {
		Cell cell = board[row][col];

		// 只处理隐藏格子
		if (cell.state != CellState.HIDDEN)
			return;

		// 第一次点击保护：确保第一次点击的位置不是地雷
		if (firstClick) {
			firstClick = false;
			// 如果第一次点击是地雷，重新生成棋盘直到该位置不是地雷
			while (board[row][col].isMine) {
				// 重置棋盘
				for (int r = 0; r < ROWS; r++) {
					for (int c = 0; c < COLS; c++) {
						board[r][c].reset();
					}
				}
				generateBoard();
			}
			cell = board[row][col];
		}

		// 策略模式消耗点击次数
		if (gameMode == GameMode.STRATEGY) {
			remainingClicks--;
			if (remainingClicks <= 0) {
				endGame();
				return;
			}
		}

		if (cell.isMine) {
			// 点击地雷 - 触发连锁爆炸
			triggerExplosion(row, col);
		} else {
			// 点击普通格子 - 只打开当前格子
			cell.state = CellState.REVEALED;
		}

		updateStatusLabels();
	}

	private void triggerExplosion(int hitRow, int hitCol) {
		isAnimating = true;
		playExplosionSound();

		Set<Point> toEliminate = new HashSet<>();
		Set<Point> chainMines = new HashSet<>();

		// 步骤1: 消除被点击地雷的9宫格
		for (int dr = -1; dr <= 1; dr++) {
			for (int dc = -1; dc <= 1; dc++) {
				int nr = hitRow + dr;
				int nc = hitCol + dc;
				if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS) {
					toEliminate.add(new Point(nr, nc));
				}
			}
		}

		// 步骤2: 找出爆炸前已打开的数字格周围的隐藏地雷
		List<Point> revealedNumbers = new ArrayList<>();
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Cell cell = board[r][c];
				if (cell.state == CellState.REVEALED && !cell.isMine && cell.adjacentMines > 0) {
					revealedNumbers.add(new Point(r, c));
				}
			}
		}

		// 找出这些数字格周围的隐藏地雷
		for (Point p : revealedNumbers) {
			for (int dr = -1; dr <= 1; dr++) {
				for (int dc = -1; dc <= 1; dc++) {
					int nr = p.x + dr;
					int nc = p.y + dc;
					if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS) {
						Cell cell = board[nr][nc];
						if (cell.isMine && cell.state == CellState.HIDDEN) {
							chainMines.add(new Point(nr, nc));
						}
					}
				}
			}
		}

		// 步骤3: 消除每个连锁地雷的9宫格
		for (Point mine : chainMines) {
			for (int dr = -1; dr <= 1; dr++) {
				for (int dc = -1; dc <= 1; dc++) {
					int nr = mine.x + dr;
					int nc = mine.y + dc;
					if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS) {
						toEliminate.add(new Point(nr, nc));
					}
				}
			}
		}

		// 步骤4: 消除所有爆炸前已打开的空白格
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Cell cell = board[r][c];
				if (cell.state == CellState.REVEALED && !cell.isMine && cell.adjacentMines == 0) {
					toEliminate.add(new Point(r, c));
				}
			}
		}

		// 计算分数
		int eliminatedCount = toEliminate.size();
		int bonus = chainMines.size();
		int roundScore = eliminatedCount * (1 + bonus);
		score += roundScore;

		// 开始爆炸动画
		for (Point p : toEliminate) {
			Cell cell = board[p.x][p.y];
			cell.state = CellState.EXPLODING;
			cell.flashCount = 0;
			explodingCells.add(p);
		}
	}

	private void startFallingAnimation() {
		// 计算每列需要下落的距离（使用基础格子大小）
		for (int c = 0; c < COLS; c++) {
			int emptyCount = 0;
			// 从下往上扫描
			for (int r = ROWS - 1; r >= 0; r--) {
				Cell cell = board[r][c];
				if (cell.state == CellState.EMPTY) {
					emptyCount++;
				} else if (emptyCount > 0) {
					// 这个格子需要下落（使用基础格子大小，绘制时会按比例缩放）
					fallingCells.put(new Point(r, c), emptyCount * BASE_CELL_SIZE);
				}
			}
		}

		// 如果没有需要下落的，直接重新生成
		if (fallingCells.isEmpty()) {
			isAnimating = false;
			regenerateBoard();
		}
	}

	private void regenerateBoard() {
		// 保存地雷总数
		int totalMines = difficulty.mineCount;

		// 重置所有格子但保留已打开状态
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Cell cell = board[r][c];
				if (cell.state != CellState.REVEALED) {
					cell.reset(); // 隐藏或消除的格子完全重置
				} else {
					// 已打开的格子只重置地雷和数字信息，状态保留
					cell.isMine = false;
					cell.adjacentMines = 0;
					cell.animAlpha = 1.0f;
					cell.animOffsetY = 0;
				}
			}
		}

		// 重新随机放置地雷（不放在已打开的格子上）
		Random rand = new Random();
		int placed = 0;
		int attempts = 0;
		while (placed < totalMines && attempts < 10000) {
			int r = rand.nextInt(ROWS);
			int c = rand.nextInt(COLS);
			Cell cell = board[r][c];
			if (!cell.isMine && cell.state != CellState.REVEALED) {
				cell.isMine = true;
				placed++;
			}
			attempts++;
		}

		// 重新计算周围地雷数
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Cell cell = board[r][c];
				if (!cell.isMine) {
					cell.adjacentMines = countAdjacentMines(r, c);
				}
			}
		}

		// 每次刷新棋盘后重置第一次点击保护，确保下一次点击也不会点到雷
		firstClick = true;

		updateStatusLabels();
	}

	// ==================== 游戏计时器 ====================
	private void startTimerThread() {
		timerFuture = executorService.submit(() -> {
			try {
				while (remainingTime > 0 && currentScreen == GameScreen.PLAYING) {
					Thread.sleep(1000);
					if (currentScreen == GameScreen.PLAYING) {
						remainingTime--;
						SwingUtilities.invokeLater(this::updateStatusLabels);
					}
				}
				if (remainingTime <= 0 && currentScreen == GameScreen.PLAYING) {
					SwingUtilities.invokeLater(this::endGame);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	private void endGame() {
		// 判断胜负
		if (gameMode == GameMode.ENTERTAINMENT) {
			gameWon = score >= 10000;
		} else {
			gameWon = score >= 1000;
		}

		// 更新结束界面
		finalScoreLabel.setText("最终得分: " + score);
		resultLabel.setText(gameWon ? "胜利！" : "失败");
		resultLabel.setForeground(gameWon ? new Color(0, 150, 0) : new Color(200, 0, 0));

		// 切换界面
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, "OVER");
		currentScreen = GameScreen.GAME_OVER;
	}

	private void showMenu() {
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, "MENU");
		currentScreen = GameScreen.MENU;
	}

	// ==================== 音效系统 ====================
	private void playExplosionSound() {
		executorService.submit(() -> {
			try {
				float sampleRate = 44100;
				int duration = 300; // 毫秒
				int samples = (int) (sampleRate * duration / 1000);

				byte[] buffer = new byte[samples * 2];

				// 生成爆炸音效（噪声+衰减）
				Random rand = new Random();
				for (int i = 0; i < samples; i++) {
					// 衰减包络
					double envelope = Math.exp(-4.0 * i / samples);
					// 噪声
					short value = (short) (rand.nextGaussian() * 15000 * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
				SourceDataLine line = AudioSystem.getSourceDataLine(format);
				line.open(format);
				line.start();
				line.write(buffer, 0, buffer.length);
				line.drain();
				line.close();
			} catch (Exception e) {
				// 静默处理音效错误
			}
		});
	}

	// ==================== 辅助方法 ====================
	private void updateStatusLabels() {
		scoreLabel.setText("分数: " + score);
		if (gameMode == GameMode.ENTERTAINMENT) {
			int min = remainingTime / 60;
			int sec = remainingTime % 60;
			statusLabel.setText(String.format("剩余时间: %02d:%02d", min, sec));
		} else {
			statusLabel.setText("剩余点击: " + remainingClicks);
		}
	}

	private String getDifficultyName(Difficulty d) {
		switch (d) {
		case EASY:
			return "简单";
		case NORMAL:
			return "普通";
		case HARD:
			return "困难";
		case EXTREME:
			return "极难";
		default:
			return "普通";
		}
	}

	// ==================== 主方法 ====================
	public static void main(String[] args) {
		// 设置UI外观
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// 使用默认外观
		}

		// EDT线程启动
		SwingUtilities.invokeLater(() -> {
			new ZhaZhaLe().setVisible(true);
		});
	}
}
