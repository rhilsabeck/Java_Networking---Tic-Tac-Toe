
package tictactoe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**This is a networked tic tac toe game. One player will choose to be the 
 * server and the other player will be the client. For the first game, the
 * server will go first and then every game after that it will alternate between
 * the server and client. Server will always be "O" and client will always be
 * "X". The two can also chat back and forth during the game via the chat text
 * field. After a game is over, they will both be asked if they want to play
 * again. If the local player doesn't then they will send a message to the 
 * remote player stating that and they app will close down on both sides. If 
 * the local player wants to continue, but the remote player doesn't want to
 * play anymore, then a message will be sent to the local play stating that
 * and the app will close down on both sides. If they both want to play again,
 * the game will reset and gameplay will start again.
 * @author Ryan Hilsabeck
 */
public class TicTacToe 
{

    public static void main(String[] args) throws IOException 
    {
        JFrame frame = new TTTFrame();

        
        String[] possibleValues =
        {
            "Tic Tac Toe Server", "Tic Tac Toe Client"
        };
        String selectedValue = (String)JOptionPane.showInputDialog(frame,
               "Select a Tic Tac Toe Role", "CSD 469/569 Tic Tac Toe",
               JOptionPane.INFORMATION_MESSAGE, null,
               possibleValues, possibleValues[0]);
        System.out.println("You selected " + selectedValue);
        if(selectedValue.contains("Server"))
        {
            NetComm.setUpServer();
        }
        else
        {
            NetComm.setUpClient();
        }
        GameGlobals.init();
        
    }
}
// this class creates frame for the main panel where all UI components will be
// located
class TTTFrame extends JFrame
{
    public TTTFrame() throws IOException
    {
        super("CSC 469 Tic Tac Toe");
        JPanel mainPanel = new MainTTTPanel();
        this.add(mainPanel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }
}
//this mainTTTPanel will add the tttBoard to a gridlayout and put it in a panel
//on the west portion of the panels borderlayout. It will add a text area for 
//the chat history to in on the east portion of the panels borderlayout. It will 
//then put the text field and the status label onto one label and add that to 
//the main panels south portion of the borderlayou
class MainTTTPanel extends JPanel
{
    public MainTTTPanel()
    {
        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(3, 3, 5, 5));
        boardPanel.setPreferredSize(new Dimension(250,250));
        
        
        LocalPlayerListener listener = new LocalPlayerListener();
        
        for(int r = 0; r < GameGlobals.tttBoard.length; r++)
        {
          for(int c = 0; c < GameGlobals.tttBoard[r].length; c++)
          {
              GameGlobals.tttBoard[r][c] = new TTTButton(r, c, PlayerId.NONE);
              GameGlobals.tttBoard[r][c].setOwner(PlayerId.NONE);
              GameGlobals.tttBoard[r][c].addActionListener(listener);
              boardPanel.add(GameGlobals.tttBoard[r][c]);    
          }
        }
        JPanel westPanel = new JPanel();
        westPanel.add(boardPanel);
        this.add(westPanel, BorderLayout.WEST);
        
        JPanel textStatus = new JPanel();
        textStatus.setLayout(new GridLayout(2,1,5,5));
        JPanel textField = new JPanel();
        JPanel statusArea = new JPanel();
        GameGlobals.chatMessageTextField.addActionListener
                (new ChatMessagesListener());
        textField.add(GameGlobals.chatMessageTextField);
        statusArea.add(GameGlobals.gameStatusLabel);
        textStatus.add(textField);
        textStatus.add(statusArea);
        this.add(textStatus, BorderLayout.SOUTH);
        
        GameGlobals.chatHistoryTextArea.setEditable(false);
        JScrollPane textAreaScroll = new JScrollPane
                (GameGlobals.chatHistoryTextArea);
        this.add(textAreaScroll, BorderLayout.EAST);
    }

}

enum PlayerId {LOCAL, REMOTE, NONE};
class GameGlobals
{
   //Game state
   static Boolean isServer;
   static boolean isLocalPlayerTurn;
   static boolean localPlayAgain;
   static String localPlayerMarker;
   static String remotePlayerMarker;
   static int numberCellsFilled;
   static boolean isGameOver = false;
   //Games played, client goes first when GamesCompleted is odd
   static int numberGamesCompleted = 0;

