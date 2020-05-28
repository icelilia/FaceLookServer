package redis.activeMq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class MyActiveMqConnection {

    public static MyActiveMqConnection INSTANCE;

    static {
        try {
            INSTANCE = new MyActiveMqConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    Connection connection;

    public MyActiveMqConnection() throws JMSException {
        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_USER,
                        ActiveMQConnectionFactory.DEFAULT_PASSWORD,
                        "tcp://175.24.41.121:61616");
        connection = connectionFactory.createConnection();
        connection.start();
    }

    public Connection getConnection() {
        return connection;
    }
}
