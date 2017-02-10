package manga.element;

import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;

import manga.process.subtitle.SubtitleProcessor;
import manga.process.video.FrameImage;
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
	private ArrayList<MangaBalloon> balloonList;
	
	private static int panelCount = 0;
	
	public MangaPanel(MangaPage page) {
		this.page = page;
		panelCount++;
		this.layer = page.getComp().addNewEmptyLayer("Panel "+panelCount, false);
		this.panelImg = new MangaPanelImage();
		this.bound = new Rectangle2D.Float();
		this.balloonList = new ArrayList<>();
	}
	
//	public MangaPanel(Composition comp, Rectangle2D bound) {
//		this.comp = comp;
//		this.layer = comp.addNewEmptyLayer("Panel "+panelCount, false);
//		panelCount++;
//		this.panelImg = new MangaPanelImage();
//		this.bound = bound;
//		this.balloonList = new ArrayList<>();
//	}

	public void setBound(Rectangle2D boundingRect) {
		this.bound = boundingRect;
	}
	
	public Rectangle2D getBound() {
		return this.bound;
	}
	
	public ImageLayer getLayer() {
		return this.layer;
	}
	
	/**
	 * for testing purpose
	 */
	public void printBound() {
		System.out.println(this.bound);
	}
	
	public void setImage(MangaPanelImage panelImg) {
		this.panelImg = panelImg;
	}
	
    /**
     * Image fit to panel bounding rectangle, starts from top-left corner.
     * The image will be drawn onto a new layer, bounded by the area within selection.
     */
    public void fitImageToPanelBound(FrameImage frameImage) {
        if (frameImage != null) {
        	// setup selection with the bounding rectangle of MangaPanel
        	Composition comp = page.getComp();
            Selection selection = new Selection(bound, comp.getIC());
            comp.setNewSelection(selection);
            
	        panelImg = new MangaPanelImage(comp, frameImage.getImg(), frameImage.getcShotTimestamp(), bound);
	        
	        // image to new layer
	        ImageLayer layer = panelImg.getLayer();
	        
	        layer.setImageWithSelection(panelImg.getSubImage());
	        
	        // Deselect. Don't use selection.die() directly, which leads to error
	        comp.deselect(AddToHistory.NO);
        }
    }
    
    public void addMangaBalloon() {
		String linkedText = "";
		long frameTimestamp = panelImg.getFrameTimestamp();
		ArrayList<String> subTextList = SubtitleProcessor.getSubTextList(frameTimestamp, VideoProcessor.getNextKeyframeTimestamp(frameTimestamp));
		
		if (subTextList.size() != 0) {
			for (String str: subTextList) {
				linkedText = linkedText + str +" ";
			}
	    	
	    	Composition comp = page.getComp();

//	    	WordBalloon balloonRef = new WordBalloon(bound.getX(), bound.getY(), -100, -100);
//	    	UserDrag balloonDrag = new UserDrag(bound.getX()+100, bound.getY()+100, bound.getX(), bound.getY());
	    	UserDrag balloonDrag = new UserDrag(bound.getX(), bound.getY(), bound.getX()+100, bound.getY()+100);
	    	WordBalloon balloonRef = (WordBalloon) ShapeType.WORDBALLOON.getShape(balloonDrag);

			Font testFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
			balloonRef = MangaBalloon.calculateWordBalloonRef(balloonRef.getBounds2D(), balloonRef.getTextBound2D(), testFont, linkedText);
			balloonDrag = createUserDragFromShape(balloonRef);
			
	    	MangaText mangaTextLayer = new MangaText(comp, balloonRef);
	    	mangaTextLayer.setDefaultSetting();
	    	
	    	// any change to textSettings need to use setSettings(), otherwise it will not be applied
	    	TextSettings oldSettings = mangaTextLayer.getSettings();
	        TextSettings newSettings = new TextSettings(oldSettings);
	        
	        newSettings.setText(linkedText);
	        mangaTextLayer.setSettings(newSettings);
	    	
	        // set text translation within balloon bound, to be changed
	    	mangaTextLayer.setTranslation((int)balloonRef.getTextBound2D().getX(), (int)balloonRef.getTextBound2D().getY());
	    	
	    	MangaBalloon balloon = new MangaBalloon(this, mangaTextLayer, balloonRef);
	    	balloonList.add(balloon);
	    	
			// initialize shapes tool for drawing balloon
	        ShapesTool shapesTool = Tools.SHAPES;
	        shapesTool.setShapeType(ShapeType.WORDBALLOON);
	        shapesTool.setAction(ShapesAction.FILL_AND_STROKE);
	        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
	        shapesTool.setFill(TwoPointBasedPaint.BACKGROUND);
	        // reset stroke join and width to default
	        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.ROUND, 5);
	        
	        // paint balloon
//	    	shapesTool.paintShapeOnIC(balloon.getBallloonLayer(), new UserDrag(bound.getX(), bound.getY(), bound.getX()+100, bound.getY()+100));
	    	shapesTool.paintShapeOnIC(balloon.getBallloonLayer(), balloonDrag);
//	    	System.out.println("Userdrag: "+new UserDrag(bound.getX()+100, bound.getY()+100, bound.getX(), bound.getY()));
        }
    }

	public ArrayList<MangaBalloon> getBalloons() {
		return balloonList;
	}
	
	public MangaPage getPage() {
		return page;
	}
	
	private static UserDrag createUserDragFromShape(WordBalloon balloonRef) {
		Rectangle2D bound = balloonRef.getBounds2D();
	    return new UserDrag(bound.getX(), bound.getY(), bound.getX()+bound.getWidth(), bound.getY()+bound.getHeight());
	}
}
