package GameCollection.MainWindow;

import javax.swing.*;

import GameCollection.ChildWindow.BulletGame;
import GameCollection.ChildWindow.PlantKingdom;
import GameCollection.ChildWindow.ZhaZhaLe;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 小游戏合集 - 统一主菜单 Java 21 + Swing 实现 连接三个游戏：植物王国崛起、炸炸乐、弹幕贪吃蛇
 */
public class MainMenu extends JFrame {
	// 窗口尺寸
	private static final int WINDOW_WIDTH = 600;
	private static final int WINDOW_HEIGHT = 500;

	// 跟踪所有打开的游戏窗口 - 核心机制
	private static final List<Window> activeGameWindows = new ArrayList<>();

	// 主菜单单例引用（用于游戏窗口关闭时回调）
	private static MainMenu mainMenuInstance;

	public MainMenu() {
		// 保存单例引用
		mainMenuInstance = this;

		// 窗口基本设置
		setTitle("小游戏合集");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		setResizable(false);
		setLocationRelativeTo(null);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));

		// 创建主面板
		JPanel mainPanel = createMainPanel();
		add(mainPanel);
	}

	/**
	 * 创建主面板
	 */
	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(new Color(30, 35, 45));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(15, 20, 15, 20);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		// 标题
		JLabel titleLabel = new JLabel(" 小游戏合集 ", SwingConstants.CENTER);
		titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 42));
		titleLabel.setForeground(new Color(100, 220, 255));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(30, 20, 40, 20);
		panel.add(titleLabel, gbc);

		// 副标题
		JLabel subtitleLabel = new JLabel("选择你想玩的游戏", SwingConstants.CENTER);
		subtitleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
		subtitleLabel.setForeground(new Color(180, 180, 180));
		gbc.gridy = 1;
		gbc.insets = new Insets(0, 20, 30, 20);
		panel.add(subtitleLabel, gbc);

		// 游戏1按钮：植物王国崛起
		JButton game1Btn = createGameButton("🌱 植物王国崛起", "回合制策略游戏 - 建立你的植物王国", new Color(80, 180, 100),
				new Color(100, 220, 120));
		game1Btn.addActionListener(e -> launchPlantKingdom());
		gbc.gridy = 2;
		gbc.insets = new Insets(10, 50, 10, 50);
		panel.add(game1Btn, gbc);

		// 游戏2按钮：炸炸乐
		JButton game2Btn = createGameButton("💥 炸炸乐", "休闲消除游戏 - 扫雷与爆炸的完美融合", new Color(220, 100, 80),
				new Color(255, 130, 100));
		game2Btn.addActionListener(e -> launchZhaZhaLe());
		gbc.gridy = 3;
		panel.add(game2Btn, gbc);

		// 游戏3按钮：弹幕贪吃蛇
		JButton game3Btn = createGameButton("🐍 弹幕贪吃蛇", "动作射击游戏 - 躲避弹幕，收集能量", new Color(80, 150, 220),
				new Color(100, 180, 255));
		game3Btn.addActionListener(e -> launchBulletGame());
		gbc.gridy = 4;
		panel.add(game3Btn, gbc);

		// 退出按钮
		JButton exitBtn = new JButton("退出程序");
		exitBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
		exitBtn.setPreferredSize(new Dimension(200, 45));
		exitBtn.setBackground(new Color(120, 120, 120));
		exitBtn.setForeground(Color.BLACK);
		exitBtn.setFocusPainted(false);
		exitBtn.setBorder(BorderFactory.createRaisedBevelBorder());
		exitBtn.addActionListener(e -> System.exit(0));
		gbc.gridy = 5;
		gbc.insets = new Insets(25, 80, 30, 80);
		panel.add(exitBtn, gbc);

		return panel;
	}

	/**
	 * 创建游戏按钮 确保按钮内文字不超出边界
	 */
	private JButton createGameButton(String title, String description, Color bgColor, Color hoverColor) {
		JButton btn = new JButton("<html><div style='text-align:center;width:380px;'>"
				+ "<span style='font-size:18px;font-weight:bold;'>" + title + "</span><br />"
				+ "<span style='font-size:11px;color:black !important;'>" + description + "</span>" + "</div></html>");
		btn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		btn.setPreferredSize(new Dimension(420, 65));
		btn.setMaximumSize(new Dimension(420, 65));
		btn.setBackground(bgColor);
		btn.setForeground(Color.BLACK);
		btn.setForeground(new Color(0, 0, 0, 0));
		btn.setFocusPainted(false);
		btn.setBorder(BorderFactory.createRaisedBevelBorder());
		btn.setHorizontalTextPosition(SwingConstants.CENTER);

		// 悬停效果
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				btn.setBackground(hoverColor);
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				btn.setBackground(bgColor);
			}
		});

		return btn;
	}

	/**
	 * 启动植物王国崛起 - 特殊处理：有两个窗口（天赋选择 + 游戏主窗口）
	 */
	private void launchPlantKingdom() {
		// 真正关闭主菜单窗口，不是隐藏
		this.dispose();
		mainMenuInstance = null;

		SwingUtilities.invokeLater(() -> {
			try {
				// 直接调用PlantKingdom的main方法
				PlantKingdom.main(new String[] {});

				// 启动窗口检测定时器，持续检测新窗口
				Timer windowDetector = new Timer(0, null);
				windowDetector.addActionListener(e -> {
					for (Window window : Window.getWindows()) {
						if (window instanceof JFrame && !activeGameWindows.contains(window) && window.isVisible()) {
							JFrame gameFrame = (JFrame) window;
							String title = gameFrame.getTitle();
							// 匹配植物王国的两个窗口标题
							if (title.contains("植物") || title.contains("王国") || title.contains("Plant")
									|| title.contains("天赋") || (title.length() > 0 && !title.equals("小游戏合集"))) {
								trackGameWindow(gameFrame);
							}
						}
					}
				});
				windowDetector.setRepeats(true);
				windowDetector.start();

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "启动植物王国崛起失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
				reopenMainMenu();
			}
		});
	}

	/**
	 * 启动炸炸乐
	 */
	private void launchZhaZhaLe() {
		// 真正关闭主菜单窗口
		this.dispose();
		mainMenuInstance = null;

		SwingUtilities.invokeLater(() -> {
			try {
				ZhaZhaLe game = new ZhaZhaLe();
				trackGameWindow(game);
				game.setVisible(true);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "启动炸炸乐失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
				reopenMainMenu();
			}
		});
	}

	/**
	 * 启动弹幕贪吃蛇
	 */
	private void launchBulletGame() {
		// 真正关闭主菜单窗口
		this.dispose();
		mainMenuInstance = null;

		SwingUtilities.invokeLater(() -> {
			try {
				BulletGame game = new BulletGame();
				trackGameWindow(game);
				game.setVisible(true);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "启动弹幕贪吃蛇失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
				reopenMainMenu();
			}
		});
	}

	/**
	 * 核心：跟踪游戏窗口 设置关闭行为并添加关闭监听器 只有当所有窗口都关闭时才重新打开主菜单
	 */
	private synchronized void trackGameWindow(JFrame gameFrame) {
		// 修改关闭行为：关闭时只销毁窗口，不退出程序
		gameFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// 添加到活动窗口列表（避免重复添加）
		if (!activeGameWindows.contains(gameFrame)) {
			activeGameWindows.add(gameFrame);
		}

		// 添加窗口关闭监听器
		gameFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				synchronized (MainMenu.class) {
					// 从活动列表中移除
					activeGameWindows.remove(gameFrame);

					// 关键修复：延迟1000ms后再检查，解决PlantKingdom天赋窗口切换时序问题
					// 确保天赋窗口关闭后，游戏主窗口有足够时间被添加到跟踪列表
					Timer checkTimer = new Timer(1000, evt -> {
						synchronized (MainMenu.class) {
							// 只有当所有窗口都真正关闭时才重新打开主菜单
							if (activeGameWindows.isEmpty()) {
								SwingUtilities.invokeLater(MainMenu::reopenMainMenu);
							}
						}
					});
					checkTimer.setRepeats(false);
					checkTimer.start();
				}
			}
		});
	}

	/**
	 * 重新打开主菜单
	 */
	private static synchronized void reopenMainMenu() {
		if (mainMenuInstance == null) {
			SwingUtilities.invokeLater(() -> {
				mainMenuInstance = new MainMenu();
				mainMenuInstance.setVisible(true);
			});
		} else {
			mainMenuInstance.setVisible(true);
		}
	}

	/**
	 * 程序入口
	 */
	public static void main(String[] args) {
		// 设置Swing外观
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// 使用默认外观
		}

		// 在EDT线程中启动UI
		SwingUtilities.invokeLater(() -> {
			MainMenu menu = new MainMenu();
			menu.setVisible(true);
		});
	}
}
