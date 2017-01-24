package filegraph;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.phoenixkahlo.nodenet.proxy.Proxy;
import com.phoenixkahlo.nodenet.proxy.RuntimeDisconnectionException;
import com.phoenixkahlo.nodenet.proxy.RuntimeProxyException;
import com.phoenixkahlo.nodenet.serialization.SerializationUtils;

public class LocalFileReceiver implements FileReceiver {

	private static final int SECTION_SIZE = 1024;

	private File file;
	private RandomAccessFile receptacle;
	private long sourceSize;
	private List<IndexRange> neededSections;
	private Runnable successHandler;
	private Runnable failureHandler;

	public LocalFileReceiver(File file, Proxy<AvailableFile> source, Runnable successHandler, Runnable failureHandler)
			throws DownloadException, IOException {
		this.successHandler = successHandler;
		this.failureHandler = failureHandler;

		this.file = file;
		this.receptacle = new RandomAccessFile(file.getPath() + ".download", "rw");

		JSONObject metadataObj = new JSONObject();
		long sectionCount;
		try {
			metadataObj.put("file_id", source.blocking().getID().toString());
			metadataObj.put("section_size", SECTION_SIZE);
			sourceSize = source.blocking().getSize();
			sectionCount = sourceSize / SECTION_SIZE + (sourceSize % SECTION_SIZE == 0 ? 0 : 1);
			metadataObj.put("section_count", sectionCount);
		} catch (RuntimeProxyException | RuntimeDisconnectionException e) {
			throw new DownloadException(e);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		StringBuilder metadataBuilder = new StringBuilder();
		metadataBuilder.append(metadataObj.toString());
		if (metadataBuilder.length() > 200)
			throw new DownloadException("Metadata illegally large");
		while (metadataBuilder.length() < 200)
			metadataBuilder.append(' ');
		String metadata = metadataBuilder.toString();

		this.receptacle.setLength(sourceSize + sectionCount + 200);
		this.receptacle.seek(sourceSize + sectionCount);
		this.receptacle.write(SerializationUtils.stringToBytes(metadata));
	}

	@Override
	public synchronized List<IndexRange> getNeededSections() {
		return new ArrayList<>(neededSections);
	}

	@Override
	public synchronized void provide(IndexRange section, byte[] data) throws IOException, IllegalArgumentException {
		for (int i = 0; i < neededSections.size(); i++) {
			if (neededSections.get(i).equals(section)) {
				receptacle.seek(section.getStartIndex());
				receptacle.write(data);
				receptacle.seek(sourceSize + i);
				receptacle.write(1);
				neededSections.remove(i);
				return;
			}
		}
		throw new IllegalArgumentException();
	}

	private long checksum() throws IOException {
		long n = 0;
		receptacle.seek(0);
		for (int i = 0; i < sourceSize; i++) {
			n ^= (long) Math.pow(2, receptacle.read());
		}
		return n;
	}

	@Override
	public void finish(long checksum) {
		try {
			if (checksum == checksum()) {
				receptacle.setLength(sourceSize);
				receptacle.close();

				String path = file.getPath();
				if (path.length() >= 9 && path.substring(path.length() - ".download".length()).equals(".download"))
					file.renameTo(new File(path.substring(0, path.length() - ".download".length())));

				successHandler.run();
			} else {
				receptacle.close();
				file.delete();
				failureHandler.run();
			}
		} catch (IOException e) {
			failureHandler.run();
		}
	}

}
