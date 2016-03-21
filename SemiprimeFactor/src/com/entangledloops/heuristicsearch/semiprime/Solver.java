package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
  private static final AtomicBoolean periodicStats    = new AtomicBoolean(true); ///< timer prints stats according to user preferences
  private static final AtomicBoolean detailedStats    = new AtomicBoolean(false); ///< if true---and at great expense---detailed stats will be recorded during search (debug)
  private static final AtomicBoolean favorPerformance = new AtomicBoolean(true); ///< if true, will take additional steps to trade memory for more CPU;
  private static final AtomicBoolean compressMemory   = new AtomicBoolean(false); ///< if true, will take additional steps to trade CPU for more memory
  private static final AtomicBoolean restrictDisk     = new AtomicBoolean(true); ///< should we allow disk i/o during search to cache nodes?
  private static final AtomicBoolean restrictNetwork  = new AtomicBoolean(false); ///< allow frequent network comm. during search?
  private static final AtomicBoolean background       = new AtomicBoolean(false); ///< must wait until machine is idle before working
  private static final AtomicBoolean printAllNodes    = new AtomicBoolean(false); ///< if false, fewer sanity checks are performed on values
  private static final AtomicBoolean writeCsv         = new AtomicBoolean(false); ///< controls outputting csv-formatted node info during search
  private static final AtomicInteger processors       = new AtomicInteger(Runtime.getRuntime().availableProcessors()); ///< num cores allowed
  private static final AtomicInteger processorCap     = new AtomicInteger(100); ///< percentage use allowed
  private static final AtomicInteger memoryCap        = new AtomicInteger(100); ///< percentage use allowed

  // search state
  private static final List<Thread>                    threads    = Collections.synchronizedList(new ArrayList<>()); ///< worker threads
  private static final List<Heuristic>                 heuristics = new CopyOnWriteArrayList<>(); ///< the list of heuristics to use for this search
  private static final PriorityBlockingQueue<Node>     open       = new PriorityBlockingQueue<>(); ///< unbounded queue backed by heap for fast pop() behavior w/o sorting
  private static final ConcurrentHashMap<Node, Node>   closed     = new ConcurrentHashMap<>(); ///< closed hash table
  private static final AtomicReference<Node>           goal       = new AtomicReference<>(); ///< set if/when goal is found; if set, search will end
  private static final AtomicReference<Consumer<Node>> callback   = new AtomicReference<>(); ///< a function to receive the goal node (or null) upon completion

  // some stats tracking
  private static final AtomicReference<Timer> statsTimer       = new AtomicReference<>(); ///< periodic reporting on search
  private static final AtomicLong             nodesGenerated   = new AtomicLong();
  private static final AtomicLong             nodesRegenerated = new AtomicLong();
  private static final AtomicLong             nodesIgnored     = new AtomicLong();
  private static final AtomicLong             nodesExpanded    = new AtomicLong();
  private static final AtomicLong             nodesClosed      = new AtomicLong();
  private static final AtomicLong             startTime        = new AtomicLong(); ///< nanoseconds
  private static final AtomicLong             endTime          = new AtomicLong(); ///< nanoseconds
  private static final AtomicLong             totalDepth       = new AtomicLong(); ///< nanoseconds
  private static final AtomicInteger          maxDepth         = new AtomicInteger(0);

  // target info
  private static final AtomicReference<BigInteger> semiprime    = new AtomicReference<>(BigInteger.ZERO); ///< the target semiprime value
  private static final AtomicInteger               primeLen1    = new AtomicInteger(0); ///< optional: if set, only primes w/this len will be searched for
  private static final AtomicInteger               primeLen2    = new AtomicInteger(0); ///< using 0 searches for all length possibilities
  private static final AtomicInteger               internalBase = new AtomicInteger(2); ///< the base that will be used internally for the search representation

  // vars cached for performance
  private static String semiprimeString10; ///< cached base 10
  private static String semiprimeString2; ///< cached base 2
  private static String semiprimeStringInternal; ///< cached internal base
  private static int    semiprimeLen10; ///< cached base 10
  private static int    semiprimeLen2; ///< cached bit len
  private static int    semiprimeLenInternal; ///< cached internal len

  // heuristic cache
  static int    semiprimeBitLen; ///< cached internal len
  static int    semiprime1s; ///< cached internal len
  static int    semiprime0s; ///< cached internal len
  static int    maxFactorLen; ///< max(primeLen1, primeLen2)
  static double semiprime1sToLen; ///< cached internal len

  private Solver(final String semiprime, final int semiprimeBase, final int internalBase)
  {
    // check for invalid params
    if (null == semiprime || "".equals(semiprime) || semiprimeBase < 2 || internalBase < 2) throw new NullPointerException("invalid target or base");

    // set and validate + basic checks
    Solver.semiprime.set(new BigInteger(semiprime, semiprimeBase));
    if (!semiprime().testBit(0)) throw new NullPointerException("input is even");
    if (semiprime().compareTo(BigInteger.valueOf(9)) < 0) throw new NullPointerException("input is not a semiprime number");

    // cache
    Solver.internalBase.set(internalBase);
    Solver.semiprimeString10 = Solver.semiprime().toString(10);
    Solver.semiprimeString2 = Solver.semiprime().toString(2);
    Solver.semiprimeStringInternal = Solver.semiprime().toString(internalBase);
    Solver.semiprimeLen10 = Solver.semiprimeString10.length();
    Solver.semiprimeLen2 = semiprimeString2.length();
    Solver.semiprimeLenInternal = semiprimeStringInternal.length();
    Solver.semiprimeBitLen = semiprime().bitLength();
    Solver.semiprime1s = semiprime().bitCount();
    Solver.semiprime0s = Solver.semiprimeLen2 - Solver.semiprime1s;
    Solver.semiprime1sToLen = (double) semiprime1s / (double) semiprimeBitLen;
  }

  @Override public String toString() { return semiprimeString10; }
  public String toString(int base) { return 10 == base ? semiprimeString10 : internalBase() == base ? semiprimeStringInternal : semiprime().toString(base); }


  public static boolean periodicStats() { return Solver.periodicStats.get(); }
  public static void periodicStats(boolean enabled) { Solver.periodicStats.set(enabled); }

  public static boolean detailedStats() { return Solver.detailedStats.get(); }
  public static void detailedStats(boolean enabled) { Solver.detailedStats.set(enabled); }

  public static boolean favorPerformance() { return Solver.favorPerformance.get(); }
  public static void favorPerformance(boolean enabled) { Solver.favorPerformance.set(enabled); }

  public static boolean compressMemory() { return Solver.compressMemory.get(); }
  public static void compressMemory(boolean enabled) { Solver.compressMemory.set(enabled); }

  public static boolean restrictDisk() { return Solver.restrictDisk.get(); }
  public static void restrictDisk(boolean enabled) { Solver.restrictDisk.set(enabled); }

  public static boolean restrictNetwork() { return Solver.restrictNetwork.get(); }
  public static void restrictNetwork(boolean enabled) { Solver.restrictNetwork.set(enabled); }

  public static void background(boolean background) { Solver.background.set(background); }
  public static boolean background() { return background.get(); }

  public static boolean printAllNodes() { return Solver.printAllNodes.get(); }
  public static void printAllNodes(boolean enabled) { Solver.printAllNodes.set(enabled); }

  public static boolean writeCsv() { return Solver.writeCsv.get(); }
  public static void writeCsv(boolean enabled) { Solver.writeCsv.set(enabled); }

  public static void processors(int processors) { Solver.processors.set(processors); }
  public static int processors() { return processors.get(); }

  public static void processorCap(int cap) { Solver.processorCap.set(cap); }
  public static int processorCap() { return processorCap.get(); }

  public static void memoryCap(int cap) { Solver.memoryCap.set(cap); }
  public static int memoryCap() { return memoryCap.get(); }

  public static int internalBase() { return internalBase.get(); }
  public static int length() { return semiprimeLenInternal; }
  public static int length(int base) { return internalBase() == base ? semiprimeLenInternal : (10 == base ? semiprimeLen10 : (2 == base ? semiprimeLen2 : semiprime().toString(base).length())); }

  public static int prime1Len() { return primeLen1.get(); }
  public static void prime1Len(int len) { if (len < 0) Log.e("invalid len: " + len); else primeLen1.set(len); }

  public static int prime2Len() { return primeLen2.get(); }
  public static void prime2Len(int len) { if (len < 0) Log.e("invalid len: " + len); else primeLen2.set(len); }

  public static boolean primeLengthsFixed() { return 0 != prime1Len() && 0 != prime2Len(); }

  public static long startTime() { return startTime.get(); }
  public static long endTime() { return endTime.get(); }
  public static long elapsed() { return endTime.get() - startTime.get(); }

  public static void callback(Consumer<Node> callback) { Solver.callback.set(callback); }
  public static Consumer<Node> callback() { return callback.get(); }

  public static List<Heuristic> heuristics() { return heuristics; }
  public static void addHeuristic(Heuristic heuristic)
  {
    if (null == heuristic) return;
    for (Heuristic h : heuristics) if (h.name().equals(heuristic.name())) return;
    heuristics.add(heuristic);
  }
  public static void heuristics(Heuristic... heuristics)
  {
    if (null == heuristics || 0 == heuristics.length) return;
    Solver.heuristics().clear();
    Collections.addAll(Solver.heuristics, heuristics);
  }

  public static BigInteger semiprime() { return semiprime.get(); }
  private static Node goal() { return goal.get(); }

  /**
   * goal test w/a somewhat optimized order of comparisons
   * if it's the goal, it will be stored
   * @param n a node to test against the target
   * @return true if this is the goal or a goal node has been found
   */
  private static boolean goal(Node n)
  {
    return null == n ? null != goal() : (n.goalFactors() && semiprime().equals(n.product) && (goal.compareAndSet(null, n) || null != goal()));
  }

  private static long nodesGenerated() { return nodesGenerated.get(); }
  private static long nodesRegenerated() { return nodesRegenerated.get(); }
  private static long nodesIgnored() { return nodesIgnored.get(); }
  private static long nodesExpanded() { return nodesExpanded.get(); }
  private static long nodesClosed() { return nodesClosed.get(); }
  private static long maxDepth() { return maxDepth.get(); }
  private static long totalDepth() { return totalDepth.get(); }
  private static long avgDepth() { return totalDepth() / nodesExpanded(); }

  /**
   * if this node is newly closed, ensure we update the counter in a thread-safe manner
   * @param n
   * @return the input node (to assist w/function chaining)
   */
  private static Node close(Node n)
  {
    final Node prev = closed.put(n, n);
    if (null != prev) {  nodesRegenerated.addAndGet(1); return null; }
    nodesClosed.addAndGet(1);
    return n;
  }

  /**
   * if this node is already known, record that we wasted effort to aid in future improvements
   * @param n a node to attempt adding
   * @return false on exception (possible null pointer or out of heap memory)
   */
  private static boolean push(Node n) { if (!open.offer(n)) nodesRegenerated.addAndGet(1); return true; }

  /**
   * pop available node of opened
   * @return the next available node or null if goal was found or error occurred
   */
  @SuppressWarnings("StatementWithEmptyBody")
  private static Node pop()
  {
    Node node;
    try { while (null == (node = open.poll(checkForWorkTimeout, checkForWorkTimeUnit))) if (null != goal()) return null; } catch (Throwable t) { return null; }
    return node;
  }

  /**
   * expands the current node, pushing any generated children
   * @param n
   * @return true continues the search, false indicates completion
   */
  private static boolean expand(final Node n)
  {
    // stats
    nodesExpanded.addAndGet(1);
    if (printAllNodes()) Log.o("expanding: " + n);
    if (periodicStats())
    {
      maxDepth.set(Math.max(maxDepth.get(), n.depth()));
      totalDepth.addAndGet(n.depth());
    }

    // generate all node combinations
    final int internalBase = internalBase();
    for (int i = 0; i < internalBase; ++i)
    {
      for (int j = 0; j < internalBase; ++j)
      {
        Node generated = close(new Node(n, i, j));
        if (null == generated || !generated.validFactors()) { nodesIgnored.addAndGet(1); continue; }
        nodesGenerated.addAndGet(1);
        if (printAllNodes()) Log.o("generated: " + generated);
        if (goal(generated) || !push(generated)) return false;
      }
    }

    return true;
  }

  private static String stats(final long elapsedNanos)
  {
    final long seconds = (elapsedNanos/1000000000L);
    return "\n\tnodesGenerated:\t" + nodesGenerated +
        "\n\tnodesRegenerated:\t" + nodesRegenerated +
        "\n\tnodesIgnored:\t" + nodesIgnored +
        "\n\tnodesExpanded:\t" + nodesExpanded +
        "\n\tnodesClosed:\t" + nodesClosed +
        "\n\tmaxDepth:\t" + maxDepth() + ", avgDepth: " + avgDepth() +
        "\n\telapsed:\t" + (seconds/60L) + " minutes, " + (seconds%60L) + " seconds";
  }

  // resets the search
  public static void reset()
  {
    // kill any running search threads
    if (!threads.isEmpty()) { Log.o("a previous search task is still running, terminating..."); interrupt(); }
    Log.o("preparing solver for new search...");

    // wipe previous search info
    nodesGenerated.set(0);
    nodesRegenerated.set(0);
    nodesIgnored.set(0);
    nodesExpanded.set(0);
    nodesClosed.set(0);

    maxDepth.set(0);
    totalDepth.set(0);
    startTime.set(0);
    endTime.set(0);

    prime1Len(0);
    prime2Len(0);

    goal.set(null);
    callback.set(null);

    open.clear();
    closed.clear();

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

    // cache vars for performance (and consistency if changed during search)
    maxFactorLen = Math.max(prime1Len(), prime2Len());

    // atomically cancel and clear any previous timer tasks
    Timer timer = statsTimer.getAndSet(null);
    if (null != timer) { timer.cancel(); endTime.set(0); startTime.set(0); }

    // inform user of contract-bound search parameters
    Log.o("\ninitial parameters:" +
        "\n\theuristics: " + (heuristics().size() > 0 ? heuristics().stream().skip(1).map(Object::toString).reduce(heuristics().get(0).toString(), (h1,h2) -> h1 + ", " + h2) : "(none)") +
        "\n\ttarget (base 10): " + semiprimeString10 +
        "\n\ttarget (base " + internalBase + "):  " + semiprimeStringInternal +
        "\n\tlength (base 10): " + semiprimeLen10 +
        "\n\tlength (base " + internalBase + "): " + semiprimeLenInternal +
        "\n\tp1 length (base " + internalBase + "): " + (0 != prime1Len() ? prime1Len() : "any") +
        "\n\tp2 length (base " + internalBase + "): " + (0 != prime2Len() ? prime2Len() : "any") +
        "\n\tbackground: " + background() +
        "\n\tprocessors: " + processors() +
        "\n\tprocessorCap: " + processorCap() +
        "\n\tfavorPerformance: " + favorPerformance +
        "\n\tcompressMemory: " + compressMemory +
        "\n\tmodifying settings during execution may produce undefined behavior" +
        "\n");

    // build worker threads to search until goal is found or no nodes left
    IntStream.range(0, processors()).forEach((i) -> threads.add(new Thread(() -> { try { Log.o("thread " + i + ": started"); while (expand(pop())); Log.o("thread " + i + ": finished"); } catch (Throwable ignored) {} })));

    // safe to assume these are the only valid first 3 roots
    if (open.isEmpty()) push(new Node());

    // properly schedule a new timer
    if (periodicStats() && statsTimer.compareAndSet(null, (timer = new Timer())))
    {
      startTime.set(nanoTime());
      timer.schedule(new TimerTask() { @Override public void run() { Log.o("progress:" + stats((nanoTime() - startTime.get()))); } }, statsPeriodMillis, statsPeriodMillis);
    }

    // launch all worker threads and wait for completion
    threads.stream().forEach(Thread::start);
    try { threads.stream().forEach((thread) -> { try { thread.join(); } catch (Throwable t) { Log.e("searching thread interrupted", t); } }); } catch (Throwable ignored) {}
    threads.clear();

    // cancel the timer and record end time
    if (null != (timer = statsTimer.getAndSet(null))) { timer.cancel(); endTime.set(System.nanoTime()); }

    // print final stats after all work is done
    Log.o("\nfinal stats:" + stats(elapsed()) + "\n\topen.size(): " + open.size() + "\n\tclosed.size(): " + closed.size());

    // notify waiters that we've completed factoring
    final Consumer<Node> callback = Solver.callback.get();
    if (null != callback) callback.accept(goal()); else { final Node goal = goal(); Log.o(null != goal ? "\nfactors found:\n\n\t" + goal : "factors not found"); }

    // print final message and quit
    Log.o("\n***** all threads joined and search is complete *****");
  }

  // primary driver for factoring
  public static Solver newInstance(final String semiprime) { return newInstance(semiprime, 10); } ///< constructor assuming base 10
  public static Solver newInstance(final String semiprime, int semiprimeBase) { return newInstance(semiprime, semiprimeBase, 2); } ///< constructor assuming optimal internal rep
  public static Solver newInstance(final String semiprime, int semiprimeBase, int internalBase) { try { return new Solver(semiprime, semiprimeBase, internalBase); } catch (Throwable t) { Log.e(t); return null; } }
}
