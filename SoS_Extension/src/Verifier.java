import java.io.*;
import java.util.*;

public class Verifier {
    public Boolean verifyLog(int s_index, int r_index, String property, int threshold) {
        boolean ret = false;
        
        switch(property) {
            case "operationTime":
                ret = operationTimeVerification(s_index, r_index, threshold);
                System.out.println(ret);
                break;
                
            case "operationSuccessRate":
                
                break;
        }
        
        return ret;
    }
    
    private Boolean operationTimeVerification(int s_index, int r_index, int threshold) {
        boolean ret = true;
    
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(".\\SoS_Extension\\logs\\"
                + s_index + "_" + r_index + "plnData.txt")));
        
            String line;
            String startEnd;
            String vehId;
            String rcvPltId;
            float time;
            ArrayList<Message> messages = new ArrayList<>();
        
            while ((line = reader.readLine()) != null) {
            
                StringTokenizer st = new StringTokenizer(line, "\t ");
            
                if (st.countTokens() == 9) {
                    String temp = st.nextToken();
                    if(temp.equals("timeStep")) continue;
                    
                    time = Float.valueOf(temp);
                    vehId = st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    rcvPltId = st.nextToken();
                    startEnd = st.nextToken();
                    
                    if(startEnd.contains("Start")) {
                        Message msg = new Message();
                        msg.time = time;
                        msg.commandSent = startEnd.split("_")[0];
                        msg.senderPltId = vehId;
                        messages.add(msg);
                    } else if (startEnd.contains("End")) {
                        for(int i = 0; i < messages.size(); i++) {
                            switch(startEnd.split("_")[0]) {
                                case "Split":
                                    if(messages.get(i).commandSent.equals(startEnd.split("_")[0])
                                        && messages.get(i).senderPltId.equals(rcvPltId)) {
                                        if(time - messages.get(i).time > threshold) ret = false;
                                        else ret = true;
                                        messages.remove(i);
                                    }
                                    break;
                                default:
                                    if(messages.get(i).commandSent.equals(startEnd.split("_")[0])
                                        && messages.get(i).senderPltId.equals(vehId)) {
                                        if(time - messages.get(i).time > threshold) ret = false;
                                        else ret = true;
                                        messages.remove(i);
                                    }
                                    break;
                            }
                            
                        }
                    }
                }
            }
        
            reader.close();
        
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
}
