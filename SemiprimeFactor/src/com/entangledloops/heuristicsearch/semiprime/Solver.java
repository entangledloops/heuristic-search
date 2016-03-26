package com.entangledloops.heuristicsearch.semiprime;

import com.entangledloops.heuristicsearch.semiprime.client.Client;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.nanoTime;

/**
 * @author Stephen Dunn
 * @since November 2, 2015
 */
public class Solver implements Runnable, Serializable
{
  public static final String VERSION = "0.4.6a";

  /// list containing all instantiated solvers
  private static final ConcurrentLinkedQueue solvers = new ConcurrentLinkedQueue();

  // state vars
  private static final AtomicBoolean           networkSearch    = new AtomicBoolean(false); ///< true if and only if this search is hosted remotely
  private static final AtomicBoolean           networkHost      = new AtomicBoolean(false); ///< true if and only if this is the search host
  private static final AtomicBoolean           stats            = new AtomicBoolean(true); ///< timer prints stats according to user preferences
  private static final AtomicBoolean           detailedStats    = new AtomicBoolean(false); ///< if true---and at great expense---detailed stats will be recorded during search (debug)
  private static final AtomicBoolean           favorPerformance = new AtomicBoolean(true); ///< if true, will take additional steps to trade memory for more CPU;
  private static final AtomicBoolean           compressMemory   = new AtomicBoolean(false); ///< if true, will take additional steps to trade CPU for more memory
  private static final AtomicBoolean           restrictDisk     = new AtomicBoolean(true); ///< should we allow disk i/o during search to cache nodes?
  private static final AtomicBoolean           restrictNetwork  = new AtomicBoolean(false); ///< allow frequent network comm. during search?
  private static final AtomicBoolean           background       = new AtomicBoolean(false); ///< must wait until machine is idle before working
  private static final AtomicBoolean           printAllNodes    = new AtomicBoolean(false); ///< if false, fewer sanity checks are performed on values
  private static final AtomicBoolean           writeCsv         = new AtomicBoolean(false); ///< controls outputting csv-formatted node info during search
  private static final AtomicInteger           processors       = new AtomicInteger(1); ///< num cores allowed
  private static final AtomicInteger           processorCap     = new AtomicInteger(100); ///< percentage use allowed
  private static final AtomicInteger           memoryCap        = new AtomicInteger(100); ///< percentage use allowed
  private static final AtomicReference<String> csvPath          = new AtomicReference<>(null); ///< path to csv file that will be written if writeCsv is set

  // target info
  private static final AtomicInteger                   pLength      = new AtomicInteger(0); ///< optional: if set, only primes w/this len will be searched for
  private static final AtomicInteger                   qLength      = new AtomicInteger(0); ///< using 0 searches for all length possibilities
  private static final AtomicInteger                   internalBase = new AtomicInteger(2); ///< the base that will be used internally for the search representation
  private static final AtomicReference<Consumer<Node>> callback     = new AtomicReference<>(); ///< a function to receive the goal node (or null) upon completion
  private static final List<Heuristic>                 heuristics   = new CopyOnWriteArrayList<>(); ///< the list of heuristics to use for this search
  // wait-for-work timeout
  private static final long     statsPeriodMillis    = 10000L;
  private static final long     checkForWorkTimeout  = 10000L;
  private static final TimeUnit checkForWorkTimeUnit = TimeUnit.NANOSECONDS;

  // networking
  private static final AtomicReference<Client> client = new AtomicReference<>(); ///< worker threads

  // vars cached for performance

  // this instance's search state
  private final List<Thread>                  threads = Collections.synchronizedList(new ArrayList<>()); ///< worker threads
  private final PriorityBlockingQueue<Node>   open    = new PriorityBlockingQueue<>(); ///< unbounded queue backed by heap for fast pop() behavior w/o sorting
  private final ConcurrentHashMap<Node, Node> closed  = new ConcurrentHashMap<>(); ///< closed hash table
  private final AtomicReference<Node>         goal    = new AtomicReference<>(); ///< set if/when goal is found; if set, search will end
  private final AtomicBoolean                 solving = new AtomicBoolean(false);

