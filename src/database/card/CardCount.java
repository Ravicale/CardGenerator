package database.card;

public enum CardCount {
	NONE,
	SOME,
	MAX,
	INVALID;

	@Override
	public String toString() {
		switch (this) {
			case NONE: return "NONE";
			case SOME: return "SOME";
			case MAX: return "MAX";
			default: return "INVALID";
		}
	}
}
