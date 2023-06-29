package database.image;

import application.LTags;
import database.card.Card;
import database.card.Rarity;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

class ImgGenerator {
	private ImgGenerator() {}
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private static final MutableAttributeSet LEFT_ALIGN = new SimpleAttributeSet();
	private static final MutableAttributeSet CENTER_ALIGN = new SimpleAttributeSet();
	private static final MutableAttributeSet RIGHT_ALIGN = new SimpleAttributeSet();
	private static final MutableAttributeSet RARE_TITLE = new SimpleAttributeSet();
	private static final MutableAttributeSet UC_TITLE = new SimpleAttributeSet();
	static {
		StyleConstants.setAlignment(LEFT_ALIGN, StyleConstants.ALIGN_LEFT);
		StyleConstants.setSpaceAbove(LEFT_ALIGN, 0);
		StyleConstants.setSpaceBelow(LEFT_ALIGN, 0);

		StyleConstants.setAlignment(RARE_TITLE, StyleConstants.ALIGN_LEFT);
		StyleConstants.setForeground(RARE_TITLE, Color.YELLOW);
		StyleConstants.setSpaceAbove(RARE_TITLE, 0);
		StyleConstants.setSpaceBelow(RARE_TITLE, 0);

		StyleConstants.setAlignment(UC_TITLE, StyleConstants.ALIGN_LEFT);
		StyleConstants.setForeground(UC_TITLE, new Color(77, 255, 255));
		StyleConstants.setSpaceAbove(UC_TITLE, 0);
		StyleConstants.setSpaceBelow(UC_TITLE, 0);

		StyleConstants.setAlignment(CENTER_ALIGN, StyleConstants.ALIGN_CENTER);
		StyleConstants.setSpaceAbove(CENTER_ALIGN, 0);
		StyleConstants.setSpaceBelow(CENTER_ALIGN, 0);

		StyleConstants.setAlignment(RIGHT_ALIGN, StyleConstants.ALIGN_RIGHT);
		StyleConstants.setSpaceAbove(RIGHT_ALIGN, 0);
		StyleConstants.setSpaceBelow(RIGHT_ALIGN, 0);
	}
	private static final int HEADER_FONT_SIZE = 32;
	private static final int SUBHEADER_FONT_SIZE = 18;
	private static final int COST_FONT_SIZE = 52;
	private static final int[] TEXT_LENGTH_BOUNDS = {60, 120, 180};
	private static final int[] TEXT_FONT_SIZES = {26, 24, 22, 20};
	private static final int STATS_FONT_SIZE = 22;

	private static final Rectangle ART_BOX = new Rectangle(17, 66, 446, 446);
	private static final Rectangle NAME_BOX = new Rectangle(22, 22, 360, 43);
	private static final Rectangle COST_BOX = new Rectangle(390, 25, 62, 60);
	private static final Rectangle TYPE_BOX = new Rectangle(22, 65, 357, 23);
	private static final Rectangle FACTION_BOX = new Rectangle(105, 486, 354, 23);
	private static final Rectangle DESC_BOX = new Rectangle(22, 510, 439, 125);
	private static final Rectangle DESC_BOX_NO_STATS = new Rectangle(22, 510, 439, 150);
	private static final Rectangle STATS_BOX = new Rectangle(19, 630, 442, 27);

