package gui.panes;

import application.LTags;
import database.card.CardDatabase;
import gui.Gui;
import gui.panes.models.CardTableFilter;
import org.tinylog.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

public class MenuBar extends JMenuBar {
	private static final FileFilter DEC_FILTER = new FileNameExtensionFilter("Decklist (.dec)", "dec");
	private static final FileFilter JSON_FILTER = new FileNameExtensionFilter("Javascript Object Notation (.json)", "json");
	private static final FileFilter IMG_FILTER = new FileNameExtensionFilter("Portable Network Graphics (.PNG)", "png");
	private final JLabel deckSizeCounter = new JLabel("Deck Size - 0");
	private final JMenu packMenu;
	private final JCheckBoxMenuItem deckOnly;
	private final JCheckBoxMenuItem notDeckOnly;

	public MenuBar() {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');
		JMenuItem readDatabase = new JMenuItem("Read Cards");
		readDatabase.addActionListener((e) -> importPopup(JSON_FILTER, CardDatabase::initCardDatabase));
		readDatabase.setMnemonic('c');
		fileMenu.add(readDatabase);
		JMenuItem readDeck = new JMenuItem("Import Deck");
		readDeck.addActionListener((e) -> importPopup(DEC_FILTER, CardDatabase::readDeck));
		readDeck.setMnemonic('d');
		fileMenu.add(readDeck);
		JMenuItem exportDeck = new JMenuItem("Save Deck");
		exportDeck.addActionListener((e) -> exportPopup(DEC_FILTER, ".dec", CardDatabase::saveDeck));
		exportDeck.setMnemonic('e');
		fileMenu.add(exportDeck);
		JMenuItem exportImage = new JMenuItem("Export Deck Image");
		exportImage.addActionListener((e) -> exportPopup(IMG_FILTER, ".png", CardDatabase::saveDeckImage));
		exportImage.setMnemonic('e');
		fileMenu.add(exportImage);
		add(fileMenu);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		JMenuItem clearDeck = new JMenuItem("Clear Deck");
		clearDeck.addActionListener((e) -> CardDatabase.clearDeck());
		clearDeck.setMnemonic('d');
		editMenu.add(clearDeck);
		add(editMenu);

		JMenuItem viewMenu = new JMenu("View");
		viewMenu.setMnemonic('v');
		JCheckBoxMenuItem trunkOnly = new JCheckBoxMenuItem("In Trunk");
		trunkOnly.addActionListener((e) -> CardTableFilter.setViewTrunkOnly(trunkOnly.isSelected()));
		trunkOnly.setMnemonic('t');
		viewMenu.add(trunkOnly);
		deckOnly = new JCheckBoxMenuItem("In Deck");
		deckOnly.addActionListener(this::viewDeckOnly);
		deckOnly.setMnemonic('d');
		viewMenu.add(deckOnly);
		notDeckOnly = new JCheckBoxMenuItem("Not In Deck");
		notDeckOnly.addActionListener(this::viewNotDeckOnly);
		notDeckOnly.setMnemonic('n');
		viewMenu.add(notDeckOnly);
		add(viewMenu);

		JMenu trunkMenu = new JMenu("Trunk");
		trunkMenu.setMnemonic('t');
		JMenuItem fill = new JMenuItem("Fill");
		fill.addActionListener((e) -> CardDatabase.fillTrunk());
		fill.setMnemonic('f');
		trunkMenu.add(fill);
		JMenuItem empty = new JMenuItem("Empty");
		empty.addActionListener((e) -> CardDatabase.clearTrunk());
		empty.setMnemonic('e');
		trunkMenu.add(empty);
		packMenu = new JMenu("Open Pack");
		packMenu.setMnemonic('p');
		trunkMenu.add(packMenu);
		packMenu.setEnabled(false);
		add(trunkMenu);

		add(Box.createHorizontalGlue());
		add(deckSizeCounter);
	}

	public void setDeckSizeCounter(int value) {
		deckSizeCounter.setText("Deck Size - " + value);
	}

	private void viewDeckOnly(ActionEvent e) {
		boolean selectionState = deckOnly.isSelected();
		notDeckOnly.setEnabled(!selectionState);
		CardTableFilter.setViewDeckOnly(selectionState);
	}

	private void viewNotDeckOnly(ActionEvent e) {
		boolean selectionState = notDeckOnly.isSelected();
		deckOnly.setEnabled(!selectionState);
		CardTableFilter.setViewNotDeckOnly(selectionState);
	}

	private void importPopup(FileFilter filter, Consumer<File> action) {
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(filter);
		int result = fileChooser.showOpenDialog(Gui.getFrame());
		if (result == JFileChooser.APPROVE_OPTION) {
			File selection = fileChooser.getSelectedFile();
			if (selection.canRead()) {
				action.accept(selection);
			} else {
				Logger.tag(LTags.USER_INPUT.tag).warn("Could not read file '{}'.", selection.getAbsolutePath());
				JOptionPane.showMessageDialog(Gui.getFrame(), "Could not read file.");
			}
		}
	}

	private void exportPopup(FileFilter filter, String extension, Consumer<File> action) {
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(filter);
		int result = fileChooser.showSaveDialog(Gui.getFrame());
		if (result == JFileChooser.APPROVE_OPTION) {
			File selection = fileChooser.getSelectedFile();
			Logger.tag(LTags.USER_INPUT.tag).info("Attempting to export deck to '{}'", selection.getAbsolutePath());
			if (!selection.getName().endsWith(extension)) {
				selection = new File(selection.getAbsolutePath() + extension);
			}

			if (selection.exists()) {
				if (!selection.delete()) {
					Logger.tag(LTags.USER_INPUT.tag).warn("Could not remove exististing file '{}'.", selection.getAbsolutePath());
					JOptionPane.showMessageDialog(Gui.getFrame(), "Could not remove exististing file.");
					return;
				}
			}

			if (selection.getParentFile().canWrite()) {
				action.accept(selection);
			} else {
				Logger.tag(LTags.USER_INPUT.tag).warn("Unable to write to selected file at '{}'.", selection.getAbsolutePath());
				JOptionPane.showMessageDialog(Gui.getFrame(), "No write permissions for selected file.");
			}
		}
	}

	private void packPopup(String packName) {
		int packCount = 0;
		try {
			packCount = Integer.parseInt(JOptionPane.showInputDialog(Gui.getFrame(),
					"Number of " + packName + " to open.",
					"1"));
		} catch (NumberFormatException e) {
			Logger.tag(LTags.USER_INPUT.tag).warn("User attempts to open a non-numeric number of packs.");
		}

		if (packCount <= 0) {
			Gui.displayPopup("You must enter a number of packs greater than 0 to open.");
		}

		CardDatabase.openPacks(packName, packCount);
	}

	public void setPacks(Set<String> packs) {
		packMenu.removeAll();
		if (packs.isEmpty()) {
			packMenu.setEnabled(false);
		} else {
			for (String pack : packs) {
				JMenuItem packSelector = new JMenuItem(pack);
				packSelector.addActionListener((e) -> packPopup(pack));
				packMenu.add(packSelector);
			}
			packMenu.setEnabled(true);
		}
	}
}
