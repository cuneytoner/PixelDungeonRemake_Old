package com.coner.pixeldungeon.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.coner.android.util.JsonHelper;
import com.coner.android.util.TrackedRuntimeException;
import com.coner.pixeldungeon.levels.FakeLastLevel;
import com.coner.pixeldungeon.levels.GutsLevel;
import com.coner.pixeldungeon.levels.LastLevel;
import com.coner.pixeldungeon.levels.NecroBossLevel;
import com.coner.pixeldungeon.levels.NecroLevel;
import com.coner.pixeldungeon.levels.PredesignedLevel;
import com.coner.pixeldungeon.levels.ShadowLordLevel;
import com.coner.pixeldungeon.remake.BuildConfig;
import com.coner.pixeldungeon.remake.EventCollector;
import com.coner.pixeldungeon.spiders.levels.SpiderLevel;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.PixelDungeon;
import com.watabou.pixeldungeon.levels.CavesBossLevel;
import com.watabou.pixeldungeon.levels.CavesLevel;
import com.watabou.pixeldungeon.levels.CityBossLevel;
import com.watabou.pixeldungeon.levels.CityLevel;
import com.watabou.pixeldungeon.levels.DeadEndLevel;
import com.watabou.pixeldungeon.levels.FakeLastLevel2;
import com.watabou.pixeldungeon.levels.HallsBossLevel;
import com.watabou.pixeldungeon.levels.HallsLevel;
import com.watabou.pixeldungeon.levels.LastShopLevel;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.PrisonBossLevel;
import com.watabou.pixeldungeon.levels.PrisonLevel;
import com.watabou.pixeldungeon.levels.SewerBossLevel;
import com.watabou.pixeldungeon.levels.SewerLevel;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.windows.WndStory;
import com.coner.pixeldungeon.levels.DragonPitsLevel;
import com.coner.pixeldungeon.levels.DragonPitsBossLevel;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class DungeonGenerator {
	private static final String DEAD_END_LEVEL = "DeadEndLevel";
	private static final String HALLS_LEVEL    = "HallsLevel";
	private static final String CITY_LEVEL     = "CityLevel";
	private static final String CAVES_LEVEL    = "CavesLevel";
	private static final String PRISON_LEVEL   = "PrisonLevel";
	private static final String SEWER_LEVEL    = "SewerLevel";
	private static final String SPIDER_LEVEL   = "SpiderLevel";
	private static final String GUTS_LEVEL     = "GutsLevel";
    private static final String DRAGON_PITS_LEVEL = "DragonPitsLevel";

	public static final String UNKNOWN        = "unknown";

	static private JSONObject mDungeonMap;
	static private JSONObject mLevels;
	static private JSONObject mGraph;

	@NonNull
	private static String mCurrentLevelId;
	private static int    mCurrentLevelDepth;

	static private HashMap<String, Class<? extends Level>> mLevelKindList;
	static private HashMap<String, Integer>                mStoryMap;

	static {
		initLevelsMap();
	}
	private static void registerLevelClass(Class<? extends Level> levelClass) {
		mLevelKindList.put(levelClass.getSimpleName(), levelClass);
	}

	private static void initLevelsMap() {
		if(PixelDungeon.isAlpha() && BuildConfig.DEBUG) {
			mDungeonMap = JsonHelper.readJsonFromAsset("levelsDesc/Dungeon_alpha.json");
		} else {
			mDungeonMap = JsonHelper.readJsonFromAsset("levelsDesc/Dungeon.json");
		}

		try {
			mLevels = mDungeonMap.getJSONObject("Levels");
			mGraph = mDungeonMap.getJSONObject("Graph");
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}

		mLevelKindList = new HashMap<>();

		registerLevelClass(SewerLevel.class);
		registerLevelClass(SewerBossLevel.class);
		registerLevelClass(SpiderLevel.class);
		registerLevelClass(PrisonLevel.class);
		registerLevelClass(PrisonBossLevel.class);
		registerLevelClass(CavesLevel.class);
		registerLevelClass(CavesBossLevel.class);
		registerLevelClass(CityLevel.class);
		registerLevelClass(CityBossLevel.class);
		registerLevelClass(LastShopLevel.class);
		registerLevelClass(HallsLevel.class);
		registerLevelClass(HallsBossLevel.class);
		registerLevelClass(FakeLastLevel2.class);
		registerLevelClass(DeadEndLevel.class);

		registerLevelClass(PredesignedLevel.class);
		registerLevelClass(GutsLevel.class);
		registerLevelClass(ShadowLordLevel.class);
		registerLevelClass(FakeLastLevel.class);

		registerLevelClass(NecroLevel.class);
		registerLevelClass(NecroBossLevel.class);

        registerLevelClass(DragonPitsLevel.class);
        registerLevelClass(DragonPitsBossLevel.class);

		registerLevelClass(LastLevel.class);
	}

	public static String getEntryLevelKind() {
		try {
			return mLevels.getJSONObject(getEntryLevel()).getString("kind");
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	public static String getEntryLevel() {
		try {
			return mDungeonMap.getString("Entrance");
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	public static int exitCount(String levelId) {
		try {
			return mGraph.getJSONArray(levelId).getJSONArray(0).length();
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	public static Position ascend(Position current) {
		return descendOrAscend(current, false);
	}

	private static Position descendOrAscend(Position current, boolean descend) {
		try {

			if (current.levelId.equals("unknown")) {
				current.levelId = guessLevelId(current.levelKind, current.levelDepth);
			}

			JSONArray currentLevel = mGraph.getJSONArray(current.levelId);

			JSONArray nextLevelSet = currentLevel.getJSONArray(descend ? 0 : 1);
			Position next = new Position();
			int index = 0;

			next.cellId = -1;

			if (descend && !current.levelId.equals(getEntryLevel())) {
				if (Dungeon.level != null) { // not first descend
					if(Dungeon.level.isExit(current.cellId)) {
						index = Dungeon.level.exitIndex(current.cellId);
						next.cellId = -(index + 1);
					}
				}
			}

			if (!descend) {
				if (currentLevel.length() > 2) {
					int exitIndex = currentLevel.getJSONArray(2).getInt(0);
					next.cellId = -exitIndex;
				}
			}

			if(index >= nextLevelSet.length()) {
				index = 0;
				EventCollector.logEvent("DungeonGenerator","wrong next level index");
			}

			mCurrentLevelId = nextLevelSet.getString(index);

			JSONObject nextLevelDesc = mLevels.getJSONObject(mCurrentLevelId);

			next.levelId      = mCurrentLevelId;
			mCurrentLevelDepth = next.levelDepth = nextLevelDesc.getInt("depth");
			next.levelKind    = nextLevelDesc.getString("kind");

			JSONArray levelSize = nextLevelDesc.getJSONArray("size");
			next.xs = levelSize.getInt(0);
			next.ys = levelSize.getInt(1);

			return next;
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	@Nullable
	private static String getLevelProperty(String id, String property) {

		try {
			JSONObject levelDesc = mLevels.getJSONObject(id);

			if(levelDesc.has(property)) {
				return levelDesc.getString(property);
			}
		} catch (JSONException e) {
			EventCollector.logException(e);
		}
		return null;
	}

	@Nullable
	public static String tiles(String id) {
		return getLevelProperty(id, "tiles");
	}

	@Nullable
	public static String water(String id) {
		return getLevelProperty(id, "water");
	}

	@Nullable
	public static String music(String id) {
		return getLevelProperty(id, "music");
	}

	public static Level.Feeling getCurrentLevelFeeling(String id) {
		try {
			String feeling = getLevelProperty(id, "feeling");
			if(feeling==null) {
				return Level.Feeling.UNDEFINED;
			}
			return Level.Feeling.valueOf(getLevelProperty(id, "feeling"));
		} catch (IllegalArgumentException e){
			return Level.Feeling.UNDEFINED;
		}
	}


	public static Position descend(Position current) {
		return descendOrAscend(current, true);
	}

	public static Level createLevel(Position pos) {
		Class<? extends Level> levelClass = mLevelKindList.get(pos.levelKind);
		if (levelClass == null) {
			GLog.w("Unknown level type: %s", pos.levelKind);
			pos.levelKind = DEAD_END_LEVEL;

			return createLevel(pos);
		}
		try {
			Level ret;
			if (levelClass == PredesignedLevel.class) {
				String levelFile = mLevels.getJSONObject(pos.levelId).getString("file");
				ret = new PredesignedLevel(levelFile);
			} else {
				ret = levelClass.newInstance();
			}
			ret.levelId = pos.levelId;
			return ret;
		} catch (InstantiationException | IllegalAccessException | JSONException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	public static void showStory(Level level) {
		if (mStoryMap == null) {
			mStoryMap = new HashMap<>();
			mStoryMap.put(SEWER_LEVEL, WndStory.ID_SEWERS);
			mStoryMap.put(SPIDER_LEVEL, WndStory.ID_SPIDERS);
			mStoryMap.put(PRISON_LEVEL, WndStory.ID_PRISON);
			mStoryMap.put(CAVES_LEVEL, WndStory.ID_CAVES);
			mStoryMap.put(CITY_LEVEL, WndStory.ID_METROPOLIS);
			mStoryMap.put(HALLS_LEVEL, WndStory.ID_HALLS);
			mStoryMap.put(GUTS_LEVEL, WndStory.ID_GUTS);
            mStoryMap.put(DRAGON_PITS_LEVEL, WndStory.ID_DRAGON_PITS);
		}

		Integer id = mStoryMap.get(level.levelKind());
		if (id == null) {
			return;
		}

		WndStory.showChapter(id);
	}

	static String guessLevelId(String levelKind, int levelDepth) {
		try {
			JSONArray ids = mLevels.names();
			for (int i = 0; i < ids.length(); i++) {
				String id = ids.getString(i);
				JSONObject levelDesc = mLevels.getJSONObject(id);

				if (levelDesc.getString("kind").equals(levelKind)) {
					if (levelDesc.getInt("depth") == levelDepth) {
						return id;
					}
				}
			}
		} catch (JSONException e) {
			throw new TrackedRuntimeException(e);
		}
		return "1";
	}

	@NonNull
	public static String getCurrentLevelId() {
		return mCurrentLevelId;
	}

	public static int getCurrentLevelDepth() {
		return mCurrentLevelDepth;
	}

	public static void loadingLevel(Position next) {
		mCurrentLevelId    = next.levelId;
		mCurrentLevelDepth = next.levelDepth;
	}


}