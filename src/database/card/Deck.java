package database.card;

import application.LTags;
import org.tinylog.Logger;

import java.util.*;

public class Deck {
	private final int maxCopies;
	private final int maxSize;

	private int size = 0;

	private int hashedIndices = 0;
	private static final double MIN_HASH_LOAD_FACTOR = 0.25;
	private static final double IDEAL_HASH_LOAD_FACTOR = 0.5;
	private static final double MAX_HASH_LOAD_FACTOR = 0.75;
	private final int minHashSize;
	private int[] cards;
	private final Deck boundingCollection;
	private List<Card> cachedList;
	private List<Card> cachedSet;

	public Deck(int maxCopies, int maxSize) {
		this.maxCopies = maxCopies;
		this.maxSize = maxSize;
		boundingCollection = null;
		minHashSize = 40;
		cards = new int[minHashSize];
		Arrays.fill(cards, -1);
	}

	public Deck(int maxCopies, int maxSize, Deck boundingCollection) {
		this.maxCopies = maxCopies;
		this.maxSize = maxSize;
		this.boundingCollection = boundingCollection;
		minHashSize = 40;
		cards = new int[minHashSize];
		Arrays.fill(cards, -1);
	}

	private int hashGetIndex(int cardId) {
		int cardHash = (cardId * 2) % cards.length; //Get hash using simple modulo technique.
		while (true) {
			int storedId = cards[cardHash];
			if (storedId == cardId) { //Card is found.
				return cardHash;
			} else if (storedId == -1) { //Card is not found.
				return -1;
			} else { //Try next hash using linear probing.
				cardHash = (cardHash + 2) % cards.length;
			}
		}
	}

	private void rehash() {
		Logger.tag(LTags.DECK.tag).debug("Rehashing Card Table.");
		int[] newCards = new int[Integer.max(minHashSize, (int) (hashedIndices * (1.0 / IDEAL_HASH_LOAD_FACTOR)))];
		Arrays.fill(newCards, -1);
		int[] oldCards = cards;
		cards = newCards;
		hashedIndices = 0;
		if (size > 0) {
			for (int i = 0; i < oldCards.length; i += 2) {
				int cardId = oldCards[i];
				int cardCount = oldCards[i + 1];
				if (cardId != -1 && cardCount > 0) {
					hashPutValue(cardId, cardCount, false);
				}
			}
		}
	}

	private void hashPutValue(int cardId, int count, boolean canRehash) {
		cachedList = null;
		cachedSet = null;
		int cardHash = (cardId * 2) % cards.length; //Get hash using simple modulo technique.
		int origCardHash = cardHash;
		while (true) {
			int storedId = cards[cardHash];
			int storedValue = cards[cardHash + 1];
			if (storedId == cardId || storedValue == 0) { //An empty slot is found.
				cards[cardHash] = cardId;
				cards[cardHash + 1] = count;
				if (count == 0) {
					hashedIndices -= 2;
				}
				break;
			} else if (storedId == -1) { //Card is not found.
				cards[cardHash] = cardId;
				cards[cardHash + 1] = count;
				if (count != 0) {
					hashedIndices += 2;
				}
				break;
			} else { //Try next hash using linear probing.
				cardHash = (cardHash + 2) % cards.length;
				//If we have looped all the way around without finding a slot, rehash.
				//Should never occur, but kept in defensively to prevent infinite loops.
				if (cardHash == origCardHash) {
					if (canRehash) {
						RuntimeException e = new RuntimeException("Attempted to rehash Card Collection while in the process of transfering data for another rehash.");
						Logger.tag(LTags.DECK.tag).error(e);
						throw e;
					}
					rehash();
					cardHash = (2 * cardId) % cards.length; //Get hash using simple modulo technique.
					origCardHash = cardHash;
				}
			}
		}

		if (canRehash) {
			double loadFactor = (double) (hashedIndices) / (double) cards.length;
			double loadFloor = ((double) (minHashSize)) * IDEAL_HASH_LOAD_FACTOR;
			if (hashedIndices >= loadFloor && (loadFactor > MAX_HASH_LOAD_FACTOR || loadFactor < MIN_HASH_LOAD_FACTOR)) {
				rehash();
			}
		}
	}

	private int maxCopies(int cardId) {
		if (boundingCollection != null) {
			return Integer.min(boundingCollection.getCopies(cardId), maxCopies);
		}
		return maxCopies;
	}

	public CardCount setCopies(int cardId, int count) {
		int currCopies = getCopies(cardId);
		int nextSize = size + count - currCopies;
		if (nextSize >= 0 && nextSize <= maxSize) {
			int maxForCard = maxCopies(cardId);
			if (count > 0 && count <= maxForCard) {
				hashPutValue(cardId, count, true);
				size = nextSize;
				Logger.tag(LTags.DECK.tag).info("Set card count for card #{} to {} copies.", cardId, count);
				return CardCount.SOME;
			} else if (count == maxForCard) {
				hashPutValue(cardId, count, true);
				size = nextSize;
				Logger.tag(LTags.DECK.tag).info("Set card count for card #{} to max {} copies.", cardId, count);
				return CardCount.MAX;
			} else if (count <= 0) {
				hashPutValue(cardId, count, true);
				size = nextSize;
				Logger.tag(LTags.DECK.tag).info("Removed card #{} from collection.", cardId);
				return CardCount.NONE;
			} else {
				Logger.tag(LTags.DECK.tag).info("Cannot set card #{} to {} copies. Max: {}", cardId, count, maxForCard);
			}
		} else {
			Logger.tag(LTags.DECK.tag).info("Collection at maximum size, cannot set card #{} to {} copies.", cardId, count);
		}
		return CardCount.INVALID;
	}

	public void clear() {
		if (cards.length * 2 > ((double) minHashSize) / MIN_HASH_LOAD_FACTOR) {
			cards = new int[minHashSize];
		}
		Arrays.fill(cards, -1);
		size = 0;
		hashedIndices = 0;
	}

	public int getCopies(int cardId) {
		int index = hashGetIndex(cardId);
		if (index != -1) {
			return cards[index + 1];
		}

		return 0;
	}

	public CardCount getCopiesState(int cardId) {
		int copies = getCopies(cardId);
		if (copies == 0) {
			if (size == maxSize) {
				return CardCount.INVALID;
			}
			return CardCount.NONE;
		} else if (size == maxSize || copies == maxCopies(cardId)) {
			return CardCount.MAX;
		}
		return CardCount.SOME;
	}

	public int getSize() {
		return size;
	}

	public List<Card> getCardList() {
		if (cachedList == null) {
			List<Card> collectionList = new ArrayList<>(size);
			for (int i = 0; i < cards.length; i += 2) {
				int cardId = cards[i];
				int cardCount = cards[i+1];
				if (cardId != -1 && cardCount > 0) {
					Card card = CardDatabase.getCard(cardId);
					for (int copy = 0; copy < cardCount; copy++) {
						collectionList.add(card);
					}
				}
			}
			cachedList = Collections.unmodifiableList(collectionList);
		}

		return cachedList;
	}

	public List<Card> getCardSet(){
		if (cachedSet == null) {
			List<Card> collectionList = new ArrayList<>(size);
			for (int i = 0; i < cards.length; i += 2) {
				int cardId = cards[i];
				int cardCount = cards[i + 1];
				if (cardId != -1 && cardCount > 0) {
					collectionList.add(CardDatabase.getCard(cardId));
				}
			}
			cachedSet = Collections.unmodifiableList(collectionList);
		}

		return cachedSet;
	}
}
