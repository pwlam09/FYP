package manga.element;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.Buffer;

import javax.sound.midi.SysexMessage;

import org.assertj.swing.junit.ant.ImageHandler;
import org.bytedeco.javacpp.opencv_dnn.Layer;

import manga.process.subtitle.SubtitleProcessor;
import manga.process.video.VideoProcessor;
import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

/**
 * @author PuiWa
 *
 */
public class MangaPanelImage {
	private long frameTimestamp;	// timestamp of the extracted frame
	private BufferedImage subImage;
	private ImageLayer layer;	// the layer the image belong to
	
	private static int imgCount = 0;

	
	public MangaPanelImage() {
		// TODO Auto-generated constructor stub
	}
	
	public MangaPanelImage(Composition comp, BufferedImage image, long framTimestamp, Rectangle2D panelBound) {
		this.frameTimestamp = framTimestamp;
		this.subImage = scaleAndCropSubImage(image, panelBound);
		imgCount++;
		this.layer = comp.addNewEmptyLayer("Image "+imgCount, false);
	}
	//	
//	/**
//	 * @param image image converted from frame
//	 */
//	public MangaPanelImage(BufferedImage image, Rectangle2D bound) {
//		this.originalImage = image;
//		this.subImage = scaleAndCropSubImage(image, bound);
//		imgLayerCount++;
//		this.layer = MangaGenerator.getActivePage().getComp().addNewEmptyLayer("Image "+imgLayerCount, false);
//	}
//	
	/**
	 * To be changed
	 * @return
	 */
	public BufferedImage getSubImage() {
		return this.subImage;
	}
	
	public ImageLayer getLayer() {
		return layer;
	}
	
	
	/**
	 * Creat subimage.
	 * The original image will first be scaled and then cropped to fit panel bound.
	 * @param image
	 * @param panelBound
	 * @return image after scale and crop
	 */
	private BufferedImage scaleAndCropSubImage(BufferedImage image, Rectangle2D panelBound) {
		BufferedImage tempImg = scaleImage(image, panelBound);
		tempImg = cropImage(tempImg, panelBound);
		return tempImg;
	}
	
	/**
	 * Helper method of scaleAndCropSubImage().
	 * Scale input image to fill MangaPanel bounding rectangle. 
	 * @param image Image to be scaled. Original image in this case 
	 * @param panelBound MangaPanel bounding rectangle
	 * @return Scaled image (enlarge if smaller than panel, shrink if bigger than panel)
	 */
	private BufferedImage scaleImage(BufferedImage image, Rectangle2D panelBound) {
		double panelW = panelBound.getWidth();
		double panelH = panelBound.getHeight();
		
		double ratio = 1.0;

		// ratio of image height to panel height or image width to panel width, whichever shorter
		if (image.getWidth() > image.getHeight()) {
			ratio = panelH / image.getHeight();
		} else {
			ratio = panelW / image.getWidth();
		}
		
		BufferedImage afterScale = new BufferedImage((int)(image.getWidth()*ratio), (int) (image.getHeight()*ratio), BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(ratio, ratio);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		afterScale = scaleOp.filter(image, afterScale);
		
		return afterScale;
	}
	
	/**
	 * Helper method of |Helper method of scaleAndCropSubImage().
	 * Crop input image to MangaPanel bound.
	 * Now only crop from image center. Will be changed later to crop according to speaker's position.
	 * @param image
	 * @param panelBound
	 * @return image cropped by panel bound
	 */
	private BufferedImage cropImage(BufferedImage image, Rectangle2D panelBound) {
		int imgCentreX = image.getWidth() / 2;
		int imgCentreY = image.getHeight() / 2;
		int newImgTopLeftX = (int) (imgCentreX - panelBound.getWidth() / 2);
		int newImgTopLeftY = (int) (imgCentreY - panelBound.getHeight() / 2);
		
		BufferedImage afterCrop = image.getSubimage(newImgTopLeftX, newImgTopLeftY, (int) panelBound.getWidth(), (int) panelBound.getHeight());
		return afterCrop;
	}

	/**
	 * To be changed
	 * @param extractFrame
	 */
	public void setSubImage(BufferedImage extractFrame) {
		this.subImage = extractFrame;
	}
	
	public long getFrameTimestamp() {
		return frameTimestamp;
	}
}
