package database.card;

import application.LTags;
import database.image.ImgStore;
import gui.Gui;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.tinylog.Logger;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 Class that handles data for all cards. Any mutations to cards should be done through this. */
public class CardDatabase {
	//Some of the tasks here can take a bit. So keep them off of the Swing event thread.
	private static final ExecutorService databaseWorkerThread = Executors.newSingleThreadExecutor();
	//Reference to a card loading event. Used to allow for the thread to be interrupted if something else gets clicked.
	private static Future<?> cardToLoad;
	//The previously loaded card image.
	private static int previousCard = -1;
	//Database instance. Should be initialized via initCardDatabase before anything else happens.
	private static CardDatabaseInstance instance = new CardDatabaseInstance();
	private static final AtomicBoolean isLoadingInstance = new AtomicBoolean(false);

	private static class CardDatabaseInstance {
		//Map of cards sorted by name. Immutable.
		private final Map<String, Card> cardMap;
		//List of cards. The id number on a Card object correponds to this. Try using this over the map when possible. Immutable.
		private final List<Card> cardList;
		private final Map<String, Map<Rarity, List<Card>>> packs;
		private final Deck deck;
		private final Deck trunk;

		private CardDatabaseInstance() {
			cardMap = new HashMap<>(0);
			cardList = new ArrayList<>(0);
			packs = new HashMap<>(0);
			trunk = new Deck(Integer.MAX_VALUE, Integer.MAX_VALUE, null);
			deck = new Deck(4, 40, trunk);
		}

		private CardDatabaseInstance(File path) throws IOException {
			InputStream databaseStream;
			databaseStream = Files.newInputStream(Paths.get(path.getAbsolutePath()));

			Logger.tag(LTags.DB_INIT.tag).info("Loading cards.");
			cardList = new ArrayList<>(300);
			cardMap = new HashMap<>(300);
			trunk = new Deck(Integer.MAX_VALUE, Integer.MAX_VALUE);
			deck = new Deck(4, 40, trunk);

			//Read in json objects for cards one at a time to avoid reading all of the JSON to memory at once.
			JSONTokener databaseJson = new JSONTokener(databaseStream);
			databaseJson.next('[');
			int cardNum = 0;
			while (databaseJson.skipTo('{') != 0) {
				try {
					JSONObject cardJson = new JSONObject(databaseJson);
					Card card = new Card(cardJson, cardNum);
					cardList.add(card);
					cardMap.put(card.name, card);
					cardNum++;
					Logger.tag(LTags.DB_INIT.tag).debug(() -> "Loaded card " + card.name);
				} catch (JSONException e) {
					Logger.tag(LTags.DB_INIT.tag).error(e, "Unable to create card #{}", cardNum);
					Gui.displayPopup(e.getMessage());
					break;
				}
			}
			Logger.tag(LTags.DB_INIT.tag).info("Loaded {} cards. Building Packs", cardList.size());

			packs = new HashMap<>(10);
			for (Card card : cardList) {
				Logger.tag(LTags.DB_INIT.tag).info("Card #{} - {} has a rarity of {}.", card.id, card.name, card.rarity.abbreviation);
				if (card.rarity != Rarity.UNKNOWN) {
					Map<Rarity, List<Card>> pack = packs.computeIfAbsent(card.pack, (p) -> new EnumMap<>(Rarity.class));
					List<Card> pool = pack.computeIfAbsent(card.rarity, (r) -> new ArrayList<>(30));
					pool.add(card);
				}
			}

			getMetrics();
			Logger.tag(LTags.DB_INIT.tag).info("Built {} packs. Updating GUI.", packs.size());
		}

