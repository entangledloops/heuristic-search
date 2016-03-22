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

  // ensures singleton behavior is maintained
  private static final AtomicReference<Solver> instance = new AtomicReference<>();

  // state vars
  private static final AtomicBoolean solving          = new AtomicBoolean(false); ///< true if and only if a search is running
  private static final AtomicBoolean paused           = new AtomicBoolean(false); ///< true if and only if a search is running
  private static final AtomicBoolean networkSearch    = new AtomicBoolean(false); ///< true if and only if a search is running
  private static final AtomicBoolean networkHost      = new AtomicBoolean(false); ///< true if and only if a search is running
  private static final AtomicBoolean stats            = new AtomicBoolean(true); ///< timer prints stats according to user preferences
  private static final AtomicBoolean detailedStats    = new AtomicBoolean(false); ///< if true---and at great expense---detailed stats will be recorded during search (debug)
  private static final AtomicBoolean favorPerformance = new AtomicBoolean(true); ///< if true, will take additional steps to trade memory for more CPU;
  private static final AtomicBoolean compressMemory   = new AtomicBoolean(false); ///< if true, will take additional steps to trade CPU for more memory
  private static final AtomicBoolean restrictDisk     = new AtomicBoolean(true); ///< should we allow disk i/o during search to cache nodes?
  private static final AtomicBoolean restrictNetwork  = new AtomicBoolean(false); ///< allow frequent network comm. during search?
  private static final AtomicBoolean background       = new AtomicBoolean(false); ///< must wait until machine is idle before working
  private static final AtomicBoolean printAllNodes    = new AtomicBoolean(false); ///< if false, fewer sanity checks are performed on values
  private static final AtomicBoolean writeCsv         = new AtomicBoolean(false); ///< controls outputting csv-formatted node info during search
  private static final AtomicInteger processors       = new AtomicInteger(1); ///< num cores allowed
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
  private static final AtomicReference<Timer> statsTimer  = new AtomicReference<>(); ///< periodic reporting on search
  private static final AtomicLong             generated   = new AtomicLong();
  private static final AtomicLong             regenerated = new AtomicLong();
  private static final AtomicLong             ignored     = new AtomicLong();
  private static final AtomicLong             expanded    = new AtomicLong();
  private static final AtomicLong             startTime   = new AtomicLong(); ///< nanoseconds
  private static final AtomicLong             endTime     = new AtomicLong(); ///< nanoseconds
  private static final AtomicLong             totalDepth  = new AtomicLong(); ///< nanoseconds
  private static final AtomicInteger          maxDepth    = new AtomicInteger(0);

  // target info
  private static final AtomicReference<BigInteger> semiprime    = new AtomicReference<>(BigInteger.ZERO); ///< the target semiprime value
  private static final AtomicInteger               pLength      = new AtomicInteger(0); ///< optional: if set, only primes w/this len will be searched for
  private static final AtomicInteger               qLength      = new AtomicInteger(0); ///< using 0 searches for all length possibilities
  private static final AtomicInteger               internalBase = new AtomicInteger(2); ///< the base that will be used internally for the search representation

  // wait-for-work timeout
  private static final long     statsPeriodMillis    = 10000L;
  private static final long     checkForWorkTimeout  = 10000L;
  private static final TimeUnit checkForWorkTimeUnit = TimeUnit.NANOSECONDS;

  // networking
  private static final AtomicReference<Client> client = new AtomicReference<>(); ///< worker threads

  // vars cached for performance
  private static String cacheSemiprimeString10; ///< cached base 10
  private static String cacheSemiprimeString2; ///< cached base 2
  private static String cacheSemiprimeStringInternal; ///< cached internal base
  private static int    cacheSemiprimeLen10; ///< cached base 10
  private static int    cacheSemiprimeLen2; ///< cached bit len
  private static int    cacheSemiprimeLenInternal; ///< cached internal len

  // search cache
  static Client         cacheClient;
  static Consumer<Node> cacheCallback;
  static Heuristic[]    cacheHeuristics;
  static BigInteger     cacheSemiprime;
  static int            cacheInternalBase;
  static int            cacheSemiprimeBitLen; ///< cached internal len
  static int            cacheSemiprimeBitCount; ///< cached internal len
  static int            cachePLength;
  static int            cacheQLength;
  static int            cacheMaxDepth; ///< max(pLength, qLength)
  static int            cacheProcessors;
  static double         cacheSemiprimeBitCountOverBitLen; ///< cached internal len
  static boolean        cachePaused;
  static boolean        cacheNetworkSearch;
  static boolean        cacheNetworkHost;
  static boolean        cacheStats;
  static boolean        cacheDetailedStats;
  static boolean        cachePrintAllNodes;

  private Solver(final String semiprime, final int semiprimeBase)
  {
    // check for invalid params
    if (null == semiprime || "".equals(semiprime) || semiprimeBase < 2) throw new NullPointerException("invalid target or base");

    // set and validate + basic checks
    final BigInteger sp = new BigInteger(semiprime, semiprimeBase);
    if (!sp.testBit(0)) throw new NullPointerException("input is even");
    if (sp.compareTo(BigInteger.valueOf(9)) < 0) throw new NullPointerException("input is not a semiprime number");

    // store the provided value
    Solver.semiprime.set(sp);
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
      Log.o("\n********** preparing for new search **********\n");

      // atomically cancel and clear any previous search
      if (!solving.compareAndSet(false, true))
      {
        interruptAndJoin();
        if (!solving.compareAndSet(false, true)) { Log.e("failed to launch solver"); return; }
        startTime.set( System.nanoTime() );
      }

      // ensure any leftover threads are now wiped
      threads.clear();

      // cache search settings for consistency and speed
      Log.o("building cache...");
      try
      {
        Solver.cacheCallback = callback(); if (null == cacheCallback) cacheCallback = (n) -> Log.o(null != n ? "\nsolution found:\n\n\t" + n : "solution not found");
        Solver.cacheInternalBase = internalBase();
        Solver.cacheSemiprime = BigInteger.ZERO.add( semiprime() );
        Solver.cacheSemiprimeString10 = cacheSemiprime.toString(10);
        Solver.cacheSemiprimeString2 = cacheSemiprime.toString(2);
        Solver.cacheSemiprimeStringInternal = cacheSemiprime.toString(cacheInternalBase);
        Solver.cacheSemiprimeLen10 = Solver.cacheSemiprimeString10.length();
        Solver.cacheSemiprimeLen2 = cacheSemiprimeString2.length();
        Solver.cacheSemiprimeLenInternal = cacheSemiprimeStringInternal.length();
        Solver.cacheSemiprimeBitLen = cacheSemiprime.bitLength();
        Solver.cacheSemiprimeBitCount = cacheSemiprime.bitCount();
        Solver.cachePLength = pLength();
        Solver.cacheQLength = qLength();
        Solver.cacheProcessors = Math.max(0, Math.min(Runtime.getRuntime().availableProcessors(), processors()));
        Solver.cacheSemiprimeBitCountOverBitLen = (double) cacheSemiprimeBitCount / (double) cacheSemiprimeBitLen;
        Solver.cacheMaxDepth = (0 < cachePLength || 0 < cacheQLength ? Math.max(cachePLength, cacheQLength) : cacheSemiprimeBitLen) - 2;  // -1 converts len -> depth, -1 again b/c this is used for children
        Solver.cachePaused = paused();
        Solver.cacheNetworkSearch = networkSearch();
        Solver.cacheNetworkHost = networkHost();
        Solver.cacheStats = stats();
        Solver.cacheDetailedStats = detailedStats();
        Solver.cachePrintAllNodes = printAllNodes();
        Solver.cacheHeuristics = new Heuristic[ heuristics().size() ];
        for (int i = 0; i < cacheHeuristics.length; ++i) cacheHeuristics[i] = heuristics().get(i);
      }
      catch (Throwable t) { Log.e("cache preparation failure", t); return; }

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
      threads.stream().forEach(Thread::start);
      try { threads.stream().forEach((thread) -> { try { thread.join(); } catch (Throwable t) { Log.e("solving get interrupted", t); } }); } catch (Throwable ignored) {}
      threads.clear();

      // cancel the stats timer and record end time
      if (cacheStats)
      {
        final Timer timer = statsTimer.getAndSet(null);
        if (null != timer) { timer.cancel(); endTime.set(System.nanoTime()); }
      }

      // print full final stats after all work is done
      final boolean detail = detailedStats.getAndSet(true);
      Log.o("\nfinal stats:" + stats(elapsed()));
      detailedStats.set(detail);

      // notify waiters that we've completed factoring
      cacheCallback.accept( goal() );
    }
    catch (Throwable t) { Log.e("an error occurred during search", t); }
    finally { Log.o("\n********** search finished **********\n"); solving.set(false); }
  }

  public static boolean solving() { return solving.get(); }

  public static boolean paused() { return paused.get(); }
  public static boolean pause() { Log.o("search paused"); cachePaused = true; return paused.getAndSet(true); }
  public static boolean resume() { Log.o("search resumed"); cachePaused = false; return paused.getAndSet(false); }

  public static boolean networkSearch() { return Solver.networkSearch.get(); }
  public static void networkSearch(boolean enabled) { Solver.networkSearch.set(enabled); }

  public static boolean networkHost() { return Solver.networkHost.get(); }
  public static void networkHost(boolean enabled) { Solver.networkHost.set(enabled); }

  public static Client client() { return Solver.client.get(); }
  public static void client(Client client) { cacheClient = client; Solver.client.set(client); }

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
  public static int length() { return cacheSemiprimeLenInternal; }
  public static int length(int base) { return internalBase() == base ? cacheSemiprimeLenInternal : (10 == base ? cacheSemiprimeLen10 : (2 == base ? cacheSemiprimeLen2 : semiprime().toString(base).length())); }

  public static int pLength() { return pLength.get(); }
  public static void pLength(int len) { if (len < 0) Log.e("invalid len: " + len); else pLength.set(len); }

  public static int qLength() { return qLength.get(); }
  public static void qLength(int len) { if (len < 0) Log.e("invalid len: " + len); else qLength.set(len); }

  public static boolean primeLengthsFixed() { return 0 != pLength() && 0 != qLength(); }

  public static long startTime() { return startTime.get(); }
  public static long endTime() { return endTime.get(); }
  public static long elapsed() { return endTime.get() - startTime.get(); }

  public static boolean stats() { return Solver.stats.get(); }
  public static void stats(boolean enabled) { Solver.stats.set(enabled); }
  private static String stats(final long elapsedNanos)
  {
    final long seconds = (elapsedNanos/1000000000L);
    return
        "<table border=\"1\">" +
        "<tr>" +
        "<th>generated</th>" + "<th>regenerated</th>" + "<th>regenerated</th>" +
        "<th>ignored</th>" + "<th>expanded</th>" + "<th>maxDepth</th>" +
        "</tr><tr>" +
        "<td>" + generated + "</td>" + "<td>" + regenerated + "</td>" + "<th>" + regenerated + "</td>" +
        "<td>" + ignored + "</td>" + "<td>" + expanded + "</td>" + "<th>" + maxDepth + "</td>" +
        "</tr>" +
        "</table>" +
        (cacheDetailedStats ? "\topen.size():\t" + open.size() : "") +
        (cacheDetailedStats ? "\tclosed.size():\t" + closed.size() : "") +
        "elapsed:\t" + (seconds/60L) + " minutes, " + (seconds%60L) + " seconds";
  }

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

  public static BigInteger semiprime() { return semiprime.get(); }

  /**
   * goal test w/a somewhat optimized order of comparisons
   * if it's the goal, it will be stored
   * @param n a node to test against the target
   * @return true if this is the goal or a goal node has been found
   */
  private static boolean goal(Node n) { return null == n ? null != goal() : (n.goal() && (goal.compareAndSet(null, n) || null != goal())); }
  private static Node goal() { return goal.get(); }

  private static long generated() { return generated.get(); }
  private static long regenerated() { return regenerated.get(); }
  private static long ignored() { return ignored.get(); }
  private static long expanded() { return expanded.get(); }
  private static long maxDepth() { return maxDepth.get(); }
  private static long totalDepth() { return totalDepth.get(); }
  private static long avgDepth()
  {
    final long expanded = expanded();
    return 0 != expanded ? totalDepth() / expanded : 0;
  }

  /**
   * if this node is newly closed, ensure we update the counter in a get-safe manner
   * @param n
   * @return the input node (to assist w/function chaining)
   */
  private static Node close(Node n)
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
  private static boolean push(Node n)
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
    final boolean identicalFactors = n.p.equals(n.q);
    for (int i = 0; i < cacheInternalBase; ++i)
    {
      for (int j = 0; j < cacheInternalBase; ++j)
      {
        if (identicalFactors && i > j) continue;
        
        final Node generated = close(new Node(n, i, j));
        if (null != generated && generated.validFactors())
        {
          Solver.generated.addAndGet(1);
          if (cachePrintAllNodes) Log.o("generated: " + generated);
          if (!push(generated)) return false;
        }
        else
        {
          Solver.ignored.addAndGet(1);
          if (cachePrintAllNodes) Log.o("ignored: " + generated);
        }
      }
    }

    return true;
  }

  // resets the search
  public static void reset()
  {
    Log.o("resetting solver...");

    if (solving()) interruptAndJoin();
    instance.set(null);

    generated.set(0);
    regenerated.set(0);
    ignored.set(0);
    expanded.set(0);
    maxDepth.set(0);
    totalDepth.set(0);
    startTime.set(0);
    endTime.set(0);

    pLength(0);
    qLength(0);

    goal.set(null);
    callback.set(null);

    heuristics.clear();
    open.clear();
    closed.clear();

    solving.set(false);
    paused.set(false);

    Log.o("solver reset");
  }

  public static void interrupt() { threads.stream().forEach(Thread::interrupt); }
  public static void join() { threads.stream().forEach(t -> { try { t.join(); } catch (Throwable ignored) {} }); }
  public static void interruptAndJoin() { try { interrupt(); join(); } catch (Throwable ignored) {} }

  /**
   * Ensures a single instance is launched.
   * @param semiprime number to solve
   * @return an un-launched thread configured for the target
   */
  public static Thread get(final String semiprime) { return get(semiprime, 10); }
  public static Thread get(final String semiprime, int semiprimeBase)
  {
    try
    {
      Solver solver = instance.getAndSet(null);
      if (null != solver && Solver.solving()) return null;
      solver = new Solver(semiprime, semiprimeBase);
      if (!instance.compareAndSet(null, solver)) return null;
      return new Thread(solver);
    }
    catch (Throwable t) { Log.e(t); return null; }
  }
}
