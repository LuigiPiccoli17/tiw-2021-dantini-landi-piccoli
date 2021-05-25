package it.polimi.tiw.js.dao;

import it.polimi.tiw.js.beans.Bid;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BidDAO {
    private Connection con;

    public BidDAO(Connection con){
        this.con=con;
    }

    /***
     * @author Alfredo Landi
     * @param auctionId of the current auction
     * @return a list o bids for the current auction
     */
    public List<Bid> findBidsByIdAuction(int auctionId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String query = "SELECT idbid, bidprice, UNIX_TIMESTAMP(datetime) AS datetime, idbidder, idauction" +
                " FROM bid WHERE idauction = ? ORDER BY datetime DESC";
        try (PreparedStatement pstatement = con.prepareStatement(query)) {
            pstatement.setInt(1, auctionId);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Bid bid = new Bid();
                    bid.setIdBid(result.getInt("idbid"));
                    bid.setBidPrice(result.getFloat("bidprice"));
                    bid.setDateTime(new Date(result.getLong("datetime") * 1000));
                    bid.setIdBidder(result.getInt("idbidder"));
                    bid.setIdAuction(result.getInt("idauction"));
                    bids.add(bid);
                }
            }

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return bids;
    }

    /**
     * @author Marco D'Antini
     * query used to insert a new legit Bid into the daatabase called by  GotoBidPage
     * @param bidPrice
     * @param idBidder
     * @param idAuction
     * @return the idBid of the bid added in the database, 0 in case of db error
     * @throws SQLException
     */
    public int insertNewBid(float bidPrice, int idBidder, int idAuction)throws SQLException{
        ResultSet result;
        Date date = new Date();
        Long dateTime = date.getTime();
        String query = "INSERT INTO bid ( bidprice, datetime, idbidder, idauction) VALUES (?,?,?,?)";

        try (PreparedStatement pstatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setFloat(1, bidPrice);
            pstatement.setLong(2, dateTime * 1000);
            pstatement.setInt(3, idBidder);
            pstatement.setInt(4, idAuction);
            int affectedRows = pstatement.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }
            result = pstatement.getGeneratedKeys();
            if(result!= null && result.next())
                return result.getInt(1);
        }catch (SQLException sqle){
            sqle.printStackTrace();
        }
        return -1;
    }
}