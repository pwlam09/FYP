package manga.process.subtitle;

import java.util.ArrayList;

import org.opencv.core.Mat;
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
			System.out.printf("%d sTime:%f eTime:%f text:%s\n", ++subTextCounter, subtitle.sTime ,subtitle.eTime, subtitle.text);
			linkedSubtitlesText = linkedSubtitlesText + subtitle.text +" ";
		}
		return linkedSubtitlesText;
	}
}