  // some stats tracking
  private final AtomicReference<Timer> statsTimer  = new AtomicReference<>(); ///< periodic reporting on search
  private final AtomicLong             generated   = new AtomicLong();
  private final AtomicLong             regenerated = new AtomicLong();
  private final AtomicLong             ignored     = new AtomicLong();
  private final AtomicLong             expanded    = new AtomicLong();
  private final AtomicLong             startTime   = new AtomicLong(); ///< nanoseconds
  private final AtomicLong             endTime     = new AtomicLong(); ///< nanoseconds
  private final AtomicLong             totalDepth  = new AtomicLong(); ///< nanoseconds
  private final AtomicInteger          maxDepth    = new AtomicInteger(0);

  // search cache
  protected final BigInteger cacheSemiprime;
  protected final String     cacheSemiprimeString10; ///< cached base 10
  protected final String     cacheSemiprimeString2; ///< cached base 2
  protected final String     cacheSemiprimeStringInternal; ///< cached internal base
  protected final int        cacheSemiprimeLen10; ///< cached base 10
  protected final int        cacheSemiprimeLen2; ///< cached bit len
  protected final int        cacheSemiprimeLenInternal; ///< cached internal len
  protected final int        cacheInternalBase;
  protected final int        cacheSemiprimeBitLen; ///< cached internal len
  protected final int        cacheSemiprimeBitCount; ///< cached internal len
  protected final double         cacheSemiprimeBitCountOverBitLen; ///< cached internal len

  private final long           cacheStatsPeriodMillis;
  private final long           cacheCheckForWorkTimeout;
  private final TimeUnit       cacheCheckForWorkTimeUnit;
  private final Thread         cacheThread;
  private final Consumer<Node> cacheCallback;
  private final Heuristic[]    cacheHeuristics;
  private final int            cachePLength;
  private final int            cacheQLength;
  private final int            cacheMaxDepth; ///< max(pLength, qLength)
  private final int            cacheProcessors;
  private final boolean        cacheNetworkSearch;
  private final boolean        cacheNetworkHost;
  private final boolean        cacheStats;

  private boolean cachePrintAllNodes;
  private boolean cacheDetailedStats;
  private boolean cachePaused;
  private Client  cacheClient;

