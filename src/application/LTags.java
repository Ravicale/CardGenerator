package application;

public enum LTags {
	DECK_IMAGE("Deck Image", LogLevel.OFF),
	IMG_LOAD("Image Loading", LogLevel.OFF),
	IMG_GEN("Image Gen", LogLevel.OFF),
	DB_INIT("Database Init", LogLevel.TRACE),
	DB_ACTION("Database Action", LogLevel.OFF),
	USER_INPUT("User Input", LogLevel.OFF),
	UI_UPDATES("UI Updates", LogLevel.OFF),
	UI_SYNC("UI Sync", LogLevel.OFF),
	DECK("Card Collection", LogLevel.TRACE);

	private static final boolean WRITE_TO_FILE = !System.getProperty("java.class.path").contains("idea_rt.jar");

	@SuppressWarnings("unused")
	private enum LogLevel {
		OFF("off"),
		TRACE("trace"),
		DEBUG("debug"),
		INFO("info"),
		WARN("warn"),
		ERROR("error");

		public final String value;
		LogLevel(String valueName) {
			value = valueName;
		}
	}

	public final String tag;
	private final LogLevel level;

	LTags(String tagName, LogLevel levelName) {
		tag = tagName;
		level = levelName;
	}

	public static void configureLogging() {
		if (WRITE_TO_FILE) {
			org.tinylog.configuration.Configuration.set("writer", "file");
			org.tinylog.configuration.Configuration.set("writer.file", "log.txt");
			org.tinylog.configuration.Configuration.set("writer.charset", "UTF-8");
		} else {
			org.tinylog.configuration.Configuration.set("writer", "console");
		}

		org.tinylog.configuration.Configuration.set("writingthread", "true");
		org.tinylog.configuration.Configuration.set("writer.buffered", "true");
		org.tinylog.configuration.Configuration.set("writer.format", "{thread} {date} - [\"{tag}\"|{level}] : {message|indent=4}");

		LTags[] tags = values();
		StringBuilder enabledTags = new StringBuilder();
		boolean first = true;
		for (LTags tag : tags) {
			if (tag.level != LogLevel.OFF) {
				if (!first) {
					enabledTags.append(",");
				}
				enabledTags.append(tag.tag);
				enabledTags.append("@");
				enabledTags.append(tag.level.value);
				first = false;
			}
		}

		//Literally cannot use the logger here.
		//noinspection UseOfSystemOutOrSystemErr
		System.out.println("Logging \"" + enabledTags + "\"");
		org.tinylog.configuration.Configuration.set("writer.tag", enabledTags.toString());
	}
}
