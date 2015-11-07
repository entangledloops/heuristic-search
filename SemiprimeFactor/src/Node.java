import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Stephen on 10/31/2015.
 */
public class Node
{
  private AtomicBoolean locked;  ///< is this node locked by a worker client?
  private BigInteger    g, h, f; ///< the heuristic search values for this node
  public  BigInteger    p, q;    ///< the partial factors for this node

  public Node()
  {
    locked = new AtomicBoolean(false);

  }
}
