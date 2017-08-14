package prototype;

import net.ssehub.kernel_haven.Run;

/**
 * Temporary test class to test metrics.
 * 
 * @author Adam
 */
public class TestMain {

    /**
     * Starts a pipeline with testdata/test.properties.
     * 
     * @param args Ignored.
     */
    public static void main(String[] args) {
        Run.main(new String[] {"testdata/test.properties"});
    }
    
}
