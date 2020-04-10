package property.pattern;

import log.Log;
import log.Snapshot;
import property.Property;
import property.PropertyChecker;

import java.util.HashMap;

/**
 * The type Steady state probability checker.
 * checks the steady states of the property satisfaction
 */
public abstract class SteadyStateProbabilityChecker extends PropertyChecker{
    @Override
    protected abstract boolean evaluateState(Snapshot state, Property verificationProperty);

    @Override
    public boolean check(Log log, Property verificationProperty, double prob, int T) {
        HashMap<Integer, Snapshot> snapshots = log.getSnapshotMap();
        int logSize = snapshots.size(); // 0 ... 10 => size: 11, endTime: 10
        int satisfiedCount = 0;

        for (int i = 0; i < logSize; i++) {
            if (evaluateState(snapshots.get(i), verificationProperty)) {
                satisfiedCount++;
            }
        }
        
        //System.out.println(satisfiedCount);
        
        if ((double)satisfiedCount/(double)T >= prob){
            return true;
        }
        return false;
    }
}
