/*	
/	Author:		Kean Jafari
*/

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.Object;


public class Ipv6Client {

	static short headerChecksum = 0;	
	static int packetSize = 0;
	static int dataSize = 0;
	
	public static void main(String[] args) {
		try {	
			Socket socket = new Socket("codebank.xyz", 38004);
			for (int packetDataLength = 2; packetDataLength <= 4096;) {
				dataSize = packetDataLength;
				System.out.println("\nData length: " + packetDataLength);
				sendPacket(socket, packetDataLength);
				packetDataLength *= 2;
				headerChecksum = 0;
				packetSize = 0;
			}
		} catch (Exception e) {e.printStackTrace();}
		
	}
	
	/*
	// creates a byte array, given a size, and fills it with 0
	public static byte[] createPacket (int size) {
		byte[] packet;

		//Creates byte arrays, row by row, to later merge and create
		//final packet
		
		byte[] first = setFirstRow(size);
		byte[] second = setSecondRow();
		byte[] third = setThirdRow();
		byte[] fourth = setFourthRow();
		byte[] fifth = setFifthRow();
		byte[] data = new byte[size];

		//Determines total size of byte array for packet and instantiates
		//proper packet size
		packetSize = first.length + second.length + third.length +
					fourth.length + fifth.length + size;

		//adds all rows of packet together
		packet = addPacketRows(first, second, third, fourth, fifth, data);

		//calculates checksum
		getChecksum(packet, size);

		//re-enters rows into packet, with correct checksum
		byte[] newThird = setThirdRow();

		//adds packet rows again
		byte[] finalPacket = addPacketRows(first, second, newThird, fourth, fifth, data);
		
		return finalPacket;
	}*/

	public static byte[] createIpv6Packet(int size, Socket socket) throws IOException {
		Random rand = new Random();
		int version = 6;
		int trafficClass = 0;
		int flowLabel = 0;
		int payloadLength = size;
		int nextHeader = 17;	//UDP = 17, TCP = 6
		int hopLimit = 20;
		
		/*
		InetAddress ipv6_srcAdd = InetAddress.getByName("0000:0000:0000:0000:0000:FFFF:ACD9:048E");
		InetAddress ipv6_destAdd = InetAddress.getByName("0000:0000:0000:0000:0000:FFFF:3425:589A");
		String srcTest = "172.217.4.174";

		byte[] src = ipv6_srcAdd.getAddress();
		byte[] dest = ipv6_destAdd.getAddress();
		*/

		byte[] src = socket.getInetAddress().getAddress();
	  	byte[] dest = socket.getInetAddress().getAddress();


		byte[] packet = new byte[40 + size];

		//		First Row
		//Version & Traffic & Flow Label
		packet[0] = (byte) (version * 16);
		packet[1] = 0;
		packet[2] = 0;
		packet[3] = 0;

		//		Second Row
		//Payload length + Next Header + hop limit
		packet[4] = (byte) ((payloadLength & 0xFF00) >> 8);
		packet[5] = (byte) (payloadLength & 0x00FF);
		packet[6] = (byte) nextHeader;
		packet[7] = (byte) hopLimit;

		//		Row SRC ADDRESS AND DESTINATION ADDRESS
		//Adds src and dest address
		for (int i = 8; i < 18; i++) {
			packet[i] = 0;
			packet[i+16] = 0;
		}
		for (int i = 18; i < 20; i++) {
			packet[i] = (byte) 0xFF;
			packet[i+16] = (byte) 0xFF;
		}
		for (int i = 0; i < 4; i++) {
			packet[i+20] = src[i];
			packet[i+36] = dest[i];
		}

		//		Data Row(s)
		//Adds random numbers between 0-255
		for (int i = 0; i < dataSize; i++) 
			packet[i+40] = 0;
		
		return packet;

	}
	

	// Generates random numbers to fill in the data
	public static byte[] setDataRow(int size) {
		byte[] row = new byte[size];
		Random rand = new Random();
		for (int i = 0; i < size; i++)
			row[i] = (byte)rand.nextInt(255);
		return row;
	}

	// Copied and Modified from EX3
	// Calculates checksum of the package, updates global static variable
	public static void getChecksum(byte[] packet, int size) {
		//Calculates the checksum
		int length = packetSize - size;
		int i = 0;
	   	long total = 0;
	   	long sum = 0;

	    // add to sum and bit shift
	   	while (length > 1) {
	    	sum = sum + ((packet[i] << 8 & 0xFF00) | ((packet[i+1]) & 0x00FF));
	    	i = i + 2;
	    	length = length - 2;

	    	// splits byte into 2 words, adds them.
	    	if ((sum & 0xFFFF0000) > 0) {
	    		sum = sum & 0xFFFF;
	    		sum++;
	    	}
	    }

	    // calculates and adds overflowed bits, if any
		if (length > 0) {
    		sum += packet[i] << 8 & 0xFF00;
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum++;
			}
    	}

	   	total = (~((sum & 0xFFFF)+(sum >> 16))) & 0xFFFF;
	   	headerChecksum = (short) total;
	}


	// Communicates with server, sends packet
	public static void sendPacket(Socket socket, int packetDataLength) {
		try {
			OutputStream os = socket.getOutputStream();
			byte[] stream = createIpv6Packet(packetDataLength, socket);
			//System.out.println(Arrays.toString(stream));
			os.write(stream);
			getResponse(socket);
		} catch (Exception e) { e.printStackTrace(); } 
	}

	//Recieves response from server and prints.
	public static void getResponse(Socket socket) {
		try { 
			InputStream is = socket.getInputStream();
            //InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            //BufferedReader br = new BufferedReader(isr);
			String hexString = "";
			byte[] byteArray = new byte[4];
            
            //Gets byte array for response
            for (int i = 0 ; i < 4; i++) {
				int stream = is.read();
				hexString += toHex(stream);
				byteArray[i] = (byte)stream;
			}
			printString(hexString);
		} catch (Exception e) { e.printStackTrace(); }
	}

	//Converts stream to uppercase hex
	public static String toHex(int stream) {
		return Integer.toHexString(stream).toUpperCase();
	}

	// short to int, then return string
	public static String toHex(short stream) {
		return Integer.toHexString(stream).toUpperCase();
	}
	
	//Prints formatted hex string (max line length = 20)
	public static void printString(String hexString) {
		System.out.print("Response: 0x");
		for (int i = 0; i < hexString.length(); i++)
			System.out.print(hexString.charAt(i));
		System.out.println();
	}
}