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
	private Mat keyFrameImg;
	private Rectangle2D panelBound;
	private ArrayList<Face> detectedFaces;
	private double scaleRatio;
	private ArrayList<Face> relocatedFaces;
	private BufferedImage subImageAsBufferedImage;
	private Mat subImageAsMat;
	private ImageLayer layer;	// the layer the image belong to
	private int cropStartX;
	private int cropStartY;
	
	private static int imgCount = 0;

	public MangaPanelImage() {
		// TODO Auto-generated constructor stub
	}
	
	public MangaPanelImage(Composition comp, Mat keyFrameImg, Rectangle2D panelBound, ArrayList<Face> detectedFaces) {
		this.keyFrameImg = keyFrameImg;
		this.panelBound = panelBound;
		this.detectedFaces = detectedFaces;
		this.relocatedFaces = new ArrayList<>();
		this.layer = comp.addNewEmptyLayer("Image "+(++imgCount), false);
	}
	
	public BufferedImage getSubImageAsBufferedImage() {
		if (subImageAsBufferedImage == null) {
			subImageAsBufferedImage = Mat2BufferedImage(getSubImgAsMat());
		}
		return subImageAsBufferedImage;
	}
	
	public Mat getSubImgAsMat() {
		if (subImageAsMat == null) {
			subImageAsMat = scaleAndCropSubImage(keyFrameImg, panelBound, detectedFaces);
		}
		return subImageAsMat;
	}
	
	public ArrayList<Face> getRelocatedFaces() {
		return relocatedFaces;
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
	private Mat scaleAndCropSubImage(Mat image, Rectangle2D panelBound, ArrayList<Face> detectedFaces) {
		Mat processedImg = scaleImage(image, panelBound);
		scaleRatio = (double) processedImg.width() / (double) image.width();
		processedImg = cropImage(processedImg, scaleRatio, panelBound, detectedFaces); 
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
		// resize detected faces with aspect ratio
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
		
		// assign top-left coordinates of the region to crop for relocating faces
		this.cropStartX = cropRect.x;
		this.cropStartY = cropRect.y;
		relocateFaces(resizedFaces);
		
		Mat imgAfterCrop = scaledImg.submat(cropRect);
		
		// testing
//		String filename = String.format("crop%d.jpg", imgCount);
//		System.out.println(String.format("Writing %s", filename));
//		Imgcodecs.imwrite(filename, imgAfterCrop);
		
		return imgAfterCrop;
	}
	
	/**
	 * update coordinates of rescaled faces,
	 * according to scale ratio and position 
	 * of cropped region relative to the whole image
	 * @param resizedFaces
	 */
	private void relocateFaces(ArrayList<Face> resizedFaces) {
		if (resizedFaces.size() > 0) {
			for (Face resizedFace : resizedFaces) {
				Face relocatedFace = new Face(resizedFace, cropStartX, cropStartY);
				relocatedFaces.add(relocatedFace);
			}
		}
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

	public Face relocateFace(Face face) {
		// scale face to panel-to-image ratio
		Face scaledFace = new Face(face, scaleRatio);
		// Update coordinates with respect to the cropped region relative to the whole image,
		// which is to reset the coordinates of face with top-left coordinates of crop region
		// as the origin. 
		Face relocatedFace = new Face(scaledFace, cropStartX, cropStartY);
		return relocatedFace;
	}
}
