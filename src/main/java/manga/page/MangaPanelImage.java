package manga.page;

import java.awt.image.BufferedImage;

/**
 * @author PuiWa
 *
 */
public class MangaPanelImage {
	private BufferedImage originalImage;
	private BufferedImage subImage;
	
	public MangaPanelImage(BufferedImage image) {
		this.originalImage = image;
	}
	
	public void setSubImage(BufferedImage subImage) {
		this.subImage = subImage;
	}
}
