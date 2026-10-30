package com.arrl.radiocraft;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each config file must read back exactly as it was written. A value that does not (a Float, which TOML reads back
 * as a Double) is "corrected" and saved on every load, and NeoForge's file watcher then reloads the saved file, in a
 * loop ("Configuration file ... is not correct. Correcting" over and over), resetting any edit to its default.
 */
class ConfigRoundTripTest {

	private static List<String> problemsAfterReload(ModConfigSpec spec) {
		CommentedConfig config = TomlFormat.newConfig();
		spec.correct(config);
		String toml = TomlFormat.instance().createWriter().writeToString(config);
		CommentedConfig read = TomlFormat.instance().createParser().parse(toml);
		List<String> problems = new ArrayList<>();
		spec.correct(read, (action, path, from, to) -> problems.add("value " + path + ": " + from + " -> " + to),
				(action, path, from, to) -> problems.add("comment " + path + ": [" + from + "] -> [" + to + "]"));
		return problems;
	}

	@Test
	void serverConfigSurvivesASaveAndReload() {
		assertEquals(List.of(), problemsAfterReload(RadiocraftServerConfig.SPEC));
	}

	@Test
	void commonConfigSurvivesASaveAndReload() {
		assertEquals(List.of(), problemsAfterReload(CommonConfig.SPEC));
	}

	@Test
	void clientConfigSurvivesASaveAndReload() {
		assertEquals(List.of(), problemsAfterReload(RadiocraftClientConfig.SPEC));
	}
}