	private static void drawTextbox(Graphics canvas, String text, Rectangle rect, int s, boolean bold, AttributeSet style) throws InterruptedException {
		//TODO: Move to a non-swing dependent rendering method so I don't need to use ad-hoc synchronization to not break Swing's threading policy.
		//Hack is currently used to get nice text wrapping by default.
		CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			JTextPane textBox = new JTextPane();
			textBox.setMargin(new Insets(1, 1, 1, 1));
			try {
				StyledDocument textData = textBox.getStyledDocument();
				textData.insertString(0, text, style);
				textData.setParagraphAttributes(textData.getLength(), 1, style, false);
			} catch (BadLocationException e) {
				//Should never occur.
				throw new RuntimeException(e);
			}
			textBox.setLocation(rect.getLocation());
			textBox.setBounds(rect);
			textBox.setForeground(canvas.getColor());
			textBox.setBackground(TRANSPARENT);
			textBox.setFont(new Font("Lucida Sans Typewriter", bold ? Font.BOLD : Font.PLAIN, s));
			canvas.translate(rect.x, rect.y);
			textBox.paint(canvas);
			canvas.translate(-rect.x, -rect.y);
			latch.countDown();
		});
		latch.await();
	}

	static BufferedImage generateImage(Card card) throws InterruptedException {
		BufferedImage generatedImage = new BufferedImage(ImgConstants.CARD_SIZE_X, ImgConstants.CARD_SIZE_Y, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D canvas = generatedImage.createGraphics();

		if (card.image.exists()) {
			try {
				BufferedImage cardArt = ImageIO.read(card.image);
				canvas.drawImage(cardArt, ART_BOX.x, ART_BOX.y, ART_BOX.width, ART_BOX.height, null);
			} catch (IOException e) {
				Logger.tag(LTags.IMG_GEN.tag).warn("Could not read card art for {} in {}.", card.name, card.image.getAbsolutePath());
			}
		} else {
			Logger.tag(LTags.IMG_GEN.tag).info("Card art for {} does not exist in {}.", card.name, card.image.getAbsolutePath());
		}

		canvas.drawImage(ImgStore.getLocalImage("images/cardframes.png"), 0, 0, ImgConstants.CARD_SIZE_X, ImgConstants.CARD_SIZE_Y, null);

		MutableAttributeSet titleStyle = LEFT_ALIGN;
		if (card.rarity == Rarity.RARE) {
			titleStyle = RARE_TITLE;
		} else if (card.rarity == Rarity.UNCOMMON) {
			titleStyle = UC_TITLE;
		}
		drawTextbox(canvas, card.name, NAME_BOX, HEADER_FONT_SIZE, true, titleStyle);


		drawTextbox(canvas, String.valueOf(card.cost), COST_BOX, COST_FONT_SIZE, true, CENTER_ALIGN);
		drawTextbox(canvas, card.type, TYPE_BOX, SUBHEADER_FONT_SIZE, true, LEFT_ALIGN);
		drawTextbox(canvas, card.archetype.niceName, FACTION_BOX, SUBHEADER_FONT_SIZE, true,  RIGHT_ALIGN);

		String textBody = "";
		int textSize = card.textSize;
		if (!card.cardText.isEmpty()) {
			textBody = card.cardText;
			if (textSize == -1) {
				int textLength = card.cardText.length();
				for (int sizeTier = 0; sizeTier < TEXT_FONT_SIZES.length; sizeTier++) {
					textSize = TEXT_FONT_SIZES[sizeTier];
					if (sizeTier == TEXT_LENGTH_BOUNDS.length || textLength < TEXT_LENGTH_BOUNDS[sizeTier]) {
						break;
					}
				}
			}
		} else if (!card.keywords.isEmpty()){
			StringBuilder textBodyBuilder = new StringBuilder();
			boolean first = true;
			for (String keyword : card.keywords) {
				if (!first) {
					textBodyBuilder.append("\n");
				}
				textBodyBuilder.append(keyword);
				first = false;
			}
			textBody = textBodyBuilder.toString();
			if (textSize == -1) {
				textSize = TEXT_FONT_SIZES[Integer.min(card.keywords.size() - 1, TEXT_FONT_SIZES.length - 1)];
			}
		}

		StringBuilder statTextBuilder = new StringBuilder();
		boolean has_stat = false;
		if (card.power.data >= 0) {
			statTextBuilder.append("POWER-");
			statTextBuilder.append(card.power.string);
			has_stat = true;
		}

		if (card.health.data >= 0) {
			if (has_stat) {
				statTextBuilder.append(" | ");
			}
			statTextBuilder.append("HEALTH-");
			statTextBuilder.append(card.health.string);
			has_stat = true;
		}

		if (card.slots.data >= 0) {
			if (has_stat) {
				statTextBuilder.append(" | ");
			}
			statTextBuilder.append("SLOTS-");
			statTextBuilder.append(card.slots.string);
			has_stat = true;
		}

		if (card.channel.data >= 0) {
			if (has_stat) {
				statTextBuilder.append(" | ");
			}
			statTextBuilder.append("CHANNEL-");
			statTextBuilder.append(card.channel.string);
			has_stat = true;
		}

		if (card.charges.data >= 0) {
			if (has_stat) {
				statTextBuilder.append(" | ");
			}
			statTextBuilder.append("CHARGES-");
			statTextBuilder.append(card.charges.string);
			has_stat = true;
		}

		if (has_stat) {
			drawTextbox(canvas, textBody, DESC_BOX, textSize, false, LEFT_ALIGN);
			canvas.drawImage(ImgStore.getLocalImage("images/cardstatshadow.png"), 0, 0, ImgConstants.CARD_SIZE_X, ImgConstants.CARD_SIZE_Y, null);
			drawTextbox(canvas, statTextBuilder.toString(), STATS_BOX, STATS_FONT_SIZE, false, CENTER_ALIGN);
		} else {
			drawTextbox(canvas, textBody, DESC_BOX_NO_STATS, textSize, false, LEFT_ALIGN);
		}

		canvas.drawImage(ImgStore.getLocalImage("images/cardborder.png"), 0, 0, ImgConstants.CARD_SIZE_X, ImgConstants.CARD_SIZE_Y, null);

		return generatedImage;
	}
}
