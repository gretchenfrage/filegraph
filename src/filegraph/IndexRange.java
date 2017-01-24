package filegraph;

import java.util.Objects;

import com.phoenixkahlo.nodenet.serialization.FieldSerializer;
import com.phoenixkahlo.nodenet.serialization.Serializer;

public class IndexRange {

	public static Serializer serializer(Serializer subSerializer) {
		return new FieldSerializer(IndexRange.class, subSerializer, IndexRange::new);
	}

	private long startIndex;
	private long endIndex;

	private IndexRange() {
	}

	public IndexRange(long startIndex, long endIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public long getStartIndex() {
		return startIndex;
	}

	public long getEndIndex() {
		return endIndex;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IndexRange)
			return ((IndexRange) other).startIndex == startIndex && ((IndexRange) other).endIndex == endIndex;
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startIndex, endIndex);
	}

}
