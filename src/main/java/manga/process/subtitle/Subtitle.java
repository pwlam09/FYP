package manga.process.subtitle;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.opencv.core.Rect;

import manga.detect.Speaker;

/**
 * @author PuiWa
 *
 */
public class Subtitle {
	private double sTime;
	private double eTime;
	private String text;

	private Speaker speaker;

	private static int subTextCounter = 0;	// for testing
	
	public static int getSubTextCounter() {
		return subTextCounter;
	}

	public Subtitle(double sTime, double eTime, String text) {
		this.sTime = sTime;
		this.eTime = eTime;
		this.text = text;
	}

	public double getsTime() {
		return sTime;
	}

	public double geteTime() {
		return eTime;
	}
	
	/**
	 * For testing
	 */
	public String getText() {
		return text;
	}

	public Speaker getSpeaker() {
		return speaker;
	}

	public void setSpeaker(Speaker speaker) {
		this.speaker = speaker;
	}

	public static String getLinkedSubtitlesText(ArrayList<Subtitle> subtitles) {
		String linkedSubtitlesText = "";
		for (Subtitle subtitle: subtitles) {
			//System.out.printf("%d sTime:%f eTime:%f text:%s\n", ++subTextCounter, subtitle.sTime ,subtitle.eTime, subtitle.text);
			linkedSubtitlesText = linkedSubtitlesText + subtitle.text +" ";
		}
//		System.out.printf("%d sTime:%f eTime:%f linked text:%s\n", ++subTextCounter, 
//				subtitles.get(0).sTime ,subtitles.get(subtitles.size()-1).eTime, linkedSubtitlesText);
		return linkedSubtitlesText;
	}
	
	/**
	 * if the speaker faces of the two subtitle overlap >= 80% (based on the smaller face area),
	 * it will be considered as same speaker
	 *  
	 * @param subtitle
	 * @return
	 */
	public boolean hasSameSpeaker(Subtitle subtitle) {
		if (this.speaker == null || subtitle.speaker == null) {
			return false;
		}
		Rect speaker1FaceBound = this.speaker.getFace().getBound();
		Rect speaker2FaceBound = subtitle.speaker.getFace().getBound();
		Rectangle2D convertedRect1 = new Rectangle2D.Double(speaker1FaceBound.x, speaker1FaceBound.y, speaker1FaceBound.width, speaker1FaceBound.height);
		Rectangle2D convertedRect2 = new Rectangle2D.Double(speaker2FaceBound.x, speaker2FaceBound.y, speaker2FaceBound.width, speaker2FaceBound.height);
		Rectangle2D facesIntersection = convertedRect1.createIntersection(convertedRect2);
		double areaToCompare = 0.0;
		if (speaker1FaceBound.area() < speaker2FaceBound.area()) {
			areaToCompare = speaker1FaceBound.area();
		} else {
			areaToCompare = speaker2FaceBound.area();
		}
		if (facesIntersection.getWidth() * facesIntersection.getHeight() >= areaToCompare * 0.8) {
			return true;
		} else {
			return false;
		}
	}
}
