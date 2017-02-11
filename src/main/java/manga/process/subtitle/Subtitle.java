package manga.process.subtitle;

/**
 * @author PuiWa
 *
 */
public class Subtitle {
	private long sTime;
	private long eTime;
	private String text;

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

	@Override
	public String toString() {
		return "Subtitle [sTime=" + sTime + ", eTime=" + eTime + ", text=" + text + "]";
	}
}
