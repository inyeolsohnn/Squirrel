package squirrel.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import squirrel.data.json.JSON_Document;
import squirrel.data.json.JSON_IO;

public class SQL_Retriever {

	private Connection con;
	private String connectionURL;
	private Statement stmt;

	public SQL_Retriever(String host, String db, String username, String password) {
		con = null;
		stmt = null;
		connectionURL = "jdbc:mysql://" + host + "/" + db + "?" + "user=" + username + "&password="
				+ password;
	}

	public boolean open() {
		// Try to establish a connection to the DB
		try {
			con = DriverManager.getConnection(connectionURL);
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return false;
		}

		// Try to create a statement object in order to execute queries on the DB
		try {
			stmt = con.createStatement();
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return false;
		} catch (Exception e) {
			System.out.println("General Exception: " + e.toString());
			return false;
		}

		return true;
	}

	public void close() {
		// Close the DB connection
		try {
			con.close();
			stmt.close();
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
		}
	}

	public void retrieveSQLData(boolean answers) {
		String query = null;
		ResultSet rs = null;

		if (!open()) {
			System.out.println("Connection to the DB could not be made. Please try again.");
		}

		// loop through know IDs in the dataset -- hardcoded
		// limits
		docLoop: for (int docID = 1000; docID <= 1607; docID++) {

			// initialize the document properties and the object itself
			int ansID = 0;
			String title = null;
			String ques = null;
			String url = null;
			String ans = null;

			JSON_Document json = null;

			// form the query to retrieve all Question information for this document
			query = "SELECT `title`,`question`,`url`,`answers` FROM `questions` WHERE id=" + docID;

			// attempt to execute the query
			rs = executeQuery(query);

			// if results exist, assign them to their appropriate fields
			try {
				while (rs.next()) {

					if (answers && rs.getInt("answers") == 0) {
						continue docLoop;
					}

					title = rs.getString("title");
					ques = rs.getString("question");
					url = rs.getString("url");

					// create the actual object containing
					// all information for this document
					json = new JSON_Document(docID, url, title, ques);
				}
			} catch (SQLException e) {
				System.out.println("SQL exception while retrieving question " + docID + ".\n"
						+ e.toString());
			}

			// form the query to retrieve all Answer information
			// for this document
			query = "SELECT `id`,`answer` FROM `answers` WHERE `question_id`=" + docID
					+ " ORDER BY `id` ASC";

			// attempt to execute the query
			rs = executeQuery(query);

			// as long as results exist, assign them 
			// to their appropriate fields
			try {
				while (rs.next()) {

					ansID = rs.getInt("id");
					ans = rs.getString("answer");

					json.addAnswer(ansID, ans);
				}
			} catch (SQLException e) {
				System.out.println("SQL exception while retrieving answers for question " + docID
						+ "\n" + e.toString());
			}
			// Store the JSON object on disk
			JSON_IO.storeDocument(json);
		}
		close();
	}

	private ResultSet executeQuery(String query) {
		// For a given query, try to execute it and return its resultset
		try {
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return null;
		}
	}
}