		private void getMetrics() {
			for (Map.Entry<String, Map<Rarity, List<Card>>> packEntry : packs.entrySet()) {
				String packName = packEntry.getKey();
				Map<Rarity, List<Card>> pack = packEntry.getValue();
				int totalInPack = 0;
				Map<Archetype, Integer> archetypeInPack = new EnumMap<>(Archetype.class);
				Map<String, Integer> typeInPack = new HashMap<>();
				Logger.tag(LTags.DB_INIT.tag).info("Pack {}", packName);
				for (Map.Entry<Rarity, List<Card>> poolEntry : pack.entrySet()) {
					Rarity rarity = poolEntry.getKey();
					List<Card> pool = poolEntry.getValue();
					Map<Archetype, Integer> archetypeInPool = new EnumMap<>(Archetype.class);
					Map<String, Integer> typeInPool = new HashMap<>();
					for (Card card : pool) {
						int count = archetypeInPack.computeIfAbsent(card.archetype, (c) -> 0);
						archetypeInPack.put(card.archetype, count + 1);
						count = typeInPack.computeIfAbsent(card.type, (c) -> 0);
						typeInPack.put(card.type, count + 1);
						count = archetypeInPool.computeIfAbsent(card.archetype, (c) -> 0);
						archetypeInPool.put(card.archetype, count + 1);
						count = typeInPool.computeIfAbsent(card.type, (c) -> 0);
						typeInPool.put(card.type, count + 1);
					}
					totalInPack += pool.size();
					Logger.tag(LTags.DB_INIT.tag).info("    Pool {} - {} ({} cards)", packName, rarity.abbreviation, pool.size());
					for (Map.Entry<Archetype, Integer> archTotal : archetypeInPool.entrySet()) {
						Logger.tag(LTags.DB_INIT.tag).info("        {} {} cards.", archTotal.getValue(), archTotal.getKey());
					}
					for (Map.Entry<String, Integer> typeTotal : typeInPool.entrySet()) {
						Logger.tag(LTags.DB_INIT.tag).info("        {} {} cards.", typeTotal.getValue(), typeTotal.getKey());
					}
				}
				Logger.tag(LTags.DB_INIT.tag).info("    Pack contained {} cards.", totalInPack);
				for (Map.Entry<Archetype, Integer> archTotal : archetypeInPack.entrySet()) {
					Logger.tag(LTags.DB_INIT.tag).info("    {} {} cards.", archTotal.getValue(), archTotal.getKey());
				}
				for (Map.Entry<String, Integer> typeTotal : typeInPack.entrySet()) {
					Logger.tag(LTags.DB_INIT.tag).info("    {} {} cards.", typeTotal.getValue(), typeTotal.getKey());
				}
			}


			for (Archetype archetype : Archetype.values()) {
				Map<Rarity, Integer> rarity = new EnumMap<>(Rarity.class);
				Map<String, Integer> type = new HashMap<>();
				int totalCount = 0;
				for (Card card : cardList) {
					if (card.archetype == archetype && !card.type.contains("Token")) {
						int count = type.computeIfAbsent(card.type, (c) -> 0);
						type.put(card.type, count + 1);
						count = rarity.computeIfAbsent(card.rarity, (c) -> 0);
						rarity.put(card.rarity, count + 1);
						totalCount++;
					}
				}
				Logger.tag(LTags.DB_INIT.tag).info("{} - {} cards", archetype, totalCount);
				for (Map.Entry<String, Integer> typeTotal : type.entrySet()) {
					Logger.tag(LTags.DB_INIT.tag).info("    {} {} cards.", typeTotal.getValue(), typeTotal.getKey());
				}
				for (Map.Entry<Rarity, Integer> rarityTotal : rarity.entrySet()) {
					Logger.tag(LTags.DB_INIT.tag).info("    {} {} cards.", rarityTotal.getValue(),rarityTotal.getKey());
				}
			}
		}

