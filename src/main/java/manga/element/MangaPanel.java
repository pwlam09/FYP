package manga.element;

import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import manga.detect.Face;
import manga.detect.Speaker;
import manga.detect.SpeakerDetector;
import manga.process.subtitle.Subtitle;
import manga.process.video.KeyFrame;
import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.MangaText;
import pixelitor.selection.Selection;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapes.WordBalloon;
import pixelitor.tools.shapestool.BasicStrokeJoin;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.tools.shapestool.TwoPointBasedPaint;

/**
 * @author PuiWa
 *
 */
public class MangaPanel {
	private MangaPage page;	// the page the panel belongs to
	private ImageLayer layer;	// a panel refers to a layer
	private MangaPanelImage panelImg;
	private Rectangle2D bound;	//bounding rectangle of panel
	private ArrayList<MangaBalloon> balloons;
	private KeyFrame keyFrame;
	private ArrayList<Subtitle> subtitles;
	
	private static int imgCounter = 0; // for testing
	
	private static int panelCount = 0;
	
	public MangaPanel(MangaPage page, KeyFrame keyFrame, ArrayList<Subtitle> subtitles) {
		this.page = page;
		this.layer = page.getComp().addNewEmptyLayer("Panel "+(++panelCount), false);
		this.panelImg = new MangaPanelImage();
		this.bound = new Rectangle2D.Float();
		this.balloons = new ArrayList<>();
		this.keyFrame = keyFrame;
		this.subtitles = subtitles;
	}

	public void setBound(Rectangle2D boundingRect) {
		this.bound = boundingRect;
	}
	
	public ImageLayer getLayer() {
		return this.layer;
	}
	
    /**
     * Image fit to panel bounding rectangle, starts from top-left corner.
     * The image will be drawn onto a new layer, bounded by the area within selection.
     */
    public void fitImageToPanelBound() {
        if (keyFrame != null) {
        	// setup selection with the bounding rectangle of MangaPanel
        	Composition comp = page.getComp();
            Selection selection = new Selection(bound, comp.getIC());
            comp.setNewSelection(selection);
            
	        panelImg = new MangaPanelImage(comp, keyFrame.getImg(), bound, SpeakerDetector.detectFaces(keyFrame.getImg()));

	        // testing
    		String filename = String.format("output%d.jpg", ++imgCounter);
    		System.out.println(String.format("Writing %s", filename));
    		Imgcodecs.imwrite(filename, keyFrame.getImg());
	        
	        // image to new layer
	        ImageLayer layer = panelImg.getLayer();
	        
	        layer.setImageWithSelection(panelImg.getSubImageAsBufferedImage());
	        
	        // Deselect. Don't use selection.die() directly, which leads to error
	        comp.deselect(AddToHistory.NO);
        }
    }
    
