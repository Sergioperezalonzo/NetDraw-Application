# NetDraw application
 * A program (NetDraw.java and NetDrawServer) that send information to another computer(sever). The panel allows you to draw and stamp objects to the panel. When this program is run, it starts a netgame Hub that will listen on port xxx until this program is shut down.  The Hub is a basic hub that simply forwards any messages that it gets from clients to all connected clients (including the client who sent the message).       The NetDraw application allows uses to work together to   create simple drawings.  Everything drawn by one user   is transmitted to the NetDraw server (or "hub"), which   then forwards a copy to all connected clients (including   the client that sent that item to the server).  Every   client who gets the message -- except the one who sent it --   can then add the item to its own picture.  This means   that an item drawn by one client is added to every    client's picture.  Items are transmitted as strings that    describe the thing that was drawn.  Only two types of item    are supported: Lines and stamps.         A line has a color, a stroke number and two endpoints (x1,y1) and (x2,y2).   The string for a line consists of the word "line" followed by eight   numbers, all separated by spaces.  The eight numbers are the R, G, and B   values of the color, the index of the stroke in the stroke array,  and the    coordinates x1, y1, x2, and y2.  When the user drags the mouse while using   the "DRAW FREEHAND CURVES" tool, a sequence of lines is produced.         A stamp is one of a dozen images that are available in the   Tool menu.  When the user clicks the mouse while using a    stamp tool, a copy of the stamp is placed at the point where   the user clicked.  The string for a stamp consists of   the word "stamp" followed by 3 numbers.  The numbers are   the index of the stamp in the stamps array and the x and y coordinates where the stamp is placed.
