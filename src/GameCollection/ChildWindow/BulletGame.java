package GameCollection.ChildWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*;

/**
 * 弹幕游戏 - Java 21 + Swing 40x22棋盘，鼠标控制角色，贪吃蛇尾巴，多种弹幕机制 支持窗口缩放，高帧率流畅运行，音效系统
 */
public class BulletGame extends JFrame {
	// 棋盘配置
	private static final int COLS = 40;
	private static final int ROWS = 22;
	private static final int MIN_CELL_SIZE = 10;
	private static final int MAX_CELL_SIZE = 50;
	private int cellSize = 25;
	private int panelWidth = COLS * cellSize;
	private int panelHeight = ROWS * cellSize;
	// 游戏状态
	private int score = 0;
	private boolean gameOver = false;
	private boolean gameWon = false;
	private boolean paused = false;
	private boolean inMainMenu = true; // 主菜单状态
	private final Random random = new Random();
	// 蛇（角色）数据
	private Point snakeHead = new Point(COLS / 2, ROWS / 2);
	private final List<Point> snakeTail = new ArrayList<>();
	private int tailLength = 3; // 初始尾巴长度
	// 弹幕列表
	private final List<Bullet> bullets = new ArrayList<>();
	// 黄色不动点
	private final List<YellowPoint> yellowPoints = new ArrayList<>();
	// 褐色地块
	private final List<BrownBlock> brownBlocks = new ArrayList<>();
	private int brownEffectLevel = 1;
	private long lastBrownEliminationTime = System.currentTimeMillis();
	// 键盘状态 - WASD控制弹幕方向
	private boolean wPressed = false; // 弹幕向下
	private boolean aPressed = false; // 弹幕向右
	private boolean sPressed = false; // 弹幕向上
	private boolean dPressed = false; // 弹幕向左
	// 计时器
	private long lastBulletSpawn = 0;
	private long lastYellowSpawn = 0;
	private long lastBrownSpawn = 0;
	private long lastBrownEffectCheck = 0;
	private GamePanel gamePanel;
	private MainMenuPanel mainMenuPanel;

	// 音效播放器
	private SoundPlayer soundPlayer;

