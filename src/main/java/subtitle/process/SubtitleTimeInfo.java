package subtitle.process;

public class SubtitleTimeInfo {
	private long sTime;
	private long eTime;
	
	public SubtitleTimeInfo(long sTime, long eTime) {
		this.sTime = sTime;
		this.eTime = eTime;
	}

	public long getsTime() {
		return sTime;
	}

	public long geteTime() {
		return eTime;
	}

	@Override
	public String toString() {
		return "SubtitleTimeInfo [sTime=" + sTime + ", eTime=" + eTime + "]";
	}
}
