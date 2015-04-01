
import java.awt.AWTException;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ServerWindow implements ActionListener{
	
	private RemoteDataServer server;
	
	private Thread sThread; //server thread
	
	private static final int WINDOW_HEIGHT = 200;
	private static final int WINDOW_WIDTH = 350;
	
	private String ipAddress;
	
	private JFrame window = new JFrame("Remote Desktop Server");
	
	private JLabel addressLabel = new JLabel("");
	private JLabel portLabel = new JLabel("PORT: ");
	private JTextArea[] buffers = new JTextArea[3];
	private JTextField portTxt = new JTextField(5);
	private JLabel serverMessages = new JLabel("Not Connected");
	
	private JButton connectButton = new JButton("Connect");
	private JButton disconnectButton = new JButton("Disconnect");
	
	public ServerWindow(){
		server = new RemoteDataServer();
		
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		
		Container c = window.getContentPane();
		c.setLayout(new FlowLayout());
		
		try{
			InetAddress ip = InetAddress.getLocalHost();
			ipAddress = ip.getHostAddress();
			addressLabel.setText("IP Address: "+ipAddress);
		}
		catch(Exception e){addressLabel.setText("IP Address Could Not be Resolved");}
		
		int x;
		for(x = 0; x < 3; x++){
			buffers[x] = new JTextArea("", 1, 30);
			buffers[x].setEditable(false);
			buffers[x].setBackground(window.getBackground());
		}
		
		c.add(addressLabel);
		c.add(buffers[0]);
		c.add(portLabel);
		portTxt.setText("5444");
		c.add(portTxt);
		c.add(buffers[1]);
		c.add(connectButton);
		c.add(disconnectButton);
		c.add(buffers[2]);
		c.add(serverMessages);
		
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		window.setResizable(false);
	}
	
	public void log(String msg) {
		System.out.println(msg);
	}
	
	public void actionPerformed(ActionEvent e){
		Object src = e.getSource();
		
		if(src instanceof JButton){
			if((JButton)src == connectButton){
				int port = Integer.parseInt(portTxt.getText());
				runServer(port);
			}
				
			else if((JButton)src == disconnectButton){
				closeServer();
			}
		}
	}
	
	public void runServer(int port){
		if(port <= 9999){
			server.setPort(port);
			sThread = new Thread(server);
			sThread.start();
		}
		else{
			serverMessages.setText("The port Number must be less than 10000");
		}
	}
	
	public void closeServer(){
		serverMessages.setText("Disconnected");
		server.shutdown();
		connectButton.setEnabled(true);
	}
	
	public static void main(String[] args){
		new ServerWindow();
	}
	
	public class RemoteDataServer implements Runnable{
		int PORT;
		private DatagramSocket server;
		//just for transfer image
		private final int imgTransPort = 6473;
		private DatagramSocket imgTransSocket;
		private int phoneImgTransPort=-1;
		private InetAddress phoneImgTransIP;
		private boolean connected = false;
		private byte[] imgBuf = new byte[8192];
    	public static final String msgInit="ImgTransInit", msgBegin="ImgTransBegin", msgEnd="ImgTransEnd";
		
		private byte[] buf;
		private DatagramPacket dgp;
		
		private String message;
		private AutoBot bot;
		
		public RemoteDataServer(int port){
			PORT = port;
			buf = new byte[1000];
			dgp = new DatagramPacket(buf, buf.length);
			bot = new AutoBot();
			serverMessages.setText("Not Connected");
		}
		
		public RemoteDataServer(){
			buf = new byte[1000];
			dgp = new DatagramPacket(buf, buf.length);
			bot = new AutoBot();
			serverMessages.setText("Not Connected");
		}
		
		public String getIpAddress(){
			String returnStr;
			try{
					InetAddress ip = InetAddress.getLocalHost();
					returnStr = ip.getCanonicalHostName();
			}
			catch(Exception e){ returnStr = new String("Could Not Resolve Ip Address");}
			return returnStr;
		}
		
		public void setPort(int port){
			PORT = port;
		}
		
		public void shutdown(){
			try{server.close();
				serverMessages.setText("Disconnected");}
			catch(Exception e){}
		}
		
		public void run(){
//			boolean connected = false;
			try {InetAddress ip = InetAddress.getLocalHost(); 
				serverMessages.setText("Waiting for connection on " + ip);
				
				server = new DatagramSocket(PORT, ip);
				imgTransSocket = new DatagramSocket(imgTransPort, ip);//init another socket 
				
				connected = true;
				connectButton.setEnabled(false);
			}
			catch(BindException e){ serverMessages.setText("Port "+PORT+" is already in use. Use a different Port"); }
			catch(Exception e){serverMessages.setText("Unable to connect");}
			
			createImgTransMsgSender();//thread blocked here ... for no such timeout
			
			while(connected){
				// get message from sender
				try{ server.receive(dgp);//thread blocked here ... for no such timeout
				
					// translate and use the message to automate the desktop
					message = new String(dgp.getData(), 0, dgp.getLength());
					if (message.equals("Connectivity")){
						//send response to confirm connectivity
						serverMessages.setText("Trying to Connect");
						log(new String("current client info: "+dgp.getAddress()+":"+dgp.getPort()));
						server.send(dgp); //echo the message back
					}else if(message.equals("Connected")){
						serverMessages.setText("Connected");
//						Thread.sleep(3000); 
						
						server.send(dgp); //echo the message back
					}else if(message.equals("Close")){
						serverMessages.setText("Controller has Disconnected. Trying to reconnect."); //echo the message back
					}else{
						serverMessages.setText("Connected to Controller");
						bot.handleMessage(message);
					}
				}catch(Exception e){
					serverMessages.setText("Disconnected");
					connected = false;}
			}//end while
		}

		private void createImgTransMsgSender() {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					while (connected) {
						//receive data from phone side 
						DatagramPacket imgTransPacket = new DatagramPacket(imgBuf, imgBuf.length);
						try {
							imgTransSocket.receive(imgTransPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String strImgTransContent = new String(imgTransPacket.getData(), 0, imgTransPacket.getLength());
						//switch -- consume msg:
						
						if (strImgTransContent.equals(RemoteDataServer.msgInit)) {
							//receive msg "init"
							phoneImgTransPort=imgTransPacket.getPort();
							phoneImgTransIP=imgTransPacket.getAddress();
							try {
								imgTransSocket.send(imgTransPacket);//echo: send back the package
							} catch (IOException e) {
								log("Send Back package Error!");
								e.printStackTrace();
							}
						} else if (strImgTransContent.equals(RemoteDataServer.msgBegin)) {
//							//receive msg "begin"
//							try {
//								imgTransSocket.send(imgTransPacket);//echo: send back the package
//							} catch (IOException e) {
//								log("Send Back package Error!");
//								e.printStackTrace();
//							}
//							//echo end....
							//begin send data without stop...
							while (connected) {//get in another loop, never ever get out only if the app is terminated
								//process capture & sending logic //TODO: Double Check Needed! 
								try {
									//send package "Begin"
									imgBuf = RemoteDataServer.msgBegin.getBytes();
									imgTransPacket = new DatagramPacket(imgBuf, imgBuf.length, phoneImgTransIP, phoneImgTransPort);
									imgTransSocket.send(imgTransPacket);
									
									//get screen shot in Bytes[]
									byte[] targetTotalBuf = getScreenShot();
									ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(targetTotalBuf);//transfer byte[] into InputStream
									int n = -1;
									imgBuf = new byte[8192];//redefinition, rebuild...
									while ((n = byteArrayInputStream.read(imgBuf)) != -1) {
										imgTransPacket = new DatagramPacket(imgBuf, imgBuf.length, phoneImgTransIP, phoneImgTransPort);
										imgTransSocket.send(imgTransPacket);
										//send package...
									}
									
									//send package "End"
									imgBuf = RemoteDataServer.msgEnd.getBytes();
									imgTransPacket = new DatagramPacket(imgBuf, imgBuf.length, phoneImgTransIP, phoneImgTransPort);
									imgTransSocket.send(imgTransPacket);
									
								} catch (IOException e) {
									e.printStackTrace();
								}
							}//end while(connected)---- inside is sending logic
							
							
						}//end msgBegin
					}//end while(true)
				}
			}).start();
		}//method end
		
		/**
		 * get current screen shot
		 * @param robot No Special Requirements
		 */
		public byte[] getScreenShot() {
			log("print screen!\n>>>>>>");
			
			//new Robot...
			Robot robot = null;
			try {
				robot = new Robot();
			} catch (AWTException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//get screen shot...
			BufferedImage image = RobotHelper.captureWholeScreen(robot, 0);
//			File iSaveFile = new File("temp.png");
			
			byte[] result = null;
			try {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				//-----begin---绘制鼠标 因为直接截屏之后的图片是没有鼠标的
				Image cursorImg = ImageIO.read(new File("z:/cursor.png"));//TODO: REMEBER to change the file path
				
				int curCursorX = MouseInfo.getPointerInfo().getLocation().x;
				int curCursorY = MouseInfo.getPointerInfo().getLocation().y;
				
				Graphics2D graphics2d=image.createGraphics();
				graphics2d.drawImage(cursorImg, curCursorX, curCursorY, 32, 32, null);
				//-----end---

				ImageIO.write(image, "png", byteArrayOutputStream);//write into output stream
//				ImageIO.write(image, "png", iSaveFile);

				result = byteArrayOutputStream.toByteArray();
				byteArrayOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			log("screen print success");
			return result;
		}//end method
		
	}
}
