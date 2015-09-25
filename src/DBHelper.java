import java.net.SocketAddress;
import java.sql.*;

/**
 * Created by Hamed on 9/23/2015.
 */
public class DBHelper {
    public static RegistrationRespondInfo register(RegistrationRequestInfo requestInfo, String symmetricKey) {
        RegistrationRespondInfo respondInfo = new RegistrationRespondInfo();
        //email validation
        if (!ValidationUtil.validateEmail(requestInfo.getEmail())) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("email not valid");
        } else {
            DBUtil dbUtil = new DBUtil();
            dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO person (person_email,\n" +
                        "person_password,\n" +
                        "person_first_name,\n" +
                        "person_last_name,\n" +
                        "person_nickname,\n" +
                        "person_salt)\n" +
                        "VALUES (?, ?, ?, ?, ?, ?);");
                preparedStatement.setString(1, requestInfo.getEmail());
                preparedStatement.setString(2, requestInfo.getPassword());
                preparedStatement.setString(3, requestInfo.getFirstName());
                preparedStatement.setString(4, requestInfo.getLastName());
                preparedStatement.setString(5, requestInfo.getNickname());
                preparedStatement.setString(6, symmetricKey);
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
        }
        return respondInfo;
    }

    public static LoginRespondInfo login(LoginRequestInfo requestInfo, String symmetricKey, SocketAddress ip) {
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
            prs.setString(1, requestInfo.getEmail());
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
                        try {
                            String sessionID = SymmetricUtil.getSessionID(requestInfo.email, ip, symmetricKey);
                            PreparedStatement preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO sessions (session_id,\n" +
                                    "person_email,\n" +
                                    "session_ip,\n" +
                                    "session_salt)\n" +
                                    "VALUES (?, ?, ?, ?);");
                            preparedStatement.setString(1, sessionID);
                            preparedStatement.setString(2, requestInfo.getEmail());
                            preparedStatement.setString(3, ip.toString());
                            preparedStatement.setString(4, symmetricKey);
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
                                respondInfo.setSessionID(sessionID);
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

    public static GroupInfo createGroup(GroupCreateInfo request) {
        GroupInfo respondInfo = new GroupInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");

        try {
            Connection con = dbUtil.getConnection();
            ResultSet rs;

            PreparedStatement prs = con.prepareStatement("SELECT person.person_email, "
                    + "person_isremove "
                    + "FROM person "
                    + "INNER JOIN sessions "
                    + "ON person.person_email = sessions.person_email"
                    + "WHERE ( sessions.session_id = ?)");
            prs.setString(1, request.getSession());
            rs = prs.executeQuery();
            try {
                while (rs.next()) {
                    //check isremove
                    if (rs.getInt("person_isremove") == 0) {
                        respondInfo.setId(SymmetricUtil.getGroupID(rs.getString("person.person_email"), rs.getString("person_isremove")));
                        respondInfo.setOwner(rs.getString("person.person_email"));
                        respondInfo.setName(request.getName());
                        respondInfo.setMembers(request.getMembers());
                        //set group
                        try {
                            PreparedStatement preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO groups (group_id,\n" +
                                    "group_name,\n" +
                                    "person_email)\n" +
                                    "VALUES (?, ?, ?);");
                            preparedStatement.setString(1, respondInfo.getId());
                            preparedStatement.setString(2, respondInfo.getName());
                            preparedStatement.setString(3, respondInfo.getOwner());
                            int executeUpdate = 0;
                            try {
                                executeUpdate = preparedStatement.executeUpdate();
                            } catch (SQLException e) {
                                respondInfo.setSucceed(false);
                            }
                            if (executeUpdate == 1) {
                                //respondInfo.setSucceed(true);
                                //set group members
                                StringBuilder query = new StringBuilder();
                                query.append("INSERT INTO person_group (person_email,group_id) VALUES ");
                                int memberSize = respondInfo.getMembers().size();
                                for (int i = 0; i < memberSize; i++) {
                                    query.append("(?,?)");
                                    if((memberSize)!=(i-1)){
                                        query.append(" , ");
                                    }
                                   // query.append("(" + respondInfo.getMembers().get(i) + "," + respondInfo.getId() + ")");
                                }

                                try {
                                    PreparedStatement pres = dbUtil.getConnection().prepareStatement(query.toString());
                                    for (int i = 0; i < memberSize; i++) {
                                        pres.setString((i+1), respondInfo.getMembers().get(i).getMember());
                                        pres.setString((i+2), respondInfo.getId());
                                    }
                                    int execute = 0;
                                    try {
                                        execute = pres.executeUpdate();
                                    } catch (SQLException e) {
                                        respondInfo.setSucceed(false);
                                    }
                                    if (execute == 1) {
                                        respondInfo.setSucceed(true);
                                    }
                                } catch (SQLException e) {
                                    respondInfo.setSucceed(false);
                                    e.printStackTrace();
                                }


                            }
                        } catch (SQLException e) {
                            respondInfo.setSucceed(false);
                            e.printStackTrace();
                        }
                    }
                    break;
                }

                respondInfo.setSucceed(false);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }
}
