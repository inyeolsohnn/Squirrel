package squirrel.data.json;

import java.util.ArrayList;

import com.google.gson.GsonBuilder;

public class JSON_Document {

	public class Answer {
		public Answer(int ansID, String ans) {
			this.ansID = ansID;
			this.ans = ans;
		}

		private int ansID;
		private String ans;

		public int getAnsID() {
			return ansID;
		}

		public String getAns() {
			return ans;
		}

	}

	private int id;
	private String url;
	private String title;
	private String ques;
	private ArrayList<Answer> answers = new ArrayList<Answer>();

	public JSON_Document(int id, String url, String title, String ques) {
		this.id = id;
		this.url = url;
		this.title = title;
		this.ques = ques;
	}

	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getTitle() {
		return title;
	}

	public String getQuestion() {
		return ques;
	}

	public ArrayList<Answer> getAnswers() {
		return answers;
	}

	public String getAnswersText() {
		StringBuilder text = new StringBuilder();
		for (Answer a : answers) {
			text.append(" " + a.getAns());
		}
		return text.toString();
	}

	public void addAnswer(int ansID, String ans) {
		answers.add(new Answer(ansID, ans));
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}

}
