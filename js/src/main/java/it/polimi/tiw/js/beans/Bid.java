package it.polimi.tiw.js.beans;

import java.util.Date;

public class Bid {
    private int idBid;
    private float bidPrice;
    private Date dateTime;
    private int idBidder;
    private int idAuction;

    public int getIdBid() {
        return idBid;
    }

    public void setIdBid(int idBid) {
        this.idBid = idBid;
    }

    public float getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(float bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public int getIdBidder() {
        return idBidder;
    }

    public void setIdBidder(int idBidder) {
        this.idBidder = idBidder;
    }

    public int getIdAuction() {
        return idAuction;
    }

    public void setIdAuction(int idAuction) {
        this.idAuction = idAuction;
    }
}