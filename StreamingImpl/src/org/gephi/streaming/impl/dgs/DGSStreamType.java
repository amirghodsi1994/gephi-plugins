/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>, Andre Panisson <panisson@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.gephi.streaming.impl.dgs;

import org.gephi.streaming.api.StreamReader;
import org.gephi.streaming.api.StreamType;
import org.gephi.streaming.api.StreamWriter;
import org.openide.util.lookup.ServiceProvider;

/**
 * The GraphStream DSG file format.
 * 
 * @author panisson
 *
 */
@ServiceProvider(service = StreamType.class)
public class DGSStreamType implements StreamType {
    
    public String getType() {
        return "DGS";
    }
    
    public Class<? extends StreamReader> getStreamReaderClass() {
        return DGSStreamReader.class;
    }
    
    public Class<? extends StreamWriter> getStreamWriterClass() {
        return DGSStreamWriter.class;
    }

    @Override
    public String toString() {
        return getType();
    }

}