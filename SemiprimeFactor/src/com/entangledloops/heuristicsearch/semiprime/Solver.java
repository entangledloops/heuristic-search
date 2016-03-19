package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.lang.System.nanoTime;

/**
 * @author Stephen Dunn
 * @since November 2, 2015
 */
public class Solver implements Runnable, Serializable
{
  // wait-for-work timeout
  private static final long     statsPeriodMillis    = 10000L;
  private static final long     checkForWorkTimeout  = 1L;
  private static final TimeUnit checkForWorkTimeUnit = TimeUnit.MILLISECONDS;


  // state vars
  private static final AtomicBoolean safetyConscious = new AtomicBoolean(true); ///< if false, fewer sanity checks are performed on values and most statistics will be ignored
  private static final AtomicBoolean cpuConscious    = new AtomicBoolean(true); ///< if true, will take additional steps to trade memory for more CPU;
  private static final AtomicBoolean memoryConscious = new AtomicBoolean(false); ///< if true, will take additional steps to trade CPU for more memory
  private static final AtomicBoolean background      = new AtomicBoolean(false);
  private static final AtomicBoolean printAllNodes   = new AtomicBoolean(false); ///< if false, fewer sanity checks are performed on values
  private static final AtomicBoolean writeCsv        = new AtomicBoolean(false); ///< controls outputting csv-formatted node info during search
  private static final AtomicInteger processors      = new AtomicInteger(Runtime.getRuntime().availableProcessors());
  private static final AtomicInteger processorCap    = new AtomicInteger(100);
  private static final AtomicInteger memoryCap       = new AtomicInteger(100);

  // search state
  private static final List<Thread>                    threads  = Collections.synchronizedList(new ArrayList<>()); ///< worker threads
  private static final PriorityBlockingQueue<Node>     open     = new PriorityBlockingQueue<>(); ///< unbounded queue backed by heap for fast pop() behavior w/o sorting
  private static final ConcurrentHashMap<String, Node> opened   = new ConcurrentHashMap<>(); ///< the opened hash table for faster lookup times
  private static final ConcurrentHashMap<String, Node> closed   = new ConcurrentHashMap<>(); ///< closed hash table
  private static final AtomicReference<Node>           goal     = new AtomicReference<>(); ///< set if/when goal is found; if set, search will end
  private static final AtomicReference<Consumer<Node>> callback = new AtomicReference<>(); ///< a function to receive the goal node (or null) upon completion

  // some stats tracking
  private static final AtomicReference<BigInteger> nodesGenerated   = new AtomicReference<>(BigInteger.ZERO);
  private static final AtomicReference<BigInteger> nodesRegenerated = new AtomicReference<>(BigInteger.ZERO);
  private static final AtomicReference<BigInteger> nodesIgnored     = new AtomicReference<>(BigInteger.ZERO);
  private static final AtomicReference<BigInteger> nodesExpanded    = new AtomicReference<>(BigInteger.ZERO);
  private static final AtomicReference<BigInteger> nodesClosed      = new AtomicReference<>(BigInteger.ZERO);
  private static final AtomicReference<Timer>      statsTimer       = new AtomicReference<>(); ///< periodic reporting on search
  private static final AtomicLong startTime = new AtomicLong(); ///< nanoseconds
  private static final AtomicLong endTime = new AtomicLong(); ///< nanoseconds

  // numerical info
  private static final AtomicReference<BigInteger> semiprime    = new AtomicReference<>(BigInteger.ZERO); ///< the target semiprime value
  private static final AtomicInteger               internalBase = new AtomicInteger(2); ///< the base that will be used internally for the search representation
  private static final AtomicInteger               primeLen1    = new AtomicInteger(0); ///< optional: if set, only primes w/this len will be searched for
  private static final AtomicInteger               primeLen2    = new AtomicInteger(0); ///< using 0 searches for all length possibilities


  // vars cached for performance
  private static String semiprimeString10; ///< cached base 10
  private static String semiprimeStringInternal; ///< cached internal base
  private static int    semiprimeLen10; ///< cached base 10
  private static int    semiprimeLenInternal; ///< cached internal len

  // heuristic aids
  static String semiprimeBinary;
  static int    semiprimeBinaryLen; ///< cached internal len
  static int    semiprimeBinaryCount0; ///< cached internal len
  static int    semiprimeBinaryCount1; ///< cached internal len
  static double semiprimeBinary0sTo1s; ///< cached internal len

