package database.card;

import application.LTags;
import database.image.ImgStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 Object representing a magic the gathering card. Should only be mutated by CardDatabase, but may be referenced elsewhere
 for convenience or general sanity. */
public class Card {
	/** Field names for JTables that are displaying cards. */
	public static final String[] DATA_FIELD_NAMES = {"Name", "Type", "Rarity", "Cost", "Power", "Health", "Channel", "Charges", "Slots", "In Trunk", "In Deck"};
	public static final int NAME = 0;
	public static final int TYPE = 1;
	public static final int RARITY = 2;
	public static final int COST = 3;
	public static final int POWER = 4;
	public static final int HEALTH = 5;
	public static final int CHANNEL = 6;
	public static final int CHARGES = 7;
	public static final int SLOTS = 8;
	public static final int IN_TRUNK = 9;
	public static final int IN_DECK = 10;

	/** Index of this card in the card list, for fast lookups. */
	public final int id;

	public static final int BLANK_INT = -1;
	public static final CardField BLANK_FIELD = new CardField("", BLANK_INT);

	public final String name;
	public final String type;
	public final Integer cost;
	public final CardField power;
	public final CardField health;
	public final CardField channel;
	public final CardField charges;
	public final CardField slots;
	public final int textSize;

	/** The text inside the text box for the card. */
	public final String cardText;
	/** List of colors making up this card's color identity. */
	public final String pack;
	public final Rarity rarity;
	public final Archetype archetype;
	/** Keyworks associated with the card.*/
	public final List<String> keywords;
	public final File image;

	/**
	 Constructs a card object from JSON.
	 @param cardJson The json to construct the card from.
	 @param id       The ID number for the card.
	 @throws JSONException         If the json object returns a parsing error.
	 */
	public Card(JSONObject cardJson, int id) throws JSONException {
		this.id = id;
		Logger.tag(LTags.DB_INIT.tag).info("Initializing card {}", id);
		name = cardJson.getString("Name");
		type = cardJson.getString("Type");
		archetype = Archetype.fromString(cardJson.getString("Archetype"));
		cost = cardJson.getInt("Cost");
		int powerInt = cardJson.optInt("Power", BLANK_INT);
		power = powerInt == BLANK_INT ? BLANK_FIELD : new CardField(powerInt);

		int healthInt = cardJson.optInt("Health", BLANK_INT);
		health = healthInt == BLANK_INT ? BLANK_FIELD : new CardField(healthInt);

		int channelInt = cardJson.optInt("Channel", BLANK_INT);
		channel = channelInt == BLANK_INT ? BLANK_FIELD : new CardField(channelInt);

		int durationInt = cardJson.optInt("Charges", BLANK_INT);
		charges = durationInt == BLANK_INT ? BLANK_FIELD : new CardField(durationInt);

		String slotsString = cardJson.optString("Slots", "-1");
		int slotsInt = Integer.parseInt(slotsString.split(" ")[0]);
		slots = slotsInt == BLANK_INT ? BLANK_FIELD : new CardField(slotsString, slotsInt);

		cardText = cardJson.optString("Text", "");
		textSize = cardJson.optInt("TextSize",  -1);

		List<String> mutKeywords = new ArrayList<>(1);
		JSONArray jsonKeywords = cardJson.optJSONArray("Keywords");
		if (jsonKeywords != null) {
			for (Object keyword : jsonKeywords) {
				if (keyword instanceof String) {
					mutKeywords.add((String) keyword);
				} else {
					Logger.tag(LTags.DB_INIT.tag).warn("Non-string item found in keyword array for {}.", name);
				}
			}
		}
		keywords = Collections.unmodifiableList(mutKeywords);

		image = new File(ImgStore.CARD_IMAGE_PATH + cardJson.optString("ImageName", "none"));
		if (!image.exists()) {
			Logger.tag(LTags.DB_INIT.tag).warn("Card image for {} not found in {}.", name, image.getAbsolutePath());
		}

		rarity = Rarity.fromChar(cardJson.optString("Rarity", "?").charAt(0));
		pack = cardJson.optString("Pack", "NONE");
	}

	/**
	 Returns the desired CardField. Valid fields are the public static ints provided by Card. For use by JTables.
	 * @param value  The type of field that's desired.
	 * @return       The desired CardField.
	 */
	public Comparable<?> get(int value) {
		switch (value) {
			case NAME:
				return name;
			case TYPE:
				return type;
			case RARITY:
				return rarity;
			case COST:
				return cost;
			case POWER:
				return power;
			case HEALTH:
				return health;
			case CHANNEL:
				return channel;
			case CHARGES:
				return charges;
			case SLOTS:
				return slots;
			case IN_TRUNK:
				return CardDatabase.getCountInTrunk(id);
			case IN_DECK:
				return CardDatabase.getCountInDeck(id);
			default:
				throw new IllegalArgumentException("Attempted to look up a nonexistent card field.");
		}
	}
}
