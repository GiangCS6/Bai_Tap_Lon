package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.user.Admin;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.common.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDao {
    private final Connection con;
    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    /*public UserDao(){
        Connection fake=null;
        try{
            fake=DatabaseConnection.getInstance().getConnection();
        }
        catch (SQLException e){
            logger.log(Level.SEVERE,"Error connecting to the database in UserDao:",e);
        }
        this.con=fake;
    }*/
    public UserDao(){
        this.con = null;
    }
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    //save
    public void save(User user) {
        String sql = """
        INSERT INTO users 
        (id, username, password, email, role, isActive, is_deleted, balance, create_at) 
        VALUES (?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole());
            stmt.setBoolean(6, true);
            stmt.setBoolean(7, false);
            stmt.setLong(8, 0);
            stmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Failed to insert user: " + user.getUsername());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving user: " + user.getUsername(), e);
            throw new DatabaseException("Error saving user: " + e.getMessage());
        }
    }
    /*public void save(User user) {
        String sql="insert into users (id,username,password,email,role,isActive,is_deleted,balance,create_at) values (?,?,?,?,?,?,?,?,?)";

        try(PreparedStatement stmt=con.prepareStatement(sql)){
            stmt.setString(1,user.getId());
            stmt.setString(2,user.getUsername());
            stmt.setString(3,user.getPassword());
            stmt.setString(4,user.getEmail());
            stmt.setString(5,user.getRole());
            stmt.setBoolean(6,true);
            stmt.setBoolean(7,false);
            stmt.setLong(8,0);
            stmt.setObject(9, Timestamp.valueOf(user.getCreatedAt()));


            int rows=stmt.executeUpdate();
            if(rows==0){throw new SQLException("Failed to insert into user");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error saving user: "+e.getMessage());
        }
    }*/

    //find-username < khi đăng nhập >
    public User findUserName(String Username){
        String sql="select * from users where username=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,Username);


            ResultSet rs=stmt.executeQuery();
            if(rs.next()){
                return createUser(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error finding user by username: "+e.getMessage());
        }
        return null;
    }
    private User createUser(ResultSet rs) {
        try {
            User user;

            String id = rs.getString("id");
            String name = rs.getString("username");
            String password = rs.getString("password");
            String email = rs.getString("email");
            String role = rs.getString("role");
            boolean active = rs.getBoolean("isActive");

            if (role.equals("SELLER")) {
                user = new Seller(name, password, email);
            } else if (role.equals("BIDDER")) {
                user = new Bidder(name, password, email);
            } else if (role.equals("ADMIN")) {
                user = new Admin(name, password, email);
            } else {
                throw new SQLException("Invalid role:" + role);
            }
            user.setActive(active);
            user.setId(id);
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating user from ResultSet: " + e.getMessage());
        }
    }

    //find-id
    public User findUserId(String id){
        String sql="select * from users where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,id);

            ResultSet rs=stmt.executeQuery();
            if(rs.next()){return createUser(rs);}
        }
        catch (SQLException e){
            throw new DatabaseException("Error finding user by id: "+e.getMessage());
        }
        return null;
    }

    //check-username existence
    public boolean checkUserName(String username){
        String sql="select count(*) from users where username=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,username);

            ResultSet rs=stmt.executeQuery();
            if(rs.next()){
                return rs.getInt(1)>0;
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error checking username existence: "+e.getMessage());
        }
        return false;
    }

    //update-active
    public void updateActive(String userId,boolean active){
        String sql="update users set isActive=? where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setBoolean(1,active);
            stmt.setString(2,userId);

            int rows=stmt.executeUpdate();
            if(rows==0){throw new SQLException("Active update was not complted");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error updating user active status: "+e.getMessage());
        }
    }

    //update-password
    public void updatePassword(String userId,String newPassword){
        String sql="update users set password=? where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,newPassword);
            stmt.setString(2,userId);

            int rows=stmt.executeUpdate();
            if(rows==0){throw new SQLException("Password update was not completed");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error updating user password: "+e.getMessage());
        }
    }

    //find all user
    public List<User> findAllUser(){
        String sql="select * from users";

        List<User> UserList=new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            ResultSet rs=stmt.executeQuery();
            while(rs.next()){
                UserList.add(createUser(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error finding all users: "+e.getMessage());
        }
        return UserList;
    }
    public User findByEmail(String email){
        String sql = "select * from users where email=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return createUser(rs);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    public void incrementBalance(String userId, long amount){
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ? AND is_deleted = false";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setLong(1, amount);
            stmt.setString(2, userId);
            int rows = stmt.executeUpdate();
            if (rows == 0) throw new SQLException("User not found: " + userId);
        }
        catch (SQLException e) {
            throw new DatabaseException("Error incrementing user balance: " + e.getMessage());
        }
    }

    public long findBalanceById(String userId){
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("balance");
            throw new SQLException("User not found: " + userId);
        }
        catch (SQLException e) {
            throw new DatabaseException("Error finding user balance: " + e.getMessage());
        }
    }

    public boolean decrementBalance(String userId, long amount){
        // WHERE balance >= amount để tránh âm, atomic
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ? AND is_deleted = false";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setLong(1, amount);
            stmt.setString(2, userId);
            stmt.setLong(3, amount);
            int rows = stmt.executeUpdate();
            return rows > 0; // false = không đủ tiền
        }
        catch (SQLException e) {
            throw new DatabaseException("Error decrementing user balance: " + e.getMessage());
        }
    }

}