  private Solver(final String semiprime, final int semiprimeBase, final int internalBase)
  {
    // check for invalid params
    if (null == semiprime || "".equals(semiprime) || semiprimeBase < 2 || internalBase < 2) throw new NullPointerException("invalid target or base");

    // check for obviously composite values
    Solver.semiprime.set(new BigInteger(semiprime, semiprimeBase));
    if (semiprime().compareTo(BigInteger.valueOf(4)) < 0) throw new NullPointerException("input is not a semiprime number");
    if (semiprime().mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO)) throw new NullPointerException("input is divisible by 2");

    // setup cache
    Solver.semiprimeString10 = Solver.semiprime().toString(10);
    Solver.semiprimeLen10 = Solver.semiprimeString10.length();

    Solver.semiprimeBinary = Solver.semiprime().toString(2);
    Solver.semiprimeBinaryLen = Solver.semiprimeBinary.length();

    Solver.internalBase.set(internalBase);
    Solver.semiprimeStringInternal = semiprime().toString(internalBase);
    Solver.semiprimeLenInternal = Solver.semiprimeStringInternal.length();

    // cached heuristics aids
    for (final char c : semiprimeBinary.toCharArray()) { if ('0' == c) ++semiprimeBinaryCount0; else ++semiprimeBinaryCount1; }
    semiprimeBinary0sTo1s = (double)semiprimeBinaryCount0/(double)semiprimeBinaryLen;
  }

  @Override public String toString() { return semiprimeString10; }
  public String toString(int base) { return 10 == base ? semiprimeString10 : internalBase() == base ? semiprimeStringInternal : semiprime().toString(base); }

  public static BigInteger semiprime() { return semiprime.get(); }

  public static int length() { return semiprimeLen10; }
  public static int length(int base) { return 10 == base ? semiprimeLen10 : internalBase() == base ? semiprimeLenInternal : semiprime().toString(base).length(); }

  public static int prime1Len() { return primeLen1.get(); }
  public static void prime1Len(int len) { if (len < 0) Log.e("invalid len: " + len); else primeLen1.set(len); }

  public static int prime2Len() { return primeLen2.get(); }
  public static void prime2Len(int len) { if (len < 0) Log.e("invalid len: " + len); else primeLen2.set(len); }

  public static boolean primeLengthsFixed() { return 0 != prime1Len() && 0 != prime2Len(); }

  public static long startTime() { return startTime.get(); }
  public static long endTime() { return endTime.get(); }
  public static long elapsed() { return endTime.get() - startTime.get(); }

  public static void background(boolean background) { Solver.background.set(background); }
  public static boolean background() { return background.get(); }

  public static void processors(int processors) { Solver.processors.set(processors); }
  public static int processors() { return processors.get(); }

  public static void processorCap(int cap) { Solver.processorCap.set(cap); }
  public static int processorCap() { return processorCap.get(); }

  public static void memoryCap(int cap) { Solver.memoryCap.set(cap); }
  public static int memoryCap() { return memoryCap.get(); }

  public static void callback(Consumer<Node> callback) { Solver.callback.set(callback); }
  public static Consumer<Node> callback() { return callback.get(); }

  private static Node goal() { return goal.get(); }

  public static boolean safetyConscious() { return Solver.safetyConscious.get(); }
  public static void safetyConscious(boolean enabled) { Solver.safetyConscious.set(enabled); }

  public static boolean cpuConscious() { return Solver.cpuConscious.get(); }
  public static void cpuConscious(boolean enabled) { Solver.cpuConscious.set(enabled); }

  public static boolean memoryConscious() { return Solver.memoryConscious.get(); }
  public static void memoryConscious(boolean enabled) { Solver.memoryConscious.set(enabled); }

  public static boolean printAllNodes() { return Solver.printAllNodes.get(); }
  public static void printAllNodes(boolean enabled) { Solver.printAllNodes.set(enabled); }

  public static boolean writeCsv() { return Solver.writeCsv.get(); }
  public static void writeCsv(boolean enabled) { Solver.writeCsv.set(enabled); }

  public static int internalBase() { return internalBase.get(); }

  /**
   * goal test w/a somewhat optimized order of comparisons
   * if it's the goal, it will be stored
   * @param n a node to test against the target
   * @return true if this is the goal or a goal node has been found
   */
  private static boolean goal(Node n)
  {
    return null == n ? null != goal() : (semiprime().equals(n.product) && n.goalFactors() && (goal.compareAndSet(null, n) || null != goal()));
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void generated() { while (!nodesGenerated.compareAndSet(nodesGenerated.get(), BigInteger.ONE.add(nodesGenerated.get()))); }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void regenerated() { while (!nodesRegenerated.compareAndSet(nodesRegenerated.get(), BigInteger.ONE.add(nodesRegenerated.get()))); }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void ignored() { while (!nodesIgnored.compareAndSet(nodesIgnored.get(), BigInteger.ONE.add(nodesIgnored.get()))); }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void expanded() { while (!nodesExpanded.compareAndSet(nodesExpanded.get(), BigInteger.ONE.add(nodesExpanded.get()))); }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void closed() { while (!nodesClosed.compareAndSet(nodesClosed.get(), BigInteger.ONE.add(nodesClosed.get()))); }

  /**
   * if this node is newly closed, ensure we update the counter in a thread-safe manner
   * @param n
   * @return the input node (to assist w/function chaining)
   */
  private static Node close(Node n)
  {
    if (null == n) return null;
    if (null == closed.putIfAbsent(n.toString(), n)) closed();
    return n;
  }

  /**
   * if this node is already known, record that we wasted effort to aid in future improvements
   * @param n a node to attempt adding
   * @return false on exception (possible null pointer or out of heap memory)
   */
  private static boolean push(Node n)
  {
    try { if (null != opened.putIfAbsent(n.toString(), n) || !open.offer(n)) regenerated(); return true; }
    catch (Throwable t) { Log.e(t); return false; }
  }

  /**
   * pop available node of opened
   * @return the next available node or null if goal was found
   */
  @SuppressWarnings("StatementWithEmptyBody")
  private static Node pop()
  {
    Node node;
    try { while (null == (node = close(open.poll(checkForWorkTimeout, checkForWorkTimeUnit)))) if (null != goal()) return null; } catch (Throwable t) { return null; }
    return node;
  }

  /**
   * expands the current node, pushing any generated children
   * @param n
   * @return true continues the search, false indicates completion
   */
  private static boolean expand(final Node n)
  {
    //if (nodesGenerated.get().compareTo(BigInteger.valueOf(100)) > 0) System.exit(0);
    if (printAllNodes()) Log.o("expanding: " + n.product.toString(10) + " / " + n.product.toString(internalBase.get()) + " : [" + n.toString() + ":" + n.h + "]");

    // check if we found the goal already or this node is the goal
    if (goal(n)) return false; else expanded();

    // cache some vars
    final String p1 = n.factor(0, 2), p2 = n.factor(1, 2);

    // ensure we should bother w/this node at all
    if (n.product.compareTo(semiprime()) > 0 || p1.length() >= semiprimeBinaryLen || p2.length() >= semiprimeBinaryLen) return true;

    // generate all node combinations
    final int internalBase = internalBase();
    for (int i = 0; i < internalBase; ++i)
    {
      for (int j = 0; j < internalBase; ++j)
      {
        // generate value and hash of new candidate child
        final String np1 = i + p1, np2 = j + p2;
        final String key = Node.hash(np1, np2);
        if (null != closed.get(key) || null != opened.get(key)) continue;

        // prevents the same nodes from occurring w/children in reverse order
        final String keyAlt = Node.hash(np2, np1);
        if (null != closed.get(keyAlt) || null != opened.get(keyAlt)) continue;

        // try to push the new child
        Node generated = new Node(key, np1, np2); generated();
        if (printAllNodes()) Log.o("generated: " + generated.product.toString(10) + " / " + generated.product.toString(internalBase) + " : [" + generated.toString() + ":" + generated.h + "]");
        if (!generated.validFactors()) { ignored(); close(generated); }
        else if (!push(generated)) return false;
      }
    }

    // push all generated nodes simultaneously as an array
    return true;
  }

  private static String stats(final long elapsedNanos)
  {
    final long seconds = (elapsedNanos/1000000000L);
    return "\n\telapsed: " + (seconds/60L) + " minutes, " + (seconds%60L) + " seconds" +
       "\n\tnodesGenerated: " + nodesGenerated +
       "\n\tnodesIgnored: " + nodesIgnored +
       "\n\tnodesExpanded: " + nodesExpanded +
       "\n\tnodesClosed: " + nodesClosed;
  }

  // resets the search
  public static void reset()
  {
    // kill any running search threads
    if (!threads.isEmpty()) { Log.o("a previous search task is still running, terminating..."); interrupt(); }
    Log.o("preparing solver for new search...");

    // wipe previous search info
    startTime.set(0);
    endTime.set(0);

    nodesGenerated.set(BigInteger.ZERO);
    nodesRegenerated.set(BigInteger.ZERO);
    nodesIgnored.set(BigInteger.ZERO);
    nodesExpanded.set(BigInteger.ZERO);

    prime1Len(0);
    prime2Len(0);

    goal.set(null);
    callback.set(null);

    open.clear();
    opened.clear();
    closed.clear();

    push(new Node(new String[] {"1", "1"})); // safe to assume these are valid first 2 semiprime roots

    Log.o("solver reset");
  }

  public static void interrupt()
  {
    threads.stream().forEach(Thread::interrupt);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override public void run()
  {
    Log.o("\n***** searching for factors *****");
    final int internalBase = Solver.internalBase();

    // ensure user provided valid flags
    if (safetyConscious() && !cpuConscious() && !memoryConscious())
    {
      cpuConscious(true);
      Log.e("safety checks determined that your configuration is probably ineffective" +
          "\n\t- having all optimizations disabled makes no positive space/time trade-offs" +
          "\n\t- CPU optimizations have been automatically enabled" +
          "\n\t- you can force the requested behavior by disabling safety checks");
    }

    // atomically cancel and clear any previous timer tasks
    Timer timer = statsTimer.getAndSet(null);
    if (null != timer) { timer.cancel(); endTime.set(0); startTime.set(0); }

    // inform user of contract-bound search parameters
    Log.o("\ninitial parameters:" +
        "\n\ttarget (base 10): " + semiprimeString10 +
        "\n\ttarget (base " + internalBase + "):  " + semiprimeStringInternal +
        "\n\tlength (base 10): " + semiprimeLen10 +
        "\n\tlength (base " + internalBase + "): " + semiprimeLenInternal +
        "\n\tp1 length (base " + internalBase + "): " + (0 != prime1Len() ? prime1Len() : "any") +
        "\n\tp2 length (base " + internalBase + "): " + (0 != prime2Len() ? prime2Len() : "any") +
        "\n\tbackground: " + background() +
        "\n\tprocessors: " + processors() +
        "\n\tprocessorCap: " + processorCap() +
        "\n\tcpuConscious: " + cpuConscious +
        "\n\tmemoryConscious: " + memoryConscious +
        "\n\tmodifying settings during execution may produce undefined behavior" +
        "\n");

    // build worker threads to search until goal is found or no nodes left
    IntStream.range(0, processors()).forEach((i) -> threads.add(new Thread(() -> { try { Log.o("thread " + i + ": started"); while (expand(pop())); Log.o("thread " + i + ": finished"); } catch (Throwable ignored) {} })));

    // properly schedule a new timer
    if (statsTimer.compareAndSet(null, (timer = new Timer())))
    {
      startTime.set(nanoTime());
      timer.schedule(new TimerTask() { @Override public void run() { Log.o("progress:" + stats((nanoTime() - startTime.get()))); } }, statsPeriodMillis, statsPeriodMillis);
    }

    // launch all worker threads and wait for completion
    threads.parallelStream().forEach(Thread::start);
    try { threads.stream().forEach((thread) -> { try { thread.join(); } catch (Throwable t) { Log.e("searching thread interrupted", t); } }); } catch (Throwable ignored) {}
    threads.clear();

    // cancel the timer and record end time
    if (null != (timer = statsTimer.getAndSet(null)))
    {
      timer.cancel();
      endTime.set(System.nanoTime());
    }

    // print final stats after all work is done
    Log.o("\nfinal stats:" + stats(elapsed()) +
            "\n\topen.size(): " + open.size() +
            "\n\topened.size(): " + opened.size() +
            "\n\tclosed.size(): " + closed.size());

    // notify waiters that we've completed factoring
    final Consumer<Node> callback = Solver.callback.get();
    final Node goal = goal();
    if (null != callback) callback.accept(goal); else Log.o((null != goal ? "factors found:\n" + goal : "factors not found"));

    // print final message and quit
    Log.o("\n***** all threads joined and search is complete *****");
  }

  // primary driver for factoring
  public static Solver newInstance(final String semiprime) { return newInstance(semiprime, 10); } ///< constructor assuming base 10
  public static Solver newInstance(final String semiprime, int semiprimeBase) { return newInstance(semiprime, semiprimeBase, 2); } ///< constructor assuming optimal internal rep
  public static Solver newInstance(final String semiprime, int semiprimeBase, int internalBase) { try { return new Solver(semiprime, semiprimeBase, internalBase); } catch (Throwable t) { Log.e(t); return null; } }
}
