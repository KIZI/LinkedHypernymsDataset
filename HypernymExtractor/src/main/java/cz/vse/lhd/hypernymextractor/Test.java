package cz.vse.lhd.hypernymextractor;

import java.io.IOException;
import java.net.InetSocketAddress;
import net.spy.memcached.MemcachedClient;

/**
 *
 * @author venca
 */
public class Test {

    public static void main(String[] args) throws IOException {

        MemcachedClient memClient = new MemcachedClient(new InetSocketAddress("192.168.116.129", 11211));
    }
}
