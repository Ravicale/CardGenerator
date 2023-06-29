package gui.panes.models;

import database.card.Archetype;
import database.card.Card;
import database.card.CardDatabase;
import gui.Gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class CardTableFilter {
	private CardTableFilter() {}
	private static List<Predicate<Card>> currentFilters;
	public static boolean viewDeckOnly = false;
	public static boolean viewNotDeckOnly = false;
	public static boolean viewTrunkOnly = false;

	public static Predicate<Card> createArchetypeFilter(Collection<Archetype> archetypes) {
		return (card) -> archetypes.contains(card.archetype);
	}

	public static void setViewTrunkOnly(boolean value) {
		viewTrunkOnly = value;
		createTableFilter();
	}

	public static void setViewDeckOnly(boolean value) {
		viewDeckOnly = value;
		createTableFilter();
	}

	public static void setViewNotDeckOnly(boolean value) {
		viewNotDeckOnly = value;
		createTableFilter();
	}

	public static Predicate<Card> createNameFilter(String query) {
		if (query.startsWith("REGEX:")) {
			Pattern regex = Pattern.compile(query.toLowerCase().substring("REGEX:".length()));
			return (card -> regex.matcher(card.name.toLowerCase()).find());
		} else if (query.startsWith("EQUALS:")) {
			String squery = query.substring("EQUALS:".length()).toLowerCase();
			return (card -> card.name.equalsIgnoreCase(squery));
		} else {
			String lquery = query.toLowerCase();
			return (card) -> card.name.toLowerCase().contains(lquery);
		}
	}

	public static Predicate<Card> createTextFilter(String query) {
		if (query.startsWith("REGEX:")) {
			Pattern regex = Pattern.compile(query.toLowerCase().substring("REGEX:".length()));
			return (card -> regex.matcher(card.cardText).find());
		} else if (query.startsWith("EQUALS:")) {
			String squery = query.substring("EQUALS:".length()).toLowerCase();
			return (card -> card.cardText.equals(squery));
		} else {
			String lquery = query.toLowerCase();
			return (card) -> card.cardText.contains(lquery);
		}
	}

	public static Predicate<Card> createTypeFilter(String query) {
		if (query.startsWith("REGEX:")) {
			Pattern regex = Pattern.compile(query.toLowerCase().substring("REGEX:".length()));
			return (card -> regex.matcher(card.type.toLowerCase()).find());
		} else if (query.startsWith("EQUALS:")) {
			String squery = query.substring("EQUALS:".length()).toLowerCase();
			return (card -> card.type.equalsIgnoreCase(squery));
		} else {
			String lquery = query.toLowerCase();
			return (card) -> card.type.toLowerCase().contains(lquery);
		}
	}

	public static Predicate<Card> createKeywordFilter(String query) {
		String[] splitStrings = query.split("\\+");
		Collection<String[]> andGroups = new ArrayList<>(splitStrings.length);
		for (String splitString : splitStrings) {
			andGroups.add(splitString.split(","));
		}

		return (card) -> {
			for (String[] group : andGroups) {
				boolean isValid = false;
				for (String keyword : group) {
					if (card.keywords.stream().anyMatch((cardKeyword) -> cardKeyword.toLowerCase().contains(keyword.toLowerCase()))) {
						isValid = true;
						break;
					}
				}

				if (!isValid) {
					return false;
				}
			}

			return true;
		};
	}

	public static void setFilterList(List<Predicate<Card>> filters) {
		currentFilters = filters;
		createTableFilter();
	}

	private static void createTableFilter() {
		Gui.setCardFilter(new RowFilter<CardTableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends CardTableModel, ? extends Integer> entry) {
				Card card = CardDatabase.getCard(entry.getIdentifier());

				if (viewNotDeckOnly || viewDeckOnly) {
					int copiesInDeck = CardDatabase.getCountInDeck(card.id);

					if (viewDeckOnly && copiesInDeck == 0) {
						return false;
					} else if (viewNotDeckOnly && copiesInDeck > 0) {
						return false;
					}
				}

				if (viewTrunkOnly) {
					if (CardDatabase.getCountInTrunk(card.id) == 0) {
						return false;
					}
				}

				if (currentFilters != null) {
					//Test provided filters.
					for (Predicate<Card> filter : currentFilters) {
						if (!filter.test(card)) {
							return false;
						}
					}
				}
				return true;
			}
		});
	}
}
