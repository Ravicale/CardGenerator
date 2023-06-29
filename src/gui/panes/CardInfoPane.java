package gui.panes;

import application.LTags;
import database.card.CardCount;
import database.card.CardDatabase;
import gui.UIConstants;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CardInfoPane extends JPanel {

	private final JLabel cardImage = new JLabel(UIConstants.DEFAULT_CARD_ICON);
	private int cardId;
	private final JButton addButton = new JButton("Add");
	private final JButton removeButton = new JButton("Remove");

	public CardInfoPane() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		addButton.setPreferredSize(UIConstants.CARD_BUTTON_SIZE);
		addButton.addActionListener(this::addToDeck);

		removeButton.setPreferredSize(UIConstants.CARD_BUTTON_SIZE);
		removeButton.addActionListener(this::removeFromDeck);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGap(UIConstants.MARGIN)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(cardImage)
						.addGroup(layout.createSequentialGroup()
								.addComponent(addButton)
								.addComponent(removeButton)
						)
				)
				.addGap(UIConstants.MARGIN)
		);

		layout.setVerticalGroup(layout.createSequentialGroup()
						.addGap(UIConstants.MARGIN)
						.addGroup(layout.createParallelGroup()
								.addComponent(addButton)
								.addComponent(removeButton)
						)
						.addComponent(cardImage)
				//No margin, since there's another panel right below with its own margin.
		);

		addButton.setEnabled(false);
		removeButton.setEnabled(false);
		setPreferredSize(UIConstants.CARD_INFO_PANE_SIZE);
		setMaximumSize(UIConstants.CARD_INFO_PANE_SIZE);
	}

	public void setCard(int newCardId, CardCount cardCount, Icon newImage) {
		if (newCardId != -1) {
			Logger.tag(LTags.UI_UPDATES.tag).info("Setting selected card to card #{}", newCardId);
			cardImage.setIcon(newImage == null ? UIConstants.DEFAULT_CARD_ICON : newImage);
			cardId = newCardId;
			addButton.setEnabled(cardCount != CardCount.MAX && cardCount != CardCount.INVALID);
			removeButton.setEnabled(cardCount != CardCount.NONE && cardCount != CardCount.INVALID);
		} else {
			Logger.tag(LTags.UI_UPDATES.tag).info("Deselecting selected card");
			addButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
	}

	private void addToDeck(ActionEvent actionEvent) {
		Logger.tag(LTags.USER_INPUT.tag).info("Adding card #{} to deck.", cardId);
		CardDatabase.addToDeck(cardId, 1);
	}

	private void removeFromDeck(ActionEvent actionEvent) {
		Logger.tag(LTags.USER_INPUT.tag).info("Removing card #{} from deck.", cardId);
		CardDatabase.addToDeck(cardId, -1);
	}

	public void updateCard(int id, CardCount cardCount) {
		Logger.tag(LTags.UI_UPDATES.tag).info("Updating Card Info Pane for #{} to {}.", id, cardCount);
		if (id == cardId) {
			updateCard(cardCount);
		} else {
			Logger.tag(LTags.UI_UPDATES.tag).info("Skipping update for #{}, since selected card is {}.", id, cardId);
		}
	}

	public void updateCard(CardCount cardCount) {
		Logger.tag(LTags.UI_UPDATES.tag).info("Updating Card Info Pane for #{} to {}.", cardId, cardCount);
		if (cardCount != CardCount.INVALID) {
			addButton.setEnabled(cardCount != CardCount.MAX);
			removeButton.setEnabled(cardCount != CardCount.NONE);
		} else {
			Logger.tag(LTags.UI_UPDATES.tag).info("Skipping update for #{}, since copy count state is invalid.", cardId);
		}
	}
}
