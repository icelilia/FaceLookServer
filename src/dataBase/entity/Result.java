package dataBase.entity;

public class Result {
	private String username;
	private String result;

	public Result() {

	}

	public Result(String username, String result) {
		this.username = username;
		this.result = result;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
