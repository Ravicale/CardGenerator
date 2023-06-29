package database.card;

public enum Archetype {
	ALIEN ("Alien"),
	CRIM ("Criminal"),
	CULT ("Cult"),
	COLONY ("Colony"),
	ROBOT ("Robot"),
	UNKNOWN ("?????");

	public final String niceName;

	Archetype(String niceName) {
		this.niceName = niceName;
	}

	public static Archetype fromString(String color) {
		color = color.toUpperCase();
		for (Archetype value : values()) {
			if (color.equals(value.name())) {
				return value;
			}
		}
		return UNKNOWN;
	}
}
