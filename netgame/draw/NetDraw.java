/**
 * A program that send information to another computer(sever). The panel allows you to draw and stamp objects to the panel.
 * @author Sergio Perez
 *@author David Eck
 */
package netgame.draw;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import netgame.common.*;
import java.util.Scanner;
/**
 * This subclass of JPanel defines a client for the NetDraw application.
 * 
 *    The NetDraw application allows uses to work together to
 * create simple drawings.  Everything drawn by one user
 * is transmitted to the NetDraw server (or "hub"), which
 * then forwards a copy to all connected clients (including
 * the client that sent that item to the server).  Every 
 * client who gets the message -- except the one who sent it --
 * can then add the item to its own picture.  This means
 * that an item drawn by one client is added to every 
 * client's picture.  Items are transmitted as strings that 
 * describe the thing that was drawn.  Only two types of item 
 * are supported: Lines and stamps.
 * 
 *    A line has a color, a stroke number and two endpoints (x1,y1) and (x2,y2).
 * The string for a line consists of the word "line" followed by eight
 * numbers, all separated by spaces.  The eight numbers are the R, G, and B
 * values of the color, the index of the stroke in the stroke array,  and the 
 * coordinates x1, y1, x2, and y2.  When the user drags the mouse while using
 * the "DRAW FREEHAND CURVES" tool, a sequence of lines is produced.
 * 
 *    A stamp is one of a dozen images that are available in the
 * Tool menu.  When the user clicks the mouse while using a 
 * stamp tool, a copy of the stamp is placed at the point where
 * the user clicked.  The string for a stamp consists of
 * the word "stamp" followed by 3 numbers.  The numbers are
 * the index of the stamp in the stamps array and the x and y 
 * coordinates where the stamp is placed.
 */
public class NetDraw extends JPanel {

    // A main program to allow this class to be run as an application.
    public static void main(String[] args) {
        JFrame window = new JFrame();
        NetDraw content = new NetDraw();
        window.setContentPane(content);
        window.setJMenuBar(content.createMenuBar());
        window.pack();
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocation(50,50);
        window.setVisible(true);
    }

    //-----------------------------------------------------------------------------------

    private JLabel message;           // For displaying messages to the user.
    private Display display;          // The display area where the drawing is shown.
    private BufferedImage image;      // An off-screen copy of the drawing.
    private Graphics2D imageGraphics; // For drawing to the off-screen copy.
    private BufferedImage[] stamps;   // An array containing the 12 stamp images, created in constructor.
    private int tool = -1;            // Current tool; -1 means curve, >= 0 is a stamp number.
    private Color color;              // Current color for drawing lines.
    private int lineWidthIndex;       // Current index into the strokeList array, for drawing lines.

    private static BasicStroke[] strokeList; // Strokes of different line widths, created in constructor.
    private static int[] strokeWidths = { 1, 2, 3, 4, 5, 7, 10, 15, 20 };  // stroke widths for "Line Width" menu

    private static Color[] colorList = { // Standard colors for the "Curve Color" menu.
            Color.BLACK, Color.RED, new Color(0,180,0), Color.BLUE, Color.YELLOW, new Color(150,0,150)
    };
    private static String[] colorNames = { // Color names for colors in the colorList array.
            "Black", "Red", "Green", "Blue", "Yellow", "Purple" 
    };


    /**
     * This class defines the display area of the panel, where
     * the drawing is shown.  All drawing is actually done to the
     * off-screen copy, image.  The paintComponent() method in this
     * class simply copies the image to the display.
     */
    private class Display extends JPanel {
        protected void paintComponent(Graphics g) {
            synchronized( NetDraw.this ) {
                g.drawImage(image,0,0,getWidth(),getHeight(),null);
            }
        }
    }