	public BulletGame() {
		setTitle("弹幕贪吃蛇游戏");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(true); // 允许窗口缩放
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon1.png")));

		// 初始化音效播放器
		soundPlayer = new SoundPlayer();

		// 创建主菜单面板和游戏面板
		mainMenuPanel = new MainMenuPanel();
		gamePanel = new GamePanel();

		// 初始显示主菜单
		add(mainMenuPanel);
		pack();
		setLocationRelativeTo(null);

		// 添加窗口大小变化监听器
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateCellSize();
			}
		});
		// 游戏主循环 - 提高到60fps (16ms) 更丝滑
		Timer gameTimer = new Timer(16, e -> {
			if (!inMainMenu && !gameOver && !gameWon && !paused) {
				updateGame();
			}
			if (inMainMenu) {
				mainMenuPanel.repaint();
			} else {
				gamePanel.repaint();
			}
		});
		gameTimer.start();
	}

	// 开始游戏
	private void startGame() {
		inMainMenu = false;
		getContentPane().removeAll();
		getContentPane().add(gamePanel);
		resetGame();
		revalidate();
		repaint();
		gamePanel.requestFocusInWindow();
	}

	// 主菜单面板
	private class MainMenuPanel extends JPanel {
		private JButton startButton;
		private JButton exitButton;

		public MainMenuPanel() {
			setPreferredSize(new Dimension(panelWidth, panelHeight));
			setLayout(new GridBagLayout());
			setBackground(Color.BLACK);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(15, 15, 15, 15);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			// 游戏标题
			JLabel titleLabel = new JLabel("弹幕贪吃蛇游戏", SwingConstants.CENTER);
			titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
			titleLabel.setForeground(new Color(0, 255, 100));
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			add(titleLabel, gbc);

			// 副标题
			JLabel subtitleLabel = new JLabel("Bullet Snake Game", SwingConstants.CENTER);
			subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 24));
			subtitleLabel.setForeground(Color.CYAN);
			gbc.gridy = 1;
			add(subtitleLabel, gbc);

			// 装饰分隔线
			gbc.gridy = 2;
			add(Box.createVerticalStrut(30), gbc);

			// 开始游戏按钮
			startButton = new JButton("开始游戏");
			startButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
			startButton.setPreferredSize(new Dimension(250, 60));
			startButton.setBackground(new Color(50, 200, 50));
			startButton.setForeground(Color.BLACK);
			startButton.setFocusPainted(false);
			startButton.setBorder(BorderFactory.createRaisedBevelBorder());
			startButton.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseEntered(java.awt.event.MouseEvent evt) {
					startButton.setBackground(new Color(80, 255, 80));
				}

				public void mouseExited(java.awt.event.MouseEvent evt) {
					startButton.setBackground(new Color(50, 200, 50));
				}
			});
			startButton.addActionListener(e -> startGame());
			gbc.gridy = 3;
			gbc.gridwidth = 1;
			add(startButton, gbc);

			// 退出游戏按钮
			exitButton = new JButton("退出游戏");
			exitButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
			exitButton.setPreferredSize(new Dimension(250, 60));
			exitButton.setBackground(new Color(200, 50, 50));
			exitButton.setForeground(Color.BLACK);
			exitButton.setFocusPainted(false);
			exitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseEntered(java.awt.event.MouseEvent evt) {
					exitButton.setBackground(new Color(255, 80, 80));
				}

				public void mouseExited(java.awt.event.MouseEvent evt) {
					exitButton.setBackground(new Color(200, 50, 50));
				}
			});
			exitButton.addActionListener(e -> dispose());
			gbc.gridy = 4;
			add(exitButton, gbc);

			// 操作提示
			gbc.gridy = 5;
			add(Box.createVerticalStrut(40), gbc);

			JLabel hintLabel = new JLabel("鼠标控制角色 | WASD改变弹幕方向 | ESC暂停", SwingConstants.CENTER);
			hintLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
			hintLabel.setForeground(Color.LIGHT_GRAY);
			gbc.gridy = 6;
			gbc.gridwidth = 2;
			add(hintLabel, gbc);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			// 高质量渲染设置
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// 绘制动态背景粒子效果
			g2d.setColor(new Color(0, 100, 150, 50));
			long time = System.currentTimeMillis();
			for (int i = 0; i < 30; i++) {
				int x = (int) ((Math.sin(time / 1000.0 + i * 0.5) + 1) * getWidth() / 2);
				int y = (int) ((Math.cos(time / 800.0 + i * 0.7) + 1) * getHeight() / 2);
				int size = 5 + (int) (Math.sin(time / 500.0 + i) * 3);
				g2d.fillOval(x, y, size, size);
			}

			// 绘制边框装饰
			g2d.setColor(new Color(0, 255, 100, 100));
			g2d.setStroke(new BasicStroke(3));
			g2d.drawRect(20, 20, getWidth() - 40, getHeight() - 40);
		}
	}

	// 音效播放器内部类
	private class SoundPlayer {
		// 播放清脆叮音效（正弦波）
		public void playDing() {
			try {
				float sampleRate = 44100;
				int duration = 150; // 毫秒
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					double angle = i / (sampleRate / 1200.0) * 2.0 * Math.PI;
					short value = (short) (Math.sin(angle) * 32767 * 0.5);
					// 包络 - 快速衰减
					double envelope = Math.exp(-i / (samples * 0.3));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放电脑错误音效
		public void playError() {
			try {
				float sampleRate = 44100;
				int duration = 200;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 低频方波模拟错误音效
					double angle = i / (sampleRate / 220.0) * 2.0 * Math.PI;
					short value = (short) ((angle % (2 * Math.PI) < Math.PI ? 1 : -1) * 16000);
					double envelope = Math.exp(-i / (samples * 0.5));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放敲碎石头声音
		public void playCrush() {
			try {
				float sampleRate = 44100;
				int duration = 180;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 噪音 + 低频冲击模拟碎石
					double noise = (random.nextDouble() * 2 - 1);
					double lowFreq = Math.sin(i / (sampleRate / 150.0) * 2.0 * Math.PI);
					short value = (short) ((noise * 0.7 + lowFreq * 0.3) * 32767 * 0.6);
					double envelope = Math.exp(-i / (samples * 0.15));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放强劲鼓点
		public void playDrum() {
			try {
				float sampleRate = 44100;
				int duration = 250;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 快速下降的正弦波模拟鼓点
					double freq = 150 * Math.exp(-i / (samples * 0.3));
					double angle = i / (sampleRate / freq) * 2.0 * Math.PI;
					short value = (short) (Math.sin(angle) * 32767 * 0.8);
					double envelope = Math.exp(-i / (samples * 0.2));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放气泡破裂
		public void playBubble() {
			try {
				float sampleRate = 44100;
				int duration = 100;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 上升频率正弦波模拟气泡
					double freq = 400 + 600 * (double) i / samples;
					double angle = i / (sampleRate / freq) * 2.0 * Math.PI;
					short value = (short) (Math.sin(angle) * 32767 * 0.4);
					double envelope = Math.exp(-i / (samples * 0.5));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放断裂声
		public void playSnap() {
			try {
				float sampleRate = 44100;
				int duration = 120;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 高频噪音模拟断裂
					double noise = (random.nextDouble() * 2 - 1);
					double highFreq = Math.sin(i / (sampleRate / 2000.0) * 2.0 * Math.PI);
					short value = (short) ((noise * 0.6 + highFreq * 0.4) * 32767 * 0.5);
					double envelope = i < samples * 0.1 ? (double) i / (samples * 0.1)
							: Math.exp(-(i - samples * 0.1) / (samples * 0.1));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		// 播放撞墙声
		public void playWallHit() {
			try {
				float sampleRate = 44100;
				int duration = 200;
				int samples = (int) (sampleRate * duration / 1000);
				byte[] buffer = new byte[samples * 2];

				for (int i = 0; i < samples; i++) {
					// 低频冲击 + 共振模拟撞墙
					double noise = (random.nextDouble() * 2 - 1);
					double lowFreq = Math.sin(i / (sampleRate / 100.0) * 2.0 * Math.PI);
					short value = (short) ((noise * 0.4 + lowFreq * 0.6) * 32767 * 0.7);
					double envelope = Math.exp(-i / (samples * 0.25));
					value = (short) (value * envelope);
					buffer[i * 2] = (byte) (value & 0xFF);
					buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
				}

				playSound(buffer, sampleRate);
			} catch (Exception e) {
				// 静默失败
			}
		}

		private void playSound(byte[] audioData, float sampleRate) {
			new Thread(() -> {
				try {
					AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
					SourceDataLine line = AudioSystem.getSourceDataLine(format);
					line.open(format);
					line.start();
					line.write(audioData, 0, audioData.length);
					line.drain();
					line.close();
				} catch (Exception e) {
					// 静默失败，不影响游戏
				}
			}).start();
		}
	}

	private void updateCellSize() {
		int availableWidth = getContentPane().getWidth();
		int availableHeight = getContentPane().getHeight();

		if (availableWidth > 0 && availableHeight > 0) {
			int cellByWidth = availableWidth / COLS;
			int cellByHeight = availableHeight / ROWS;
			cellSize = Math.min(cellByWidth, cellByHeight);
			cellSize = Math.clamp(cellSize, MIN_CELL_SIZE, MAX_CELL_SIZE);
			panelWidth = COLS * cellSize;
			panelHeight = ROWS * cellSize;
		}
	}

	private void resetGame() {
		score = 0;
		gameOver = false;
		gameWon = false;
		paused = false;
		snakeHead = new Point(COLS / 2, ROWS / 2);
		snakeTail.clear();
		tailLength = 3;
		bullets.clear();
		yellowPoints.clear();
		brownBlocks.clear();
		brownEffectLevel = 1;
		lastBrownEliminationTime = System.currentTimeMillis();
		lastBulletSpawn = 0;
		lastYellowSpawn = 0;
		lastBrownSpawn = 0;
		lastBrownEffectCheck = 0;
		wPressed = false;
		aPressed = false;
		sPressed = false;
		dPressed = false;
	}

	private void checkWinLose() {
		if (score >= 500) {
			gameWon = true;
		} else if (score <= -50) {
			gameOver = true;
		}
	}

	private void updateGame() {
		long now = System.currentTimeMillis();
		// 生成弹幕 - 每2-10秒生成，速度不同
		if (now - lastBulletSpawn > random.nextInt(8000) + 2000) {
			spawnBullets();
			lastBulletSpawn = now;
		}
		// 生成黄色不动点 - 每5秒一个
		if (now - lastYellowSpawn > 5000) {
			spawnYellowPoint();
			lastYellowSpawn = now;
		}
		// 生成褐色地块 - 每10秒
		if (now - lastBrownSpawn > 10000) {
			spawnBrownBlock();
			lastBrownSpawn = now;
		}
		// 褐色地块效果检查 - 每25秒效果x2（翻倍）
		if (now - lastBrownEffectCheck > 25000) {
			if (!brownBlocks.isEmpty()) {
				brownEffectLevel *= 2; // 效果翻倍
			}
			lastBrownEffectCheck = now;
		}
		// 检查15秒内是否消除过褐色地块
		if (now - lastBrownEliminationTime > 15000 && !brownBlocks.isEmpty()) {
			// 数量翻倍
			int currentSize = brownBlocks.size();
			for (int i = 0; i < currentSize; i++) {
				spawnBrownBlock();
			}
			lastBrownEliminationTime = now;
		}
		// 检测到场上没有褐色地块时，褐色地块的效果变为一倍
		if (brownBlocks.isEmpty()) {
			brownEffectLevel = 1;
		}
		// 更新弹幕位置 - WASD改变所有弹幕方向
		updateBullets();
		// 更新黄色点扣分（每秒-1分）
		updateYellowPoints();
		// 检查碰撞
		checkCollisions();
		// 更新蛇尾巴
		updateSnakeTail();
		// 检查胜负
		checkWinLose();
	}

	private void spawnBullets() {
		// 每次生成5-7个弹幕
		int count = random.nextInt(3) + 5;
		for (int i = 0; i < count; i++) {
			// 决定弹幕类型
			int type;
			double rand = random.nextDouble();
			if (rand < 0.07) { // 紫色 - 青色的1/10左右
				type = 2;
			} else if (rand < 0.40) { // 青色 ~33%
				type = 0;
			} else { // 红色 ~60%
				type = 1;
			}
			// 从四边随机选择出生边
			int side = random.nextInt(4);
			int x, y, dx, dy;
			double speed = (random.nextDouble() * 0.8 + 0.2) * 0.3; // 每个弹幕速度不同
			switch (side) {
			case 0 -> { // 上边出，向下走
				x = random.nextInt(COLS);
				y = 0;
				dx = 0;
				dy = 1;
			}
			case 1 -> { // 下边出，向上走
				x = random.nextInt(COLS);
				y = ROWS - 1;
				dx = 0;
				dy = -1;
			}
			case 2 -> { // 左边出，向右走
				x = 0;
				y = random.nextInt(ROWS);
				dx = 1;
				dy = 0;
			}
			default -> { // 右边出，向左走
				x = COLS - 1;
				y = random.nextInt(ROWS);
				dx = -1;
				dy = 0;
			}
			}
			bullets.add(new Bullet(x, y, dx, dy, speed, type));
		}
	}

	private void spawnYellowPoint() {
		int x, y;
		do {
			x = random.nextInt(COLS);
			y = random.nextInt(ROWS);
		} while (isSnakePosition(x, y));
		yellowPoints.add(new YellowPoint(x, y, System.currentTimeMillis()));
	}

	private void spawnBrownBlock() {
		int x, y;
		int attempts = 0;
		do {
			x = random.nextInt(COLS);
			y = random.nextInt(ROWS);
			attempts++;
		} while (isSnakePosition(x, y) && attempts < 100);
		if (attempts < 100) {
			brownBlocks.add(new BrownBlock(x, y));
		}
	}

	private boolean isSnakePosition(int x, int y) {
		if (snakeHead.x == x && snakeHead.y == y)
			return true;
		for (Point p : snakeTail) {
			if (p.x == x && p.y == y)
				return true;
		}
		return false;
	}

	private void updateBullets() {
		List<Bullet> toRemove = new ArrayList<>();
		for (Bullet bullet : bullets) {
			// WASD控制所有弹幕方向 - 不保留原方向，完全按照WASD
			int newDx = bullet.dx;
			int newDy = bullet.dy;

			if (wPressed) {
				newDx = 0;
				newDy = 1; // W - 所有弹幕向下
			}
			if (sPressed) {
				newDx = 0;
				newDy = -1; // S - 所有弹幕向上
			}
			if (aPressed) {
				newDx = 1;
				newDy = 0; // A - 所有弹幕向右
			}
			if (dPressed) {
				newDx = -1;
				newDy = 0; // D - 所有弹幕向左
			}
			// 使用弹幕自身的速度，方向由WASD完全控制
			bullet.x += newDx * bullet.speed;
			bullet.y += newDy * bullet.speed;
			// 移除出界的弹幕
			if (bullet.x < -2 || bullet.x > COLS + 2 || bullet.y < -2 || bullet.y > ROWS + 2) {
				toRemove.add(bullet);
			}
		}
		bullets.removeAll(toRemove);
	}

	private void updateYellowPoints() {
		long now = System.currentTimeMillis();
		for (YellowPoint yp : yellowPoints) {
			// 每秒减1分
			long seconds = (now - yp.spawnTime) / 1000;
			if (seconds > yp.lastPenaltySecond) {
				score--;
				yp.lastPenaltySecond = seconds;
			}
		}
	}

	private void updateSnakeTail() {
		// 在头部插入当前位置
		snakeTail.add(0, new Point(snakeHead.x, snakeHead.y));
		// 保持尾巴长度
		while (snakeTail.size() > tailLength) {
			snakeTail.remove(snakeTail.size() - 1);
		}
	}

	private void checkCollisions() {
		List<Bullet> bulletsToRemove = new ArrayList<>();
		List<BrownBlock> brownToRemove = new ArrayList<>();
		// 弹幕与蛇的碰撞
		for (Bullet bullet : bullets) {
			int bx = (int) Math.round(bullet.x);
			int by = (int) Math.round(bullet.y);
			// 蛇头碰撞
			if (bx == snakeHead.x && by == snakeHead.y) {
				if (bullet.type == 0) { // 青色
					score++;
					bulletsToRemove.add(bullet);
					soundPlayer.playDing(); // 清脆叮音效
				} else if (bullet.type == 1) { // 红色
					score--;
					bulletsToRemove.add(bullet);
					soundPlayer.playError(); // 电脑错误音效
				} else if (bullet.type == 2) { // 紫色 - 蛇头加5分
					score += 5;
					tailLength += 1;
					bulletsToRemove.add(bullet);
					soundPlayer.playSnap(); // 断裂声
				}
			}
			// 蛇尾碰撞
			for (Point tail : snakeTail) {
				if (bx == tail.x && by == tail.y) {
					if (bullet.type == 0) { // 青色 - 尾巴碰到也加分
						score++;
						bulletsToRemove.add(bullet);
						soundPlayer.playDing(); // 清脆叮音效
					} else if (bullet.type == 1) { // 红色 - 尾巴清除不扣分
						bulletsToRemove.add(bullet);
						soundPlayer.playCrush(); // 敲碎石头声音
					} else if (bullet.type == 2) { // 紫色 - 蛇尾减20分
						score -= 20;
						bulletsToRemove.add(bullet);
						soundPlayer.playWallHit(); // 撞墙声
					}
					break;
				}
			}
		}
		// 褐色地块碰撞 - 蛇头触碰
		for (BrownBlock bb : brownBlocks) {
			if (bb.x == snakeHead.x && bb.y == snakeHead.y) {
				tailLength = Math.max(0, tailLength - brownEffectLevel);
				brownToRemove.add(bb);
				lastBrownEliminationTime = System.currentTimeMillis();
				soundPlayer.playBubble(); // 气泡破裂
			}
		}
		bullets.removeAll(bulletsToRemove);
		brownBlocks.removeAll(brownToRemove);
	}

	private class GamePanel extends JPanel {
		private int offsetX = 0;
		private int offsetY = 0;

		public GamePanel() {
			setPreferredSize(new Dimension(panelWidth, panelHeight));
			setBackground(Color.BLACK);
			setDoubleBuffered(true); // 双缓冲消除闪烁
			// 鼠标移动控制角色 - 修复偏移问题
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					// 减去偏移量，修正鼠标定位
					int x = Math.clamp((e.getX() - offsetX) / cellSize, 0, COLS - 1);
					int y = Math.clamp((e.getY() - offsetY) / cellSize, 0, ROWS - 1);
					snakeHead.setLocation(x, y);
				}
			});
			// 鼠标点击 - 修复偏移问题
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						int gridX = Math.clamp((e.getX() - offsetX) / cellSize, 0, COLS - 1);
						int gridY = Math.clamp((e.getY() - offsetY) / cellSize, 0, ROWS - 1);

						// 检查是否点击黄色点且角色在该地块
						if (gridX == snakeHead.x && gridY == snakeHead.y) {
							List<YellowPoint> toRemove = new ArrayList<>();
							for (YellowPoint yp : yellowPoints) {
								if (yp.x == gridX && yp.y == gridY) {
									score += 10;
									tailLength += 5;
									toRemove.add(yp);
									soundPlayer.playDrum(); // 强劲鼓点
								}
							}
							yellowPoints.removeAll(toRemove);
						}
					}
				}
			});
			// 键盘控制WASD + ESC暂停 + R重新开始 + Q退出 + M返回主菜单
			setFocusable(true);
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_W -> wPressed = true;
					case KeyEvent.VK_A -> aPressed = true;
					case KeyEvent.VK_S -> sPressed = true;
					case KeyEvent.VK_D -> dPressed = true;
					case KeyEvent.VK_ESCAPE -> {
						if (!gameOver && !gameWon) {
							paused = !paused;
						}
					}
					case KeyEvent.VK_R -> resetGame();
					case KeyEvent.VK_Q -> dispose();
					case KeyEvent.VK_M -> {
						// 返回主菜单
						inMainMenu = true;
						getContentPane().removeAll();
						getContentPane().add(mainMenuPanel);
						revalidate();
						repaint();
					}
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_W -> wPressed = false;
					case KeyEvent.VK_A -> aPressed = false;
					case KeyEvent.VK_S -> sPressed = false;
					case KeyEvent.VK_D -> dPressed = false;
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			// 高质量渲染设置
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			// 计算偏移，使棋盘居中
			offsetX = (getWidth() - panelWidth) / 2;
			offsetY = (getHeight() - panelHeight) / 2;
			// 绘制背景
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, getWidth(), getHeight());
			// 绘制网格
			g2d.setColor(new Color(30, 30, 30));
			for (int i = 0; i <= COLS; i++) {
				g2d.drawLine(offsetX + i * cellSize, offsetY, offsetX + i * cellSize, offsetY + panelHeight);
			}
			for (int i = 0; i <= ROWS; i++) {
				g2d.drawLine(offsetX, offsetY + i * cellSize, offsetX + panelWidth, offsetY + i * cellSize);
			}
			// 绘制褐色地块
			for (BrownBlock bb : brownBlocks) {
				g2d.setColor(new Color(139, 69, 19));
				g2d.fillRect(offsetX + bb.x * cellSize + 2, offsetY + bb.y * cellSize + 2, cellSize - 4, cellSize - 4);
			}
			// 绘制黄色不动点
			for (YellowPoint yp : yellowPoints) {
				g2d.setColor(Color.YELLOW);
				g2d.fillOval(offsetX + yp.x * cellSize + cellSize / 5, offsetY + yp.y * cellSize + cellSize / 5,
						cellSize - cellSize * 2 / 5, cellSize - cellSize * 2 / 5);
			}
			// 绘制蛇尾巴
			for (int i = 0; i < snakeTail.size(); i++) {
				Point p = snakeTail.get(i);
				float alpha = 1f - (float) i / snakeTail.size() * 0.7f;
				g2d.setColor(new Color(0, 255, 0, (int) (alpha * 255)));
				g2d.fillRect(offsetX + p.x * cellSize + 3, offsetY + p.y * cellSize + 3, cellSize - 6, cellSize - 6);
			}
			// 绘制蛇头
			g2d.setColor(Color.GREEN);
			g2d.fillRect(offsetX + snakeHead.x * cellSize + 2, offsetY + snakeHead.y * cellSize + 2, cellSize - 4,
					cellSize - 4);
			// 绘制弹幕
			for (Bullet bullet : bullets) {
				switch (bullet.type) {
				case 0 -> g2d.setColor(Color.CYAN); // 青色
				case 1 -> g2d.setColor(Color.RED); // 红色
				case 2 -> g2d.setColor(new Color(148, 0, 211)); // 紫色
				}
				g2d.fillOval(offsetX + (int) (bullet.x * cellSize) + cellSize / 5,
						offsetY + (int) (bullet.y * cellSize) + cellSize / 5, cellSize - cellSize * 2 / 5,
						cellSize - cellSize * 2 / 5);
			}
			// 根据窗口大小动态调整字体
			int fontSize = Math.max(12, cellSize * 3 / 4);
			// 绘制左上角状态
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
			g2d.drawString("分数: " + score, offsetX + 10, offsetY + fontSize + 5);
			g2d.drawString("尾巴长度: " + tailLength, offsetX + 10, offsetY + fontSize * 2 + 10);
			g2d.drawString("按ESC查看帮助 | M返回主菜单", offsetX + 10, offsetY + fontSize * 3 + 15);

			// 显示褐色地块当前效果等级
			if (!brownBlocks.isEmpty()) {
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize - 2));
				g2d.drawString("褐色效果: x" + brownEffectLevel, offsetX + 10, offsetY + fontSize * 4 + 20);
			}
			// WASD状态指示
			StringBuilder wasdStatus = new StringBuilder();
			if (wPressed)
				wasdStatus.append("W(下) ");
			if (sPressed)
				wasdStatus.append("S(上) ");
			if (aPressed)
				wasdStatus.append("A(右) ");
			if (dPressed)
				wasdStatus.append("D(左) ");
			if (wasdStatus.length() > 0) {
				g2d.setColor(Color.CYAN);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize - 2));
				g2d.drawString("弹幕方向: " + wasdStatus, offsetX + 10, offsetY + fontSize * 5 + 25);
			}
			// 暂停画面 - 修复文字重叠问题，使用动态间距
			if (paused) {
				g2d.setColor(new Color(0, 0, 0, 210));
				g2d.fillRect(0, 0, getWidth(), getHeight());
				int centerX = getWidth() / 2;
				int centerY = getHeight() / 2;

				// 动态计算字体大小和行间距，基于窗口高度
				int lineHeight = Math.max(22, cellSize);
				int smallFont = Math.max(14, cellSize - 4);
				int titleFont = Math.max(32, cellSize * 2);
				// 计算起始Y位置，基于内容总行数
				int totalLines = 15;
				int startY = centerY - (totalLines * lineHeight) / 2;
				int currentY = startY;
				// 暂停标题
				g2d.setColor(Color.YELLOW);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, titleFont));
				String pauseText = "游戏暂停";
				FontMetrics fm = g2d.getFontMetrics();
				g2d.drawString(pauseText, centerX - fm.stringWidth(pauseText) / 2, currentY);
				currentY += lineHeight * 2;
				// 操作说明标题
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, smallFont + 4));
				String opTitle = "===== 操作说明 =====";
				fm = g2d.getFontMetrics();
				g2d.drawString(opTitle, centerX - fm.stringWidth(opTitle) / 2, currentY);
				currentY += lineHeight;
				// 操作说明内容
				g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, smallFont));
				String[] ops = { "鼠标移动 - 控制角色位置", "WASD - 改变所有弹幕方向", "鼠标左键 - 站在黄点上点击消除", "ESC - 暂停/继续",
						"R - 重新开始 | M - 返回主菜单 | Q - 退出游戏" };
				for (String op : ops) {
					fm = g2d.getFontMetrics();
					g2d.drawString(op, centerX - fm.stringWidth(op) / 2, currentY);
					currentY += lineHeight;
				}
				currentY += lineHeight / 2;
				// 元素功效标题
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, smallFont + 4));
				String elemTitle = "===== 元素功效 =====";
				fm = g2d.getFontMetrics();
				g2d.drawString(elemTitle, centerX - fm.stringWidth(elemTitle) / 2, currentY);
				currentY += lineHeight;
				// 元素功效内容
				g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, smallFont));
				g2d.setColor(Color.CYAN);
				String s1 = "● 青色弹幕: 头/尾接住 +1分";
				fm = g2d.getFontMetrics();
				g2d.drawString(s1, centerX - fm.stringWidth(s1) / 2, currentY);
				currentY += lineHeight;

				g2d.setColor(Color.RED);
				String s2 = "● 红色弹幕: 头接-1分, 尾巴接住清除不扣分";
				fm = g2d.getFontMetrics();
				g2d.drawString(s2, centerX - fm.stringWidth(s2) / 2, currentY);
				currentY += lineHeight;

				g2d.setColor(new Color(148, 0, 211));
				String s3 = "● 紫色弹幕: 头接+5分+1尾巴, 尾巴接-20分";
				fm = g2d.getFontMetrics();
				g2d.drawString(s3, centerX - fm.stringWidth(s3) / 2, currentY);
				currentY += lineHeight;

				g2d.setColor(Color.YELLOW);
				String s4 = "● 黄色点: 每秒-1分, 站上去左键消除+10分,+尾巴";
				fm = g2d.getFontMetrics();
				g2d.drawString(s4, centerX - fm.stringWidth(s4) / 2, currentY);
				currentY += lineHeight;

				g2d.setColor(new Color(139, 69, 19));
				String s5 = "● 褐色块: 触碰尾巴-x" + brownEffectLevel + ", 每25秒效果翻1倍, 15秒不消数量翻倍";
				fm = g2d.getFontMetrics();
				g2d.drawString(s5, centerX - fm.stringWidth(s5) / 2, currentY);
				currentY += lineHeight;
				// 胜负规则
				g2d.setColor(Color.LIGHT_GRAY);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, smallFont + 2));
				String rule = "胜利: 500分 | 失败: -50分";
				fm = g2d.getFontMetrics();
				g2d.drawString(rule, centerX - fm.stringWidth(rule) / 2, currentY + lineHeight / 2);
				currentY += lineHeight * 1.5;
				// 继续提示
				g2d.setColor(Color.GREEN);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, smallFont + 4));
				String resumeText = "按 ESC 继续游戏";
				fm = g2d.getFontMetrics();
				g2d.drawString(resumeText, centerX - fm.stringWidth(resumeText) / 2, currentY);
			}
			// 游戏结束/胜利
			if (gameOver || gameWon) {
				g2d.setColor(new Color(0, 0, 0, 200));
				g2d.fillRect(0, 0, getWidth(), getHeight());
				g2d.setColor(gameWon ? Color.GREEN : Color.RED);
				int bigFont = Math.max(36, cellSize * 2 + 10);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, bigFont));
				String text = gameWon ? "胜利!" : "游戏结束!";
				FontMetrics fm = g2d.getFontMetrics();
				g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2 - 20);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(20, cellSize + 5)));
				g2d.setColor(Color.WHITE);
				String scoreText = "最终分数: " + score;
				fm = g2d.getFontMetrics();
				g2d.drawString(scoreText, (getWidth() - fm.stringWidth(scoreText)) / 2, getHeight() / 2 + 30);
				g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(16, cellSize)));
				String restartText = "按 R 重新开始 | 按 M 返回主菜单 | 按 Q 退出游戏";
				fm = g2d.getFontMetrics();
				g2d.drawString(restartText, (getWidth() - fm.stringWidth(restartText)) / 2, getHeight() / 2 + 70);
			}
		}
	}

	// 弹幕类
	private static class Bullet {
		double x, y;
		int dx, dy;
		double speed; // 每个弹幕保留自己的速度
		int type; // 0:青色, 1:红色, 2:紫色

		Bullet(double x, double y, int dx, int dy, double speed, int type) {
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.speed = speed;
			this.type = type;
		}
	}

	// 黄色不动点
	private static class YellowPoint {
		int x, y;
		long spawnTime;
		long lastPenaltySecond;

		YellowPoint(int x, int y, long spawnTime) {
			this.x = x;
			this.y = y;
			this.spawnTime = spawnTime;
			this.lastPenaltySecond = 0;
		}
	}

	// 褐色地块
	private static class BrownBlock {
		int x, y;

		BrownBlock(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static void main(String[] args) {
		// 系统属性优化
		System.setProperty("sun.java2d.opengl", "true");
		System.setProperty("sun.java2d.d3d", "true");
		System.setProperty("sun.java2d.noddraw", "false");

		SwingUtilities.invokeLater(() -> {
			new BulletGame().setVisible(true);
		});
	}
}
