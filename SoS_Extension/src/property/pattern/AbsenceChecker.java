package property.pattern;

import log.Log;
import log.Snapshot;
import property.Property;
import property.PropertyChecker;

import java.util.HashMap;

/**
 * The type Absence checker.
 * checks whether a property is not satisfied for all logs.
 */
public abstract class AbsenceChecker extends PropertyChecker {
    @Override
    protected abstract boolean evaluateState(Snapshot snapshot, Property verificationProperty);

    @Override
    public boolean check(Log log, Property verificationProperty) {
        HashMap<Integer, Snapshot> snapshots = log.getSnapshotMap();
        int logSize = snapshots.size(); // 0 ... 10 => size: 11, endTime: 10

        for (int i = 0; i < logSize; i++) {
            if (evaluateState(snapshots.get(i), verificationProperty)) {
                return false;
            }
        }
        return true;
    }
}