		private void readDeckFile(File file) {
			Logger.tag(LTags.DB_ACTION.tag).info("Reading collection file at '{}'.", file.getAbsolutePath());
			clearDeck();
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				long lineNum = 0;
				String line;

				while ((line = reader.readLine()) != null) {
					try {
						lineNum++;
						if (!line.isEmpty() && !line.startsWith("//")) {
							int count = Integer.parseInt(line.substring(0, line.indexOf(' ')));
							String name = line.substring(line.indexOf(' ')).trim();
							Card card = cardMap.get(name);
							if (card != null) {
								addToDeck(card.id, count);
							} else {
								Logger.tag(LTags.DB_ACTION.tag).error("Unable to find card named {} in database!", name);
							}
						}
					} catch (NumberFormatException e) {
						Logger.tag(LTags.DB_ACTION.tag).error(e, "Unable to read number of cards in line {} or {}", lineNum, file.getName());
					}
				}
			} catch (IOException e) {
				Logger.tag(LTags.DB_ACTION.tag).error(e, "Error while reading file {}.", file.getName());
			}
		}

		private void addToDeck(int cardId, int count) {
			Logger.tag(LTags.DB_ACTION.tag).info("Setting number of copies of card #{} in deck to {}", cardId, count);
			CardCount result;
			synchronized (deck) {
				result = deck.setCopies(cardId, deck.getCopies(cardId) + count);
			}
			if (result != CardCount.INVALID) {
				Gui.updateDeck(cardId, deck.getCopiesState(cardId), deck.getSize());
			}
		}

		public void openPacks(String packName, int numPacks) {
			if (!packs.containsKey(packName)) {
				Logger.tag(LTags.DB_ACTION.tag).error("Attempted to open nonexistent pack {}.", packName);
				return;
			}
			Map<Rarity, List<Card>> pack = packs.get(packName);
			Random rng = new Random(System.currentTimeMillis());
			Set<Card> pulls = new HashSet<>(numPacks * Rarity.cardsPerPack() / 2);

			synchronized (trunk) {
				for (int i = 0; i < numPacks; i++) {
					for (Rarity rarity : Rarity.values()) {
						for (int j = 0; j < rarity.quantity; j++) {
							if (!pack.containsKey(rarity)) {
								Logger.tag(LTags.DB_ACTION.tag).error("Attempted to pull cards of rarity {} from a pack with no cards defined at that rarity.", packName);
								Gui.displayPopup(packName + " does not contain any cards with a rarity of " + rarity.abbreviation);
								return;
							}
							List<Card> pool = pack.get(rarity);
							Card card = pool.get(rng.nextInt(pool.size()));
							CardCount result = trunk.setCopies(card.id, trunk.getCopies(card.id) + 1);
							if (result != CardCount.INVALID) {
								pulls.add(card);
							}
						}
					}
				}
			}

			for (Card card : pulls) {
				Gui.updateCardInfo(card.id, deck.getCopiesState(card.id));
			}
		}

		public int getCopiesInDeck(int cardId) {
			synchronized (deck) {
				return deck.getCopies(cardId);
			}
		}

		public int getCopiesInTrunk(int cardId) {
			synchronized (trunk) {
				return trunk.getCopies(cardId);
			}
		}

		public void fillTrunk() {
			Logger.tag(LTags.DB_ACTION.tag).info("Filling trunk.");
			synchronized (trunk) {
				for (Card card : cardList) {
					trunk.setCopies(card.id, 99);
				}
			}

			//Look into adding a proper bulk update later.
			Gui.rebuildCardList(packs.keySet());
		}

		public void clearTrunk() {
			Logger.tag(LTags.DB_ACTION.tag).info("Clearing trunk.");
			clearDeck();
			synchronized (trunk) {
				trunk.clear();
			}
		}

