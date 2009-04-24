package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
	private final int RESULT_LIMIT = 100;
	private String content;
	private int[] pos_vector;
	private int min_lat;
	private int min_lng;
	private byte[] lat_vector;
	private byte[] lng_vector;
	
	private int[] result_pos;
	private int count;
	
	public Searcher() {
		result_pos = new int[RESULT_LIMIT];
		loadData();
	}
	
	public List<String> search(String s) {
		List<String> result = new LinkedList<String>();
		if (content == null) {
			result.add("NO CONTENT LOADED");
			return result;
		}
		Log.e("s", "substring " + s);
		if (s.length() == 0) {
			result.add("-NOTHING TO SEARCH-");
			return result;
		}
		int searchStart = 0;
		int totalFound = 0;
		final int content_length = content.length();
		while (searchStart < content_length) {
			int pos = content.indexOf(s, searchStart);
			if (pos == -1) break;

			// -1 + 1 = 0
			int start = content.lastIndexOf('\n', pos) + 1;
			int end = content.indexOf('\n', start + 1);
			if (end == -1) end = content_length;
			result.add(content.substring(start, end));
			result_pos[totalFound] = start;
			totalFound++;
			if (totalFound >= RESULT_LIMIT) {
				Log.e("s", "got " + totalFound + " results, truncated");
				break;
			}
			searchStart = end + 1;
		}
		if (result.size() == 0) {
			result.add("-NOT FOUND-");
		}
		Log.e("s", "num results " + result.size());
		return result;
	}
	
	public void getCoordsForResult(int num, double latlng[]) {
		Log.e("s", "num = " + num);
		int pos = result_pos[num]; 
		Log.e("pos", "pos = " + pos);
	   	int idx = getIndex(pos);
	   	Log.e("idx", "idx = " + idx);
	   	latlng[0] = (Get3ByteInt(lat_vector, idx) + min_lat) * 1e-7;
	   	latlng[1] = (Get3ByteInt(lng_vector, idx) + min_lng) * 1e-7;
	}
	
    private int Get3ByteInt(byte[] vector, int idx) {
    	final int offset = 3 * idx;
    	int t = 0;
        for(int i = 2; i >= 0; --i) {
          t *= 256;
          int b = vector[offset + i];
          t += b & 0xff;
        }
        return t; 
	}

	private void loadData() {
    	Log.e("s", "Loading search data...");
    	String stringDataFile = "/sdcard/maps/string.dat";
    	String indexDataFile = "/sdcard/maps/index.dat";
        try {
        	loadContent(stringDataFile);
        	loadCoords(indexDataFile);
        	makePosVector();
        } catch (IOException f) {
        	Log.e("s", "Cannot read file");
        }
        Log.e("s", "Loaded search data.");
    }
    
    private int readInt(byte[] buffer, int offset) {
    	int t = 0;
        for(int i = 3; i >= 0; --i) {
          t *= 256;
          int b = buffer[offset+i];
          t += b & 0xff;
        }
        return t; 
    }
    
    private void loadContent(String stringDataFile) throws IOException {
    	FileInputStream stream = new FileInputStream(stringDataFile);
    	// Read file with UTF-8
    	InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
    	content = "";
    	StringBuilder builder = new StringBuilder();
    	// NOTE: this must be slightly above real number of characters
    	// TODO(alexvod): remove this hack
    	builder.ensureCapacity(1930000);
		char[] inputBuffer = new char[8192];
		while (true) {
    		int numChars = reader.read(inputBuffer);
    		if (numChars <= 0) break;
    		builder.append(inputBuffer, 0, numChars);
    	}
		content = builder.toString();
		Log.e("s", "Total " + content.length() + " characters loaded");
    	stream.close();
    }
    
    private void loadCoords(String indexDataFile) throws IOException {
    	InputStream stream = new FileInputStream(indexDataFile);
    	byte[] buffer = new byte[20];
    	stream.read(buffer, 0, 12);
    	count = readInt(buffer, 0);
    	Log.e("s", "num entries " + count);
    	min_lat = readInt(buffer, 4);
    	min_lng = readInt(buffer, 8);
    	Log.e("s", "min_lat=" + min_lat + " min_lng" + min_lng);
    	lat_vector = new byte[3 * count];
    	lng_vector = new byte[3 * count];
    	stream.read(lat_vector, 0, 3 * count);
    	stream.read(lng_vector, 0, 3 * count);
    	stream.close();
    }
    
    private void makePosVector() {
    	pos_vector = new int[count + 1];
    	int pos = -1;
    	int idx = 0;
    	while (true) {
    		pos = content.indexOf('\n', pos + 1);
    		if (pos == -1) break;
    		pos_vector[idx] = pos;
    		idx++;
    	}
    	Log.e("s", "found " + idx + " items in strings, must be == " + count);
    	count = idx;
    }
    
    private int getIndex(int pos) {
    	// Check bounds
    	if (pos <= pos_vector[0]) return 0;
    	if (pos >= pos_vector[count-1]) return count-1;
    	// Do binary search.
       	int min_idx = 0;
        int max_idx = count-1;
    	int mid_idx, mid_pos;
    	while (max_idx - min_idx > 1) {
    		mid_idx = (min_idx + max_idx) / 2;
    		mid_pos = pos_vector[mid_idx];
    		if (pos == mid_pos) return mid_idx;
    		if (pos >= mid_pos) {
    			min_idx = mid_idx;
    		} else {
    			max_idx = mid_idx;
    		}
    	}
    	if (pos_vector[min_idx] == pos) return min_idx;
    	return max_idx;
    }
}