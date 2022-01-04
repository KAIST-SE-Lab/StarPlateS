import java.io.*;
import java.lang.reflect.Array;
import java.sql.Struct;
import java.util.*;

public class Verifier {
    public Boolean verifyLog(String txtdir, String property, int threshold) {
        boolean ret = false;

        switch(property) {
            case "operationTime":
                ret = operationTimeVerification(txtdir, threshold);

                // CSV generation for tendency analysis of the verification property success rate
//                File file2 = new File(System.getProperty("user.dir") + "/examples/platoon_SoS/results/" + "time_" + threshold + ".csv");
//                System.out.println(System.getProperty("user.dir") + "/examples/platoon_SoS/results/"+ "time_" + threshold + ".csv");
//                FileWriter writer2 = null;
//                try {
//                    writer2 = new FileWriter(file2, true);
////                    writer.write("operationSuccessRate for " + txtdir.replace(System.getProperty("user.dir") + "/StarPlateS/SoS_Extension/", "") + " is " + Boolean.toString(ret) + "\n");
//                    writer2.write(txtdir.replace(System.getProperty("user.dir") + "/examples/platoon_SoS/results/", "") + "," + Boolean.toString(ret) + "\n");
//                    writer2.flush();
//                } catch(IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    try {
//                        if(writer2 != null) writer2.close();
//                    } catch(IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                System.out.println(ret);
                break;

            case "operationSuccessRate":
                ret = operationSuccessRateVerification(txtdir, threshold);

                // CSV generation for tendency analysis of the verification property success rate
                File file = new File(System.getProperty("user.dir") + "/SoS_Extension/Verification_Results" + "_" + threshold + ".csv");
                FileWriter writer = null;
                try {
                    writer = new FileWriter(file, true);
//                    String log_id = txtdir.replace(System.getProperty("user.dir") + "/SoS_Extension/logs/", "");
                    String log_id = "";
                    writer.write(log_id+ "," + Boolean.toString(ret) + "\n");
                    writer.flush();
                } catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(writer != null) writer.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

        return ret;
    }

    public Boolean verifyLog(String txtdir, String txtdir_veh, String property) {
        boolean ret = false;
        String analysisResult = "";

        switch(property) {
            case "collision":
                ret = collisionDetection(txtdir);
                if (!ret) analysisResult = collisionAnalysis(txtdir_veh);
                File file = new File(System.getProperty("user.dir") + "/SoS_Extension/Verification_Results_Collision.csv");
                FileWriter writer = null;
                try {
                    writer = new FileWriter(file, true);
                    String log_id = txtdir.replace(System.getProperty("user.dir") + "/SoS_Extension/logs/", "");
                    writer.write(log_id + "," + Boolean.toString(ret) + "," + analysisResult + "\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (writer != null) writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

        return ret;
    }

    private Boolean collisionDetection(String txtdir) {
        boolean ret = true;
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(new File(txtdir)));
            while ((line = reader.readLine()) != null) {
                if(line.contains("Collisions")) {
                    ret = false;
                }
            }
        } catch(Exception e) {
            System.out.println(e);
        }
        return ret;
    }

    private String collisionAnalysis(String txtdir_veh) {
        String ret = "";
        BufferedReader reader;
        boolean missingFlag;
        float colLanePos = -1;
        String colLane = "";

        ArrayList<collisionLog> prevList = new ArrayList<>();
        ArrayList<collisionLog> curntList = new ArrayList<>();
        try {
            String line;
            reader = new BufferedReader(new FileReader(new File(txtdir_veh)));
            while ((line = reader.readLine()) != null) {
                missingFlag = false;
                StringTokenizer st = new StringTokenizer(line, "\t ");

                if(st.countTokens() == 10) {
                    collisionLog tempLog = new collisionLog();
                    String temp = st.nextToken();
                    if(temp.equals("index")) continue;
                    tempLog.time = Float.valueOf(st.nextToken());
                    tempLog.vehId = st.nextToken();
                    tempLog.lane = st.nextToken();//lane
                    tempLog.lanePos = Float.valueOf(st.nextToken());//lanepos
//                    st.nextToken();//speed
//                    st.nextToken();//timegapsetting
//                    st.nextToken();//timegap
                    curntList.add(tempLog);
                } else if (curntList.size() != 0) {
                    if (prevList.size() != 0) {
                        for (collisionLog log : prevList) {
                            boolean matchingFlag = false;
                            for (collisionLog curntLog : curntList) {
                                if (log.vehId.equals(curntLog.vehId)) { // prev와 비교하였을때, vehData.txt 에서 사라진 차량 발견
                                    matchingFlag = true;
                                    break;
                                }
                            }
                            if (!matchingFlag) { // 사라진 차량의 정보 저장
                                missingFlag = true;
                                ret = log.vehId;
                                colLanePos = log.lanePos;
                                colLane = log.lane;
                            }
                        }
                    }

                    if (missingFlag) { // prevLog에서 해당 차량과 가장 근접한 거리에 있는 다른 차량 선정 후 저장
                        float difference = -1;
                        String minVehId = "";
                        for (collisionLog log : prevList) {
                            if (difference == -1 && colLane.equals(log.lane)) {
                                difference = Math.abs(colLanePos - log.lanePos);
                                minVehId = log.vehId;
                            } else {
                                if (colLanePos - log.lanePos == 0) continue;
                                if (difference > Math.abs(colLanePos - log.lanePos) && colLane.equals(log.lane)) {
                                    difference = Math.abs(colLanePos - log.lanePos);
                                    minVehId = log.vehId;
                                }
                            }
                        }
                        ret += "," + minVehId;
                    }

                    prevList.clear();
                    prevList = (ArrayList) curntList.clone();
                    curntList.clear();
                }
            }
        } catch(Exception e) {
            System.out.println(e);
        }
        return ret;
    }

    class collisionLog {
        float time;
        String vehId;
        String lane;
        float lanePos;
    }

    /**
    * Distnace Checker Implementation 
    * @txtdir_pltConfig - pln data txt file
    * @txtdir_veh - pln data txt file
    * @property - [default] DistanceChecker
    * @threshold 
    * @return boolean of distance checker
    **/

    public Boolean verifyLog(String txtdir_pltConfig, String txtdir_veh, String property, float threshold) {
        boolean ret = false;

        switch(property) {
            case "DistanceChecker":
                ret = DistanceVerification(txtdir_pltConfig, txtdir_veh, threshold);
                break;
        }
        return ret;
    }

    private Boolean DistanceVerification(String txtdir_pltConfig, String txtdir_veh, float threshold) {
        boolean ret = true;

        BufferedReader reader_veh = null;
        BufferedReader reader_pltConfig = null;

        ArrayList<Message> message_veh = new ArrayList<>();
        ArrayList<Message> message_pltConfig = new ArrayList<>();

        try {

            // read data from vehicle text file
            reader_veh = new BufferedReader(new FileReader(new File(txtdir_veh)));

            // fields of each vehicle data
            String line;
            String vehId;
            float time;
            float distance;

            while ((line = reader_veh.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line, "\t ");

                if (st.countTokens() == 10) {
                    String temp = st.nextToken();
                    if(temp.equals("index")) continue;
                    time = Float.valueOf(st.nextToken());
                    vehId = st.nextToken();
                    st.nextToken();//lane
                    st.nextToken();//lanepos
                    st.nextToken();//speed
                    st.nextToken();//timegapsetting
                    st.nextToken();//timegap
                    distance = Float.valueOf(st.nextToken());

                    // Collect all vehicle data for later calculation
                    Message msg_veh = new Message();
                    msg_veh.time = time;
                    msg_veh.senderPltId = vehId;
                    msg_veh.distance = distance;
                    message_veh.add(msg_veh);
                }
            }
            reader_veh.close();

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
        * @currentIndex - to index vehicles in same group
        **/
        int currentIndex = 0;

        try {
            // read platoon data
            reader_pltConfig = new BufferedReader(new FileReader(new File(txtdir_pltConfig)));

            String line;
            int index;
            float time;
            String vehId;

            while ((line = reader_pltConfig.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line, "\t ");

                if (st.countTokens() == 9) {
                    String temp = st.nextToken();
                    if(temp.equals("index")) continue; // Initial index starts from index 0. Indexes are the same as long as they are 
                                                       // in a group of vehicles at a time. A change of index means all neccessary data 
                                                       // is read and ready to process the calculation. 

                    index = Integer.valueOf(temp);
                    time = Float.valueOf(st.nextToken());
                    vehId = st.nextToken();
                    st.nextToken();//pltMode
                    st.nextToken();//pltId
                    st.nextToken();//pltDepth
                    st.nextToken();//pltSize
                    st.nextToken();//pltOptSize
                    st.nextToken();//pltMaxSize

                    if(index != currentIndex) {

                        // Have data of previous vehicles group, calculate distance difference
                        if(message_pltConfig.size() > 0) {
                            float totalDiff[] = new float[message_pltConfig.size()-1];
                            for(int i = 1; i < message_pltConfig.size(); i++) {
                                float diff = message_pltConfig.get(i).distance - message_pltConfig.get(i-1).distance;
                                totalDiff[i-1] = diff;
                            }

                            for(int i = 1; i < totalDiff.length; i++) {
                                if((totalDiff[i] - totalDiff[i-1]) > threshold)
                                    ret = false; // verifier is not correct if the distance between vehicles is greater than a given thresold
                            }
                        }

                        currentIndex = index; // Calculation has been done. Nex index has begin and new data will be collected.
                        message_pltConfig = new ArrayList<>(); // Prepare for the new group as empty array
                    }

                    float distance = 0.0f;

                    // Map with vehicle distance
                    for(int i = 0; i < message_veh.size(); i++) {
                        if(message_veh.get(i).time.equals(time)
                                && message_veh.get(i).senderPltId.equals(vehId)) {
                            distance = message_veh.get(i).distance;
                            message_veh.remove(i);
                        }
                    }

                    // Collect each vehicle data
                    Message msg_pltConfig = new Message();
                    msg_pltConfig.time = time;
                    msg_pltConfig.senderPltId = vehId;
                    msg_pltConfig.distance = distance;
                    message_pltConfig.add(msg_pltConfig);
                }
            }

            reader_pltConfig.close();

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
    /* -- */

    private Boolean operationTimeVerification(String txtdir, int threshold) {
        boolean ret = true;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(txtdir)));

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
                    st.nextToken();//fromState
                    st.nextToken();//toState
                    st.nextToken();//commandSent
                    st.nextToken();//rcvId
                    st.nextToken();//senderId
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
                                    //ret = true; // For analyzing specific operation
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
    
    private Boolean operationSuccessRateVerification(String txtdir, int threshold) {
        boolean ret = true;
        ArrayList<Message> messages = new ArrayList<>();
        int addCount = 0;
        int delCount = 0;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(txtdir)));

            String line;
            String command1;
            String command2;
            String senderPltId;
            String rcvId;
            String vehId;
            float time;

            while ((line = reader.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line, "\t ");

                if (st.countTokens() == 9) {
                    String temp = st.nextToken();
                    if(temp.equals("timeStep")) continue;

                    time = Float.valueOf(temp);
                    vehId = st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    command1 = st.nextToken();
                    rcvId = st.nextToken();
                    senderPltId = st.nextToken();
                    st.nextToken();
                    command2 = st.nextToken();

                    if(command1.contains("REQ")) {
                        Message msg = new Message();
                        msg.receiverId = rcvId;
                        msg.senderPltId = senderPltId;
                        msg.commandSent = command1.split("_")[0];
                        messages.add(msg);
                        addCount++;
                        continue;
                    } if(command2.contains("Start")) {
                        Message msg = new Message();
                        if(senderPltId.equals("-") || rcvId.equals("-")) continue;
                        msg.commandSent = command2.split("_")[0];
                        msg.senderPltId = senderPltId;
                        msg.receiverId = rcvId;
                        messages.add(msg);
                        addCount++;
                    }

                    if (command2.contains("End")) {
                        switch(command2.split("_")[0]) {
                            case "Split":
                                for(int i = 0; i < messages.size(); i++) {
                                    if (messages.get(i).commandSent.toLowerCase().equals("split")
                                            && messages.get(i).senderPltId.equals(rcvId) && messages.get(i).receiverId.equals(senderPltId)) {
                                        messages.remove(i);
                                        delCount++;
                                        break;
                                    }
                                }
                                break;
                            case "Merge":
                                for(int i = 0; i < messages.size(); i++) {
                                    if (messages.get(i).commandSent.equals("MERGE") && messages.get(i).receiverId.equals(rcvId)) {
                                        messages.remove(i);
                                        delCount++;
                                        break;
                                    }
                                }
                                break;
                            default:
                                for(int i = 0; i < messages.size(); i++) {
                                    if (messages.get(i).commandSent.equals(command2.split("_")[0])
                                            && messages.get(i).senderPltId.equals(vehId)) {
                                        messages.remove(i);
                                        delCount++;
                                        break;
                                    }
                                }
                                break;

                        }
                    }
                }
            }

            for(int i = 0; i <messages.size(); i++) {
//                System.out.println(messages.get(i).commandSent);
            }
//            System.out.println(delCount/addCount);
            if(delCount < addCount * threshold/100) ret = false;

            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
