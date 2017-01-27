package subtitle.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import video.process.VideoProcessor;

public class SubtitleProcessor {
	private static TreeMap<SubtitleTimeInfo, String> subTextMap;
	private static int subTextCounter = 0;
	private static SubtitleProcessor instance = new SubtitleProcessor();

	/*	group 1:	subtitle numeric counter
		group 2-5:	start time
		group 6-9:	end time
		group 11:	subtitle text
	*/
	private static final String nl = "\\\n";
	private static final String sp = "[ \\t]*";
	private static Pattern r = Pattern.compile("(?s)(\\d+)" 
			+ sp + nl + "(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d\\d)"
			+ sp + "-->"
			+ sp + "(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)" 
			+ sp + "(X1:\\d.*?)??" + nl + "(.*?)" + nl + nl);
	
	private SubtitleProcessor() {
		if (subTextMap == null) {
			subTextMap = new TreeMap<>(
					new Comparator<SubtitleTimeInfo>() {

						@Override
						public int compare(SubtitleTimeInfo o1, SubtitleTimeInfo o2) {
							if (o1.getsTime() > o2.getsTime()) {
								return 1;
							} else {
								if (o1.getsTime() == o2.getsTime())
									return 0;
								else 
									return -1;
							}
						}
					});
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
			// trim subtitle text style e.g. text contains "<....>", text starts with "-"
			long sTimestamp = convertTimeToTimestamp(m.group(2), m.group(3), m.group(4), m.group(5));
			long eTimestamp = convertTimeToTimestamp(m.group(6), m.group(7), m.group(8), m.group(9));
//			System.out.println("sTimestamp: "+sTimestamp);
			String subText = null;
			if (m.group(11).contains("<") && m.group(11).contains(">")) {
				subText = getSubtitleTextWithoutStyle(m.group(11));
				subTextMap.put(new SubtitleTimeInfo(sTimestamp, eTimestamp), subText);
			}
		}
	}
	
	private static long convertTimeToTimestamp(String hour, String minute, String sec, String millisec) {
		long timestamp = (long) ((
				Integer.parseInt(hour) * 60 * 60 + 
				Integer.parseInt(minute) * 60 + 
				Integer.parseInt(sec) + 
				Integer.parseInt(millisec) / 1000.0) * 1000000L);
//		System.out.println("timestamp: "+timestamp);
		return timestamp;
	}
	
	private static String getSubtitleTextWithoutStyle(String styledText) {
		String[] splitedText = styledText.split(">");
		String subText = null;
		for (String str: splitedText) {
			if (str.charAt(0) != '<') {
				subText = str.substring(0, str.indexOf("<"));
			}
		}
//		System.out.println("subText: "+subText);
		return subText;
	}

	/**
	 * This method returns a list of subtitle text between two keyframes.
	 * The timestamp of the two keyframes will be used to retrieve the 
	 * subtitle within the two timestamps.
	 * 
	 * @param timestamp1 timestamp of a keyframe
	 * @param timestamp2 timestamp of another keyframe
	 * @return a list of subtitle text between two keyframes
	 */
	public static ArrayList<String> getSubTextList(long timestamp1, long timestamp2) {
		ArrayList<String> subTextList = new ArrayList<>();
		for (Map.Entry<SubtitleTimeInfo, String> entry : subTextMap.entrySet()) {
			if (timestamp1 <= entry.getKey().getsTime() && entry.getKey().geteTime() <= timestamp2) {
//				System.out.println("time1: "+SubtitleProcessor.timestampToTimeString(timestamp1));
//				System.out.println("time2: "+SubtitleProcessor.timestampToTimeString(timestamp2));
//				System.out.printf("%s >= %s: %s\n", timestamp1+"", entry.getKey().getsTime()+"", (timestamp1 >= entry.getKey().getsTime())+"");
//				System.out.printf("%s <= %s: %s", entry.getKey().geteTime()+"", timestamp2+"", (entry.getKey().geteTime() <= timestamp2)+"");
				subTextList.add(entry.getValue());
//				System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
			}
		}
		return subTextList;
	}
	
	/**
	 * For testing.
	 */
	public static void printSubText() {
		for (Map.Entry<SubtitleTimeInfo, String> entry : subTextMap.entrySet()) {
//			VideoProcessor.printFrameSubtitleTimeMatch(entry.getKey().getsTime(), entry.getKey().geteTime());
			System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
		}
	}
	
	public static String timestampToTimeString(long timestamp) {
		String colon = ":";
		int h = (int) (((timestamp / 1000000) / 60) / 60);
		int m = (int) (timestamp / (1000000 * 60) % 60);
		int s = (int) (timestamp / 1000000 % 60);
		return (h+colon+m+colon+s);
	}
}
