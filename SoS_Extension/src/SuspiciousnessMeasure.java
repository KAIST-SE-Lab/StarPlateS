import static java.lang.Math.sqrt;

public class SuspiciousnessMeasure {
    double faileds;
    double totalFailed;
    double passeds;
    double totalPassed;

    double tarantula () {
        if (totalPassed == 0 || totalFailed == 0)
            return 0;
        return (faileds/totalFailed)/(faileds/totalFailed + passeds/totalPassed);
    }

    double ochiai () {
        if (totalFailed == 0)
            return 0;
        return faileds/(sqrt(totalFailed*(faileds+passeds)));
    }

    double op2 () {
        return faileds - (passeds/(totalPassed+1));
    }

    double barinel () {
        return 1- passeds/(passeds+faileds);
    }

    double dstar () {
        if (passeds==0 && totalFailed == 0)
            return 0;
        return faileds/(passeds + totalFailed - faileds);
    }
}
