package com.haglind.jee;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This class takes a reader and a schemaname and prepends all tablename occurrences with the "schemaname.".
// Line terminators are converted to a \n.
public class FilteredSchemaReader extends FilterReader {
	// This variable holds the current line.
	// If null and emitNewline is false, a newline must be fetched.
	String curLine;

	// This is the index of the first unread character in curLine.
	// If at any time curLineIx == curLine.length, curLine is set to null.
	int curLineIx;

	// If true, the newline at the end of curLine has not been returned.
	// It would have been more convenient to append the newline
	// onto freshly fetched lines. However, that would incur another
	// allocation and copy.
	boolean emitNewline;

	// Matcher used to test every line
	Matcher matcher;

	String REPLACE = null;

	public FilteredSchemaReader(Reader in, String schema) {
		super(new BufferedReader(in));
		Pattern pattern = Pattern.compile("<([A-Z]*_)");
		matcher = pattern.matcher("");

		REPLACE = String.format("<%s.$1", schema);
	}

	// This overridden method fills cbuf with characters read from in.
	public int read(char cbuf[], int off, int len) throws IOException {
		// Fetch new line if necessary
		if (curLine == null && !emitNewline) {
			getNextLine();
		}

		// Return characters from current line
		if (curLine != null) {
			int num = Math.min(len, Math.min(cbuf.length - off, curLine
					.length()
					- curLineIx));
			// Copy characters from curLine to cbuf
			for (int i = 0; i < num; i++) {
				cbuf[off++] = curLine.charAt(curLineIx++);
			}

			// No more characters in curLine
			if (curLineIx == curLine.length()) {
				curLine = null;

				// Is there room for the newline?
				if (num < len && off < cbuf.length) {
					cbuf[off++] = '\n';
					emitNewline = false;
					num++;
				}
			}

			// Return number of character read
			return num;
		} else if (emitNewline && len > 0) {
			// Emit just the newline
			cbuf[off] = '\n';
			emitNewline = false;
			return 1;
		} else if (len > 0) {
			// No more characters left in input reader
			return -1;
		} else {
			// Client did not ask for any characters
			return 0;
		}
	}

	// Get next matching line
	private void getNextLine() throws IOException {
		curLine = ((BufferedReader) in).readLine();
		if (curLine != null) {
			matcher.reset(curLine);
			curLine = matcher.replaceAll(REPLACE);

			emitNewline = true;
			curLineIx = 0;
		}
		return;
	}

	public boolean ready() throws IOException {
		return curLine != null || emitNewline || in.ready();
	}

	public boolean markSupported() {
		return false;
	}
}
