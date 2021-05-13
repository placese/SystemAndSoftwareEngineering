import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Compiler {
    public static void main(String[] args) throws IOException {
        RandomAccessFile f = new RandomAccessFile("/home/artem/Documents/3PR.obj", "r");
        byte[] header = new byte[20];
        f.read(header);
        int ns = frun16(header[2], header[3]);
        System.out.println(ns);
        int optHeader = frun16(header[16], header[17]);
        System.out.println(optHeader);
        int symbolOffset = frun32(header[8], header[9], header[10], header[11]);
        System.out.println(symbolOffset);
        f.seek(symbolOffset);
        int nSymbols = frun32(header[12], header[13], header[14], header[15]);
        System.out.println(nSymbols);
        int stringTablePosition = symbolOffset + 18 * nSymbols;
        f.seek(stringTablePosition);
        byte[] length = new byte[4];
        f.read(length);
        int stringTableLength = frun32(length[0], length[1], length[2], length[3]);
        byte[] stringTable = new byte[stringTableLength];
        f.seek(stringTablePosition);
        f.read(stringTable);
        f.seek(symbolOffset);
        for (int i = 0; i < nSymbols;) {
            byte[] sym = new byte[18];
            f.read(sym);
            String info = getName(sym, stringTable);
            System.out.println(info);
            int skip = sym[17] & 0xFF;
            if (skip > 0) {
                i += skip + 1;
                f.skipBytes(18 * skip);
            } else {
                i++;
            }
        }
        f.seek(20 + optHeader);
        Map<String, int[]> sections = new HashMap<String, int[]>();
        for(int i = 0; i < ns; i++) {
            byte[] sectTable = new byte[40];
            f.read(sectTable);
            String sectName = new String(sectTable, 0, 8, "US-ASCII");
            int lengthSect = frun32(sectTable[16], sectTable[17], sectTable[18], sectTable[19]);
            int offsetSect = frun32(sectTable[20], sectTable[21], sectTable[22], sectTable[23]);

            System.out.println(sectName);
            sections.put(sectName, new int[] {lengthSect, offsetSect});
        }

        for (Map.Entry<String, int[]> entry : sections.entrySet()) {
            System.out.println(entry.getKey());
            int[] pair = entry.getValue();
            f.seek(pair[1]);
            byte[] data = new byte[pair[0]];
            f.read(data);
            System.out.println(new BigInteger(data).toString(16));
        }

    }

    public static int frun16(byte b0, byte b1){
        return ((b1 & 0xFF) << 8 | (b0 & 0xFF));
    }

    public static int frun32(byte b0, byte b1, byte b2, byte b3){
        int i = b3 & 0xFF;
        i = (i << 8) | (b2 & 0xFF);
        i = (i << 8) | (b1 & 0xFF);
        i = (i << 8) | (b0 & 0xFF);
        return i;
    }

    public static String getName(byte[] data, byte[] stringTable) throws UnsupportedEncodingException {
        int name = frun32(data[0], data[1], data[2], data[3]);
        if (name == 0) {
            int sofs = frun32(data[4], data[5], data[6], data[7]);
            int length = 0;
            for (int i = sofs; i < stringTable.length; i++) {
                if (stringTable[i] == 0) {
                    break;
                }
                length++;
            }
            return new String(stringTable, sofs, length, "US-ASCII");
        }
        return new String(data, 0, 8, "US-ASCII");

    }
}
