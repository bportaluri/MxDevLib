/*
 * Copyright (c) 2017 Bruno Portaluri (MaximoDev)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mxdev.iface.cron;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

// http://www.rgagnon.com/javadetails/java-0055.html

public class FlatFileProcessFilter implements FileFilter
{
	String filter = null;
	String regex = null;

	public FlatFileProcessFilter(String filter)
	{
		this.filter = filter;
		
		regex = filter;
		regex = regex.replace(".", "\\.");
		regex = regex.replace("*", ".*");
	}


	public boolean accept(File f)
	{
		if(!f.isFile())
			return false;

		// case-sensitive matching
		return Pattern.matches(regex, f.getName());
	}
	
	public String toString()
	{
		return filter + " (" + regex + ")";
	}
	
}
