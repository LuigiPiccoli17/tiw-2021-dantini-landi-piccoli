package it.polimi.tiw.js.dao;

import it.polimi.tiw.js.beans.ExtendedAuction;
import it.polimi.tiw.js.beans.ExtendedBid;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BidDAO {
    private Connection con;

    public BidDAO(Connection con) {
        this.con = con;
    }

    /***
     * @author Alfredo Landi
     * @param auctionId of the current auction
     * @return a list of bids for the current auction
     */
    public List<ExtendedBid> findBidsByIdAuction(int auctionId) throws SQLException {
        List<ExtendedBid> bids = new ArrayList<>();
        String query = "SELECT username, bidprice, UNIX_TIMESTAMP(datetime) AS datetime " +
                "FROM bid JOIN user ON idbidder=iduser WHERE idauction = ? ORDER BY datetime DESC";
        try (PreparedStatement pstatement = con.prepareStatement(query)) {
            pstatement.setInt(1, auctionId);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    ExtendedBid bid = new ExtendedBid();
                    bid.setBidderUsername(result.getString("username"));
                    bid.setBidPrice(result.getFloat("bidprice"));
                    bid.setDateTime(ZonedDateTime.ofInstant(Instant.ofEpochSecond(result.getLong("datetime")), ZoneOffset.UTC));
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
     * query used to find the ended auction awarded by the user
     */
    public List<ExtendedAuction> findWonBids(int idBidder) throws SQLException {
        ArrayList<ExtendedAuction> bidsAwarded = new ArrayList<>();
        String query = "SELECT bidprice, UNIX_TIMESTAMP(datetime) AS datetime, name, description, image " +
                "FROM (bid NATURAL JOIN auction a2 NATURAL JOIN item) WHERE a2.status = 1 AND" +
                " bid.idbidder = ? AND bidprice = (SELECT MAX(bidprice) FROM bid NATURAL JOIN auction a1" +
                " WHERE a1.iditem = a2.iditem)";
        /*String query = "SELECT bidprice, UNIX_TIMESTAMP(datetime) AS datetime, name, description, image " +
                "FROM (bid NATURAL JOIN auction NATURAL JOIN item) WHERE auction.status = 1 AND bid.idbidder = ?";*/
        try (PreparedStatement pstatement = con.prepareStatement(query)) {
            pstatement.setInt(1, idBidder);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    ExtendedAuction exAuction = new ExtendedAuction();
                    exAuction.setPrice(result.getFloat("bidprice"));
                    exAuction.setItemName(result.getString("name"));
                    exAuction.setItemDescription(result.getString("description"));
                    exAuction.setItemImage(result.getString("image"));
                    bidsAwarded.add(exAuction);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return bidsAwarded;
    }


    /**
     * @author Marco D'Antini
     * query used to find the latest visited auctions.
     * @param auctionIDs: array of string . collected from the local storage of the browser
     */
    public List<ExtendedAuction> findLatestAuctions(String[] auctionIDs) throws SQLException {
        List<ExtendedAuction> latestAuctions = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for( int i = 0 ; i < auctionIDs.length; i++ ) {
            builder.append("?,");
        }
        String placeHolders =  builder.deleteCharAt( builder.length() -1 ).toString();
        /*String query = "SELECT idauction, name, description, MAX(bidprice) FROM auction NATURAL JOIN " +
                "bid NATURAL JOIN item WHERE idauction in (" + placeHolders + ") AND status = 0 GROUP BY idauction";*/
        String query= "SELECT item.name, item.description, auction.idauction FROM item, bid, auction WHERE" +
                " item.iditem = auction.iditem AND auction.idauction IN ("+placeHolders+")"+
                " AND auction.status = 0 GROUP BY auction.idauction;";
        /*String query = "SELECT bidprice, UNIX_TIMESTAMP(datetime) AS datetime, name, description, image " +
                "FROM (bid NATURAL JOIN auction NATURAL JOIN item) WHERE auction.status = 1 AND bid.idbidder = ?";*/
        try (PreparedStatement pstatement = con.prepareStatement(query)) {
            int index = 1;;
            for( String s : auctionIDs ) {
                pstatement.setString(  index++, s ); // or whatever it applies
            }
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    ExtendedAuction exAuction = new ExtendedAuction();
                    //exAuction.setPrice(result.getFloat("MAX(bidprice)"));
                    exAuction.setIdAuction(result.getInt("idauction"));
                    exAuction.setItemName(result.getString("name"));
                    exAuction.setItemDescription(result.getString("description"));
                    latestAuctions.add(exAuction);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return latestAuctions;
    }




    /**
     * @param bidPrice
     * @param idBidder
     * @param idAuction
     * @return the idBid of the bid added in the database, 0 in case of db error
     * @throws SQLException
     * @author Marco D'Antini
     * query used to insert a new legit Bid into the daatabase called by  GotoBidPage
     */
    public int insertNewBid(float bidPrice, int idBidder, int idAuction) throws SQLException {
        ResultSet result;
        Date date = new Date();
        String query = "INSERT INTO bid ( bidprice, datetime, idbidder, idauction) VALUES (?,now(),?,?)";

        try (PreparedStatement pstatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setFloat(1, bidPrice);
            pstatement.setInt(2, idBidder);
            pstatement.setInt(3, idAuction);
            int affectedRows = pstatement.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }
            result = pstatement.getGeneratedKeys();
            if (result != null && result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return -1;
    }

    /**
     * @return the actual minimum price for a new bid
     * @author Marco D'Antini
     * query the minimum raise and the maximum price offered in the list of bids
     */
    public float findPriceForNewBid(int idAuction) throws SQLException {
        float actualPrice = -1;
        String query = "SELECT MAX(bidprice), minraise FROM (bid NATURAL JOIN auction) WHERE idauction = ?";
        String query2 = "SELECT initialprice, minraise FROM auction WHERE idauction = ? ";
        try {
            con.setAutoCommit(false);
            try (PreparedStatement pstatement = con.prepareStatement(query);
                 PreparedStatement pstatement2 = con.prepareStatement(query2)) {
                pstatement.setInt(1, idAuction);
                pstatement2.setInt(1,idAuction);
                ResultSet result = pstatement.executeQuery();

                if (result != null && result.next()) {
                    if (result.getString("MAX(bidprice)") != null) {
                        actualPrice = result.getFloat("MAX(bidprice)") + result.getFloat("minraise");
                    } else {
                        ResultSet result2 = pstatement2.executeQuery();
                        if (result2 != null && result2.next()) {
                            actualPrice = result2.getFloat("initialprice") + result2.getFloat("minraise");
                        }
                    }
                }
            }
        } catch (SQLException sqle) {
            con.rollback();
            sqle.printStackTrace();
        } finally {
            con.setAutoCommit(true);
        }

        return actualPrice;

    }

    /**
     * @return the minraise for this bid
     * @author Marco D'Antini
     */
    public float findMinRaise(int auctionId) throws SQLException {
        float minR = -1;
        String query = "SELECT minraise FROM auction WHERE idauction = ?";
        try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
            preparedStatement.setInt(1, auctionId);
            ResultSet result = preparedStatement.executeQuery();
            if (result != null && result.next())
                minR = result.getFloat("minraise");
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new SQLException(sqle);
        }
        return minR;
    }

    /***
     * @author Alfredo Landi
     * @param auctionId
     * @return id of the winner
     */
    public int findWinnerIdByAuctionId(int auctionId) throws SQLException {
        String query = "SELECT idbidder FROM auction LEFT JOIN bid ON auction.idauction = bid.idauction " +
                "WHERE auction.idauction = ? AND bidprice = (SELECT max(bidprice) FROM bid WHERE bid.idauction = ?)";
        ResultSet result = null;
        int resultId = 0;

        try (PreparedStatement pstatement = con.prepareStatement(query)){
            pstatement.setInt(1, auctionId);
            pstatement.setInt(2, auctionId);
            result = pstatement.executeQuery();
            if (result.next()) {
                resultId = result.getInt("idbidder");
            }
        } catch (SQLException sqle) {
            throw new SQLException(sqle);
        }

        return resultId;
    }
}
