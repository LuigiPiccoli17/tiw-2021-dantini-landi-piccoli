package it.polimi.tiw.js.dao;

import it.polimi.tiw.js.beans.Auction;
import it.polimi.tiw.js.beans.AuctionStatus;
import it.polimi.tiw.js.beans.ExtendedAuction;
import it.polimi.tiw.js.beans.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class AuctionDAO {
    private Connection con;

    public AuctionDAO(Connection connection) {
        this.con = connection;
    }

    /**
     * this query returns the list of sorted by date auctions filtered by keyword
     * @author Marco D'Antini
     * @param keyword
     * @return
     * @throws SQLException
     */
    public List<ExtendedAuction> findOpenAuction(String keyword, int userid) throws SQLException {
        List<ExtendedAuction> searchedList= new ArrayList<>();

        String query = "SELECT idauction, UNIX_TIMESTAMP(deadline) AS deadline, minraise, initialprice, name FROM " +
                "(item NATURAL JOIN auction) WHERE auction.idcreator != ? AND (item.name LIKE ? OR " +
                "item.description LIKE ?) AND auction.status = 0 ORDER BY auction.deadline DESC";
        try (PreparedStatement pstatement = con.prepareStatement(query)) {
            pstatement.setInt(1, userid);
            pstatement.setString(2, "%" + keyword + "%");
            pstatement.setString(3, "%" + keyword + "%");

            try (ResultSet result = pstatement.executeQuery()) {
                if (!result.isBeforeFirst()) // no results
                    return null;
                else {
                    while (result.next()) {
                        ExtendedAuction auction = new ExtendedAuction();
                        auction.setInitialPrice(result.getInt("initialprice"));
                        auction.setMinRaise(result.getInt("minraise"));
                        auction.setDeadline(new Date(result.getLong("deadline") * 1000));
                        auction.setIdAuction(result.getInt("idauction"));
                        auction.setItemName(result.getString("name"));
                        searchedList.add(auction);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException(e);
        }
        return searchedList;
    }

    /**
     * @author Alfredo Landi
     * @param status of an auction, it can be "0" -> CLOSED or "1" -> OPEN
     * @return a list of auctions for the selected status in ascending order by deadline
     */
        public List<ExtendedAuction> findAuctionsByIdAndStatus(int idUser, AuctionStatus status) throws SQLException{
            List<ExtendedAuction> auctions = new ArrayList<>();
            //FIXME: must fix SQL query
            String query = "SELECT auction.idauction, item.name, item.image, item.description, max(bid.bidprice) AS price, auction.minraise, UNIX_TIMESTAMP(auction.deadline) AS deadline FROM auction NATURAL JOIN item LEFT JOIN bid ON auction.idauction=bid.idauction JOIN user ON auction.idcreator=user.iduser WHERE user.iduser=? AND auction.status=? GROUP BY auction.idauction ORDER BY auction.deadline ASC";
            PreparedStatement pstatement = null;
            ResultSet result = null;

            try {
                pstatement = con.prepareStatement(query);
                pstatement.setInt(1, idUser);
                pstatement.setInt(2, status.getValue());
                result = pstatement.executeQuery();
                while (result.next()) {
                    ExtendedAuction auction = new ExtendedAuction();
                    auction.setIdAuction(result.getInt("idauction"));
                    auction.setItemName(result.getString("name"));
                    auction.setItemImage(result.getString("image"));
                    auction.setItemDescription(result.getString("description"));
                    auction.setPrice(result.getFloat("price"));
                    auction.setMinRaise(result.getFloat("minraise"));
                    auction.setDeadline(new Date(result.getLong("deadline") * 1000));
                    auctions.add(auction);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new SQLException(e);
            }finally {
                try{
                    if(result!=null){
                        result.close();
                    }
                }catch(SQLException e1){
                    e1.printStackTrace();
                }
                try{
                    if(pstatement!=null){
                        pstatement.close();
                    }
                }catch(SQLException e2){
                    e2.printStackTrace();
                }
            }
            return auctions;
        }

    /***
     * @author Alfredo Landi
     * This method prepares two queries, one to item relation to add a new item in the db
     * and another one to auction relation to initialize the new auction
     * @param itemName of new item added by user
     * @param itemImage of new item added by user
     * @param itemDescription of new item added by user
     * @param initialPrice of the auction selected by user
     * @param minRaise of the auction selected by user
     * @param deadline of the auction selected by user
     * @param idCreator of the user
     * @return code of success or unsuccess
     */
        public int insertNewAuction(String itemName, String itemImage, String itemDescription, float initialPrice, float minRaise, Date deadline, int idCreator) throws SQLException{
            int itemId;
            Item newItem = new Item(itemName, itemImage, itemDescription);
            ItemDAO im = new ItemDAO(con);
            PreparedStatement pstatement = null;
            int result = 0;
            String query= "INSERT INTO auction (initialprice, minraise, deadline, idcreator, iditem, status) VALUES (?,?,?,?,?,?)";
            try{
                con.setAutoCommit(false);
                itemId=im.insertNewItem(newItem);
                if(itemId==-1){
                    throw new SQLException();
                }
                pstatement = con.prepareStatement(query);
                pstatement.setFloat(1,initialPrice);
                pstatement.setFloat(2, minRaise);
                pstatement.setDate(3, (java.sql.Date)deadline);
                pstatement.setInt(4, idCreator);
                pstatement.setInt(5, itemId);
                pstatement.setInt(6, AuctionStatus.OPEN.getValue());
                result= pstatement.executeUpdate();
                con.commit();

            }catch(SQLException sqle){
                sqle.printStackTrace();
            }finally {
                try{
                    if(pstatement!=null){
                        pstatement.close();
                    }
                }catch(SQLException e1){
                    e1.printStackTrace();
                }
            }

            return result;
        }

    /***
     * @author Alfredo Landi
     * Set CLOSED (1) the attribute "status" of the selected auction
     * @param auctionId of the auction to close
     * @return 0 for success, 1 for unsuccess
     */
    public int closeAuction(int auctionId){
        int code=0;
        String query = "UPDATE auction SET status=? WHERE idauction = ?";
        PreparedStatement pstatement = null;
        try{
            pstatement = con.prepareStatement(query);
            pstatement.setInt(1, AuctionStatus.CLOSED.getValue());
            pstatement.setInt(2, auctionId);
            code = pstatement.executeUpdate();
        }catch(SQLException sqle){
            sqle.printStackTrace();
        }finally {
            try{
                if(pstatement!=null){
                    pstatement.close();
                }
            }catch(SQLException e1){
                e1.printStackTrace();
            }
        }

        return code;
    }

    /***
     * @author Alfredo Landi
     * @param auctionId of the auction needed
     * @return the required auction
     */
    public Auction findAuctionById(int auctionId) throws SQLException{
        String query = "SELECT * FROM auction WHERE idauction = ?";
        PreparedStatement pstatement = null;
        ResultSet result = null;
        ExtendedAuction auction = new ExtendedAuction();

        try{
            pstatement = con.prepareStatement(query);
            pstatement.setInt(1, auctionId);
            result=pstatement.executeQuery();
            auction.setIdAuction(result.getInt("auctionId"));
            auction.setInitialPrice(result.getFloat("initialprice"));
            auction.setMinRaise(result.getFloat("minraise"));
            auction.setDeadline(result.getDate("deadline"));
            auction.setIdCreator(result.getInt("idcreator"));
            auction.setIdItem(result.getInt("iditem"));
            auction.setStatus(AuctionStatus.OPEN);
        }catch (SQLException sqle){
            sqle.printStackTrace();
            throw new SQLException(sqle);
        }finally {
            try{
                if(result!=null){
                    result.close();
                }
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            try{
                if(pstatement!=null){
                    pstatement.close();
                }
            }catch(SQLException e2){
                e2.printStackTrace();
            }
        }

        return auction;
    }

    /**
     * @author Marco D'Antini
     * used to prevent not legit usage of database by the user
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<Integer> findLegitIdsBid(int userId) throws SQLException{
        String query = "SELECT idauction from auction WHERE idcreator != ? AND status = 0";
        List<Integer> idList = new ArrayList<>();
        ResultSet result = null;

        try(PreparedStatement pstatement = con.prepareStatement(query)){
            pstatement.setInt(1, userId);
            result = pstatement.executeQuery();
            while(result.next()){
                idList.add(result.getInt("idauction"));
            }
        }catch (SQLException sqle){
            sqle.printStackTrace();
        } finally {
            if(result!=null){
                result.close();
            }
        }
        return idList;
    }



}
