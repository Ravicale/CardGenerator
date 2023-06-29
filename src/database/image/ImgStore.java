package database.image;

import application.LTags;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import database.card.Card;
import gui.UIConstants;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImgStore {
	private ImgStore() {}
	public static final String CARD_IMAGE_PATH = "images/";
	private static final Map<String, BufferedImage> cachedLocalImages = new HashMap<>(10);

	/**
	 * Returns a BufferedImage from the jar file based on the provided path.
	 *
	 * @param path The resource path to load/get.
	 * @return A BufferedImage corresponding to the image from the path.
	 */
	public static BufferedImage getLocalImage(String path) {
		if (cachedLocalImages.containsKey(path)) {
			return cachedLocalImages.get(path);
		}

		URL resourceId = ImgStore.class.getClassLoader().getResource(path);
		if (resourceId != null) {
			try {
				BufferedImage image = ImageIO.read(resourceId);
				cachedLocalImages.put(path, image);
				return image;
			} catch (IOException e) {
				//If this fires, then the path that was provided was incorrect or something is missing from the jar.
				throw new RuntimeException(e);
			}
		}

		//If this fires, then the path that was provided was incorrect or something is missing from the jar.
		throw new NullPointerException("No URL created from path: " + path);
	}

	/**
	 * Returns an image icon from the jar file based on the provided path.
	 *
	 * @param path The resource path to load/get.
	 * @return An ImageIcon corresponding to the image from the path.
	 */
	public static ImageIcon getLocalIcon(String path) {
		return new ScaleableImageIcon(getLocalImage(path));
	}

	/**
	 * Returns an image icon from the jar file based on the provided path with the desired scaling.
	 *
	 * @param path The resource path to load/get.
	 * @param size The size to rescale the image to.
	 * @return An ImageIcon corresponding to the image from the path.
	 */
	public static ImageIcon getLocalIcon(String path, Dimension size) {
		ScaleableImageIcon icon = (ScaleableImageIcon) getLocalIcon(path);
		icon.setIconWidth(size.width);
		icon.setIconHeight(size.height);
		return icon;
	}

	public static synchronized ImageIcon getCardImage(Card card) {
		try {
			BufferedImage rawImage = ImgGenerator.generateImage(card);
			ScaleableImageIcon icon = new ScaleableImageIcon(rawImage);
			icon.setIconWidth(UIConstants.CARD_IMAGE_SIZE.width);
			icon.setIconHeight(UIConstants.CARD_IMAGE_SIZE.height);
			return icon;
		} catch (InterruptedException e) {
			return UIConstants.DEFAULT_CARD_ICON;
		}
	}
	public static synchronized void writeDeckImage(List<Card> cards, File filepath) {
		final int DECK_COUNT_X = 10;
		final int DECK_SIZE_X = ImgConstants.CARD_SIZE_X * DECK_COUNT_X;
		final int DECK_COUNT_Y = 7;
		final int DECK_SIZE_Y = ImgConstants.CARD_SIZE_Y * DECK_COUNT_Y;

		PngWriter currDeckImage = null;
		try {
			Logger.tag(LTags.DECK_IMAGE.tag).info("Writing new deck image with {} cards.", cards.size());

			final ImageInfo imageInfo = new ImageInfo(DECK_SIZE_X, DECK_SIZE_Y, 8, false);
			final ImageLineInt writerLine = new ImageLineInt(imageInfo);
			final DataBuffer[] activeBuffers = new DataBuffer[DECK_COUNT_X];

			boolean firstCard = true;
			Iterator<Card> cardIterator = cards.iterator();
			Card card = null;
			BufferedImage image = null;

			int pageNum = 0;
			final String parentPath = filepath.getParent() + File.separator;
			final String baseFilename = filepath.getName().substring(0, filepath.getName().lastIndexOf('.'));
			final String ext = filepath.getName().substring(filepath.getName().lastIndexOf('.'));

			while (cardIterator.hasNext()) {
				if (firstCard) {
					card = cardIterator.next();
					image = ImgGenerator.generateImage(card);
					firstCard = false;
				}

				//Each loop iteration writes a png 'page'.
				{
					File currFile = new File(parentPath + baseFilename + "_" + pageNum++ + ext);
					Logger.tag(LTags.DECK_IMAGE.tag).info("Writing to {}", filepath.getName());
					currDeckImage = new PngWriter(currFile, imageInfo);
				}

				//Whenever we reach a row the current cards don't extend into, get the new active buffers.
				for (int y = 0; y < DECK_SIZE_Y; y++) {
					if (y % ImgConstants.CARD_SIZE_Y == 0) {
						Logger.tag(LTags.DECK_IMAGE.tag).debug("Getting image buffers.");
						for (int index = 0; index < DECK_COUNT_X; index++) {
							if (card == null) { //When out of cards, clear buffers and print blackspace.
								Logger.tag(LTags.DECK_IMAGE.tag).debug("Getting null buffer.");
								activeBuffers[index] = null;
							} else {
								Logger.tag(LTags.DECK_IMAGE.tag).debug("Getting buffer for card {}", card.name);

								activeBuffers[index] = image.getRaster().getDataBuffer();
								if (cardIterator.hasNext()) {
									card = cardIterator.next();
									image = ImgGenerator.generateImage(card);
								} else {
									card = null;
								}
							}
						}
					}

					//Write the current row in the image.
					for (int cardX = 0; cardX < DECK_COUNT_X; cardX++) {
						DataBuffer cardBuffer = activeBuffers[cardX];
						if (cardBuffer != null) {
							for (int x = 0; x < ImgConstants.CARD_SIZE_X; x++) {
								int outIndex = 3 * (x + cardX * ImgConstants.CARD_SIZE_X);
								int cardIndex = 3 * (x + ((y % ImgConstants.CARD_SIZE_Y) * ImgConstants.CARD_SIZE_X));
								writerLine.getScanline()[outIndex] = cardBuffer.getElem(cardIndex + 2);     //R
								writerLine.getScanline()[outIndex + 1] = cardBuffer.getElem(cardIndex + 1); //G
								writerLine.getScanline()[outIndex + 2] = cardBuffer.getElem(cardIndex);        //B
							}
						} else {
							for (int x = 3 * cardX * ImgConstants.CARD_SIZE_X; x < 3 * (cardX * ImgConstants.CARD_SIZE_X + ImgConstants.CARD_SIZE_X); x++) {
								writerLine.getScanline()[x] = 0;
							}
						}
					}
					currDeckImage.writeRow(writerLine);
				}

				Logger.tag(LTags.DECK_IMAGE.tag).info("Deck image written.");
				currDeckImage.end();
			}
			currDeckImage = null;
		} catch (Exception e) {
			Logger.tag(LTags.DECK_IMAGE.tag).error(e, "Unable to write image.");
		} finally {
			if (currDeckImage != null) {
				currDeckImage.close();
			}
		}
	}

	/**
	 * Hacky class that overrides the height/width of images when drawn to allow them to look
	 * sharp on high DPI displays, and to allow for easy scaling of size.
	 */
	private static class ScaleableImageIcon extends ImageIcon {
		int width;
		int height;

		private static final Map<RenderingHints.Key, Object> renderingHintsMap;

		static {
			Map<RenderingHints.Key, Object> rh = new HashMap<>(2);
			rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			rh.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			renderingHintsMap = Collections.unmodifiableMap(rh);
		}

		public ScaleableImageIcon(Image image) {
			super(image);
			width = super.getIconWidth();
			height = super.getIconHeight();
		}

		@Override
		public int getIconWidth() {
			return width;
		}

		public void setIconHeight(int height) {
			this.height = height;
		}

		public void setIconWidth(int width) {
			this.width = width;
		}

		@Override
		public int getIconHeight() {
			return height;
		}

		@Override
		public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHints(renderingHintsMap);
			g2.drawImage(getImage(), x, y, width, height, null);
		}
	}
}