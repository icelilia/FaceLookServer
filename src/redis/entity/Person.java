package redis.entity;

import java.io.Serializable;

public class Person implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7543773312041338392L;
	String username;
    String password;

    public Person(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Person{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