  public Solver(final BigInteger semiprime)
  {
    // check for invalid params
    if (null == semiprime) throw new NullPointerException("invalid target or base");

    // basic checks
    if (!semiprime.testBit(0)) throw new NullPointerException("input is even");
    if (semiprime.compareTo(BigInteger.valueOf(9)) < 0) throw new NullPointerException("input is not a semiprime number");

    cacheThread = new Thread(this);

    // cache search settings for consistency and speed
    Log.o("\n********** building search cache **********\n");
    try
    {
      // cache all variables
      cacheStatsPeriodMillis = statsPeriodMillis;
      cacheCheckForWorkTimeout = checkForWorkTimeout;
      cacheCheckForWorkTimeUnit = checkForWorkTimeUnit;
      cacheCallback = callback(); if (null == cacheCallback) throw new NullPointerException("no callback provided for search completion");
      cacheInternalBase = internalBase();
      cacheSemiprime = BigInteger.ZERO.add(semiprime);
      cacheSemiprimeString10 = cacheSemiprime.toString(10);
      cacheSemiprimeString2 = cacheSemiprime.toString(2);
      cacheSemiprimeStringInternal = cacheSemiprime.toString(cacheInternalBase);
      cacheSemiprimeLen10 = cacheSemiprimeString10.length();
      cacheSemiprimeLen2 = cacheSemiprimeString2.length();
      cacheSemiprimeLenInternal = cacheSemiprimeStringInternal.length();
      cacheSemiprimeBitLen = cacheSemiprime.bitLength();
      cacheSemiprimeBitCount = cacheSemiprime.bitCount();
      cachePLength = pLength();
      cacheQLength = qLength();
      cacheProcessors = Math.max(0, Math.min(Runtime.getRuntime().availableProcessors(), processors()));
      cacheSemiprimeBitCountOverBitLen = (double) cacheSemiprimeBitCount / (double) cacheSemiprimeBitLen;
      cacheMaxDepth = (0 < cachePLength || 0 < cacheQLength ? Math.max(cachePLength, cacheQLength) : (cacheSemiprimeBitLen-1)) - 1;  // -1 converts len -> depth, second -1 on spLen is multiplication logic
      cachePaused = paused();
      cacheNetworkSearch = networkSearch();
      cacheNetworkHost = networkHost();
      cacheStats = stats();
      cacheDetailedStats = detailedStats();
      cachePrintAllNodes = printAllNodes();

      // cache selected heuristics for this run
      cacheHeuristics = new Heuristic[ heuristics().size() ];
      int i = -1;
      for (Heuristic heuristic : heuristics())
      {
        if (i >= cacheHeuristics.length) throw new NullPointerException("heuristics changed during prep");
        else cacheHeuristics[++i] = heuristic;
      }
    }
    catch (Throwable t) { Log.e(t); throw new NullPointerException("cache preparation failure"); }

    // build worker threads to search until goal is found or no nodes left
    if (cacheNetworkHost)
    {
      Log.o("preparing network host thread...");
      threads.add(new Thread(() -> { try { while (null == goal() && solving() && !Thread.interrupted()) Thread.sleep(1000); } catch (Throwable ignored) {} }));
    }
    else
    {
      Log.o("preparing " + (cacheNetworkSearch ? "network" : "local") + " worker threads...");
      IntStream.range(1, cacheProcessors+1).forEach((i) -> threads.add(new Thread(() ->
      {
        try
        {
          Log.o("thread " + i + ": started");
          while (expand( pop() )) { while (cachePaused) Thread.sleep(1); }
          Log.o("thread " + i + ": finished");
        }
        catch (Throwable ignored) {}
      })));

      // if networked search, prepare special network sync thread
      if (cacheNetworkSearch)
      {
        threads.add(new Thread(() ->
        {
          try
          {
            while (solving() && null == goal() && !Thread.interrupted())
            {
              if (null != cacheClient)
              {
                if (cacheClient.disconnected()) cacheClient = client();
                else cacheClient.write(Packet.Type.UPDATE, open, closed);
              }
              else cacheClient = client();
              Thread.sleep(1000);
            }
          }
          catch (Throwable ignored) {}
       }));
      }
    }
  }