   //User Interface Components
   static final TTTButton[][] tttBoard;
   static final JTextArea chatHistoryTextArea;
   static final JTextField chatMessageTextField;
   static final JLabel gameStatusLabel;
   //Static constructor used to create the UI components
   static
   {
       tttBoard = new TTTButton[3][3];
       chatHistoryTextArea = new JTextArea(20,30);
       chatMessageTextField = new JTextField(50);
       gameStatusLabel = new JLabel();
   }
   //This is the timer that will be checking for network input every second
   static Timer newInputMonitorTimer;

   //This will do all start of game initializations & starts the 
   //timer
   static void init()
   {

     //if the user is the server, it will go through these statements at the
     //start of a new game
     if(GameGlobals.isServer == true)
     {
 
         if(GameGlobals.numberGamesCompleted % 2 == 0)
         {
           GameGlobals.localPlayerMarker = "O";
           GameGlobals.remotePlayerMarker = "X";
           GameGlobals.isLocalPlayerTurn = true;
           GameGlobals.gameStatusLabel.setText("Make your move");
         }
         else
         {
          GameGlobals.localPlayerMarker = "X";
          GameGlobals.remotePlayerMarker = "0";
          GameGlobals.isLocalPlayerTurn = false;
          GameGlobals.gameStatusLabel.setText
                  ("Waiting for remote player's turn");
         }
     }
     //if the user is the client, it will go through these statements at the 
     //start of a new game.
     else
     {

         if(GameGlobals.numberGamesCompleted % 2 != 0)
         {
             GameGlobals.localPlayerMarker = "X";
             GameGlobals.remotePlayerMarker = "O";
             GameGlobals.isLocalPlayerTurn = true;
             GameGlobals.gameStatusLabel.setText("Make your move");
         }
         else
         {
             GameGlobals.localPlayerMarker = "O";
             GameGlobals.remotePlayerMarker = "X";
             GameGlobals.isLocalPlayerTurn = false;
             GameGlobals.gameStatusLabel.setText
                     ("Waiting for remote player's turn");
         }
     }
     //create new timer and add listener and then start the timer     
     NetInputListener listener = new NetInputListener();
     GameGlobals.newInputMonitorTimer = new Timer(1000,listener);

     newInputMonitorTimer.start();
   }
   //this method will process any incoming text from the other user and add
   //to chat history text area
   static void processInput(String input)
   {
       Scanner inputSc = new Scanner(input);
       inputSc.skip("chat");
       while(inputSc.hasNext())
       {
           GameGlobals.chatHistoryTextArea.append(inputSc.next() + " "); 
       }
       GameGlobals.chatHistoryTextArea.append("\n");
      
   }
   //This method processes the remote move. Once the local player makes their
   //move, they will send a protocal across the network with the move location.
   //The timer listener will grab it and send it to this method so that the
   //the remote player can enter their opponents move they just made. It will
   //set the owner of that button as the remote player. It will then check first 
   //to see if the otherplayer has won. If so, some dialog screens will pop up
   //saying that you lost. If they haven't won, it will then check to see if
   //all the cells have been filled up. If that is the case, then the game is
   //a draw. If no one has won and there isn't a draw, the game play will 
   //continue and switch them to the local player so they can make their next
   //move
   static void processRemoteMove(int row, int col)
   {
      
       GameGlobals.tttBoard[row][col].setText(GameGlobals.localPlayerMarker);
       GameGlobals.numberCellsFilled++;
       GameGlobals.tttBoard[row][col].setOwner(PlayerId.REMOTE);
       

       if(GameGlobals.hasWon(PlayerId.REMOTE) == true)
       {
          GameGlobals.gameStatusLabel.setText("YOU LOST!!!YOU LOST!!!YOU LOST" +
                  "!!!YOU LOST!!!");           
          JOptionPane.showMessageDialog(null, "Sorry! You lost!");

       }
       else
       {
       if(GameGlobals.numberCellsFilled ==9)
       {
           GameGlobals.gameStatusLabel.setText("DRAW DRAW DRAW DRAW DRAW");
           JOptionPane.showMessageDialog(null, "The game was a draw!");
       }
       else
       {
       GameGlobals.isLocalPlayerTurn = true;
       if(GameGlobals.localPlayerMarker.equals("O"))
       {
           GameGlobals.localPlayerMarker = "X";
           GameGlobals.remotePlayerMarker = "O";
       }
       else
       {
           GameGlobals.localPlayerMarker = "O";
           GameGlobals.remotePlayerMarker = "X";
       }
             
       
       GameGlobals.gameStatusLabel.setText("Make your move");
       }
       }     
   }
   //This method will check to see after every move if the local or remote
   //player has won.
   static boolean hasWon(PlayerId player)
   {
       //Check Horizontal win
       int i;
       for(i = 0; i < 3; i++)
       {
        if(GameGlobals.tttBoard[i][0].owner == player && 
           GameGlobals.tttBoard[i][1].owner == player &&
           GameGlobals.tttBoard[i][2].owner == player)
        {
            return true;
        }
       }
       //check vertical win
       for(i = 0; i < 3; i++)
       {
       if(GameGlobals.tttBoard[0][i].owner == player && 
          GameGlobals.tttBoard[1][i].owner == player &&
          GameGlobals.tttBoard[2][i].owner == player)
       {
           return true;
       }
       }
       //check for a diagonal win

       if(GameGlobals.tttBoard[0][2].owner == player &&
          GameGlobals.tttBoard[1][1].owner == player &&
          GameGlobals.tttBoard[2][0].owner == player)
       {
           return true;
       }
       
       //check for a diagonal win

       if(GameGlobals.tttBoard[0][0].owner == player &&
          GameGlobals.tttBoard[1][1].owner == player &&
          GameGlobals.tttBoard[2][2].owner == player)
       {
           return true;
       } 
     return false;
   }
   //This method will happen when the local player send the protocal stating if
   //they want to play again. If they do, then a message box on the remote
   //players screen will pop up asking if they want to play again as well. If so
   //then the boards will be cleared, and we will go back to the init() method
   //to reset the game play variables that we need to reset. If not, then the
   //remote player will send a message to the local player saying they don't 
   //want to play again and the app will close down on both ends after closing
   //the dialog box.
   static void processPlayAgainMessage(boolean flag)
   {
      if(flag == true)
      {
        String[] possibleAnswers =
        {
            "Yes", "No"
        };
        String selectedAnswer = (String)JOptionPane.showInputDialog(null,
               "The other player would like to play again.\nDo you want to " +
               "play again?", "CSC 469/569 Tic Tac Toe",
               JOptionPane.INFORMATION_MESSAGE, null,
               possibleAnswers, possibleAnswers[0]);
        System.out.println("You selected " + selectedAnswer);          
        if(selectedAnswer.contains("Yes"))
        {
       for(int x = 0; x < 3; x++)
         for(int y = 0; y < 3; y++)
         {
           GameGlobals.tttBoard[x][y].setText("");
           GameGlobals.tttBoard[x][y].setOwner(PlayerId.NONE);
         } 
         GameGlobals.numberCellsFilled = 0;
         GameGlobals.numberGamesCompleted++;
         GameGlobals.init();         
       
        }
        else
        {
            NetComm.netWriter.println("playagain " + "false");
        }          
      }
      else
      {
        JOptionPane.showMessageDialog(null, "The other play doesn't want to" +
                                      " play anymore. Goodbye.");
        System.exit(0);
      }

   }
   //This method will process the local move. After a local player clicks on 
   //the cell they want to put their marker in, the LocalPlayerListener will
   //call this method(if a valid move). It will then set the local player 
   //marker in that cell, set the owner of that cell to the local player id
   //and sent the protocal message over the network to the remote player
   //will the correct row and column of the cell. Then we will check to see
   //if the local player has won. If so, then a box will appear asking if they
   //want to play again. If they do, then they will send that protocal message
   //to the remote player and they will wait to see if the remote player wants
   //to play. If the local player decides to not want to play, they will send a
   //protocal to the remote player saying they don't want to play and the app
   //will close out. If the local player hasn't won, then we will check to
   //see if the board is full and if it is a draw. If it isn't a draw, then
   //the localplayer becomes the remote player and waits the other players
   //turn.
   static void processLocalMove(int row, int col)
   {
       GameGlobals.tttBoard[row][col].setText(GameGlobals.localPlayerMarker);
       GameGlobals.tttBoard[row][col].setOwner(PlayerId.LOCAL);
       GameGlobals.numberCellsFilled++;
       String strrow = Integer.toString(row);
       String strcol = Integer.toString(col);
       NetComm.netWriter.println("move " + strrow + " " + strcol);
       
        //checking to see if won
       if(GameGlobals.hasWon(PlayerId.LOCAL) == true)
       {
        GameGlobals.gameStatusLabel.setText("WINNER!!!!WINNER!!!!WINNER!!!");
        JOptionPane.showMessageDialog(null,"Congratulations! You won!");
        String[] possibleAnswers =
        {
            "Yes", "No"
        };
        String selectedAnswer = (String)JOptionPane.showInputDialog(null,
               "Would you like to play again?", "CSC 469/569 Tic Tac Toe",
               JOptionPane.INFORMATION_MESSAGE, null,
               possibleAnswers, possibleAnswers[0]);
        System.out.println("You selected " + selectedAnswer);
        if(selectedAnswer.contains("Yes"))
        {
           GameGlobals.localPlayAgain = true;
           String strAnswer = Boolean.toString(GameGlobals.localPlayAgain);
           NetComm.netWriter.println("playagain " + strAnswer);
        //clears the tttBoard of the markers   
        for(int x = 0; x < 3; x++)
         for(int y = 0; y < 3; y++)
         {
           GameGlobals.tttBoard[x][y].setText("");
           GameGlobals.tttBoard[x][y].setOwner(PlayerId.NONE);
         }
         GameGlobals.numberCellsFilled = 0;
         GameGlobals.numberGamesCompleted++;

         GameGlobals.init();
        
        }
        else
        {
           GameGlobals.localPlayAgain = false;
           String strAnswer = Boolean.toString(GameGlobals.localPlayAgain);
           NetComm.netWriter.println("playagain " + strAnswer);
           System.exit(0);
        }          
       }
       //This will check to see if game is a draw. If it is, then both sides
       //need to agree to play again or app will close down.
       //If the local player has not won, or it isn't a draw then we go to the
       //next else statement within this else statement to continue the 
       //current game
      else
       {
       if(GameGlobals.numberCellsFilled == 9)
       {
        GameGlobals.gameStatusLabel.setText("DRAW DRAW DRAW DRAW DRAW");
     
        JOptionPane.showMessageDialog(null,"The game is a draw!");
        String[] possibleAnswers =
        {
            "Yes", "No"
        };
        String selectedAnswer = (String)JOptionPane.showInputDialog(null,
               "Would you like to play again?", "CSC 469/569 Tic Tac Toe",
               JOptionPane.INFORMATION_MESSAGE, null,
               possibleAnswers, possibleAnswers[0]);
        System.out.println("You selected " + selectedAnswer);
        if(selectedAnswer.contains("Yes"))
        {
           GameGlobals.localPlayAgain = true;
           String strAnswer = Boolean.toString(GameGlobals.localPlayAgain);
           NetComm.netWriter.println("playagain " + strAnswer);
         //clear the board for next game 
         for(int x = 0; x < 3; x++)
          for(int y = 0; y < 3; y++)
         {
           GameGlobals.tttBoard[x][y].setText("");
           GameGlobals.tttBoard[x][y].setOwner(PlayerId.NONE);

         } 
           GameGlobals.numberCellsFilled = 0;
           GameGlobals.numberGamesCompleted++;
           GameGlobals.init();
        }
        else
        {
           GameGlobals.localPlayAgain = false;
           String strAnswer = Boolean.toString(GameGlobals.localPlayAgain);
           NetComm.netWriter.println("playagain " + strAnswer);
           System.exit(0);
        }                    
      }
      else
       {
       GameGlobals.isLocalPlayerTurn = false;
       if(GameGlobals.localPlayerMarker.equals("O"))
       
        {
           GameGlobals.localPlayerMarker = "X";
           GameGlobals.remotePlayerMarker = "0";
        }
      else
        {
           GameGlobals.localPlayerMarker = "O";
           GameGlobals.remotePlayerMarker = "X";
        }
      GameGlobals.gameStatusLabel.setText("Waiting for remote player to move");
       }
     } 
   }
}
//This class is dedicated to setting up the network communication
class NetComm
{
    public static ServerSocket serverSocket;
    public static Socket clientSocket;
    public static BufferedReader netReader;
    public static PrintWriter netWriter;
    
