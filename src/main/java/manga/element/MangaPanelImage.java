package manga.element;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.assertj.swing.hierarchy.NewHierarchy;
import org.mockito.internal.matchers.Null;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import manga.detect.Face;
import manga.detect.Speaker;
import manga.process.video.KeyFrame;
import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

/**
 * @author PuiWa
 *
 */
public class MangaPanelImage {
	private KeyFrame keyFrame;
	private Rectangle2D panelBound;
	private ArrayList<Face> faces;
//	private Mat subImage;
	private ImageLayer layer;	// the layer the image belong to
	
	private static int imgCount = 0;

	public MangaPanelImage() {
		// TODO Auto-generated constructor stub
	}
	
	public MangaPanelImage(Composition comp, KeyFrame keyFrame, Rectangle2D panelBound, ArrayList<Face> faces) {
		this.keyFrame = keyFrame;
//		this.subImage = scaleAndCropSubImage(keyFrame.getImg(), panelBound, faces);
		this.panelBound = panelBound;
		this.faces = faces;
		this.layer = comp.addNewEmptyLayer("Image "+(++imgCount), false);
	}
	
	public BufferedImage getSubImage() {
		return Mat2BufferedImage(scaleAndCropSubImage(keyFrame.getImg(), panelBound, faces));
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
	private Mat scaleAndCropSubImage(Mat image, Rectangle2D panelBound, ArrayList<Face> faces) {
		Mat processedImg = scaleImage(image, panelBound);
		double ratio = (double) processedImg.width() / (double) image.width();
		processedImg = cropImage(processedImg, ratio, panelBound, faces); 
		return processedImg;
	}
	
	private Mat scaleImage(Mat image, Rectangle2D panelBound) {
		double panelW = panelBound.getWidth();
		double panelH = panelBound.getHeight();
		
		double ratio = 1.0;

		// ratio of image height to panel height or image width to panel width, whichever shorter
		if (image.width() > image.height()) {
			ratio = panelH / image.height();
		} else {
			ratio = panelW / image.width();
		}
		
		Mat scaledImage = new Mat();
		Imgproc.resize(image, scaledImage, new Size(image.width()*ratio, image.height()*ratio));
		
		return scaledImage;
	}
	
	/**
     * Image cropped to include speakers' face.
     * If out of bound, crop to center within the region bounded by all speakers' faces
	 * @param scaledImg		image scaled to fit panel
	 * @param ratio			scale ratio (scaled image to original image)
	 * @param panelBound	the panel bound
	 * @param faces			possible speakers' faces
	 * @return cropped image
	 */
	private Mat cropImage(Mat scaledImg, double ratio, Rectangle2D panelBound, ArrayList<Face> faces) {
		ArrayList<Face> resizedFaces = new ArrayList<>();
		for (Face face : faces) {
			resizedFaces.add(new Face(face, ratio));
		}
		
		// initial setting, if no speaker detected, crop from center of image
		int imgCentreX = (int) (scaledImg.width() / 2);
		int imgCentreY = (int) (scaledImg.height() / 2);
		int newImgTopLeftX = (int) (imgCentreX - panelBound.getWidth() / 2);
		int newImgTopLeftY = (int) (imgCentreY - panelBound.getHeight() / 2);
		
		Rect speakerCrop = null;
		
		if (resizedFaces.size() > 0) {
			// calculate image top-left coordinates and width to crop
			int minImgX = scaledImg.width();
			int minImgY = scaledImg.height();
			int maxImgX = 0;
			int maxImgY = 0;
			
			for (Face face : resizedFaces) {
				if (face.getBound().x<minImgX) {
					minImgX = face.getBound().x;
				}
				
				int faceTopRightX = face.getBound().x+face.getBound().width;
				if (faceTopRightX>maxImgX) {
					maxImgX = faceTopRightX;
				}
				
				// testing
				Imgproc.rectangle(scaledImg, new Point(face.getBound().x, face.getBound().y), 
						new Point(face.getBound().x+face.getBound().width, face.getBound().y+face.getBound().height), 
						new Scalar(0, 0, 255));
			}
			
			speakerCrop = new Rect(minImgX, 0, maxImgX-minImgX, scaledImg.height());
		}
		
		if (speakerCrop != null) {
			imgCentreX = (int) (speakerCrop.x + speakerCrop.width / 2);
			imgCentreY = (int) (speakerCrop.y + speakerCrop.height / 2);
			newImgTopLeftX = (int) (speakerCrop.x);
			newImgTopLeftY = (int) (speakerCrop.y);
			
			if (newImgTopLeftX + panelBound.getWidth() > scaledImg.width()) {
				newImgTopLeftX = (int) (scaledImg.width()-panelBound.getWidth());
			}
		}
		
		Rect cropRect = new Rect(newImgTopLeftX, newImgTopLeftY, (int) panelBound.getWidth(), (int) panelBound.getHeight());
		Mat imgAfterCrop = scaledImg.submat(cropRect);
		
		// testing
		String filename = String.format("crop%d.jpg", imgCount);
		System.out.println(String.format("Writing %s", filename));
		Imgcodecs.imwrite(filename, imgAfterCrop);
		
		return imgAfterCrop;
	}
	
	public KeyFrame getKeyFrame() {
		return keyFrame;
	}
	
	/**
	 * @param m Mat of extracted frame
	 * @return BufferedImage of Mat
	 */
	private static BufferedImage Mat2BufferedImage(Mat m) {
		// Fastest code
		// output can be assigned either to a BufferedImage or to an Image
		
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if ( m.channels() > 1 ) {
		    type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte [] b = new byte[bufferSize];
		m.get(0,0,b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);  
		return image;
	}
}
