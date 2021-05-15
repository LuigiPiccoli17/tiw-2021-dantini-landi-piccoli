package it.polimi.tiw.html.dao;

import it.polimi.tiw.html.beans.Item;

import java.sql.*;

public class ItemDAO {
    private Connection con;

    /***
     * @author Alfredo Landi
     * Costructor for this DAO
     * @param con is the current connection
     */
    public ItemDAO(Connection con){
        this.con=con;
    }

    /**
     * @author Alfredo Landi
     * @param newItem to add database
     * @return the id of item if query is successful, 0 in case of db error
     */
    public int insertNewItem(Item newItem){
        ResultSet result;
        String query = "INSERT INTO item (name, image, description) VALUES (?, ?, ?)";
        PreparedStatement pstatement = null;

        try{
            pstatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstatement.setString(1, newItem.getName());
            pstatement.setString(2, newItem.getImage());
            pstatement.setString(3, newItem.getDescription());
            int affectedRows = pstatement.executeUpdate();
            if(affectedRows == 0){
                return -1;
            }
            result = pstatement.getGeneratedKeys();
            if(result != null && result.next()) {
                return result.getInt(1);
            }
        }catch (SQLException sqle){
            sqle.printStackTrace();
        }

        return -1;
    }

    /**
     * @author Marco D'Antini
     * @param
     * @return the item with its attributes
     */
    public Item getItemById(int idauction) throws SQLException {
        String query = "SELECT name, description, image FROM (item NATURAL JOIN auction) WHERE idauction = ?";
        ResultSet result = null;
        Item item = new Item();
        PreparedStatement pstatement = null;
        try{
            pstatement = con.prepareStatement(query);
            pstatement.setInt(1, idauction );
            result = pstatement.executeQuery();
            while(result.next()){
                item.setIdItem(idauction);
                item.setName(result.getString("name"));
                item.setDescription(result.getString("description"));
                item.setImage(result.getString("image"));
            }
        }catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException(e);
        } finally {
            try {
                result.close();
            } catch (Exception e1) {
                throw new SQLException(e1);
            }
            try {
                pstatement.close();
            } catch (Exception e2) {
                throw new SQLException(e2);
            }
        }
        return item;
    }
}

