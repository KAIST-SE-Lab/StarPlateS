import java.util.ArrayList;

public class OracleGenerator {
    private ArrayList<ArrayList<String>> oracle;

    public OracleGenerator() {
        oracle = new ArrayList<>();
    }

    public ArrayList<ArrayList<String>> oracleGeneration(ArrayList<InterplayModel> IMs) {
        ArrayList<Message> msgs = null;
        for (InterplayModel im : IMs) {
            System.out.println("IM_"+im.getId());
            msgs = im.getMsgSequence();

            // Check continuous Merge_Requests
            for(Message msg: msgs) {

            }

            msgs.clear();
        }
        return oracle;
    }
}
