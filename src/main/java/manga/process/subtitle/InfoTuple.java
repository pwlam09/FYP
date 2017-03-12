package manga.process.subtitle;

/**
 * @author PuiWa
 *
 */
public class InfoTuple {
	private int sentence_id;
	private int positionInSentence;
	
	public InfoTuple(int sentence_id, int positionInSentence) {
		this.sentence_id = sentence_id;
		this.positionInSentence = positionInSentence;
	}

	public int getSentenceId() {
		return sentence_id;
	}

	public int getPositionInSentence() {
		return positionInSentence;
	}

	@Override
	public String toString() {
		return "[sid=" + sentence_id + ", pos=" + positionInSentence + "]";
	}
}
