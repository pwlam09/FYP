package manga.element;

import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapestool.BasicStrokeJoin;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.tools.shapestool.TwoPointBasedPaint;
import pixelitor.Canvas;
import pixelitor.Composition;

import java.util.ArrayList;
import java.util.List;

import manga.process.subtitle.SubtitleProcessor;
import manga.process.video.KeyFrame;
import manga.process.video.VideoProcessor;

/**
 * This is the main controller of generating manga
 * 
 * @author PuiWa
 *
 */

public final class MangaGenerator {
	private static ArrayList<MangaPage> pages=new ArrayList<>();
	private static int keyFrameCount = 0;
	private static int numOfPanelsPerPage = 6;
	private static double currTimestamp = 0;

	private MangaGenerator() {
		
	}
	
	public static void preprocessing(String videoPath) {
		// extract subtitle file
		// extract key frames info (in terms of frame number)
		// extract key frames (in Mat format and store timestamps)
		VideoProcessor.preprocessing(videoPath);
		
		// parse the extracted subtitle file and store subtitle text and related timestamps
		SubtitleProcessor.parseSRT();
//		SubtitleProcessor.printAllRawSubtitles();	// testing
		
		// group and compress subtitles with reference to key frames and subtitles
		SubtitleProcessor.groupAndSummarizeSubtitles();
//		SubtitleProcessor.printAllProcessedSubtitles();	// testing
	}
	
	public static double getCurrTimestamp() {
		return currTimestamp;
	}

	public static void setCurrTimestamp(double currTimestamp) {
		MangaGenerator.currTimestamp = currTimestamp;
	}
	
	public static void addMangaPages() {
		keyFrameCount = VideoProcessor.getKeyFrameCount();
		int pageNum = keyFrameCount / numOfPanelsPerPage;
		for (int i = 0; i<pageNum; i++) {
			MangaPage page = new MangaPage();
			pages.add(page);
		}
		if (keyFrameCount % numOfPanelsPerPage != 0) {
			MangaPage page = new MangaPage();
			pages.add(page);
		}
	}
	
