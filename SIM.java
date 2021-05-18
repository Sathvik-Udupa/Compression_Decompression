/*

File:   SIM.java
Author: Sathvik Subramanya Udupa
Date:   4/1/2020
Desc:   Compression/Decompression engine
    
*/

import java.io.*;
import java.util.*;
public class SIM {
		
	public static ArrayList<String> readinstr(String file) throws Exception {			//Function to read files into lists
		String temp;		
		ArrayList<String> result = new ArrayList<>();
		Scanner s = new Scanner(new FileReader(file));
		while (s.hasNext()) {
			temp = s.nextLine();
			result.add(temp);
		}
		s.close();
		return result;
	}

	public static HashMap<String, String> Decide_Dictionary(ArrayList<String> compress_input, int ctrl) throws Exception{
																						//Dictionary for compression and decompression
		HashMap<String, Integer> hmap = new LinkedHashMap<String, Integer>();
		HashMap<String, String> map = new LinkedHashMap<String, String>();

		if(ctrl == 1){																	//Dictionary for compression
			for(int i=0; i<compress_input.size(); i++){
				int k;
				if(hmap.containsKey(compress_input.get(i))){
					k = hmap.get(compress_input.get(i));
					hmap.put(compress_input.get(i), k+1);
				}
				else{
					hmap.put(compress_input.get(i), 1);
				}
			}

			for(int j=0; j<8; j++){
				Integer maxvote = 0;
				String dict = "";
				for(String k: hmap.keySet()){
					String temp = k;
					Integer vote = hmap.get(k);
					if(vote> maxvote){
						maxvote = vote;
						dict = temp;					
					}
				}
				
				hmap.remove(dict);
				switch(j){
					case 0:
					map.put(dict,"000");
					break;
					case 1:
					map.put(dict,"001");
					break;
					case 2:
					map.put(dict,"010");
					break;
					case 3:
					map.put(dict,"011");
					break;
					case 4:
					map.put(dict,"100");
					break;
					case 5:
					map.put(dict,"101");
					break;
					case 6:
					map.put(dict,"110");
					break;
					case 7:
					map.put(dict,"111");
					break;
				}
			}
		}
		else{																			//Dictionary for decompression
			map.put("000", compress_input.get(compress_input.size()-8));
			map.put("001", compress_input.get(compress_input.size()-7));
			map.put("010", compress_input.get(compress_input.size()-6));
			map.put("011", compress_input.get(compress_input.size()-5));
			map.put("100", compress_input.get(compress_input.size()-4));
			map.put("101", compress_input.get(compress_input.size()-3));
			map.put("110", compress_input.get(compress_input.size()-2));
			map.put("111", compress_input.get(compress_input.size()-1));
		}
		
		return map;
	}


	public static void compression() throws Exception{									// Compression function
		ArrayList<String> compress_input;
		String out = "";
		HashMap<String, String> dictionary_compression = new LinkedHashMap<String, String>();
		compress_input = readinstr("original.txt");

		dictionary_compression = Decide_Dictionary(compress_input, 1);
		List<String> keys = new ArrayList<String>(dictionary_compression.keySet());

		for(int i = 0; i<compress_input.size(); i++){
			
			if((i>0) && (compress_input.get(i-1).equals(compress_input.get(i)))){		// RLE compression
				if(compress_input.get(i).equals(compress_input.get(i+1))){
					if(compress_input.get(i+1).equals(compress_input.get(i+2))){
						if(compress_input.get(i+2).equals(compress_input.get(i+3))){
							out = out + "00011";
							i = i + 3;
						}
						else{
							out = out + "00010";
							i = i + 2;
						}
					}
					else{
					out = out + "00001";
					i = i + 1;
					}
				}
				else{
					out = out + "00000";
				}
			}

			else if(dictionary_compression.containsKey(compress_input.get(i))){				//Dictionary based direct compression
				out = out + "101" + dictionary_compression.get(compress_input.get(i));
			}

			else{
				Long temp = Long.parseLong(compress_input.get(i), 2);
				String bitmask = "";
				String check = "";
				int flag = 0;

				for(int k = 0; k<8; k++){
					int val = (int) (temp ^ Long.parseLong(keys.get(k),2));					// XOR with each dictionary entries
					Long vals = temp ^ Long.parseLong(keys.get(k),2);

					if(Integer.bitCount(val) <= 4){
						int diff = 0;
						List<Integer> pos = new ArrayList<Integer>();

						for(int j=0; j<32; j++){

							if(compress_input.get(i).charAt(j) != keys.get(k).charAt(j)){		// Look for mismatches
								diff = diff + 1;
								pos.add(j);								
							}
						}

						if(diff == 1){															// 1-bit mismatch
							out = out + "010" + String.format("%5s", Integer.toBinaryString(pos.get(0))).replace(' ', '0') 
									+ dictionary_compression.get(keys.get(k));
							flag = 1;
							break;
						}
						
						if((diff == 2) && (pos.get(1) - pos.get(0) == 1)){						// 2-bit consecutive mismatch
							out = out + "011" + String.format("%5s", Integer.toBinaryString(pos.get(0))).replace(' ', '0')
									+ dictionary_compression.get(keys.get(k));
							flag = 1;
							break;
						}
						else{
							if((pos.get(pos.size()-1) - pos.get(0)) <= 3){						// Bit-mask based compression
								if(vals >= 8 ){
									bitmask = "001" + String.format("%5s", Integer.toBinaryString(pos.get(0))).replace(' ', '0')
										+ Integer.toBinaryString(val).substring(0,4) + dictionary_compression.get(keys.get(k));
								}
								else
									bitmask = "001" + "11100"
									+ String.format("%4s", Integer.toBinaryString(val)).replace(' ', '0') + dictionary_compression.get(keys.get(k));

							}

							else if(diff == 2){													// 2-bit mismatch at different locations
								check = "100" + String.format("%5s", Integer.toBinaryString(pos.get(0))).replace(' ', '0')
									+ String.format("%5s", Integer.toBinaryString(pos.get(1))).replace(' ', '0')
									+ dictionary_compression.get(keys.get(k));
							}
						}
					}
				}

				if(flag == 0){
					if(bitmask != ""){
					out = out + bitmask;
					}

					else if(check != ""){
						out = out + check;
					}

					else
						out = out + "110" + compress_input.get(i);								// Original binaries
				}

			}
		}

		PrintWriter writer = new PrintWriter("cout.txt");
		
		while(out.length() % 32 != 0){
			out = out + "1";
		}

		for(int iter = 0; iter<(out.length()); iter = iter+32){
			writer.println(out.substring(iter, iter+32));
		}

		writer.println("xxxx");

		for(int jk = 0; jk<8; jk++){
			writer.println(keys.get(jk));
		}

		writer.close();		
	}
	
