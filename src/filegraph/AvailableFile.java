package filegraph;

import java.io.IOException;

import com.phoenixkahlo.util.UUID;

public interface AvailableFile {

	String getName();

	UUID getID();
	
	long getSize();
	
	byte[] getSection(IndexRange section) throws IOException;

}