    /**
     * This class defines the MouseListener that handles mouse clicking
     * and dragging.  All the actual work of drawing is actually done by 
     * calling the drawStamp() and drawLine() methods in the DrawPanel class.
     */
    private class MouseHandler implements MouseListener, MouseMotionListener {
        private int prevX, prevY;
        private boolean dragging;
        public void mousePressed(MouseEvent evt) {
            if (dragging)
                return;
            if (tool >= 0) {
                drawStamp(tool, evt.getX(), evt.getY());
                return;
            }
            dragging = true;
            prevX = evt.getX();
            prevY = evt.getY();
        }
        public void mouseDragged(MouseEvent evt) {
            if (!dragging)
                return;
            int thisX = evt.getX();
            int thisY = evt.getY();
            drawLine(color, lineWidthIndex, thisX, thisY, prevX, prevY);
            prevX = thisX;
            prevY = thisY;
        }
        public void mouseReleased(MouseEvent evt) {
            dragging = false;
        }
        public void mouseMoved(MouseEvent evt) { }
        public void mouseClicked(MouseEvent evt) { }
        public void mouseEntered(MouseEvent evt) { }
        public void mouseExited(MouseEvent evt) { }
    }


    /**
     * The DrawPanel constructor asks the user to enter the IP address
     * or host name of the server.  It then opens a connection to the
     * server, which will be represented by the instance variable named
     * "client".  If an error occurs, or if the user cancels, the
     * program ends.  Otherwise, the panel is created with a large
     * display area and a message lable at the bottom.
     */
    public NetDraw() {
        display = new Display();
        display.setPreferredSize(new Dimension(800,600));
        message = new JLabel("Not Connected");
        message.setBackground(Color.LIGHT_GRAY);
        message.setOpaque(true);
        message.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        imageGraphics = image.createGraphics();
        imageGraphics.setColor(Color.WHITE);
        imageGraphics.fillRect(0, 0, 800, 600);
        color = Color.BLACK;
        lineWidthIndex = 3;
        strokeList = new BasicStroke[strokeWidths.length];
        for (int i = 0; i < strokeList.length; i++) {
            strokeList[i] = new BasicStroke(strokeWidths[i],BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
        }
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        stamps = new BufferedImage[12];
        for (int i = 0; i < 12; i++) {
            try {
                String resname = "netgame/draw/stamps/icon" + i + ".png";
                URL resloc = getClass().getClassLoader().getResource(resname);
                stamps[i] = ImageIO.read(resloc);
            }
            catch (Exception e) {
            }
        }
        setLayout(new BorderLayout(3,3));
        setBackground(Color.GRAY);
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(display,BorderLayout.CENTER);
        add(message,BorderLayout.SOUTH);
        MouseHandler hndl = new MouseHandler();
        addMouseListener(hndl);
        addMouseMotionListener(hndl);
    }


    /**
     * Creates a menu bar containing just a Tool menu.
     * @return the menu bar that was created
     */
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu toolMenu = new JMenu("Tool");
        menuBar.add(toolMenu);
        ActionListener toolListener = new ActionListener() { // listener fo Tools menu
            public void actionPerformed(ActionEvent evt) {
                String cmd = evt.getActionCommand();
                if (cmd.equals("Clear Drawing")) {
                    imageGraphics.setColor(Color.WHITE);
                    imageGraphics.fillRect(0, 0, display.getWidth(), display.getHeight());
                    repaint();
                }
                else if (cmd.equals("DRAW FREEHAND CURVES"))
                    tool = -1;
                else // The tool is one of the stamps
                    tool = Integer.parseInt(cmd.substring(13));
            }
        };
        JMenuItem clear = new JMenuItem("Clear Drawing");
        clear.addActionListener(toolListener);
        toolMenu.add(clear);
        toolMenu.addSeparator();
        JMenuItem curves = new JMenuItem("DRAW FREEHAND CURVES");
        curves.addActionListener(toolListener);
        toolMenu.add(curves);
        toolMenu.addSeparator();
        for (int i = 0; i < 12; i++) {
            JMenuItem item = new JMenuItem("Stamp Number " + i);
            item.setIcon(new ImageIcon(stamps[i]));
            item.addActionListener(toolListener);
            toolMenu.add(item);
        }

        JMenu colorMenu = new JMenu("Curve Color");
        menuBar.add(colorMenu);
        ActionListener colorListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String cmd = evt.getActionCommand();
                if (cmd.equals("Custom Color...")) {
                    Color c = JColorChooser.showDialog(NetDraw.this, "Select Curve Color", color);
                    if (c != null)
                        color = c;
                }
                for (int i = 0; i < colorNames.length; i++) {
                    if (cmd.equals(colorNames[i])) {
                        color = colorList[i];
                        break;
                    }
                }
            }
        };
        for (String c : colorNames) {
            JMenuItem item = new JMenuItem(c);
            item.addActionListener(colorListener);
            colorMenu.add(item);
        }
        JMenuItem colorItem = new JMenuItem("Custom Color...");
        colorItem.addActionListener(colorListener);
        colorMenu.add(colorItem);

        JMenu strokeMenu = new JMenu("Line Width");
        menuBar.add(strokeMenu);
        ActionListener strokeListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int w = Integer.parseInt(evt.getActionCommand());
                for (int i = 0; i < strokeWidths.length; i++) {
                    if (w == strokeWidths[i]) {
                        lineWidthIndex = i;
                        break;
                    }
                }
            }
        };
        for (int w : strokeWidths) {
            JMenuItem item = new JMenuItem("" + w);
            item.addActionListener(strokeListener);
            strokeMenu.add(item);
        }

        JMenu connectMenu = new JMenu("Connection");
        menuBar.add(connectMenu);
        ActionListener connectListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (evt.getActionCommand().equals("Connect..."))
                    doConnect();
                else
                    doDisconnect();
            }
        };
        JMenuItem connect = new JMenuItem("Connect...");
        connect.addActionListener(connectListener);
        connectMenu.add(connect);
        JMenuItem disconnect = new JMenuItem("Disconnect");
        disconnect.addActionListener(connectListener);
        connectMenu.add(disconnect);
        disconnect.setEnabled(false);
        menuConnect = connect;
        menuDisconnect = disconnect;

        return menuBar;
    }

    //-----------------------------------------------------------------------------------


    /**
     * This class defines the client object that represents the connection
     * to the server.  It has a method that responds to messages received
     * from the server by calling the netMessageReceived() method in the
     * DrawPanel class.  The DrawPanel class has a variable named client
     * of type DrawClient.  This variable has a method client.send(msg)
     * that can be called to send msg to the server, which then forwards
     * it to all the clients that are connected to the server.
     */
    private class DrawClient extends Client {
        public DrawClient(String hubHostName, int hubPort) throws IOException {
            super(hubHostName, hubPort);
            myConnectionID = getID();
        }
        protected void messageReceived(Object message) {
            if (message instanceof ForwardedMessage) {
                ForwardedMessage msg = (ForwardedMessage)message;
                if (msg.message instanceof String) {
                    // This is message, forwarded by the server, from one of the clients.


                    netMessageReceived((String)msg.message, msg.senderID);
                }
            }
        }
    }


    private DrawClient client;        // Represents the connection to the server, or null if there is none.
    private int myConnectionID;       // Every client has an ID, assigned by the server; this is mine.

    private JMenuItem menuConnect;    // The "Connect" command in the Connection menu.
    private JMenuItem menuDisconnect; // The "Disconnect" command in the Connection menu.


    /**
     * This method is called in response to the "Connect" command in the Connection menu
     * It should ask the user for a host name and then try to connect to a NetDraw server
     * on that host.  If successful, it should get a value for myConnectionID from the server.
     */
    private void doConnect() {
        // TODO: Create a client (if there's not one already).
        
        
        String host = JOptionPane.showInputDialog(this, "Enter host name or IP");
        
        
        
        if(host != null){
            
            try{
                
            client = new DrawClient("localhost", 32001); // or you can use the host string (337)
            message.setText(host + " is Connected......");
            
            }
            catch (IOException e){
                message.setText("Not Connected " + e);
            }
            
        } else {
            message.setText(host + " is NOT-Connected......");
        }
        menuConnect.setEnabled(false);
        menuDisconnect.setEnabled(true);
        
    }
    /**
     * This method is called in response to the "Disconnect" command in the Connection menu.
     * If a connection is open, this should close it down, and client should be reset to null.
     */
    private void doDisconnect() {
        // TODO: Close down the connection (if there is one).


        if(client != null){

            client.disconnect();
            client = null;


        }
        menuConnect.setEnabled(true);
        menuDisconnect.setEnabled(false);

    }


    /**
     * This method is called when the user places a stamp in the drawing.
     * It draws the stamp to the image in this panel.  Furthermore, if there
     * is a connection to a server, then this method also sends a message
     * to the server about the stamp.  NOTE: This method should NOT be
     * called in response to a message from the server, since that would
     * result in ANOTHER message being sent back to the server!
     * @param stampNumber The index of the stamp in the array of available stamps
     * @param x the x-coordinate where the stamp is to be placed
     * @param y the x-coordinate where the stamp is to be placed
     */
    synchronized private void drawStamp(int stampNumber, int x, int y) {
        BufferedImage img = stamps[stampNumber];
        imageGraphics.drawImage(img, x - img.getWidth()/2, y - img.getHeight()/2, null);
        display.repaint();
        // TODO:  send this drawing operation over the network
        if(client != null){
            String message = "stamp " + stampNumber + " " + x + " " + y;
            client.send(message);
        }
    }


    /**
     * This method is called when the user drags the mouse from one point to 
     * another.  A line is drawn between the two points with the stroke indicated
     * by strokeIndex and in the specified color.  Furthermore, if there is a connection,
     * then a message is sent to the server about the line.  NOTE: This method
     * should NOT be called in response to a message from the server, since 
     * that would result in ANOTHER message being sent back to the server!
     * @param lineColor the color of the line
     * @param strokeIndex the index in the stroke array of the stroke for the line
     * @param x1 x-coord of the first endpoint
     * @param y1 y-coord of the first endpoint
     * @param x2 x-coord of the second endpoint
     * @param y2 x-coord of the second endpoint
     */
    synchronized private void drawLine(Color lineColor, int strokeIndex, 
            int x1, int y1, int x2, int y2) {
        Rectangle rect = new Rectangle(x1,y1);
        rect.add(x2,y2);
        rect.grow(12,12);
        imageGraphics.setColor(lineColor);
        imageGraphics.setStroke( strokeList[strokeIndex] );
        imageGraphics.drawLine(x1, y1, x2, y2);
        display.repaint(rect);
        // TODO:  send this drawing operation over the network
        if(client != null){

            String message = "line " + lineColor.getRed() + " " +
                    lineColor.getGreen() + " " + lineColor.getBlue() + " " + strokeIndex +
                    " " + x1 + " " + y1 + " " + x2 + " " + y2;
            client.send(message);

        }
    }


    /**
     * This method is called when a string is received as a message from
     * the server.  The message actually comes from one of the clients that
     * are connected to the server.  Note then when the server gets a 
     * message from THIS client, it sends a copy of that message back
     * to this client!  Messages that originated from this client should
     * be ignored because the graphics item represented by the message
     * has already been drawn by this client.  You can tell that a message
     * comes from this same server by checking the senderID.
     * @param stringReceived the message from the server, which should
     *    describe either a stamp or a line.
     * @param senderID the ID number of the client who sent the message
     *    to the server.  If this is the same as my ID, the message
     *    should be ignored.
     */
    synchronized private void netMessageReceived(String stringReceived, int senderID) {
        // TODO:  react to a message received from the network
        //synchronized private void drawStamp(int stampNumber, int x, int y) {

        Scanner read = new Scanner(stringReceived);

        if(senderID != myConnectionID){


            if(read.next().equals("line")){

                int[] data;
                data = new int[8];

                for(int i = 0; i <= 7; i++){

                    data[i] = read.nextInt();
                }

                Color newColor = new Color(data[0],data[1],data[2]);

                ////Code to draw a line////

                Rectangle rect = new Rectangle(data[4],data[5]);
                rect.add(data[6],data[7]);
                rect.grow(12,12);
                imageGraphics.setColor(newColor);
                imageGraphics.setStroke( strokeList[data[3]] );
                imageGraphics.drawLine(data[4], data[5], data[6], data[7]);
                display.repaint(rect);


            } else  {

                BufferedImage img = stamps[read.nextInt()];
                imageGraphics.drawImage(img, read.nextInt() - img.getWidth()/2, read.nextInt() - img.getHeight()/2, null);
                display.repaint();


                


            }
        }

    }


}