	public static void decompression() throws Exception{									// Decompression function
		ArrayList<String> compress_input;
		String out = "";
		HashMap<String, String> dictionary_decompression = new LinkedHashMap<String, String>();
		compress_input = readinstr("compressed.txt");

		dictionary_decompression = Decide_Dictionary(compress_input, 2);					// Get dictionary for decompression

		for (int i=0; i<(compress_input.size()-9); i++){
			out = out + compress_input.get(i);
		}

		PrintWriter writer = new PrintWriter("dout.txt");
		String previous = "";

		while(out != ""){
			String code = out.substring(0,3);

			switch(code){																				
				case "000":{																		// RLE decompression
					int temp = Integer.parseInt(out.substring(3,5), 2);
					for(int k=0; k<=temp; k++){
						writer.println(previous);
					}
					out = out.substring(5);
					break;
				}

				case "001":{																		// Bit-masked based decompression
					int loc = Integer.parseInt(out.substring(3,8), 2);
					int mask = Integer.parseInt(out.substring(8,12), 2);
					String dict = out.substring(12,15);

					previous = dictionary_decompression.get(dict);
					String bits = String.format("%4s", Integer.toBinaryString(Integer.parseInt(
							previous.substring(loc, loc+4), 2) ^ mask)).replace(' ', '0');

					previous = previous.substring(0,loc) + bits + previous.substring(loc+4);					
					writer.println(previous);
					out = out.substring(15);
					break;
				}

				case "010":{																		// 1 bit mismatch
					int loc = Integer.parseInt(out.substring(3,8), 2);
					String dict = out.substring(8,11);
					char bit;

					previous = dictionary_decompression.get(dict);
					if(previous.charAt(loc) == '0'){
						bit = '1';
					}
					else
						bit = '0';

					previous = previous.substring(0,loc) + String.valueOf(bit) + previous.substring(loc + 1);
					writer.println(previous);
					out = out.substring(11);
					break;
				}

				case "011":{																		// 2 bit consecutive
					int loc = Integer.parseInt(out.substring(3,8), 2);
					String dict = out.substring(8,11);
					char bit1;
					char bit2;

					previous = dictionary_decompression.get(dict);
					if(previous.charAt(loc) == '0'){
						bit1 = '1';
					}
					else
						bit1 = '0';
					if(previous.charAt(loc+1) == '0'){
						bit2 = '1';
					}
					else
						bit2 = '0';
					previous = previous.substring(0,loc) + String.valueOf(bit1) + String.valueOf(bit2) + previous.substring(loc + 2);
					writer.println(previous);
					out = out.substring(11);
					break;
				}

				case "100":{																		// 2-bit mismatch anywhere	
					int loc1 = Integer.parseInt(out.substring(3,8), 2);
					int loc2 = Integer.parseInt(out.substring(8,13), 2);
					String dict = out.substring(13,16);
					char bit1;
					char bit2;

					previous = dictionary_decompression.get(dict);
					if(previous.charAt(loc1) == '0'){
						bit1 = '1';
					}
					else
						bit1 = '0';
					if(previous.charAt(loc2) == '0'){
						bit2 = '1';
					}
					else
						bit2 = '0';
					previous = previous.substring(0,loc1) + String.valueOf(bit1) + previous.substring(loc1 + 1, loc2)
						+ String.valueOf(bit2) + previous.substring(loc2+1);
					writer.println(previous);
					out = out.substring(16);
					break;
				}

				case "101":{																		// Direct mapping to dictionary
					String dict = out.substring(3,6);
					previous = dictionary_decompression.get(dict);
					writer.println(previous);
					out = out.substring(6);
					break;
				}

				case "110":{																		// original
					previous = out.substring(3,35);
					writer.println(previous);
					out = out.substring(35);
					break;
				}

				default:
					if(out.length() < 32){															// discarding padded ones
						out = "";
					}
					else
						System.out.println("Error!!");
					break;
			}
		}
		writer.close();
	}
	
	public static void main(String[] args)throws Exception {
		int N = Integer.parseInt(args[0]);
		if(N == 2){
			decompression();
		}
		else if(N == 1){
			compression();
		}					
	}

}