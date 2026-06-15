package GameCollection.ChildWindow;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 植物王国崛起 - 回合制策略游戏 基于Java Swing + Java 21 LTS开发 采用MVC架构设计
 */
public class PlantKingdom {
	// ==================== 游戏常量定义 ====================
	private static final int WIN_TURN = 46; // 胜利条件：活过的回数
	private static final int INITIAL_ENERGY = 5; // 初始能量
	private static final int BASE_ENERGY_CAP = 10; // 基础能量上限
	private static final int ADULT_LIFESPAN = 10; // 成年植株基础寿命
	private static final int JUVENILE_GROWTH_TIME = 3; // 幼年植株基础成长时间
	private static final int ADULT_INITIAL_ENERGY = 5; // 成年植株初始能量
	private static final int POPULATION_PENALTY_THRESHOLD = 15; // 种群惩罚阈值
	private static final int FLOWER_COST = 1; // 开花基础能量消耗
	private static final int FRUIT_COST = 2; // 结果基础能量消耗
	private static final int SEEDLINGS_PER_FRUIT = 4; // 每个果实基础出苗数
	private static final int SPECIAL_BREEDING_THRESHOLD = 4;// 特殊繁殖触发阈值
	private static final int SPECIAL_BREEDING_BONUS = 2; // 特殊繁殖额外幼苗
	private static final int DEATH_COMPENSATION_MAX = 3; // 死亡补偿最大植株数
	private static final int DEATH_COMPENSATION_ENERGY = 2; // 每株补偿能量
	private static final int BASE_ENERGY_REGEN = 2; // 每回合基础能量回复
	// 事件概率
	private static final double GOOD_WEATHER_CHANCE = 0.1;
	private static final double PROSPEROUS_CHANCE = 0.083;
	private static final double LOCUST_CHANCE = 0.1;
	private static final double COLD_WAVE_CHANCE = 0.9;
	private static final double RACE_COMPETITION_CHANCE = 0.1;
	// 窗口尺寸
	private static final int WINDOW_MIN_WIDTH = 800;
	private static final int WINDOW_MIN_HEIGHT = 600;

	// ==================== 天赋枚举 ====================
	public enum Talent {
		FAST_GROWTH("快速成长", "生命的力量在于生长的速度。你的后代将比其他植物更快地成熟，在危机来临前站稳脚跟。"),
		SUPER_FRUIT("超级果实", "你的果实蕴含着更强大的生命力，每一颗都能孕育出更多的新生命。"),
		COLD_RESISTANT("耐寒", "漫长的进化让你拥有了抵御严寒的能力，即使在最寒冷的冬天，你也能顽强存活。"),
		WILD_GRASS("野草", "野火烧不尽，春风吹又生。你拥有惊人的生命力和复活能力，任何灾难都无法彻底消灭你。"),
		SUPER_EFFICIENCY("超级效率", "你是光合作用的大师，能够更高效地将阳光转化为生命的能量。"), LONGEVITY("长寿", "你的生命比其他植物更加悠长，有更多的时间繁衍后代，建立你的王国。"),
		SMALL_FLOWER("小花", "你开出的花朵小巧而精致，消耗更少的能量就能完成繁殖，但在激烈的竞争中，它们有时难以成功结果。");

		public final String name;
		public final String description;

		Talent(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}

	// ==================== 植株状态枚举 ====================
	public enum PlantState {
		NORMAL("正常"), FLOWERING("开花中"), FRUITING("结果中");

		public final String display;

		PlantState(String display) {
			this.display = display;
		}
	}

	// ==================== 事件类型枚举 ====================
	public enum EventType {
		GOOD_WEATHER("风调雨顺", true), PROSPEROUS("盛季", true), LOCUST("蝗灾", false), COLD_WAVE("寒潮", false),
		RACE_COMPETITION("种族竞争", false);

		public final String name;
		public final boolean isBeneficial;

		EventType(String name, boolean isBeneficial) {
			this.name = name;
			this.isBeneficial = isBeneficial;
		}
	}

	// ==================== 活动事件类 ====================
	public static class ActiveEvent implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		public EventType type;
		public int remainingTurns;
		public int layer; // 用于种族竞争叠加

		public ActiveEvent(EventType type, int duration) {
			this.type = type;
			this.remainingTurns = duration;
			this.layer = 1;
		}
	}

	// ==================== 植株类 ====================
	public static class Plant implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		private static int nextId = 1;
		public final int id;
		public boolean isAdult;
		public int age; // 成年：剩余寿命；幼年：剩余成长回合
		public double energy;
		public PlantState state;
		public int flowers; // 当前开花数量
		public int fruits; // 当前结果数量
		public boolean hasReviveChance; // 野草天赋复活机会
		public int reviveCounter; // 野草天赋复活计数器

		public Plant(boolean isAdult) {
			this.id = nextId++;
			this.isAdult = isAdult;
			this.age = isAdult ? ADULT_LIFESPAN : JUVENILE_GROWTH_TIME;
			this.energy = isAdult ? INITIAL_ENERGY : 0;
			this.state = PlantState.NORMAL;
			this.flowers = 0;
			this.fruits = 0;
			this.hasReviveChance = false;
			this.reviveCounter = 0;
		}

