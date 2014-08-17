/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

public class MutingPrintStream extends PrintStream {
    public static ThreadLocal<Boolean> ignore = new ThreadLocal<Boolean>();

    private PrintStream delegate;

    public MutingPrintStream(OutputStream out, PrintStream ps) {
        super(out);
        this.delegate = ps;
    }
    public int hashCode() {
        return delegate.hashCode();
    }
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }
    public String toString() {
        return delegate.toString();
    }
    public void flush() {
        delegate.flush();
    }
    public void close() {
        delegate.close();
    }
    public boolean checkError() {
        return delegate.checkError();
    }
    public void write(int b) {
        delegate.write(b);
    }
    public void write(byte[] buf, int off, int len) {
        delegate.write(buf, off, len);
    }
    public void print(boolean b) {
        delegate.print(b);
    }
    public void print(char c) {
        delegate.print(c);
    }
    public void print(int i) {
        delegate.print(i);
    }
    public void print(long l) {
        delegate.print(l);
    }
    public void print(float f) {
        delegate.print(f);
    }
    public void print(double d) {
        delegate.print(d);
    }
    public void print(char[] s) {
        delegate.print(s);
    }
    public void print(String s) {
        if (ignore.get() == null || !ignore.get()) {
            delegate.print(s);
        }
    }

    public void print(Object obj) {
        delegate.print(obj);
    }
    public void println() {
        if (ignore.get() == null || !ignore.get()) {
            delegate.println();
        }
    }
    public void println(boolean x) {
        delegate.println(x);
    }
    public void println(char x) {
        delegate.println(x);
    }
    public void println(int x) {
        delegate.println(x);
    }
    public void println(long x) {
        delegate.println(x);
    }
    public void println(float x) {
        delegate.println(x);
    }
    public void println(double x) {
        delegate.println(x);
    }
    public void println(char[] x) {
        delegate.println(x);
    }
    public void println(String x) {
        if (ignore.get() == null || !ignore.get()) {
            delegate.println(x);
        }
    }
    public void println(Object x) {
        delegate.println(x);
    }
    public PrintStream printf(String format, Object... args) {
        return delegate.printf(format, args);
    }
    public PrintStream printf(Locale l, String format, Object... args) {
        return delegate.printf(l, format, args);
    }
    public PrintStream format(String format, Object... args) {
        return delegate.format(format, args);
    }
    public PrintStream format(Locale l, String format, Object... args) {
        return delegate.format(l, format, args);
    }
    public PrintStream append(CharSequence csq) {
        return delegate.append(csq);
    }
    public PrintStream append(CharSequence csq, int start, int end) {
        return delegate.append(csq, start, end);
    }
    public PrintStream append(char c) {
        return delegate.append(c);
    }


}