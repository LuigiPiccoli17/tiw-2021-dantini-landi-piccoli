package it.polimi.tiw.js.beans;

public class ExtendedAuction extends Auction {
    private String itemName;
    private String itemDescription;
    private String itemImage;
    private Float price;


    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemImage() {
        return itemImage;
    }

    public void setItemImage(String itemImage) {
        this.itemImage = itemImage;
    }

    public boolean isOpen(){
        if(this.getStatus().getValue()==0){
            return true;
        }

        return false;
    }

    public boolean isClose(){
        if(this.getStatus().getValue()==1){
            return true;
        }

        return false;
    }
}
