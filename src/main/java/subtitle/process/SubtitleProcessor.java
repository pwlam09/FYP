package subtitle.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubtitleProcessor {
	String pattern = "";
	protected static final String nl = "\\\n";
	protected static final String sp = "[ \\t]*";
	static Pattern r = Pattern.compile("(?s)(\\d+)" 
			+ sp + nl + "(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d\\d)"
			+ sp + "-->"
			+ sp + "(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)" 
			+ sp + "(X1:\\d.*?)??" + nl + "(.*?)" + nl + nl);
	
	public static void parseSRT() {
		File srt = new File("./sub.srt");
//		System.out.println(srt);
		FileReader fr = null;
		try {
			fr = new FileReader(srt);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(fr);
		int count = 0;
		String line = "";
		String subtitleStr = "";
		try {
			while ((line = br.readLine()) != null) {
//				count++;
				subtitleStr = subtitleStr+line+"\n";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println(testSub);
		Matcher m = r.matcher(subtitleStr);
//		while (m.find( )) {
//			System.out.println("Found value: " + m.group(1) );
//			System.out.println("Found value: " + m.group(2) );
//			System.out.println("Found value: " + m.group(3) );
//			System.out.println("Found value: " + m.group(4) );
//			System.out.println("Found value: " + m.group(11) );
//		}
	}
}
