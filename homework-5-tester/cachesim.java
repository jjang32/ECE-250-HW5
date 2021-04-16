import java.io.*;
import java.util.*;

public class cachesim {

    static int [] memory = new int[(int)Math.pow(2, 16)];

    static class cacheblock {
        private int valid;
        private boolean dirty;
        private int tag;
        private int[]data;
        private int order;

        public cacheblock(int v, boolean d, int t, int[] data, int o) {
            valid = v;
            dirty = d;
            tag = t;
            this.data = data;
            order = o;
        }
    }

    public static void main(String [] args) throws FileNotFoundException {
        //taking in arguments and setting all important values
        File f = new File(args[0]);
        int size = Integer.parseInt(args[1]) * 1024;
        int way = Integer.parseInt(args[2]);
        String write = args[3];
        int block = Integer.parseInt(args[4]);
        int frames = size / block;
        int sets = frames / way;
        int offsetBits = log2(block);
        int indexBits = log2(sets);
        //create the cache and fill it depending on what is on our given file
        cacheblock [][] cache = new cacheblock[sets][way];
        for(int i = 0; i < cache.length; i++) {
            for(int j = 0; j < cache[i].length; j++) {
                cache[i][j] = new cacheblock(0, false, 0, new int[block], -1);
            }
        }
        Scanner in = new Scanner(f);
        int count = 0;
        while(in.hasNextLine()) {
            //get the values of each line in our given file
            String[]vals = in.nextLine().split(" ");
            int address = Integer.parseInt(vals[1], 16);
            int offset = address % block;
            int index = (address / block) % sets;
            int tag = address / (sets * block);
            int access = Integer.parseInt(vals[2]);
            //determine if something is a hit or a miss
            boolean isHit = false;
            int hitIndex = -1;
            StringBuilder hit = new StringBuilder("");
            for(int i = 0; i < cache[index].length; i++) {
                if(cache[index][i].tag == tag && cache[index][i].valid == 1) {
                    isHit = true;
                    for(int j = offset; j < offset + access; j++) {
                        hit.append(back(cache[index][i].data[j]));
                    }
                    hitIndex = i;
                    break;
                }
            }
            StringBuilder prompt = new StringBuilder(vals[0] + " " + vals[1] + " ");
            if(write.equals("wt")) {
                if(vals[0].equals("load")) {
                    if(isHit) {
                        cache[index][hitIndex].order = count;
                        prompt.append("hit " + hit);
                    }
                    else {
                        StringBuilder res = new StringBuilder("");
                        //get data instance and the value of the data from memory for our cacheblock
                        int addIndex = cacheIndex(cache[index]);
                        for(int i = 0; i < block; i++) {
                            cache[index][addIndex].data[i] = memory[address - offset + i];
                            if(address - offset + i >= address && address - offset + i < address + access) {
                                res.append(back(memory[address - offset + i]));
                            }
                        }
                        cache[index][addIndex].order = count;
                        cache[index][addIndex].tag = tag;
                        cache[index][addIndex].valid = 1;
                        prompt.append("miss " + res);
                    }
                }
                else if(vals[0].equals("store")) {
                    //2-character ASCII conversion into decimal for block bits
                    int[]written = new int[vals[3].length() / 2];
                    for(int i = 0; i < vals[3].length(); i += 2) {
                        written[i / 2] = Integer.parseInt(vals[3].substring(i, i + 2), 16);
                    }
                    //hit vs miss
                    if(isHit) {
                        for(int i = offset; i < offset + access; i++) {
                            cache[index][hitIndex].data[i] = written[i - offset];
                        }
                        cache[index][hitIndex].valid = 1;
                        cache[index][hitIndex].order = count;
                        //write into memory
                        for(int i = address; i < address + access; i++) {
                            memory[i] = written[i - address];
                        }
                        prompt.append("hit");
                    }
                    else {
                        //write into memory
                        for(int i = address; i < address + access; i++) {
                            memory[i] = written[i - address];
                        }
                        prompt.append("miss");
                    }
                }
            }
            else if(write.equals("wb")) {
                if(vals[0].equals("load")) {
                    if(isHit) {
                        cache[index][hitIndex].order = count;
                        prompt.append("hit " + hit);
                    }
                    else {
                        int addIndex = cacheIndex(cache[index]);
                        if(cache[index][addIndex].dirty) {
                            for(int i = 0; i < block; i++) {
                                memory[(((cache[index][addIndex].tag << indexBits) + index) << offsetBits) + i] = cache[index][addIndex].data[i];
                            }
                        }
                        StringBuilder res = new StringBuilder("");
                        //get data instance and the value of the data from memory for our cacheblock
                        for(int i = 0; i < block; i++) {
                            cache[index][addIndex].data[i] = memory[address - offset + i];
                            if(address - offset + i >= address && address - offset + i < address + access) {
                                res.append(back(memory[address - offset + i]));
                            }
                        }
                        cache[index][addIndex].order = count;
                        cache[index][addIndex].dirty = false;
                        cache[index][addIndex].tag = tag;
                        cache[index][addIndex].valid = 1;
                        prompt.append("miss " + res);
                    }
                }
                else if(vals[0].equals("store")) {
                    //2-character ASCII conversion into decimal for block bits
                    int[]written = new int[vals[3].length() / 2];
                    for(int i = 0; i < vals[3].length(); i += 2) {
                        written[i / 2] = Integer.parseInt(vals[3].substring(i, i + 2), 16);
                    }
                    //hit vs miss
                    if(isHit) {
                        for(int i = offset; i < offset + access; i++) {
                            cache[index][hitIndex].data[i] = written[i - offset];
                        }
                        cache[index][hitIndex].dirty = true;
                        cache[index][hitIndex].order = count;
                        cache[index][hitIndex].valid = 1;
                        cache[index][hitIndex].tag = tag;
                        prompt.append("hit");
                    }
                    else {
                        int addIndex = cacheIndex(cache[index]);
                        if(cache[index][addIndex].dirty) {
                            for(int i = 0; i < block; i++) {
                                memory[(((cache[index][addIndex].tag << indexBits) + index) << offsetBits) + i] = cache[index][addIndex].data[i];
                            }
                        }
                        //get data instance and the value of the data from memory for our cacheblock
                        for(int i = 0; i < block; i++) {
                            cache[index][addIndex].data[i] = memory[address - offset + i];
                        }
                        for(int i = offset; i < offset + access; i++) {
                            cache[index][addIndex].data[i] = written[i - offset];
                        }
                        cache[index][addIndex].dirty = true;
                        cache[index][addIndex].order = count;
                        cache[index][addIndex].valid = 1;
                        cache[index][addIndex].tag = tag;
                        prompt.append("miss");
                    }
                }
            }
            System.out.println(prompt.toString());
            count++;
        }
    }

    //used to determine most of the number of bits necessary for reading addresses
    public static int log2(int val) {
        int res = (int)(Math.log(val) / Math.log(2));
        return res;
    }

    //decimal to hex
    public static String back(int decimal) {
        StringBuilder address = new StringBuilder("");
        while(decimal > 0){
            int val = decimal % 16;
            if(val > 9) {
                address.append((char)('a' + val - 10));
            }
            else {
                address.append(decimal % 16);
            }
            decimal /= 16;
        }
        while(address.length() < 2) {
            address.append("0");
        }
        return address.reverse().toString();
    }

    //check if set is full (if not, add to open space; if yes, remove least recently used and add there)
    public static int cacheIndex(cacheblock[]c) {
        int min = Integer.MAX_VALUE;
        int minIndex = 0;
        for(int i = 0; i < c.length; i++) {
            if(c[i].valid == 0) {
                minIndex = i;
                break;
            }
            if(c[i].order < min) {
                min = c[i].order;
                minIndex = i;
            }
        }
        return minIndex;
    }
}
