package log;

import java.util.HashMap;
import java.util.Iterator;


/**
 * The type Log.
 * A log is a set of snapshot
 */
public class Log {
  
  // Save the whole information of CS Activity & Environment State per EACH TICK
  // This snapshotMap is currently used for Property Verification (11/14)
  private HashMap<Integer, Snapshot> snapshotMap;

  // TODO Log Class Refactoring

  /**
   * Instantiates a new Log.
   */
  public Log() {
    this.snapshotMap = new HashMap<>();
    //this.simuLog = new SimulationLog();
  }

  /**
   * Add snapshot with its occurred time.
   *
   * @param tick     the tick
   * @param snapshot the snapshot
   */
  public void addSnapshot(int tick, String snapshot) {
    Snapshot tmp = new Snapshot(snapshot);
    
    snapshotMap.put(tick, tmp);
  }
//
//  public void addSimuLog(ArrayList<CS> Css, ArrayList<Integer> Evs) {
//    this.simuLog.addCsResultLog(Css);
//    this.simuLog.addEnvironmentResultLog(Evs);
//  }


  /**
   * Print a snapshot.
   */
  public void printSnapshot() {
    Iterator<Integer> keys = snapshotMap.keySet().iterator();
    System.out.println("===================== SNAPSHOT PRINT =====================");
    while(keys.hasNext()) {
      Integer key = keys.next();
      System.out.println("===================== TICK:" + key.toString() + " " + snapshotMap.get(key).getSnapshotString() + " =====================");
      //snapshotMap.get(key).printSnapshotLog();
    }
  }
  
  /*
  public SimulationLog getSimuLog() {
    return this.simuLog;
  }
*/
  public HashMap<Integer, Snapshot> getSnapshotMap() {
    return snapshotMap;
  }
}
