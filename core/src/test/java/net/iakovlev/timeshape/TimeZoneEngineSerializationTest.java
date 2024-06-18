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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnit4.class)
public class TimeZoneEngineSerializationTest {
    private static TimeZoneEngine engine = TimeZoneEngine.initialize(47.0599, 4.8237, 55.3300, 15.2486, true);

    @Test
    public void testSerialzation() {
        File f = new File ("./Engine.cache");
        try {
            serializeTimeZoneEngine (f, engine);
            TimeZoneEngine engine2 = deserializeTimeZoneEngine (f);
            assertEquals(engine.query(52.52, 13.40), engine2.query(52.52, 13.40));

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