		public String getStatusText() {
			if (isAdult) {
				return String.format("植株#%d [成年] 寿命:%d 能量:%.2f %s", id, age, energy,
						state.display + (flowers > 0 ? " 花:" + flowers : "") + (fruits > 0 ? " 果:" + fruits : ""));
			} else {
				return String.format("植株#%d [幼年] 成长:%d回合", id, age);
			}
		}
	}

	// ==================== 游戏模型类 (Model) ====================
	public static class GameModel implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		private Talent selectedTalent;
		private int currentTurn;
		private List<Plant> plants;
		private List<ActiveEvent> activeEvents;
		private List<String> eventLog;
		private boolean gameOver;
		private boolean victory;
		private int lastLocustTurn;
		private int lastColdWaveTurn;
		private int raceCompetitionLayers;
		private boolean hasColdWaveOccurred; // ← 标记是否触发过寒潮

		public GameModel() {
			this.plants = new ArrayList<>();
			this.activeEvents = new ArrayList<>();
			this.eventLog = new ArrayList<>();
			this.currentTurn = 0;
			this.gameOver = false;
			this.victory = false;
			this.lastLocustTurn = -10;
			this.lastColdWaveTurn = 0;
			this.raceCompetitionLayers = 0;
		}

		public void initGame(Talent talent) {
			this.selectedTalent = talent;
			this.plants.clear();
			this.activeEvents.clear();
			this.eventLog.clear();
			this.currentTurn = 0;
			this.gameOver = false;
			this.victory = false;
			this.lastLocustTurn = -10;
			this.lastColdWaveTurn = 0;
			this.raceCompetitionLayers = 0;
			this.hasColdWaveOccurred = false;
			Plant.nextId = 1;
			// 创建初始植株
			Plant initial = new Plant(true);

			// 长寿天赋，初始寿命改为15
			if (talent == Talent.LONGEVITY) {
				initial.age = 15;
			}

			plants.add(initial);
			addLog("在一片荒芜的土地上，一粒种子悄然苏醒...");
			addLog("你选择了天赋：" + talent.name);
		}

		// 获取当前能量上限
		public int getEnergyCap() {
			int cap = BASE_ENERGY_CAP;
			if (hasActiveEvent(EventType.PROSPEROUS)) {
				cap = (int) (cap * 1.5);
			}
			return cap;
		}

		// 获取能量回复效率
		public double getEnergyRegenMultiplier() {
			double mult = 1.0;
			// 风调雨顺和盛季持续的回合内才有能量恢复效果提升
			if (hasActiveEvent(EventType.GOOD_WEATHER)) {
				mult *= 2;
			}
			if (hasActiveEvent(EventType.PROSPEROUS)) {
				mult *= 3;
			}
			// 种族竞争减益（每层能量获取-25%）
			if (selectedTalent != Talent.WILD_GRASS) {
				for (int i = 0; i < raceCompetitionLayers; i++) {
					mult *= 0.75;
				}
			}

			// 种群超过100，能量效率减半
			mult *= getPopulationPenaltyMultiplier();

			// 无有益事件以及恶性事件时能量回复倍率为1
			return mult;
		}

		// 获取幼年成长时间
		public int getJuvenileGrowthTime() {
			int time = JUVENILE_GROWTH_TIME;
			if (selectedTalent == Talent.FAST_GROWTH) {
				time = 2;
			}
			if (hasActiveEvent(EventType.GOOD_WEATHER)) {
				time = Math.max(1, time - 1);
			}
			return time;
		}

		// 获取成年寿命
		public int getAdultLifespan() {
			return selectedTalent == Talent.LONGEVITY ? 15 : ADULT_LIFESPAN;
		}

		// 获取每个果实出苗数
		public int getSeedlingsPerFruit() {
			int count = SEEDLINGS_PER_FRUIT;
			if (selectedTalent == Talent.SUPER_FRUIT) {
				count = 6;
			}
			if (hasActiveEvent(EventType.PROSPEROUS)) {
				count += 1;
			}
			return count;
		}

		// 检查是否有活动事件
		public boolean hasActiveEvent(EventType type) {
			return activeEvents.stream().anyMatch(e -> e.type == type);
		}

		// 添加日志
		public void addLog(String message) {
			eventLog.add(message);
			if (eventLog.size() > 10000) {
				eventLog.remove(0);
			}
		}

		// 种群规模惩罚（超过100株能量效率减半）
		public double getPopulationPenaltyMultiplier() {
			if (getTotalPlants() > 100) {
				return 0.5; // 能量效率减半
			}
			return 1.0;
		}

		// 获取总植株数
		public int getTotalPlants() {
			return plants.size();
		}

		// 获取成年植株数
		public int getAdultCount() {
			return (int) plants.stream().filter(p -> p.isAdult).count();
		}

		// 获取幼年植株数
		public int getJuvenileCount() {
			return (int) plants.stream().filter(p -> !p.isAdult).count();
		}

		// 获取总能量
		public double getTotalEnergy() {
			return plants.stream().filter(p -> p.isAdult).mapToDouble(p -> p.energy).sum();
		}
	}

	// ==================== 游戏控制器类 (Controller) ====================
	public static class GameController {
		private final GameModel model;
		private final Random random;

		public GameController(GameModel model) {
			this.model = model;
			this.random = new Random();
		}

		// 执行完整回合流程
		public void executeTurn() {
			model.currentTurn++;
			model.addLog("=== 第" + model.currentTurn + "回合开始 ===");
			model.addLog("新的一天来临了，阳光洒向大地...");
			// 1. 事件触发阶段
			eventTriggerPhase();
			// 2. 能量结算阶段
			energySettlementPhase();
			// 3. 植株成长阶段
			plantGrowthPhase();
			// 4. 植株死亡阶段
			List<Plant> deadPlants = plantDeathPhase();
			// 5. 死亡补偿阶段
			deathCompensationPhase(deadPlants);
			// 6. 繁殖结算阶段（处理上一回合的花和果）
			breedingSettlementPhase();
			// 检查胜负
			checkWinLose();
		}

		// 1. 事件触发阶段
		private void eventTriggerPhase() {
			model.addLog("【事件触发阶段】");
			// 更新已有事件持续时间
			Iterator<ActiveEvent> it = model.activeEvents.iterator();
			while (it.hasNext()) {
				ActiveEvent event = it.next();
				event.remainingTurns--;
				if (event.remainingTurns <= 0 && event.type != EventType.RACE_COMPETITION) {
					it.remove();
					model.addLog(event.type.name + " 效果结束");
				}
			}
			// 第1回合必定风调雨顺
			if (model.currentTurn == 1) {
				model.activeEvents.add(new ActiveEvent(EventType.GOOD_WEATHER, 1));
				model.addLog("🌤️ 风调雨顺 - 天气晴朗，雨水充沛！能量回复×2，成长加速");
				return;
			}
			// 随机触发新事件
			// 风调雨顺
			if (model.currentTurn % 15 != 0 // 每15回合不触发有益事件
					&& random.nextDouble() < GOOD_WEATHER_CHANCE && !model.hasActiveEvent(EventType.GOOD_WEATHER)) {
				model.activeEvents.add(new ActiveEvent(EventType.GOOD_WEATHER, 1));
				model.addLog("🌤️ 风调雨顺 - 能量回复×2，成长加速");
			}
			// 盛季
			if (model.currentTurn % 15 != 0 // 每15回合不触发有益事件
					&& random.nextDouble() < PROSPEROUS_CHANCE && !model.hasActiveEvent(EventType.PROSPEROUS)) {
				model.activeEvents.add(new ActiveEvent(EventType.PROSPEROUS, 2));
				model.addLog("🌺 盛季来临 - 能量回复×3，上限×1.5，果实出苗+1");
			}
			// 蝗灾 (10回合后，且3回合内没触发过)
			if (model.currentTurn >= 10 && model.currentTurn - model.lastLocustTurn > 3) {
				if (random.nextDouble() < LOCUST_CHANCE && !model.hasActiveEvent(EventType.LOCUST)) {
					model.activeEvents.add(new ActiveEvent(EventType.LOCUST, 1));
					model.lastLocustTurn = model.currentTurn;
					model.addLog("🦗 蝗灾来袭！持续1回合！");
					model.addLog("   无法获取能量，每回合强制-1能量，能量<0立即死亡");
					model.addLog("   所有花、果实、幼年植株被吞噬！");
					model.addLog("   后续3回合内不会发生蝗灾、寒潮和种族竞争！");
				}
			}
			// 寒潮 (每15回合大概率，蝗灾后3回合内不触发，不能与有益事件同时发生)
			if (model.currentTurn - model.lastColdWaveTurn == 15 && model.currentTurn - model.lastLocustTurn > 3
					&& !model.hasActiveEvent(EventType.GOOD_WEATHER) && !model.hasActiveEvent(EventType.PROSPEROUS)) {
				if (random.nextDouble() < COLD_WAVE_CHANCE) {
					model.lastColdWaveTurn = model.currentTurn;
					model.hasColdWaveOccurred = true;

					// 清除所有种族竞争
					model.activeEvents.removeIf(e -> e.type == EventType.RACE_COMPETITION);
					model.raceCompetitionLayers = 0;

					// 寒潮来临时，清除所有有益事件
					model.activeEvents.removeIf(e -> e.type == EventType.GOOD_WEATHER);
					model.activeEvents.removeIf(e -> e.type == EventType.PROSPEROUS);

					model.addLog("❄️ 寒潮降临！");
					model.addLog("   所有有益事件被驱散，风调雨顺和盛季效果消失");
					model.addLog("   所有幼年植株死亡，成年植株能量-6");
				}

				// 第35回合强制寒潮
				if (model.currentTurn >= 35 && !model.hasColdWaveOccurred
						&& !model.hasActiveEvent(EventType.GOOD_WEATHER)
						&& !model.hasActiveEvent(EventType.PROSPEROUS)) {

					model.lastColdWaveTurn = model.currentTurn;
					model.hasColdWaveOccurred = true;

					model.activeEvents.removeIf(e -> e.type == EventType.RACE_COMPETITION);
					model.raceCompetitionLayers = 0;

					model.activeEvents.removeIf(e -> e.type == EventType.GOOD_WEATHER);
					model.activeEvents.removeIf(e -> e.type == EventType.PROSPEROUS);

					model.addLog("❄️ 寒潮降临！（强制触发）");
					model.addLog("   所有有益事件被驱散，风调雨顺和盛季效果消失");
					model.addLog("   所有幼年植株死亡，成年植株能量-6");
				}
			}
			// 种族竞争 (3回合后，蝗灾后3回合内不触发)
			if (model.currentTurn >= 3 && model.selectedTalent != Talent.WILD_GRASS
					&& model.currentTurn - model.lastLocustTurn > 3) {
				if (random.nextDouble() < RACE_COMPETITION_CHANCE) {
					Optional<ActiveEvent> existing = model.activeEvents.stream()
							.filter(e -> e.type == EventType.RACE_COMPETITION).findFirst();
					if (existing.isPresent()) {
						existing.get().layer++;
					} else {
						model.activeEvents.add(new ActiveEvent(EventType.RACE_COMPETITION, 999));
					}
					model.raceCompetitionLayers++;
					model.addLog("🌱 种族竞争加剧 (第" + model.raceCompetitionLayers + "层)");
					model.addLog("   能量获取-25%，幼年死亡率上升");
				}
			}
		}

		// 2. 能量结算阶段
		private void energySettlementPhase() {
			model.addLog("【能量结算阶段】");
			boolean hasLocust = model.hasActiveEvent(EventType.LOCUST);
			double regenMult = model.getEnergyRegenMultiplier();
			int baseRegen = model.selectedTalent == Talent.SUPER_EFFICIENCY ? 3 : BASE_ENERGY_REGEN;
			int energyCap = model.getEnergyCap();
			double totalGained = 0;
			for (Plant plant : model.plants) {
				if (!plant.isAdult)
					continue;
				if (hasLocust) {
					// 蝗灾：强制消耗1能量
					plant.energy = Math.max(0, plant.energy - 1);
				} else {
					// 正常回复
					int regen = (int) (baseRegen * regenMult);
					double oldEnergy = plant.energy;
					plant.energy = Math.min(energyCap, plant.energy + regen);
					totalGained += (plant.energy - oldEnergy);
				}
				// 野草天赋：复活计数
				if (model.selectedTalent == Talent.WILD_GRASS) {
					plant.reviveCounter++;
					if (plant.reviveCounter >= 4 && !plant.hasReviveChance) {
						plant.hasReviveChance = true;
						plant.reviveCounter = 0;
					}
				}
			}
			if (!hasLocust) {
				model.addLog(String.format("本回合共获得 %.0f 点能量，总能量: %.2f/%d", totalGained, model.getTotalEnergy(),
						energyCap * model.getAdultCount()));
			}
		}

		// 3. 植株成长阶段
		private void plantGrowthPhase() {
			model.addLog("【植株成长阶段】");
			int growthTime = model.getJuvenileGrowthTime();
			int matured = 0;
			for (Plant plant : model.plants) {
				if (plant.isAdult) {
					// 成年植株年龄增加
					plant.age--;
				} else {
					// 幼年植株成长
					plant.age--;
					if (plant.age <= 0) {
						plant.isAdult = true;
						plant.age = getAdjustedLifespan();
						plant.energy = ADULT_INITIAL_ENERGY;
						plant.state = PlantState.NORMAL;
						matured++;
						model.addLog("植株#" + plant.id + " 成长为成年植株，获得5点能量");
					}
				}
			}
			if (matured > 0) {
				model.addLog(matured + " 株幼年植株成熟");
			}
			model.addLog(model.getAdultCount() + " 株成年植株，" + model.getJuvenileCount() + " 株幼年植株");
		}

		private int getAdjustedLifespan() {
			return model.selectedTalent == Talent.LONGEVITY ? 15 : ADULT_LIFESPAN;
		}

		// 4. 植株死亡阶段
		private List<Plant> plantDeathPhase() {
			model.addLog("【植株死亡阶段】");
			List<Plant> deadPlants = new ArrayList<>();
			List<Plant> revivedPlants = new ArrayList<>();
			boolean hasLocust = model.hasActiveEvent(EventType.LOCUST);
			boolean hasColdWave = model.currentTurn == model.lastColdWaveTurn;
			// 蝗灾：清除所有花、果实、幼年植株
			if (hasLocust) {
				for (Plant plant : model.plants) {
					plant.flowers = 0;
					plant.fruits = 0;
					plant.state = PlantState.NORMAL;
					if (!plant.isAdult) {
						deadPlants.add(plant);
					}
				}
				model.addLog("蝗灾吞噬了所有花、果实和幼年植株");
			}
			// 寒潮：杀死所有幼年，成年能量-6
			if (hasColdWave) {
				int coldDamage = model.selectedTalent == Talent.COLD_RESISTANT ? 3 : 6;
				int deathThreshold = model.selectedTalent == Talent.COLD_RESISTANT ? 3 : 2;
				for (Plant plant : model.plants) {
					if (!plant.isAdult) {
						deadPlants.add(plant);
					} else {
						plant.energy -= coldDamage;
						if (plant.energy < deathThreshold) {
							// 检查野草复活
							if (model.selectedTalent == Talent.WILD_GRASS && plant.hasReviveChance && plant.age > 0) {
								plant.hasReviveChance = false;
								plant.energy = 2;
								revivedPlants.add(plant);
								model.addLog("✨ 植株#" + plant.id + " 凭借野草生命力复活了！");
							} else {
								deadPlants.add(plant);
							}
						}
					}
				}
				model.addLog("寒潮造成大量植株死亡");
			}
			// 常规死亡检查
			for (Plant plant : model.plants) {
				if (deadPlants.contains(plant) || revivedPlants.contains(plant))
					continue;
				boolean shouldDie = false;
				// 寿命耗尽
				if (plant.isAdult && plant.age <= 0) {
					shouldDie = true;
					model.addLog("植株#" + plant.id + " 寿命耗尽，自然枯萎");
				}
				// 蝗灾能量不足死亡
				if (hasLocust && plant.isAdult && plant.energy < 0) {
					if (model.selectedTalent == Talent.WILD_GRASS && plant.hasReviveChance && plant.age > 0) {
						plant.hasReviveChance = false;
						plant.energy = 2;
						revivedPlants.add(plant);
						model.addLog("✨ 植株#" + plant.id + " 凭借野草生命力复活了！");
					} else {
						shouldDie = true;
						model.addLog("植株#" + plant.id + " 因蝗灾能量耗尽而死亡");
					}
				}
				// 幼年植株种群惩罚死亡（野草天赋免疫种族竞争）
				if (!plant.isAdult && model.getTotalPlants() > POPULATION_PENALTY_THRESHOLD) {
					double deathChance = 0.2; // 基础死亡率
					// 非野草天赋才受种族竞争影响
					if (model.selectedTalent != Talent.WILD_GRASS && model.raceCompetitionLayers > 0) {
						deathChance = 0.3;
					}
					if (random.nextDouble() < deathChance) {
						shouldDie = true;
						model.addLog("植株#" + plant.id + " 因种群竞争而死亡");
					}
				}

				if (shouldDie) {
					deadPlants.add(plant);
				}
			}
			// 移除死亡植株
			model.plants.removeAll(deadPlants);
			model.addLog("本回合共 " + deadPlants.size() + " 株植株死亡");
			return deadPlants;
		}

		// 5. 死亡补偿阶段
		private void deathCompensationPhase(List<Plant> deadPlants) {
			if (deadPlants.stream().noneMatch(p -> p.isAdult))
				return;
			model.addLog("【死亡补偿阶段】");
			List<Plant> livingAdults = model.plants.stream().filter(p -> p.isAdult && p.energy < model.getEnergyCap())
					.collect(Collectors.toList());
			Collections.shuffle(livingAdults);
			int compensated = 0;
			for (int i = 0; i < Math.min(DEATH_COMPENSATION_MAX, livingAdults.size()); i++) {
				Plant p = livingAdults.get(i);
				p.energy = Math.min(model.getEnergyCap(), p.energy + DEATH_COMPENSATION_ENERGY);
				compensated++;
			}
			if (compensated > 0) {
				model.addLog(compensated + " 株存活植株获得能量补偿，每株+2能量");
			}
		}

		// 6. 繁殖结算阶段（3回合周期：开花→结果→出苗，每阶段1回合）
		private void breedingSettlementPhase() {
			model.addLog("【繁殖结算阶段】");

			// 寒潮或蝗灾时，不出苗也不结果
			boolean hasLocust = model.hasActiveEvent(EventType.LOCUST);
			boolean hasColdWave = model.currentTurn == model.lastColdWaveTurn;
			if (hasLocust || hasColdWave) {
				model.addLog("天灾期间，繁殖中断，没有新幼苗诞生");
				return;
			}
			int totalFruits = 0;
			int totalSeedlings = 0;
			int totalFlowersThisTurn = 0;
			List<Plant> newSeedlingsList = new ArrayList<>(); // 临时列表收集新幼苗，避免并发修改
			// 先统计本回合开花总数（用于特殊繁殖）
			for (Plant plant : model.plants) {
				if (plant.isAdult && plant.state == PlantState.FLOWERING) {
					totalFlowersThisTurn += plant.flowers;
				}
			}
			// 第一阶段：处理出苗（上回合的结果 -> 出苗）
			// 先处理出苗，避免刚结的果立刻出苗
			int seedlingsPerFruit = model.getSeedlingsPerFruit();
			for (Plant plant : model.plants) {
				if (!plant.isAdult)
					continue;
				if (plant.state == PlantState.FRUITING && plant.fruits > 0) {
					int successfulFruits = plant.fruits;
					// 种群惩罚：果实被取食
					if (model.getTotalPlants() + newSeedlingsList.size() > POPULATION_PENALTY_THRESHOLD) {
						int eaten = 0;
						for (int i = 0; i < plant.fruits; i++) {
							if (random.nextDouble() < 0.25) {
								eaten++;
							}
						}
						successfulFruits -= eaten;
						if (eaten > 0) {
							model.addLog("植株#" + plant.id + " 的 " + eaten + " 个果实被野生动物取食");
						}
					}
					if (successfulFruits > 0) {
						int newSeedlings = successfulFruits * seedlingsPerFruit;
						for (int i = 0; i < newSeedlings; i++) {
							Plant seedling = new Plant(false);
							seedling.age = model.getJuvenileGrowthTime();
							newSeedlingsList.add(seedling); // 添加到临时列表
						}
						totalSeedlings += newSeedlings;
						model.addLog("植株#" + plant.id + " 的果实孕育出 " + newSeedlings + " 株幼苗");
					}
					plant.fruits = 0;
					plant.state = PlantState.NORMAL;
				}
			}
			// 第二阶段：处理结果（上回合的开花 -> 结果）
			// 开花后下一回合才结果，确保3回合周期
			for (Plant plant : model.plants) {
				if (!plant.isAdult)
					continue;
				if (plant.state == PlantState.FLOWERING && plant.flowers > 0) {
					double fruitCost = model.selectedTalent == Talent.SMALL_FLOWER ? 1.2 : FRUIT_COST;
					double minEnergy = model.selectedTalent == Talent.SMALL_FLOWER ? 1.0 : 2.0;
					// 记录原始开花数，用于特殊繁殖判断
					int originalFlowers = plant.flowers;
					if (plant.energy < minEnergy) {
						plant.flowers = 0;
						plant.state = PlantState.NORMAL;
						model.addLog("植株#" + plant.id + " 能量不足，花朵枯萎");
						continue;
					}
					// 小花天赋：种族竞争时有3/5概率不结果
					if (model.selectedTalent == Talent.SMALL_FLOWER && model.raceCompetitionLayers > 0) {
						if (random.nextDouble() < 0.6) {
							model.addLog("植株#" + plant.id + " 的花朵因竞争未能授粉");
							plant.flowers = 0;
							plant.state = PlantState.NORMAL;
							continue;
						}
					}
					int maxFruits = (int) (plant.energy / fruitCost);
					int actualFruits = Math.min(plant.flowers, maxFruits);
					plant.energy -= (int) (actualFruits * fruitCost);
					plant.fruits = actualFruits;
					plant.flowers = 0;
					plant.state = PlantState.FRUITING;
					totalFruits += actualFruits;
					model.addLog("植株#" + plant.id + " 的花结出 " + actualFruits + " 个果实");
					// 特殊繁殖机制：单株开花超过4朵时，结果时立即奖励2株幼苗
					if (originalFlowers > SPECIAL_BREEDING_THRESHOLD) {
						for (int i = 0; i < SPECIAL_BREEDING_BONUS; i++) {
							Plant seedling = new Plant(false);
							seedling.age = model.getJuvenileGrowthTime();
							newSeedlingsList.add(seedling);
						}
						totalSeedlings += SPECIAL_BREEDING_BONUS;
						model.addLog("🌟 植株#" + plant.id + " 开花超过4朵，额外产生 " + SPECIAL_BREEDING_BONUS + " 株幼苗");
					}
				}
			}
			// 一次性添加所有新幼苗到主列表
			model.plants.addAll(newSeedlingsList);
			if (totalFruits > 0) {
				model.addLog("本回合共结出 " + totalFruits + " 个果实");
			}
			if (totalSeedlings > 0) {
				model.addLog("本回合共诞生 " + totalSeedlings + " 株新幼苗");
			}
			model.addLog("繁殖周期：开花(回合1) → 结果(回合2) → 出苗(回合3)");
		}

		// 玩家操作：为选中植株开花
		public boolean bloomPlants(List<Plant> selectedPlants, int flowerCount) {
			if (selectedPlants.isEmpty())
				return false;
			double flowerCost = model.selectedTalent == Talent.SMALL_FLOWER ? 0.6 : FLOWER_COST;
			boolean success = true;
			for (Plant plant : selectedPlants) {
				if (!plant.isAdult || plant.state != PlantState.NORMAL || plant.energy < 2) {
					continue;
				}
				double totalCost = flowerCount * flowerCost;
				if (plant.energy < totalCost) {
					int maxFlowers = (int) (plant.energy / flowerCost);
					if (maxFlowers <= 0)
						continue;
					plant.flowers = maxFlowers;
					plant.energy -= maxFlowers * flowerCost;
				} else {
					plant.flowers = flowerCount;
					plant.energy -= totalCost;
				}
				plant.state = PlantState.FLOWERING;
				model.addLog("植株#" + plant.id + " 开出 " + plant.flowers + " 朵花");
			}
			return success;
		}

		// 检查胜负
		private void checkWinLose() {
			if (model.plants.isEmpty()) {
				model.gameOver = true;
				model.victory = false;
				model.addLog("💀 所有植株已死亡，游戏结束");
			} else if (model.currentTurn >= WIN_TURN) {
				model.gameOver = true;
				model.victory = true;
				model.addLog("🎉 植物王国崛起！成功存活 " + WIN_TURN + " 回合！");
			}
		}

		// 保存游戏
		public boolean saveGame(String path) {
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
				oos.writeObject(model);
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		// 加载游戏
		public static GameModel loadGame(String path) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
				return (GameModel) ois.readObject();
			} catch (IOException | ClassNotFoundException e) {
				return null;
			}
		}
	}

	// ==================== 游戏视图类 (View) ====================
	public static class GameView {
		private final JFrame frame;
		private final GameModel model;
		private final GameController controller;
		// UI组件
		private JLabel turnLabel;
		private JLabel populationLabel;
		private JLabel energyLabel;
		private JList<Plant> plantList;
		private DefaultListModel<Plant> plantListModel;
		private JTextArea logArea;
		private JTextField flowerInput;
		private JLabel selectedInfoLabel;

		public GameView(GameModel model, GameController controller) {
			this.model = model;
			this.controller = controller;
			this.frame = createMainFrame();
		}

		private JFrame createMainFrame() {
			JFrame f = new JFrame("植物王国崛起");
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			f.setMinimumSize(new Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT));
			f.setLayout(new BorderLayout(10, 10));
			f.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon2.png")));
			// 设置系统观感
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}
			// 顶部状态栏
			JPanel statusPanel = createStatusPanel();
			f.add(statusPanel, BorderLayout.NORTH);
			// 中央分割面板
			JSplitPane splitPane = createSplitPane();
			f.add(splitPane, BorderLayout.CENTER);
			// 底部操作面板
			JPanel actionPanel = createActionPanel();
			f.add(actionPanel, BorderLayout.SOUTH);
			f.pack();
			f.setLocationRelativeTo(null);
			return f;
		}

		private JPanel createStatusPanel() {
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
			panel.setBorder(BorderFactory.createTitledBorder("游戏状态"));
			turnLabel = new JLabel("回合: 0");
			populationLabel = new JLabel("植株: 0 (成:0 幼:0)");
			energyLabel = new JLabel("能量: 0");
			panel.add(turnLabel);
			panel.add(populationLabel);
			panel.add(energyLabel);
			// 存档按钮
			JButton saveBtn = new JButton("保存游戏");
			saveBtn.addActionListener(e -> saveGame());
			panel.add(saveBtn);
			// 退出按钮
			JButton exitBtn = new JButton("退出游戏");
			exitBtn.addActionListener(e -> frame.dispose());
			panel.add(exitBtn);
			return panel;
		}

		private JSplitPane createSplitPane() {
			// 左侧植株列表面板
			JPanel leftPanel = new JPanel(new BorderLayout());
			leftPanel.setBorder(BorderFactory.createTitledBorder("植株列表"));
			plantListModel = new DefaultListModel<>();
			plantList = new JList<>(plantListModel);
			plantList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			plantList.setCellRenderer(new PlantCellRenderer());
			plantList.addListSelectionListener(e -> updateSelectedInfo());
			JScrollPane plantScroll = new JScrollPane(plantList);
			leftPanel.add(plantScroll, BorderLayout.CENTER);
			// 右侧日志面板
			JPanel rightPanel = new JPanel(new BorderLayout());
			rightPanel.setBorder(BorderFactory.createTitledBorder("事件日志"));
			logArea = new JTextArea();
			logArea.setEditable(false);
			logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			logArea.setLineWrap(true);
			logArea.setWrapStyleWord(true);
			JScrollPane logScroll = new JScrollPane(logArea);
			rightPanel.add(logScroll, BorderLayout.CENTER);
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
			split.setDividerLocation(350);
			return split;
		}

		private JPanel createActionPanel() {
			JPanel panel = new JPanel(new BorderLayout(10, 10));
			panel.setBorder(BorderFactory.createTitledBorder("操作面板"));
			// 选中信息
			selectedInfoLabel = new JLabel("请选择植株进行操作");
			panel.add(selectedInfoLabel, BorderLayout.NORTH);
			// 操作按钮区
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
			buttonPanel.add(new JLabel("开花数量:"));
			flowerInput = new JTextField("1", 5);
			buttonPanel.add(flowerInput);
			JButton bloomBtn = new JButton("确认开花");
			bloomBtn.addActionListener(e -> executeBloom());
			buttonPanel.add(bloomBtn);
			JButton endTurnBtn = new JButton("结束回合");
			endTurnBtn.addActionListener(e -> executeEndTurn());
			buttonPanel.add(endTurnBtn);
			panel.add(buttonPanel, BorderLayout.CENTER);
			return panel;
		}

		// 植株单元格渲染器
		private class PlantCellRenderer extends DefaultListCellRenderer {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Plant plant = (Plant) value;
				setText(plant.getStatusText());
				if (!isSelected) {
					setBackground(plant.isAdult ? new Color(200, 255, 200) : new Color(230, 255, 230));
				}
				return this;
			}
		}

		private void updateSelectedInfo() {
			List<Plant> selected = plantList.getSelectedValuesList();
			if (selected.isEmpty()) {
				selectedInfoLabel.setText("请选择成年植株进行开花操作");
			} else {
				long adultCount = selected.stream().filter(p -> p.isAdult && p.state == PlantState.NORMAL).count();
				selectedInfoLabel.setText(String.format("已选中 %d 株，其中 %d 株可开花", selected.size(), adultCount));
			}
		}

		private void executeBloom() {
			List<Plant> selected = plantList.getSelectedValuesList();
			if (selected.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "请先选择植株！");
				return;
			}
			try {
				int count = Integer.parseInt(flowerInput.getText().trim());
				if (count <= 0) {
					JOptionPane.showMessageDialog(frame, "开花数量必须大于0！");
					return;
				}
				List<Plant> validPlants = selected.stream()
						.filter(p -> p.isAdult && p.state == PlantState.NORMAL && p.energy >= 2)
						.collect(Collectors.toList());
				if (validPlants.isEmpty()) {
					JOptionPane.showMessageDialog(frame, "选中的植株中没有可以开花的！\n需要成年、正常状态、能量>=2");
					return;
				}
				controller.bloomPlants(validPlants, count);
				updateUI();
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(frame, "请输入有效的数字！");
			}
		}

		private void executeEndTurn() {
			controller.executeTurn();
			updateUI();
			if (model.gameOver) {
				showGameEndDialog();
			}
		}

		private void showGameEndDialog() {
			String message;
			String title;
			if (model.victory) {
				title = "🎉 胜利！";
				message = String.format("""
						恭喜你！植物王国崛起！

						经过 %d 个回合的艰苦奋斗，
						你的植物种群已经发展壮大到 %d 株植株！

						你成功地从一株孤草成长为统治这片土地的植物王国！
						""", model.currentTurn, model.getTotalPlants());
			} else {
				title = "💀 游戏结束";
				message = String.format("""
						经过 %d 个回合的挣扎，
						你的最后一株植株也死亡了。

						恶劣的自然环境和激烈的竞争最终战胜了你。
						但生命的力量永不消逝...
						""", model.currentTurn);
			}
			int choice = JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.YES_NO_OPTION,
					JOptionPane.INFORMATION_MESSAGE);
			if (choice == JOptionPane.YES_OPTION) {
				restartGame();
			} else {
				frame.dispose();
			}
		}

		private void restartGame() {
			frame.dispose();
			SwingUtilities.invokeLater(() -> showTalentSelection());
		}

		private void saveGame() {
			if (controller.saveGame("plantsave.dat")) {
				JOptionPane.showMessageDialog(frame, "游戏已保存！");
			} else {
				JOptionPane.showMessageDialog(frame, "保存失败！");
			}
		}

		public void updateUI() {
			// 更新状态栏
			turnLabel.setText("回合: " + model.currentTurn);
			populationLabel.setText(String.format("植株: %d (成:%d 幼:%d)", model.getTotalPlants(), model.getAdultCount(),
					model.getJuvenileCount()));
			energyLabel.setText(String.format("能量: %.2f 上限:%d", model.getTotalEnergy(), model.getEnergyCap()));
			// 更新植株列表
			plantListModel.clear();
			for (Plant plant : model.plants) {
				plantListModel.addElement(plant);
			}
			// 更新日志
			logArea.setText(String.join("\n", model.eventLog));
			logArea.setCaretPosition(logArea.getDocument().getLength());
		}

		public void show() {
			frame.setVisible(true);
			updateUI();
			// 回车键结束回合（在frame初始化完成后注册）
			frame.getRootPane().registerKeyboardAction(e -> executeEndTurn(),
					KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
	}

	// ==================== 天赋选择界面 ====================
	public static void showTalentSelection() {
		JFrame frame = new JFrame("选择天赋");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(PlantKingdom.class.getResource("/icon2.png")));
		// 故事介绍
		JTextArea intro = new JTextArea("""
				在一片荒芜的土地上，一粒种子悄然苏醒。你拥有了自我意识，成为了这株植物的灵魂。

				周围危机四伏，饥饿的蝗虫、刺骨的寒潮、激烈的种族竞争无时无刻不在威胁着你的生存。
				你必须带领你的种群挣扎求生，繁衍壮大，从一株孤草成长为统治整片土地的植物王国。

				现在，选择你的天赋，开启你的植物帝国之路：
				""");
		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		frame.add(intro, BorderLayout.NORTH);
		// 天赋按钮面板
		JPanel talentPanel = new JPanel(new GridLayout(4, 2, 10, 10));
		talentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		for (Talent talent : Talent.values()) {
			JButton btn = new JButton(
					"<html><b>" + talent.name + "</b><br><small>" + talent.description + "</small></html>");
			btn.setHorizontalAlignment(SwingConstants.LEFT);
			btn.addActionListener(e -> {
				startGame(talent);
				frame.dispose();
			});
			talentPanel.add(btn);
		}
		frame.add(talentPanel, BorderLayout.CENTER);
		// 底部按钮面板
		JPanel bottomPanel = new JPanel(new FlowLayout());
		JButton loadBtn = new JButton("读取存档");
		loadBtn.addActionListener(e -> {
			GameModel loaded = GameController.loadGame("plantsave.dat");
			if (loaded != null) {
				frame.dispose();
				GameController controller = new GameController(loaded);
				GameView view = new GameView(loaded, controller);
				view.show();
			} else {
				JOptionPane.showMessageDialog(frame, "没有找到存档或存档已损坏！");
			}
		});
		bottomPanel.add(loadBtn);
		JButton exitBtn = new JButton("退出游戏");
		exitBtn.addActionListener(e -> frame.dispose());
		bottomPanel.add(exitBtn);
		frame.add(bottomPanel, BorderLayout.SOUTH);
		frame.setSize(700, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static void startGame(Talent talent) {
		GameModel model = new GameModel();
		GameController controller = new GameController(model);
		model.initGame(talent);
		GameView view = new GameView(model, controller);
		SwingUtilities.invokeLater(view::show);
	}

	// ==================== 主入口 ====================
	public static void main(String[] args) {
		SwingUtilities.invokeLater(PlantKingdom::showTalentSelection);
	}
}
