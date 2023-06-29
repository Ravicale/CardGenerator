package application;

import database.card.CardDatabase;
import gui.Gui;

import java.io.File;

public class CardGenerator {
	public static void main(String[] args) {
		LTags.configureLogging();
		Gui.init();
		CardDatabase.initCardDatabase(new File("carddb.json"));
	}
}