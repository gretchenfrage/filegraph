package filegraph;

import java.io.File;

import com.phoenixkahlo.util.UUID;

public class LocalAvailableFile implements AvailableFile {

	private File file;
	private UUID id;
	
	public LocalAvailableFile(File file, UUID id) {
		this.file = file;
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
	
}
