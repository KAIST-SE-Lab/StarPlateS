package property.pattern;

import log.Log;
import log.Snapshot;
import property.Property;
import property.PropertyChecker;

import java.util.HashMap;

/**
 * The type Minimum duration checker.
 * checks whether a length of a property satisfaction in the log is longer than the minimum duration requirements
 */
public abstract class MinimumDurationChecker extends PropertyChecker {
    @Override
    protected abstract boolean evaluateState(Snapshot snapshot, Property verificationProperty);
    
    public boolean check(Log log, Property verificationProperty, int t, int T) {
        HashMap<Integer, Snapshot> snapshots = log.getSnapshotMap();
        int logSize = snapshots.size(); // 0 ... 10 => size: 11, endTime: 10
        int satisfiedCount = 0;
        
        for (int i = t; i < logSize; i++) {
            if (evaluateState(snapshots.get(i), verificationProperty)) {
                satisfiedCount++;
            }
        }
        
        if ((double)satisfiedCount < (double)(T-t)){
            return true;
        }
        return false;
    }
    
    @Override
    public boolean check(Log log, Property verificationProperty) {
        return false;
    }
    
    @Override
    public boolean check(Log log, Property verificationProperty, int until) {
        return false;
    }
    
    @Override
    public boolean check(Log log, Property verificationProperty, double prob, int T) {
        return false;
    }
    
    @Override
    public boolean check(Log log, Property verificationProperty, double prob, int t, int T) { return false;}
    

}