    //this will be called by both the client and server
    private static void setUp() throws IOException
    {
      InputStream in = clientSocket.getInputStream();
      netReader = new BufferedReader(new InputStreamReader(in));
      OutputStream out = clientSocket.getOutputStream();
      netWriter = new PrintWriter(out,true);
    }
    //this is where server sets up its ServerSocket and waits for a connection
    //request
    public static void setUpServer() throws IOException
    {
      serverSocket = new ServerSocket(50000);
      clientSocket = serverSocket.accept();
      GameGlobals.isServer = true;
      setUp();
    }
    //this is where the client sets up its socket and sends a connection
    //request after entering in the IP address(in this case, localhost,
    public static void setUpClient() throws UnknownHostException, IOException
    {
     String serverAddress = JOptionPane.showInputDialog(null, "Enter in the" +
             "IP Address of the server machine: ");
     
     clientSocket = new Socket(serverAddress,50000);
     GameGlobals.isServer = false; 
     setUp();
    }   
}
//This special class of a JButton will keep track of it's row and col and you
//can set who the owner of the cell is by tagging it with a player id(remote,
//local, or none)
class TTTButton extends JButton
{
    public final int row;
    public final int col;
    public PlayerId owner;
    
    public TTTButton(int r, int c, PlayerId owner)
    {
        super();
        row = r;
        col = c;
        owner = PlayerId.NONE;
        
    }
    
