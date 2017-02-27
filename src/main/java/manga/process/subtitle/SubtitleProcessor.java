package manga.process.subtitle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdesktop.swingx.JXTipOfTheDay.ShowOnStartupChoice;

import manga.process.video.VideoProcessor;

/**
 * @author PuiWa
 * 
 */
public class SubtitleProcessor {
	private static ArrayList<Subtitle> allSubtitles;
	private static SubtitleProcessor instance = new SubtitleProcessor();

	private static int subtitleCounter = 0;	// for testing

	/**
	 * group 1:		subtitle numeric counter, 
	 * group 2-5:	start time, 
	 * group 6-9:	end time, 
	 * group 11:	subtitle text
	 */
	private static final String nl = "\\\n";
	private static final String sp = "[ \\t]*";
	private static Pattern r = Pattern.compile("(?s)(\\d+)" 
			+ sp + nl + "(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d\\d)"
			+ sp + "-->"
			+ sp + "(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)" 
			+ sp + "(X1:\\d.*?)??" + nl + "(.*?)" + nl + nl);
	
	private SubtitleProcessor() {
		if (allSubtitles == null) {
			allSubtitles = new ArrayList<>();
		}
	}
	
	public SubtitleProcessor getInstance() {
		if (instance == null) {
			instance = new SubtitleProcessor();
		}
		return instance;
	}
	
	public static void parseSRT() {
		File srt = new File("./sub.srt");
		FileReader fr = null;
		try {
			fr = new FileReader(srt);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(fr);
		int count = 0;
		String line = "";
		String subtitleStr = "";
		try {
			while ((line = br.readLine()) != null) {
				count++;
				subtitleStr = subtitleStr+line+"\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Matcher m = r.matcher(subtitleStr);
		while (m.find( )) {
			// trim subtitle text style e.g. text contains "<....>"
			double sTimestamp = convertTimeToTimestamp(m.group(2), m.group(3), m.group(4), m.group(5));
			double eTimestamp = convertTimeToTimestamp(m.group(6), m.group(7), m.group(8), m.group(9));
			// subtitle may contain line break
			// subtitle may contain hyphen which indicates speaker (delete?)
			String subText = m.group(11).replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("- ", "");	
			// trim style tags of extracted text if any
			if (subText.contains("<") && subText.contains(">")) {
				subText = getSubtitleTextWithoutStyle(subText);
			}
			allSubtitles.add(new Subtitle(sTimestamp, eTimestamp, subText));
		}
	}
	
	private static double convertTimeToTimestamp(String hour, String minute, String sec, String millisec) {
		double timestamp = (double) ((
				Integer.parseInt(hour) * 60 * 60 * 1000 + 
				Integer.parseInt(minute) * 60 * 1000 + 
				Integer.parseInt(sec) * 1000 + 
				Integer.parseInt(millisec)));
		return timestamp;
	}
	
	private static String getSubtitleTextWithoutStyle(String styledText) {
		String subtitleText = "";
		String[] splitedTextList = styledText.split(">");
		for (int i = 0; i < splitedTextList.length; i++) {
			if (splitedTextList[i].charAt(0) != '<') {
				subtitleText = subtitleText + splitedTextList[i].substring(0, splitedTextList[i].indexOf("<")) + " ";
			}
		}
		return subtitleText;
	}
	
	/**
	 * for testing
	 */
	public static void printAllSubtitles() {
		for (Subtitle subtitle : allSubtitles) {
			System.out.printf("%d sTime:%f eTime:%f text:%s\n", ++subtitleCounter, subtitle.getsTime() ,subtitle.geteTime(), subtitle.getText());
		}
	}

	/**
	 * This method returns a list of subtitle text between two shots.
	 * The timestamps of the start timestamp and end timestamp of shot 
	 * will be used to retrieve the subtitle within the two timestamps.
	 * 
	 * @param timestamp1 start timestamp of the shot
	 * @param timestamp2 end timestamp of the shot
	 * @return a list of subtitle text between two keyframes
	 */
	public static ArrayList<Subtitle> getSubtitles(double timestamp1, double timestamp2) {
		ArrayList<Subtitle> subtitles = new ArrayList<>();
//		double sShotTimestamp = VideoProcessor.getsShotTimestamp(timestamp1);
//		double eShotTimestamp = VideoProcessor.getsShotTimestamp(timestamp2);
		double sShotTimestamp = timestamp1;
		double eShotTimestamp = timestamp2;
		for (Subtitle subtitle : allSubtitles) {
			double sSubtitleTimestamp = subtitle.getsTime();
			double eSubtitleTimestamp = subtitle.geteTime();
			
//			boolean isStartWithinShot = (sShotTimestamp <= sSubtitleTimestamp) && (sSubtitleTimestamp < eShotTimestamp) &&
//					(Math.abs(sSubtitleTimestamp-eShotTimestamp) >= Math.abs(eSubtitleTimestamp-eShotTimestamp));
//			boolean isStartBeforeShot = (sShotTimestamp < eSubtitleTimestamp) && (eSubtitleTimestamp <= eShotTimestamp) &&
//					(Math.abs(eSubtitleTimestamp-sShotTimestamp) > Math.abs(sSubtitleTimestamp-sShotTimestamp));
//			if (isStartWithinShot || isStartBeforeShot) {
//				subtitles.add(subtitle);
//				System.out.printf("subtitle text: %s ", subtitle.getText());
//				if (isStartWithinShot) {
//					System.out.printf("isStartWithinShot sSub-sShot: %f "+Math.abs(sSubtitleTimestamp-sShotTimestamp));
//					System.out.printf("isStartWithinShot eSub-eShot: %f \n"+Math.abs(eSubtitleTimestamp-eShotTimestamp));
//				} else {
//					System.out.printf("isStartBeforeShot eSub-sShot: %f "+Math.abs(eSubtitleTimestamp-sShotTimestamp));
//					System.out.printf("isStartBeforeShot sSub-sShot: %f \n"+Math.abs(sSubtitleTimestamp-sShotTimestamp));
//				}
//			}
			
			if (sSubtitleTimestamp >= sShotTimestamp && sSubtitleTimestamp < eShotTimestamp) {
//				System.out.println("subtitle text: "+subtitle.getText());
				subtitles.add(subtitle);
			}
		}
//		System.out.println("sShot Time: "+timestampToTimeString(sShotTimestamp));
//		System.out.println("eShot Time: "+timestampToTimeString(eShotTimestamp));
		return subtitles;
	}
	
	/**
	 * For testing
	 * @param timestamp
	 * @return
	 */
	public static String timestampToTimeString(long timestamp) {
		String colon = ":";
		int h = (int) (((timestamp / 1000) / 60) / 60);
		int m = (int) (timestamp / (1000 * 60) % 60);
		int s = (int) (timestamp / 1000 % 60);
		return (h+colon+m+colon+s);
	}

	public static ArrayList<Subtitle> getAllSubtitles() {
		return allSubtitles;
	}
}