    public void addMangaBalloons() {
		String linkedSubtitlesText = "";
		
		if (subtitles.size() > 0) {
			// assume all from same speaker
			ArrayList<Subtitle> subtitlesOfSameSpeaker = new ArrayList<>();
			subtitlesOfSameSpeaker = subtitles;
			
			linkedSubtitlesText = Subtitle.getLinkedSubtitlesText(subtitlesOfSameSpeaker);
//			System.out.println("linkedSubtitlesText: "+linkedSubtitlesText);
			
			// select the first subtitle with non-null speaker (if more than one) as reference
			// or the speaker of the only subtitle
			// both case may have null speaker
			Speaker selectedSpeakerRef = null;
			if (subtitlesOfSameSpeaker.size() > 1) {
				for (Subtitle subtitle : subtitlesOfSameSpeaker) {
					if (subtitle.getSpeaker() != null) {
						selectedSpeakerRef = subtitle.getSpeaker();
						break;
					}
				}
			} else {
				selectedSpeakerRef = subtitlesOfSameSpeaker.get(0).getSpeaker();
			}
			
//				System.out.println("selectedSpeakerRef: "+selectedSpeakerRef);
			
			Face relocatedSpeakerFace = null;
			Face matchedSpeakerFace = null;
			
			if (selectedSpeakerRef != null) {
				relocatedSpeakerFace = panelImg.relocateFace(selectedSpeakerRef.getFace());
			}

			// match speaker's face to face of current panel image.
			// if not found, the largest face of panel image detected will be used
			matchedSpeakerFace = matchSpeakerFace(relocatedSpeakerFace, panelImg.getRelocatedFaces());
//				System.out.println("matchedSpeakerFace: "+matchedSpeakerFace);
			
	    	Composition comp = page.getComp();
	    	
	    	UserDrag balloonDrag = null;
	    	
			// this font will be used to calculate the area required for displaying the subtitle text in this font
	    	Font defaultFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
			
	    	// calculate the balloon size for displaying subtitle text
	    	WordBalloon balloonRef = MangaBalloon.calculateInitWordBalloonRef(bound.getX(), bound.getY(), defaultFont, linkedSubtitlesText);
//				System.out.println("balloonRef bound after cal textbound: "+balloonRef.getBounds2D());
	    	
//		    	System.out.println("balloonRef before cal: "+balloonRef.getBounds2D());
			balloonRef = calculateBalloonPos(balloonRef, matchedSpeakerFace, panelImg.getRelocatedFaces());
//				System.out.println("balloonRef after cal: "+balloonRef.getBounds2D());
			
			Point balloonTailPt = null;
			
			// testing
			Mat testOutputImg = panelImg.getSubImgAsMat();
			// testing
			
			if (matchedSpeakerFace == null) {
				balloonTailPt = calculateBalllonTailPos(null);
			} else {
				if (matchedSpeakerFace.getMouth() == null) {
					// if mouth position not available, point to face
					balloonTailPt = calculateBalllonTailPos(matchedSpeakerFace.getBound());

					// testing
					// draw face rect
					Rect faceBoundToDraw = matchedSpeakerFace.getBound();
					Imgproc.rectangle(testOutputImg, new Point(faceBoundToDraw.x, faceBoundToDraw.y), 
							new Point(faceBoundToDraw.x+faceBoundToDraw.width, faceBoundToDraw.y+faceBoundToDraw.height), new Scalar(0, 255, 0));
					Imgproc.line(testOutputImg, new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), 
							new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), new Scalar(0, 255, 0), 5);
					// testing
				} else {
					balloonTailPt = calculateBalllonTailPos(matchedSpeakerFace.getMouth().getBound());

					// testing						
					// draw face rect
					Rect faceBoundToDraw = matchedSpeakerFace.getBound();
					Imgproc.rectangle(testOutputImg, new Point(faceBoundToDraw.x, faceBoundToDraw.y), 
							new Point(faceBoundToDraw.x+faceBoundToDraw.width, faceBoundToDraw.y+faceBoundToDraw.height), new Scalar(0, 255, 0));
					Imgproc.line(testOutputImg, new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), 
							new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), new Scalar(0, 255, 0), 5);
					// draw mouth rect
					Rect mouthBoundToDraw = matchedSpeakerFace.getMouth().getBound();
					Imgproc.rectangle(testOutputImg, new Point(mouthBoundToDraw.x, mouthBoundToDraw.y), 
							new Point(mouthBoundToDraw.x+mouthBoundToDraw.width, mouthBoundToDraw.y+mouthBoundToDraw.height), new Scalar(255, 0, 0));
					Imgproc.line(testOutputImg, new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), 
							new Point(balloonTailPt.x-bound.getX(), balloonTailPt.y-bound.getY()), new Scalar(255, 0, 0), 5);
					// testing
				}
			}
			
//				System.out.println("final balloonRef before drag: "+balloonRef.getBounds2D());
			
			// determine balloon drawing direction
			balloonDrag = createUserDragFromShape(balloonRef, balloonTailPt);
//				System.out.println("balloonDrag: "+balloonDrag);
			
			// update balloon text bound with drag
			balloonRef = (WordBalloon) ShapeType.WORDBALLOON.getShape(balloonDrag);
			
			// testing
			// draw balloon bound
			Rectangle2D boundToDraw = balloonRef.getBounds2D();
