package manga.process.subtitle;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import manga.detect.Speaker;

/**
 * @author PuiWa
 *
 */
public class Subtitle {
	private long sTime;
	private long eTime;
	private String text;
	private Speaker speaker;

	public Subtitle(long sTime, long eTime, String text) {
		this.sTime = sTime;
		this.eTime = eTime;
		this.text = text;
	}

	public long getsTime() {
		return sTime;
	}

	public long geteTime() {
		return eTime;
	}
	
	public String getText() {
		return text;
	}

	public Speaker getSpeaker() {
		return speaker;
	}

	public void setSpeaker(Speaker speaker) {
		this.speaker = speaker;
	}
}