    public void setOwner(PlayerId id)
    {
     
        owner = id;
    }   
}
//This listener is attached to each button individually. It will indicate if 
//the button is filled in already or if it is not the players turn if the 
//button is clicked on. It will also give the row and col and player id on
//that specific button that was clicked to use in other methods.
class LocalPlayerListener implements ActionListener
{
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        TTTButton button = (TTTButton)e.getSource();
        int r = button.row;
        int c = button.col;
        PlayerId current = button.owner;
        
        if(GameGlobals.isLocalPlayerTurn == true)
        {
          if(current == PlayerId.NONE)
          {
           
             GameGlobals.processLocalMove(r, c);
          }
          else
              JOptionPane.showMessageDialog
                      (null, "This square is already full. Pick again!");
        }
        else
        {
            JOptionPane.showMessageDialog(null, "Wait for your turn!!");
        }
    }    
}
//this listener is on the textfield. It will take the text a user enters after
//pushing enter and send  a protocal on the netWriter to the other player
class ChatMessagesListener implements ActionListener
{
    @Override
    public void actionPerformed(ActionEvent e) 
    {
       String input = GameGlobals.chatMessageTextField.getText();
       GameGlobals.chatHistoryTextArea.append(input + "\n");
       GameGlobals.chatMessageTextField.setText("");
       NetComm.netWriter.println("chat " + input);
    }    
}
//this listener will be listening in on the netReader to see if any protocal
//commands come in. Once one is ready, it scans it, reads the protocal attached
//to the message and sends it to the correct method to take care of it
class NetInputListener implements ActionListener
{
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        try {
            
            if(NetComm.netReader.ready())
            {
             String message = NetComm.netReader.readLine();

             Scanner sc = new Scanner(message);
             String keyword = sc.next();
                    
             switch(keyword)
             {
               case "chat":
                    GameGlobals.processInput(message);
                    break;
               case "move":
                    String row = sc.next();
                    String col = sc.next();
                    int numrow = Integer.parseInt(row);
                    int numcol = Integer.parseInt(col);
                    GameGlobals.processRemoteMove(numrow, numcol);
                    break;
               case "playagain":
                    String playAgain = sc.next();
                    boolean playAgainMessage = Boolean.parseBoolean(playAgain);
                    GameGlobals.processPlayAgainMessage(playAgainMessage);
                    break;   
              }
              }
           } catch (IOException ex) {
            Logger.getLogger(NetInputListener.class.getName()).log
                    (Level.SEVERE, null, ex);
        } 
    }  
}

