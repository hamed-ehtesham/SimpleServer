import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Hamed on 9/23/2015.
 */
public class RegistrationDBHelper {
    public static RegistrationRespondInfo register(RegistrationRequestInfo requestInfo) {
        RegistrationRespondInfo respondInfo = new RegistrationRespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO person (person_email,\n" +
                    "person_password,\n" +
                    "person_first_name,\n" +
                    "person_last_name,\n" +
                    "person_nickname)\n" +
                    "VALUES (?, ?, ?, ?, ?);");
            preparedStatement.setString(1, requestInfo.getEmail());
            preparedStatement.setString(2, requestInfo.getPassword());
            preparedStatement.setString(3, requestInfo.getFirstName());
            preparedStatement.setString(4, requestInfo.getLastName());
            preparedStatement.setString(5, requestInfo.getNickname());
            int executeUpdate = 0;
            try {
                executeUpdate = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.getMessage());
            }
            if (executeUpdate == 1) {
                respondInfo.setSucceed(true);
                respondInfo.setMessage("User with email address:" + requestInfo.getEmail() + " has registered successfully!");
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }
}
