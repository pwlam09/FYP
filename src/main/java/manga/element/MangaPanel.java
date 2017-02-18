package manga.element;

import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;

import org.jdesktop.swingx.JXTipOfTheDay.ShowOnStartupChoice;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import manga.detect.Face;
import manga.detect.Speaker;
import manga.process.subtitle.Subtitle;
import manga.process.subtitle.SubtitleProcessor;
import manga.process.video.KeyFrame;
import manga.process.video.VideoProcessor;
import pixelitor.Composition;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedMangaTextPainter;
import pixelitor.gui.ImageComponents;
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
	
	private static int imgCounter = 0; // for testing
	
	private static int panelCount = 0;
	
	public MangaPanel(MangaPage page) {
		this.page = page;
		this.layer = page.getComp().addNewEmptyLayer("Panel "+(++panelCount), false);
		this.panelImg = new MangaPanelImage();
		this.bound = new Rectangle2D.Float();
		this.balloons = new ArrayList<>();
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
    public void fitImageToPanelBound(KeyFrame keyFrame) {
        if (keyFrame != null) {
        	// setup selection with the bounding rectangle of MangaPanel
        	Composition comp = page.getComp();
            Selection selection = new Selection(bound, comp.getIC());
            comp.setNewSelection(selection);
            
            ArrayList<Face> speakerFaces = new ArrayList<>();
            for (Subtitle subtitle : SubtitleProcessor.getAllSubtitles()) {
            	if (subtitle.getsTime() >= keyFrame.getsShotTimestamp() && subtitle.geteTime() <= keyFrame.geteShotTimestamp()) {
            		if (subtitle.getSpeaker() != null) {
                		speakerFaces.add(subtitle.getSpeaker().getFace());
                		
                		// testing
            			Rect speakerFaceBound = subtitle.getSpeaker().getFace().getBound();
                		Imgproc.rectangle(keyFrame.getImg(), new Point(speakerFaceBound.x, speakerFaceBound.y), 
                				new Point(speakerFaceBound.x+speakerFaceBound.width, speakerFaceBound.y+speakerFaceBound.height), 
                				new Scalar(0, 255, 0));
            		}
            	}
            }
            
	        panelImg = new MangaPanelImage(comp, keyFrame, bound, speakerFaces);

	        // testing
    		String filename = String.format("output%d.jpg", ++imgCounter);
    		System.out.println(String.format("Writing %s", filename));
    		Imgcodecs.imwrite(filename, keyFrame.getImg());
	        
	        // image to new layer
	        ImageLayer layer = panelImg.getLayer();
	        
	        layer.setImageWithSelection(panelImg.getSubImage());
	        
	        // Deselect. Don't use selection.die() directly, which leads to error
	        comp.deselect(AddToHistory.NO);
        }
    }
    
    public void addMangaBalloons() {
		KeyFrame keyFrame = panelImg.getKeyFrame();
//		if (MangaGenerator.getCurrTimestamp() != 0) {
//			MangaGenerator.setCurrTimestamp(keyFrame.getsShotTimestamp());
//		}
		System.out.println("current timestamp for balloon: "+MangaGenerator.getCurrTimestamp());
		System.out.println("end timestamp for shot: "+keyFrame.geteShotTimestamp());
		ArrayList<Subtitle> subtitles = SubtitleProcessor.getSubtitles(MangaGenerator.getCurrTimestamp(), keyFrame.geteShotTimestamp());
		
		System.out.println("substitles.size(): "+subtitles.size());
		
		MangaGenerator.setCurrTimestamp(keyFrame.geteShotTimestamp());
    	
		String linkedSubtitlesText = "";
		
		if (subtitles.size() != 0) {
			linkedSubtitlesText = Subtitle.getLinkedSubtitlesText(subtitles);
	    	
	    	Composition comp = page.getComp();

	    	UserDrag balloonDrag = new UserDrag(bound.getX(), bound.getY(), bound.getX()+100, bound.getY()+100);
	    	WordBalloon balloonRef = (WordBalloon) ShapeType.WORDBALLOON.getShape(balloonDrag);

			// this font will be used to calculate the area required for displaying the subtitle text in this font
	    	Font defaultFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
			
	    	// calculate the balloon for displaying subtitle text
			balloonRef = MangaBalloon.calculateWordBalloonRef(balloonRef.getBounds2D(), balloonRef.getTextBound2D(), defaultFont, linkedSubtitlesText);
			balloonDrag = createUserDragFromShape(balloonRef);
			
	    	MangaText mangaTextLayer = new MangaText(comp, balloonRef);
	    	mangaTextLayer.setAndCommitDefaultSetting(defaultFont, linkedSubtitlesText);
	    	
	        // set text translation within balloon bound
	    	mangaTextLayer.setTranslation((int)balloonRef.getTextBound2D().getX(), (int)balloonRef.getTextBound2D().getY());
	    	
	    	MangaBalloon balloon = new MangaBalloon(this, mangaTextLayer, balloonRef);
	    	balloons.add(balloon);
	    	
			// initialize shapes tool for drawing balloon
	        ShapesTool shapesTool = Tools.SHAPES;
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

	public ArrayList<MangaBalloon> getBalloons() {
		return balloons;
	}
	
	public MangaPage getPage() {
		return page;
	}
	
	private static UserDrag createUserDragFromShape(WordBalloon balloonRef) {
		Rectangle2D bound = balloonRef.getBounds2D();
	    return new UserDrag(bound.getX(), bound.getY(), bound.getX()+bound.getWidth(), bound.getY()+bound.getHeight());
	}
}
