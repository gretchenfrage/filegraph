package filegraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.phoenixkahlo.util.UUID;

public class LocalAvailableFile implements AvailableFile {

	private File file;
	private RandomAccessFile reader;
	private UUID id;
	
	public LocalAvailableFile(File file, UUID id) throws FileNotFoundException {
		this.file = file;
		this.reader = new RandomAccessFile(file, "r");
		this.id = id;
	}
	
	@Override
	public String getName() {
		return file.getName();
	}
	
	@Override
	public UUID getID() {
		return id;
	}
	
	@Override
	public long getSize() {
		return file.length();
	}

	@Override
	public synchronized byte[] getSection(IndexRange section) throws IOException {
		reader.seek(section.getStartIndex());
		byte[] arr = new byte[(int) (section.getEndIndex() - section.getStartIndex() + 1)];
		reader.readFully(arr);
		return arr;
	}
	
}