//				System.out.println("boundToDraw: "+boundToDraw);
			Imgproc.rectangle(testOutputImg, new Point(boundToDraw.getX()-bound.getX(), boundToDraw.getY()-bound.getY()), 
					new Point(boundToDraw.getX()-bound.getX()+Math.abs(boundToDraw.getWidth()), 
							boundToDraw.getY()-bound.getY()+Math.abs(boundToDraw.getHeight())), 
					new Scalar(0, 0, 255));
			// draw tail direction point
			Point2D dragEndPt = balloonDrag.getEndPoint();
			Imgproc.line(testOutputImg, new Point(dragEndPt.getX()-bound.getX(), dragEndPt.getY()-bound.getY()),
					new Point(dragEndPt.getX()-bound.getX(), dragEndPt.getY()-bound.getY()), new Scalar(0, 0, 255), 5);
			// testing
			
			// testing
			String filename = String.format("balloon%d.jpg", ++imgCounter);
			System.out.println(String.format("Writing %s", filename));
			Imgcodecs.imwrite(filename, testOutputImg);
			// testing
			
	    	MangaText mangaTextLayer = new MangaText(comp, balloonRef);
	    	mangaTextLayer.setAndCommitDefaultSetting(defaultFont, linkedSubtitlesText);
	    	
	        // set text translation within balloon bound
	    	mangaTextLayer.setTranslation((int)balloonRef.getTextBound2D().getX(), (int)balloonRef.getTextBound2D().getY());
	    	
	    	MangaBalloon balloon = new MangaBalloon(this, mangaTextLayer, balloonRef);
	    	balloons.add(balloon);
	    	
			// initialize shapes tool for drawing balloon
	    	ShapesTool shapesTool = Tools.SHAPES;
	        // call reset method or the previous stroke will be used
	        shapesTool.resetDrawingAndStroke();
	        shapesTool.setShapeType(ShapeType.WORDBALLOON);
	        shapesTool.setAction(ShapesAction.FILL_AND_STROKE);
	        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
	        shapesTool.setFill(TwoPointBasedPaint.BACKGROUND);
	        // reset stroke join and width to default
	        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.ROUND, 1);
	        
	        // paint balloon
	    	shapesTool.paintShapeOnIC(balloon.getBallloonLayer(), balloonDrag);
		}
    }

	/**
	 * Match the speaker face with the faces in key frame.
	 */
	private Face matchSpeakerFace(Face relocatedSpeakerFace, ArrayList<Face> relocatedFaces) {
		if (relocatedFaces.size() == 0 || relocatedSpeakerFace == null) {
			// if no match
			return null;
		} else {
//			Face matchedFace = null;
//			
//			if (relocatedSpeakerFace == null) {
//				// return largest detected face
//				double maxArea = 0.0;
//				for (Face face : relocatedFaces) {
//					if (face.getBound().area() > maxArea) {
//						maxArea = face.getBound().area();
//						matchedFace = face;
//					}
//				}
//			} else {
//				// return match with largest intersection
//				Rect speakerFaceBound = relocatedSpeakerFace.getBound();
//				Rectangle2D speakerFaceRect2D = new Rectangle2D.Double(speakerFaceBound.x, speakerFaceBound.y, speakerFaceBound.width, speakerFaceBound.height);
//				
//				double maxArea = 0.0;
//				matchedFace = relocatedFaces.get(0);
//				
//				for (Face relocatedFace : relocatedFaces) {
//					Rect currFaceBound = relocatedFace.getBound();
//					Rectangle2D relocatedFaceRect2D = new Rectangle2D.Double(currFaceBound.x, currFaceBound.y, currFaceBound.width, currFaceBound.height);
//					Rectangle2D intersection = speakerFaceRect2D.createIntersection(relocatedFaceRect2D);
//					if (intersection.getWidth() * intersection.getHeight() > maxArea) {
//						maxArea = intersection.getWidth() * intersection.getHeight();
//						matchedFace = relocatedFace;
//					}
//				}
//			}

			/**
			 *  return match with largest intersection
			 */
			Face matchedFace = null;
			
			Rect speakerFaceBound = relocatedSpeakerFace.getBound();
			Rectangle2D speakerFaceRect2D = new Rectangle2D.Double(speakerFaceBound.x, speakerFaceBound.y, speakerFaceBound.width, speakerFaceBound.height);
			
			double maxArea = 0.0;
			matchedFace = relocatedFaces.get(0);
			
			for (Face relocatedFace : relocatedFaces) {
				Rect currFaceBound = relocatedFace.getBound();
				Rectangle2D relocatedFaceRect2D = new Rectangle2D.Double(currFaceBound.x, currFaceBound.y, currFaceBound.width, currFaceBound.height);
				Rectangle2D intersection = speakerFaceRect2D.createIntersection(relocatedFaceRect2D);
				if (intersection.getWidth() * intersection.getHeight() > maxArea) {
					maxArea = intersection.getWidth() * intersection.getHeight();
					matchedFace = relocatedFace;
				}
			}
		
			return matchedFace;
		}
	}

	/**
	 * Point to center of the region
	 * @param tailRegion
	 * @return
	 */
	private Point calculateBalllonTailPos(Rect tailRegion) {
		if (tailRegion == null) {
			return null;
		}
		return new Point(tailRegion.x+tailRegion.width/2+bound.getX(), tailRegion.y+tailRegion.height/2+bound.getY());
	}

	/**
	 * Start point of the rect (i.e. top-left coordinates) will be the top-left of balloon.
	 * End point of the rect (i.e. bottom-left coordinates) will be the balloon tail position.
	 * <p>
	 * Rule to apply:
	 * 1. Not to overlap with existing balloons
	 * 2. Not to occlude speakers' faces
	 */
	private WordBalloon calculateBalloonPos(WordBalloon currBalloonRef, Face matchedSpeakerFace, ArrayList<Face> relocatedFaces) {
		WordBalloon newBalloonRef = currBalloonRef;
		double newBalloonX = newBalloonRef.getBounds2D().getX();
		double newBalloonY = newBalloonRef.getBounds2D().getY();
		double newBalloonRefW = newBalloonRef.getBounds().getWidth();
		double newBalloonRefH = newBalloonRef.getBounds().getHeight();
		
		/**
		 * greedy approach: place balloons from top-left to bottom-right
		 */
		if (balloons.size() > 0) {
			WordBalloon previousBalloonRef = balloons.get(balloons.size()-1).getBalloonRef();
			Rectangle2D previousWordBound = previousBalloonRef.getBounds2D();
			newBalloonX = previousWordBound.getMaxX();
			if (previousWordBound.getMaxX() + newBalloonRef.getBounds2D().getWidth() < bound.getMaxX()) {
				// place balloon next to previous balloon
				newBalloonY = previousWordBound.getY();
			} else {
				// place balloon below all balloons
				newBalloonX = bound.getX();
				double maxAvailableY = 0.0;
				for (int j=0; j<balloons.size(); j++) {
					Rectangle2D tempBalloonRefBound = balloons.get(j).getBalloonRef().getBounds(); 
					if (tempBalloonRefBound.getMaxY() > maxAvailableY) {
						maxAvailableY = tempBalloonRefBound.getMaxY();
					}
				}
				newBalloonY = maxAvailableY;
			}
			newBalloonRef = new WordBalloon(newBalloonX, newBalloonY, newBalloonRefW, newBalloonRefH);
		}
		
		/**
		 * avoid all possible speakers' faces in the panel, 
		 * not using because only one speaker face assumed at a time
		 */
//		for (Face face : relocatedFaces) {
//			Rect faceRect = face.getBound();
//			System.out.println("current resized face rect: "+face.getBound());
//			// relocate face coordinates based on panel position
//			double faceConvertedRectX = faceRect.x+bound.getX();
//			double faceConvertedRectY = faceRect.y+bound.getY();
//			Rectangle2D faceConvertedRect = new Rectangle2D.Double(faceConvertedRectX, faceConvertedRectY, faceRect.width, faceRect.height);
//			
////			System.out.println("faceConvertedRect: "+faceConvertedRect);
////			System.out.println("intersect with this balloon? "+newBalloonRef.intersects(faceConvertedRect));
//			
//			// if face is within panel bound
//			if (bound.intersects(faceConvertedRect)) {
//				System.out.println("intersected face rect: "+faceConvertedRect);
//				// if balloon occludes face
//				if (newBalloonRef.intersects(faceConvertedRect)) {
////					Rectangle2D intersection = newBalloonRef.getBounds2D().createIntersection(faceConvertedRect);
////					if (Math.abs(faceConvertedRect.getMaxX()-bound.getMaxX()) > Math.abs(faceConvertedRect.getMaxY()-bound.getMaxY())) {
////						newBalloonX = faceConvertedRect.getMaxX();
////					} else {
////						newBalloonY = faceConvertedRect.getMaxY();
////					}					
//					if (bound.getMaxX()-faceConvertedRect.getMaxX() >= newBalloonRefW) {
//						// place right to face
//						newBalloonX = faceConvertedRect.getMaxX();
//					} else if (faceConvertedRect.getX()-bound.getX() >= newBalloonRefW) {
//						// place left to face
//						newBalloonX = faceConvertedRect.getX()-newBalloonRefW;
//					} else {
//						// place below face
//						newBalloonY = faceConvertedRect.getMaxY();
//					}
//				}
//				newBalloonRef = new WordBalloon(newBalloonX, newBalloonY, newBalloonRefW, newBalloonRefH);
//			}
//		}
		
		/**
		 * avoid current speaker face, assumed only one speaker
		 */
		if (matchedSpeakerFace != null) {
			Rect speakerFaceRect = matchedSpeakerFace.getBound();
//			System.out.println("current resized face rect: "+matchedSpeakerFace.getBound());
			// relocate face coordinates based on panel position
			double faceConvertedRectX = speakerFaceRect.x+bound.getX();
			double faceConvertedRectY = speakerFaceRect.y+bound.getY();
			Rectangle2D faceConvertedRect = new Rectangle2D.Double(faceConvertedRectX, faceConvertedRectY, speakerFaceRect.width, speakerFaceRect.height);
			
			// if face is within panel bound
			if (bound.intersects(faceConvertedRect)) {
//				System.out.println("intersected face rect: "+faceConvertedRect);
				// if balloon occludes face
				if (newBalloonRef.intersects(faceConvertedRect)) {		
					if (bound.getMaxX()-faceConvertedRect.getMaxX() >= newBalloonRefW) {
						// place right to face
						newBalloonX = faceConvertedRect.getMaxX();
					} else if (faceConvertedRect.getX()-bound.getX() >= newBalloonRefW) {
						// place left to face
						newBalloonX = faceConvertedRect.getX()-newBalloonRefW;
					} else {
						// place below face
						newBalloonY = faceConvertedRect.getMaxY();
					}
				}
				newBalloonRef = new WordBalloon(newBalloonX, newBalloonY, newBalloonRefW, newBalloonRefH);
			}
		}
		
		/**
		 *  ensure balloon within panel, may cover face
		 */
		if (newBalloonRef.getBounds2D().getMaxX() > bound.getMaxX()) {
			newBalloonX = bound.getMaxX()-newBalloonRefW;
			newBalloonRef = new WordBalloon(newBalloonX, newBalloonY, newBalloonRefW, newBalloonRefH);
		}
		if (newBalloonRef.getBounds2D().getMaxY() > bound.getMaxY()) {
			newBalloonY = bound.getMaxY()-newBalloonRefH;
			newBalloonRef = new WordBalloon(newBalloonX, newBalloonY, newBalloonRefW, newBalloonRefH);
		}
		
		return newBalloonRef;
	}

	public ArrayList<MangaBalloon> getBalloons() {
		return balloons;
	}
	
	public MangaPage getPage() {
		return page;
	}
	
	private static UserDrag createUserDragFromShape(WordBalloon balloonRef, Point balloonTailPt) {
		Rectangle2D balloonBound = balloonRef.getBounds2D();
		if (balloonTailPt == null) {
		    return new UserDrag(balloonBound.getX(), balloonBound.getY(), 
		    		balloonBound.getX()+balloonBound.getWidth(), balloonBound.getY()+balloonBound.getHeight());
		} else {
			// calculate the 4 points of word balloon and
			// use the closet one to the balloon tail point
			// as the end of drag
			Point topLeftPt = new Point(balloonBound.getX(), balloonBound.getY());
			Point topRightPt = new Point(balloonBound.getX()+balloonBound.getWidth(), balloonBound.getY());
			Point bottomLeftPt = new Point(balloonBound.getX(), balloonBound.getY()+balloonBound.getHeight());
			Point bottomRightPt = new Point(balloonBound.getX()+balloonBound.getWidth(), balloonBound.getY()+balloonBound.getHeight());
			
			Point[] allPts = {topLeftPt, topRightPt, bottomLeftPt, bottomRightPt};
			
			double minDiff = Double.MAX_VALUE;
			Point startPt = topLeftPt;
			Point endPt = bottomRightPt;
			
			double width = balloonBound.getWidth();
			double height = balloonBound.getHeight();
			
			for (Point pt : allPts) {
				double twoPtsDiff = calculateTwoPtsDistance(balloonTailPt, pt);
				if (twoPtsDiff < minDiff) {
					minDiff = twoPtsDiff;
					endPt = pt;
				}
			}
			
			if (endPt.equals(topLeftPt)) {
				startPt = bottomRightPt;
				width = -width;
				height = -height;
			} else if (endPt.equals(topRightPt)) {
				startPt = bottomLeftPt;
				height = -height;
			} else if (endPt.equals(bottomLeftPt)) {
				startPt = topRightPt;
				width = -width;
			}
			
		    return new UserDrag(startPt.x, startPt.y, startPt.x+width, startPt.y+height);
		}
	}
	
	private static double calculateTwoPtsDistance(Point balloonTailPt, Point point) {
		return Math.sqrt(Math.pow(balloonTailPt.x-point.x,2)+Math.pow(balloonTailPt.y-point.y,2));
	}

	private ArrayList<WordBalloon> getAllBalloonRefs() {
		ArrayList<WordBalloon> allBalloonRefs = new ArrayList<>();
		for (MangaBalloon existingBalloon : balloons) {
			allBalloonRefs.add(existingBalloon.getBalloonRef());
		}
		return allBalloonRefs;
	}
}