  @Override public String toString()
  {
    return "\n********** parameters **********\n" +
        "\nlength (base 10): " + cacheSemiprimeLen10 +
        "\ntarget (base 10): " + cacheSemiprimeString10 +
        "\n\nlength (base " + cacheInternalBase + "): " + cacheSemiprimeLenInternal +
        "\ntarget (base " + cacheInternalBase + "): " + cacheSemiprimeStringInternal +
        "\n\nheuristics: " + (cacheHeuristics.length > 0 ? Stream.of(cacheHeuristics).skip(1).map(Object::toString).reduce(cacheHeuristics[0].toString(), (h1, h2) -> h1 + ", " + h2) : "(none)") +
        "\np length (base " + cacheInternalBase + "): " + (0 != cachePLength ? cachePLength : "any") +
        "\nq length (base " + cacheInternalBase + "): " + (0 != cacheQLength ? cacheQLength : "any") +
        "\nbackground: " + background() +
        "\nprocessors: " + cacheProcessors +
        "\nprocessorCap: " + processorCap() +
        "\nfavorPerformance: " + favorPerformance +
        "\ncompressMemory: " + compressMemory +
        "\nmaxDepth: " + cacheMaxDepth +
        "\nopen.size(): " + open.size() +
        "\nclosed.size(): " + closed.size() +
        "\nthreads.size(): " + threads.size() +
        "\n";
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override public void run()
  {
    try
    {
      // atomically cancel and clear any previous search
      if (!solving.compareAndSet(false, true)) { Log.e("a solver is already running or this solver was not started properly"); return; }

      // store this thread so we can be cancelled later

      // record the start time
      startTime.set( System.nanoTime() );

      // push a new root node if open list is empty
      if (open.isEmpty()) push(new Node());

      // inform user of cache- and contract-bound search parameters
      Log.o(toString() + "\n********** search starting **********\n");

      // properly schedule a new timer if stats were requested
      if (cacheStats)
      {
        final Timer timer = new Timer();
        if (!statsTimer.compareAndSet(null, timer)) { Log.e("overlapping search request"); return; }
        startTime.set(nanoTime());
        timer.schedule(new TimerTask() { @Override public void run() { if (cachePaused) return; Log.o("progress:" + stats((nanoTime() - startTime.get()))); } }, statsPeriodMillis, statsPeriodMillis);
      }

      // launch all worker threads and wait for completion
      try { threads.stream().forEach(Thread::start); try { threads.stream().forEach((thread) -> { try { thread.join(); } catch (Throwable t) { Log.e("solving start interrupted", t); } }); } catch (Throwable ignored) {} } catch (Throwable t) { Log.e(t); }
      try { threads.stream().forEach((thread) -> { try { thread.interrupt(); } catch (Throwable ignored) {} }); } catch (Throwable ignored) {}
      threads.clear();

      // cancel the stats timer and record end time
      if (cacheStats)
      {
        final Timer timer = statsTimer.getAndSet(null);
        if (null != timer) { timer.cancel(); endTime.set(System.nanoTime()); }
      }

      // print full final stats after all work is done
      final boolean prevDetailedStats = cacheDetailedStats; cacheDetailedStats = true;
      Log.o("\nfinal stats:" + stats(elapsed()));
      cacheDetailedStats = prevDetailedStats;

      // notify waiters that we've completed factoring
      cacheCallback.accept( goal() );
    }
    catch (Throwable t) { Log.e("an error occurred during search", t); }
    finally { Log.o("\n********** search finished **********\n"); solving.set(false); }
  }

  public boolean start() { if (solving()) return false; try { cacheThread.start(); return true; } catch (Throwable t) { Log.e(t); return false; } }
  public void interrupt() { final Thread thread = cacheThread; if (null != thread) thread.interrupt(); }
  public void join() { try { final Thread thread = cacheThread; if (null != thread) thread.join(); } catch (Throwable ignored) {} }
  public void interruptAndJoin() { interrupt(); join(); }

  /**
   * if this node is newly closed, ensure we update the counter in a start-safe manner
   * @param n
   * @return the input node (to assist w/function chaining)
   */
  private Node close(Node n)
  {
    final Node prev = closed.put(n, n);
    if (null != prev) regenerated.addAndGet(1);
    return n;
  }

  /**
   * tests if goal node
   * @param n a node to attempt adding
   * @return false on goal or fatal exception, true indicates successful push
   */
  private boolean push(Node n)
  {
    if (goal(n)) return false;
    if (!open.offer(n)) { regenerated.addAndGet(1); return false; }
    return true;
  }

  /**
   * pop available node of opened
   * @return the next available node or null if goal was found or error occurred
   */
  @SuppressWarnings("StatementWithEmptyBody")
  private Node pop()
  {
    Node node;
    try { while (null == (node = open.poll(cacheCheckForWorkTimeout, cacheCheckForWorkTimeUnit))) if (null != goal()) return null; } catch (Throwable t) { return null; }
    return node;
  }

  /**
   * expands the current node, pushing any generated children
   * @param n
   * @return true continues the search, false indicates completion
   */
  private boolean expand(final Node n)
  {
    // stats
    if (cachePrintAllNodes) Log.o("expanding: " + n);
    if (cacheStats)
    {
      expanded.addAndGet(1);
      maxDepth.set(Math.max(maxDepth.get(), n.depth));
      totalDepth.addAndGet(n.depth);
    }

    // early interruptAndJoin if possible
    if (n.depth >= cacheMaxDepth) return true;

    // generate all node combinations
    for (int i = 0; i < cacheInternalBase; ++i)
    {
      for (int j = 0; j < cacheInternalBase; ++j)
      {
        if (i > j && n.identicalFactors()) continue;
        
        final Node node = close(new Node(n, i, j));
        if (null != node && node.validFactors())
        {
          generated.addAndGet(1);
          if (cachePrintAllNodes) Log.o("generated: " + node);
          if (!push(node)) return false;
        }
        else
        {
          ignored.addAndGet(1);
          if (cachePrintAllNodes) Log.o("ignored: " + node);
        }
      }
    }

    return true;
  }

  // resets the search
  public static void reset()
  {
    Log.o("resetting solver...");

    internalBase(2);
    pLength(0);
    qLength(0);

    client(null);
    callback(null);
    heuristics.clear();

    Log.o("solver reset");
  }

  public boolean solving() { return solving.get(); }
  public boolean paused() { return cachePaused; }
  public void pause() { Log.o("search paused"); cachePaused = true; }
  public void resume() { Log.o("search resumed"); cachePaused = false; }

  private String stats(final long elapsedNanos)
  {
    final long seconds = (elapsedNanos/1000000000L);
    return
        "<table border=\"1\">" +
        "<tr>" +
        "<th>generated</th>" + "<th>regenerated</th>" + "<th>ignored</th>" +
        "<th>expanded</th>" + "<th>maxDepth</th>" + "<th>avgDepth</th>" +
        "</tr>" +
        "<tr>" +
        "<td>" + generated + "</td>" + "<td>" + regenerated + "</td>" + "<td>" + ignored + "</td>" +
        "<td>" + expanded + "</td>" + "<td>" + maxDepth + "</td>" + "<td>" + avgDepth() + "</td>" +
        "</tr>" +
        "</table>" +
        (cacheDetailedStats ? "\topen.size():\t" + open.size() : "") +
        (cacheDetailedStats ? "\tclosed.size():\t" + closed.size() : "") +
        "elapsed:\t" + (seconds/60L) + " minutes, " + (seconds%60L) + " seconds";
  }


  /**
   * goal test w/a somewhat optimized order of comparisons
   * if it's the goal, it will be stored
   * @param n a node to test against the target
   * @return true if this is the goal or a goal node has been found
   */
  private boolean goal(Node n) { return null == n ? null != goal() : (n.goal() && (goal.compareAndSet(null, n) || null != goal())); }
  private Node goal() { return goal.get(); }

  private long generated() { return generated.get(); }
  private long regenerated() { return regenerated.get(); }
  private long ignored() { return ignored.get(); }
  private long expanded() { return expanded.get(); }
  private long maxDepth() { return maxDepth.get(); }
  private long totalDepth() { return totalDepth.get(); }
  private long avgDepth()
  {
    final long expanded = expanded();
    return 0 != expanded ? totalDepth() / expanded : 0;
  }

  public long startTime() { return startTime.get(); }
  public long endTime() { return endTime.get(); }
  public long elapsed() { return endTime.get() - startTime.get(); }

  public static void pauseAll()
  {

  }

  public static void resumeAll()
  {

  }

  public static boolean networkSearch() { return Solver.networkSearch.get(); }
  public static void networkSearch(boolean enabled) { Solver.networkSearch.set(enabled); }

  public static boolean networkHost() { return Solver.networkHost.get(); }
  public static void networkHost(boolean enabled) { Solver.networkHost.set(enabled); }

  public static Client client() { return Solver.client.get(); }
  public static void client(Client client) { Solver.client.set(client); }

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
  public static void internalBase(int base) { internalBase.set(base); }

  public static int pLength() { return pLength.get(); }
  public static void pLength(int len) { if (len < 0) Log.e("invalid len: " + len); else pLength.set(len); }

  public static int qLength() { return qLength.get(); }
  public static void qLength(int len) { if (len < 0) Log.e("invalid len: " + len); else qLength.set(len); }

  public static boolean primeLengthsFixed() { return 0 != pLength() && 0 != qLength(); }

  public static boolean stats() { return Solver.stats.get(); }
  public static void stats(boolean enabled) { Solver.stats.set(enabled); }

  public static void callback(Consumer<Node> callback) { Solver.callback.set(callback); }
  public static Consumer<Node> callback() { return callback.get(); }

  public static List<Heuristic> heuristics() { return heuristics; }
  public static void heuristics(Heuristic... heuristics)
  {
    if (null == heuristics || 0 == heuristics.length) return;
    for (Heuristic in : heuristics)
    {
      boolean skip = false;
      for (Heuristic existing : Solver.heuristics) if (existing.name().equals(in.name()))
      {
        skip = true;
        break;
      }
      if (!skip) Solver.heuristics.add(in);
    }
  }

  /**
   * @author Stephen Dunn
   * @since October 31, 2015
   */
  public class Node implements Serializable, Comparable
  {
    private final boolean   identicalFactors;
    private final int       hashCode;
    final        int        depth;
    public final BigInteger p, q; ///< the candidate factors
    public final BigInteger product; ///< the partial factors for this node
    private      double     h; ///< the heuristic search factors for this node

    int hashcount=0;
    Node() { this(null, 1, 1); }
    Node(final Node parent, int pBit, int qBit)
    {
      this.depth = null != parent ? parent.depth+1 : 0;

      final BigInteger f1 = null != parent ? (0 != pBit ? parent.p.setBit(depth) : parent.p) : BigInteger.valueOf(pBit);
      final BigInteger f2 = null != parent ? (0 != qBit ? parent.q.setBit(depth) : parent.q) : BigInteger.valueOf(qBit);

      final int compare = f1.compareTo(f2);
      this.identicalFactors = compare == 0;
      this.p = compare < 0 ? f1 : f2;
      this.q = compare < 0 ? f2 : f1;

      this.product = p.multiply(q);

      // cache the hash for performance during table lookups
      int hash = 37 * depth + product.hashCode();
      hash = 37 * hash + p.hashCode();
      hash = 37 * hash + q.hashCode();
      hashCode = hash;
    }

    @Override public String toString() { return product + "<sub>10</sub>:" + product.toString(cacheInternalBase) + "<sub>" + cacheInternalBase + "</sub>:" + depth + ":depth:" + p + ":p:" + q + ":q:" + h  + ":h:" + hashCode + ":hash"; }
    @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).depth == depth && p.equals(((Node) o).p) && q.equals(((Node) o).q); }
    @Override public int compareTo(Object o) { return Double.compare(h(), ((Node) o).h()); }
    @Override public int hashCode() { if (++hashcount > 1) Log.o(hashcount + " : " + toString()); return hashCode; }

    boolean identicalFactors() { return identicalFactors; }

    /**
     * This function ensures that the current partial product resembles the target semiprime
     * in the currently fixed digit positions.
     * @return true if everything looks okay
     */
    boolean validFactors()
    {
      return
          product.testBit(depth) == cacheSemiprime.testBit(depth) &&
          product.bitLength() <= cacheSemiprimeBitLen;
    }

    /**
     * Ensure that none of the factors is trivial.
     * @return true if this node is the goal
     */
    boolean goal()
    {
      return
          (0 == cachePLength || (1+depth) == cachePLength) &&
          (0 == cacheQLength || (1+depth) == cacheQLength) &&
          cacheSemiprime.equals(product) &&
          !BigInteger.ONE.equals(p) &&
          !BigInteger.ONE.equals(q);
    }


    /**
     * Sums all desired heuristic functions.
     * @return an estimate of this node's distance to goal, where 0 = goal
     */
    private double h()
    {
      if (0 != h) return h;
      for (Heuristic heuristic : cacheHeuristics) h += heuristic.apply(Solver.this, this);
      return h;
    }

  }
}
