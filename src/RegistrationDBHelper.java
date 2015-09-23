import java.net.SocketAddress;
import java.sql.*;

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

    public static LoginRespondInfo login(LoginRequestInfo requestInfo , String symmetricKey , String ip) {
        LoginRespondInfo respondInfo = new LoginRespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");

        try {
            Connection con = dbUtil.getConnection();
            ResultSet rs;

            PreparedStatement prs = con.prepareStatement("SELECT person_password, "
                    + "person_salt, "
                    + "person_isremove "
                    + "FROM person "
                    + "WHERE ( person_email = ?)");
            prs.setString(1,requestInfo.getEmail());
            rs = prs.executeQuery();
            try {
                while (rs.next()) {
                    // decrypt password by person_salt
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(rs.getString("person_salt"));
                    String password = aesEncryptionUtil.decrypt(rs.getString("person_password"));
                    //check password and isremove
                    if (password.equals(requestInfo.getPassword()) && rs.getInt("person_isremove") == 0) {
                        respondInfo.setMessage("Login  successful");

                        //set session
                        PreparedStatement preparedStatement = null;
                        try {
                            preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO \"session\" (session_id,\n" +
                                    "person_email,\n" +
                                    "session_time,\n" +
                                    "session_ip,\n" +
                                    "session_salt)\n" +
                                    "VALUES (?, ?, ?, ?, ?);");
                            preparedStatement.setString(1, SymmetricUtil.getSessionID(requestInfo.email,ip,symmetricKey));
                            preparedStatement.setString(2, requestInfo.getEmail());
                            preparedStatement.setLong(3, System.currentTimeMillis());
                            preparedStatement.setString(4, ip.toString());
                            preparedStatement.setString(5, symmetricKey);
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
                                respondInfo.setMessage("User with email address:" + requestInfo.getEmail() + " has login successfully!");
                            }
                        } catch (SQLException e) {
                            respondInfo.setSucceed(false);
                            respondInfo.setMessage("Prepared Statement Failed");
                            e.printStackTrace();
                        }

                        dbUtil.close();
                        return respondInfo;
                    }
                    break;
                }

                respondInfo.setMessage("Login Failed - username or password incorrect");
                respondInfo.setSucceed(false);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.getMessage());
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }
    /*public static void login(RegistrationRequestInfo requestInfo , String symmetricKey , String ip) {


    }*/

}