	public static MangaPage getActivePage() {
		Composition comp = ImageComponents.getActiveCompOrNull();
		for (MangaPage p: pages) {
			if (p.getComp().equals(comp)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Set active MangaPanel layer
	 * 
	 * @param i the index of layer of the current active composition
	 */
//	public static ImageLayer setActivePanelLayer(int i) {
//		MangaPanel selectedPanel = getActivePage().getPanels().get(i);
//		selectedPanel.getLayer().makeActive(AddToHistory.YES);
//		return selectedPanel.getLayer();
//	}
	
	/**
	 * Set active internal frame (i.e. MangaPage composition)
	 * 
	 * @param i the index of ArrayList of MangaPage
	 */
	public static void setActiveComp(int i) {
		List<ImageComponent> icList = ImageComponents.getICList();
		Composition selectedComp = pages.get(i).getComp();
        for (ImageComponent ic : icList) {
        	if (ic.getComp() == selectedComp) {
        		ImageComponents.setActiveIC(ic, true);
        	}
        }
	}	
	
	/**
	 * Set active internal frame (i.e. MangaPage composition)
	 */
	public static void setActiveComp(MangaPage page) {
		List<ImageComponent> icList = ImageComponents.getICList();
		Composition selectedComp = page.getComp();
        for (ImageComponent ic : icList) {
        	if (ic.getComp() == selectedComp) {
        		ImageComponents.setActiveIC(ic, true);
        	}
        }
	}
	
	
	/**
	 * Draw 6 manga panels on different MangaPanel layers
	 */
	public static void setAndDrawMangaPanels() {
		for (MangaPage page: pages) {

			// 6 panels at most for a page?
			// 1 key frame = 1 panel
	    	for (int i=0; i<numOfPanelsPerPage && keyFrameCount>0; i++) {
	    		ArrayList<KeyFrame> allKeyFrames = VideoProcessor.getKeyFrames();
	    		page.addNewMangaPanel(allKeyFrames.get(allKeyFrames.size()-keyFrameCount));
	    		keyFrameCount--;
	    	}

			ArrayList<MangaPanel> panels = page.getPanels();
	    	
	    	ImageLayer layer = panels.get(0).getLayer();	//Get 1st canvas for size reference
	        Canvas canvas = layer.getComp().getCanvas();
	        int canvasWidth = canvas.getWidth();
	        int canvasHeight = canvas.getHeight();
	        int canvasLeftRightMargin = (int)(canvas.getWidth()*0.03);
	        int canvasTopBottomMargin = (int)(canvas.getHeight()*0.03);
	        int mangaPanelWidth = (int)((canvas.getWidth()-canvasLeftRightMargin*3)/2);
	        int mangaPanelHeight = (int)((canvas.getHeight()-canvasTopBottomMargin*4)/3);
	        
	        // intialize tools for drawing panels
	        ShapesTool shapesTool = Tools.SHAPES;
	        // call reset method or the previous stroke will be used
	        shapesTool.resetDrawingAndStroke();
	        shapesTool.setShapeType(ShapeType.RECTANGLE);
	        shapesTool.setAction(ShapesAction.STROKE);
	        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
	        // set stroke join and width
	        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.MITER, 10);
	        
	        // Add two panels (left and right) at a time
	        for (int i=0; i<3; i++) {
	            // set panel bound and draw panels on different layers
	            if (i*2 < panels.size()) {
		            UserDrag leftPanelDrag = new UserDrag(canvasLeftRightMargin, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
		            		canvasLeftRightMargin+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
		            panels.get(i*2).setBound(leftPanelDrag.createPossiblyEmptyRect());
		            shapesTool.paintShapeOnIC(panels.get(i*2).getLayer(), leftPanelDrag);
	            }
	            if (i*2+1 < panels.size()) {
		            UserDrag rightPanelDrag = new UserDrag(canvasLeftRightMargin*2+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
		            		(canvasLeftRightMargin+mangaPanelWidth)*2, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
		            panels.get(i*2+1).setBound(rightPanelDrag.createPossiblyEmptyRect());
		            shapesTool.paintShapeOnIC(panels.get(i*2+1).getLayer(), rightPanelDrag);
	            }
	        }
		}
	}
	
	/**
	 * Draw word balloon on layer, similar to drawing panel.
	 */
	public static void drawWordBalloons() {
		for (MangaPage page: pages) {
			for (MangaPanel panel: page.getPanels()) {
				panel.addMangaBalloons();
			}
		}
	}
	
	/**
	 * Fill panels with key frames
	 */
	public static void drawImgsToPanel() {
		ArrayList<KeyFrame> extractedFrameImgs = VideoProcessor.getKeyFrames();
		int frameImgCounter = 0;
        for (int i = 0; i<pages.size(); i++) {
        	MangaPage currentPage = pages.get(i);
    		for (int j = 0; j<currentPage.getPanels().size(); j++) {
            	MangaPanel panel = currentPage.getPanels().get(j);
            	panel.fitImageToPanelBound();
            }
        }
	}
	
	/**
	 * Push balloon and text layers to the top.
	 * Otherwise they will be occluded by other layers
	 */
	public static void pushBalloonsAndTextToTop() {
        for (int i = 0; i<pages.size(); i++) {
        	MangaPage currentPage = pages.get(i);
        	// the active index of the top (latest created) layer
			int activeLayerIndex = currentPage.getComp().getActiveLayerIndex();
			for (MangaPanel panel: currentPage.getPanels()) {
				for (MangaBalloon balloon: panel.getBalloons()) {
					// update the layer with the new index
					// for some reason, the two layers can work with same index, still under investigation
					balloon.getBallloonLayer().dragFinished(activeLayerIndex);
					balloon.getMangaTextLayer().dragFinished(activeLayerIndex);
				}
			}
		}
	}
	
	public static ArrayList<MangaPage> getMangaPages() {
		return pages;
	}
}
