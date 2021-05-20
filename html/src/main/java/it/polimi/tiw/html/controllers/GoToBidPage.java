package it.polimi.tiw.html.controllers;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.html.beans.Bid;
import it.polimi.tiw.html.beans.Item;
import it.polimi.tiw.html.beans.User;
import it.polimi.tiw.html.dao.BidDAO;
import it.polimi.tiw.html.dao.ItemDAO;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import it.polimi.tiw.html.beans.Auction;
@WebServlet("/GoToBidPage")
public class GoToBidPage extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;
    private float currMaxPrice;
    private int idAuctionPub = 0;
    public GoToBidPage(){super();}

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
        try {
            ServletContext context = getServletContext();
            String driver = context.getInitParameter("dbDriver");
            String url = context.getInitParameter("dbUrl");
            String user = context.getInitParameter("dbUser");
            String password = context.getInitParameter("dbPassword");
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new UnavailableException("Can't load database driver");
        } catch (SQLException e) {
            throw new UnavailableException("Couldn't get db connection");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException{
        int idAuction;
        Item item ;
        List<Bid> bids;

        HttpSession s = request.getSession();
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request,response,servletContext,request.getLocale());

        try{
            idAuction = Integer.parseInt(request.getParameter("idauction"));
            idAuctionPub = idAuction;
        } catch ( NumberFormatException | NullPointerException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "incorrect param values");
            return;
        }


        try{
            ItemDAO itemDAO = new ItemDAO(connection);
            item = itemDAO.getItemById(idAuction);
            if(item == null){
                ctx.setVariable("errorMsg", "No info for this article found.");
            }else {
                ctx.setVariable("item", item);
            }
        }catch (SQLException e){
            response.sendError(500, "Database access failed");
        }
        try{
            BidDAO bidDAO = new BidDAO(connection);
            bids = bidDAO.findBidsByIdAuction(idAuction);
            if(bids == null || bids.isEmpty()) {
                currMaxPrice = (float) 0;

                ctx.setVariable("errorMsg", "No bids for this article found.");
            }else
                for(Bid b: bids){
                    if(b.getBidPrice()>currMaxPrice)
                        currMaxPrice = b.getBidPrice();
                }
                ctx.setVariable("bids", bids);
                ctx.setVariable("currMax", currMaxPrice);
        }catch(SQLException e){
            response.sendError(500, "Database access failed");
        }

        String path = "/WEB-INF/GoToBidPage.html";
        templateEngine.process(path, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException{
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        String price = request.getParameter("price");
        Float fPrice = (float) 0;
        BidDAO bidDAO = new BidDAO(connection);
        User user = (User) request.getSession().getAttribute("user");
        //Auction auction = (Auction) request.getSession().getAttribute("auction");
        int idBidder = user.getIdUser();
        int idAuction = idAuctionPub;

        if ( price == null || price.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter missing");
        }
        try{
            fPrice = Float.parseFloat(price);
        }catch(NumberFormatException e){
            ctx.setVariable("errorMsg", "Format wrong!");
        }
        if(currMaxPrice< fPrice){
            try{
                bidDAO.insertNewBid(fPrice, idBidder,idAuction);
            } catch (SQLException sqle) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "database error");
            }

        }else{
            ctx.setVariable("errorMsg", "This price is too low.");
        }
        String path = "/WEB-INF/GoToBidPage.html";
        templateEngine.process(path, ctx, response.getWriter());
    }

    @Override
    public void destroy(){
        try{
            if(connection != null){
                connection.close();
            }
        }catch (SQLException sqle){}
    }
}
