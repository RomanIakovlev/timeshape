package net.iakovlev.timeshape;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.File;
/*
import com.esri.core.geometry.Envelope;
import com.github.luben.zstd.ZstdInputStream;
import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

*/

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnit4.class)
public class TimeZoneEngineSerializationTest {
    private static TimeZoneEngine engine = TimeZoneEngine.initialize(0, 5, 0, 5, false);

    @Test
    public void testSerialzation() {
        File f = new File ("./Engine.cache");
        try {
            serializeTimeZoneEngine (f, engine);
            TimeZoneEngine engine2 = deserializeTimeZoneEngine (f);
            f.delete ();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Serializes an instance of {@link TimeZoneEngine} to a file
     * This is a blocking long running operation.
     *
     * @param f Destination File. 
     * @param eng Instance of TimeZoneEngine to serialize
     */

    public void serializeTimeZoneEngine (File f, TimeZoneEngine eng) throws IOException
    {
        FileOutputStream fileOutputStream = new FileOutputStream (f, false);
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream (new BufferedOutputStream (fileOutputStream))) {
           objectOutputStream.writeObject (eng);
           objectOutputStream.flush ();
        }
    }

    
    /**
     * Creates a new instance of {@link TimeZoneEngine} from previously serialized data.
     * This is a blocking long running operation.
     *
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine deserializeTimeZoneEngine (File f) throws IOException, ClassNotFoundException
    {
        FileInputStream fileInputStream = new FileInputStream(f);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (TimeZoneEngine) objectInputStream.readObject ();
        }
    }

}
