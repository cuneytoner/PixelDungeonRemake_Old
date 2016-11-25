package com.coner.android.util;

import android.support.annotation.NonNull;

import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.utils.GLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonHelper {

	@NonNull
	static public JSONObject tryReadJsonFromAssets(String fileName) {
		if (ModdingMode.isResourceExist(fileName)) {
			return readJsonFromAsset(fileName);
		}
		return new JSONObject();
	}

	@NonNull
	static public JSONObject readJsonFromAsset(String fileName) {
		return readJsonFromStream(ModdingMode.getInputStream(fileName));
	}

	@NonNull
	static public JSONObject readJsonFromFile(File file) {
		try {
			GLog.i("Trying to read from %s", file.getAbsoluteFile());
			return readJsonFromStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			return new JSONObject();
		}
	}


	@NonNull
	static public JSONObject readJsonFromStream(InputStream stream) {
		try {
			StringBuilder jsonDef = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

			String line = reader.readLine();

			while (line != null) {
				jsonDef.append(line);
				line = reader.readLine();
			}
			reader.close();
			return (JSONObject) new JSONTokener(jsonDef.toString()).nextValue();
		} catch (JSONException e) {
			Game.toast(e.getLocalizedMessage());
			throw new TrackedRuntimeException(e);
		} catch (IOException e) {
			Game.toast(e.getLocalizedMessage());
			throw new TrackedRuntimeException(e);
		}
	}

	static public JSONObject readFile(String fileName) {
		try {
			InputStream stream = ModdingMode.getInputStream(fileName);
			StringBuilder animationDef = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

			String line = reader.readLine();

			while (line != null) {
				animationDef.append(line);
				line = reader.readLine();
			}
			reader.close();
			return (JSONObject) new JSONTokener(animationDef.toString()).nextValue();
		} catch (JSONException e) {
			Game.toast(e.getLocalizedMessage());
			throw new TrackedRuntimeException(e);
		} catch (IOException e) {
			Game.toast(e.getLocalizedMessage());
			throw new TrackedRuntimeException(e);
		}
	}
}