		public void clearDeck() {
			Logger.tag(LTags.DB_ACTION.tag).info("Clearing deck.");
			synchronized (deck) {
				deck.clear();
			}
		}
	}

	public static void initCardDatabase(File path) {
		if (path.exists()) {
			isLoadingInstance.set(true);
			databaseWorkerThread.submit(() -> {
				Thread.currentThread().setName("Card Database Worker");
				Logger.tag(LTags.DB_INIT.tag).info("Initializing card database.");
				Gui.resetCardList();
				try {
					Gui.setBusyLoading(true);
					instance = new CardDatabaseInstance(path);
					Gui.rebuildCardList(instance.packs.keySet());
				} catch (IOException e) {
					Logger.tag(LTags.DB_INIT.tag).error(e, "Unable to open and read card database file.");
					instance = new CardDatabaseInstance();
				} catch (Exception e) {
					Logger.tag(LTags.DB_INIT.tag).error(e, "WTF????");
					instance = new CardDatabaseInstance();
				} finally {
					Gui.setBusyLoading(false);
					isLoadingInstance.set(false);
				}
			});
		}
	}

	public static void fillTrunk() {
		databaseWorkerThread.submit(() -> instance.fillTrunk());
	}

	public static void clearTrunk() {
		databaseWorkerThread.submit(() -> {
			instance.clearTrunk();
			Gui.clearDeck();
		});
	}

	public static void openPacks(String packName, int numPacks) {
		databaseWorkerThread.submit(() -> {
			Gui.setBusyLoading(true);
			instance.openPacks(packName, numPacks);
			Gui.setBusyLoading(false);
		});
	}

	public static void loadAndDisplayImage(int cardId) {
		if (cardId == previousCard) {
			return;
		}
		previousCard = cardId;

		//Stop the previous request, since it's no longer relevant.
		if (cardToLoad != null && !cardToLoad.isDone()) {
			cardToLoad.cancel(true);
		}

		cardToLoad = databaseWorkerThread.submit(() -> {
			try {
				Card card = instance.cardList.get(cardId);
				Logger.tag(LTags.DB_ACTION.tag).info("Requesting images for '{}'.", card.name);
				Gui.setBusyLoading(true);
				ImageIcon image = ImgStore.getCardImage(card);
				Gui.setSelectedCard(cardId, instance.deck.getCopiesState(cardId), image);
			} finally {
				Gui.setBusyLoading(false);
			}
		});
	}

	public static void clearDeck() {
		databaseWorkerThread.submit(() -> {
			instance.clearDeck();
			Gui.clearDeck();
		});
	}

	public static void readDeck(File file) {
		databaseWorkerThread.submit(() -> {
			instance.deck.clear();
			instance.readDeckFile(file);
		});
	}

	public static void saveDeck(File file) {
		databaseWorkerThread.submit(() -> {
			if (instance.deck.getSize() == 0) {
				Logger.tag(LTags.DB_ACTION.tag).info("User attempted to save an empty deck.");
				Gui.displayPopup("Your deck is currently empty.");
				return;
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write("// Deck with " + instance.deck.getSize() + " cards - " + Calendar.getInstance().getTime());
				writer.newLine();
				for (Card card : instance.deck.getCardSet()) {
					writer.write(instance.deck.getCopies(card.id) + " " + card.name);
					writer.newLine();
				}
				Gui.setBusyLoading(false);
			} catch (IOException e) {
				Logger.tag(LTags.DB_ACTION.tag).error("Unable to write deck to {}", file.getAbsolutePath());
			}
		});
	}

	public static void saveDeckImage(File file) {
		databaseWorkerThread.submit(() -> {
			if (instance.deck.getSize() <= 0) {
				Logger.tag(LTags.DB_ACTION.tag).info("User attempted to save an empty deck.");
				Gui.displayPopup("Your deck is currently empty.");
			} else {
				ImgStore.writeDeckImage(instance.deck.getCardList(), file);
			}
		});
	}

	public static void addToDeck(int cardId, int amount) {
		databaseWorkerThread.submit(() -> instance.addToDeck(cardId, amount));
	}

	public static Card getCard(int index) {
		return instance.cardList.get(index);
	}

	public static int getTotalCardCount() {
		return instance.cardList.size();
	}

	//TODO: Figure out how I can make this not garbage.
	public static int getCountInTrunk(int cardId) {
		if (!isLoadingInstance.get()) {
			return instance.getCopiesInTrunk(cardId);
		}
		return 0;
	}

	//TODO: Ditto
	public static int getCountInDeck(int cardId) {
		if (!isLoadingInstance.get()) {
			return instance.getCopiesInDeck(cardId);
		}
		return 0;
	}
}
