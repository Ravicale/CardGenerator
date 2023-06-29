package database.card;

public enum Rarity {
	COMMON ('C', 5),
	UNCOMMON ('U', 2),
	RARE ('R', 1),
	UNKNOWN ('?', 0);

	public final char abbreviation;
	public final int quantity;
	private static int cardsPerPack = 0;

	Rarity(char abbreviation, int quantity) {
		this.abbreviation = abbreviation;
		String.valueOf(abbreviation).intern();
		this.quantity = quantity;
	}

	public static Rarity fromChar(char rarity) {
		rarity = Character.toUpperCase(rarity);
		for (Rarity value : values()) {
			if (rarity == value.abbreviation) {
				return value;
			}
		}
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return String.valueOf(abbreviation);
	}

	public static int cardsPerPack() {
		if (cardsPerPack == 0) {
			for (Rarity rarity : values()) {
				cardsPerPack += rarity.quantity;
			}
		}

		return cardsPerPack;
	}
}
