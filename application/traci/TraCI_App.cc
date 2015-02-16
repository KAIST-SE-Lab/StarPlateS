
#include "TraCI_App.h"
#include "modules/mobility/traci/TraCIScenarioManagerInet.h"
#include "modules/mobility/traci/TraCIMobility.h"

namespace VENTOS {

Define_Module(VENTOS::TraCI_App);


TraCI_App::~TraCI_App()
{

}


void TraCI_App::initialize(int stage)
{
    TraCI_Extend::initialize(stage);

    if(stage == 0)
    {
        // get the ptr of the current module
        nodePtr = FindModule<>::findHost(this);
        if(nodePtr == NULL)
            error("can not get a pointer to the module.");

        terminate = par("terminate").doubleValue();

        bikeModuleType = par("bikeModuleType").stringValue();
        bikeModuleName = par("bikeModuleName").stringValue();
        bikeModuleDisplayString = par("bikeModuleDisplayString").stringValue();

        pedModuleType = par("pedModuleType").stringValue();
        pedModuleName = par("pedModuleName").stringValue();
        pedModuleDisplayString = par("pedModuleDisplayString").stringValue();

        collectVehiclesData = par("collectVehiclesData").boolValue();
        useDetailedFilenames = par("useDetailedFilenames").boolValue();
        collectInductionLoopData = par("collectInductionLoopData").boolValue();

        index = 1;

        cModule *rmodule = simulation.getSystemModule()->getSubmodule("router");
        router = static_cast< Router* >(rmodule);
    }
}


void TraCI_App::finish()
{
    TraCI_Extend::finish();
}


void TraCI_App::handleSelfMsg(cMessage *msg)
{
    TraCI_Extend::handleSelfMsg(msg);
}


// this method is called once (TraCI is up and running)
void TraCI_App::init_traci()
{
    TraCI_Extend::init_traci();

    simsignal_t Signal_executeFirstTS = registerSignal("executeFirstTS");
    nodePtr->emit(Signal_executeFirstTS, 1);
}


void TraCI_App::executeOneTimestep()
{
    EV << "### Sending Command to SUMO to perform simulation for TS = " << (getCurrentTimeMs()/1000.) << endl;

    TraCIScenarioManager::executeOneTimestep();

    EV << "### SUMO completed simulation for TS = " << (getCurrentTimeMs()/1000.) << endl;

    if(collectVehiclesData)
    {
        vehiclesData();   // collecting data from all vehicles in each timeStep
        if(ev.isGUI()) vehiclesDataToFile();  // write what we have collected so far
    }

    if(collectInductionLoopData)
    {
        inductionLoops();    // collecting induction loop data in each timeStep
        if(ev.isGUI()) inductionLoopToFile();  // write what we have collected so far
    }

    bool simulationDone = (simTime().dbl() >= terminate) or commandGetMinExpectedVehicles() == 0;

    simsignal_t Signal_executeEachTS = registerSignal("executeEachTS");
    nodePtr->emit(Signal_executeEachTS, (long)simulationDone);

    if(simulationDone)
    {
        if(collectVehiclesData && !ev.isGUI())
            vehiclesDataToFile();

        if(collectInductionLoopData && !ev.isGUI())
            inductionLoopToFile();

        // close TraCI connection
        commandTerminate();

        // then terminate
        endSimulation();
    }
}


void TraCI_App::addModule(std::string nodeId, std::string type, std::string name, std::string displayString, const Coord& position, std::string road_id, double speed, double angle)
{
    string vehType = commandGetVehicleTypeId(nodeId);

    if (vehType.find("TypeManual") != std::string::npos ||
        vehType.find("TypeACC") != std::string::npos ||
        vehType.find("TypeCACC") != std::string::npos ||
        vehType.find("TypeObstacle") != std::string::npos )
    {
        // this is default (do nothing)
        // type is "c3po.ned.vehicle"
        // name is "V"
    }
    else if (vehType == "TypePedestrian")
    {
        type = pedModuleType;
        name = pedModuleName;
        displayString = pedModuleDisplayString;
    }
    else if (vehType == "TypeBicycle")
    {
        type = bikeModuleType;
        name =  bikeModuleName;
        displayString = bikeModuleDisplayString;
    }
    else
        error("Unknown module type in TraCI_App::addModule");

    // ########################################
    // Copy of TraCIScenarioManager::addModule
    // ########################################

    if (hosts.find(nodeId) != hosts.end()) error("tried adding duplicate module");

    if (queuedVehicles.find(nodeId) != queuedVehicles.end()) {
        queuedVehicles.erase(nodeId);
    }
    double option1 = hosts.size() / (hosts.size() + unEquippedHosts.size() + 1.0);
    double option2 = (hosts.size() + 1) / (hosts.size() + unEquippedHosts.size() + 1.0);

    if (fabs(option1 - penetrationRate) < fabs(option2 - penetrationRate)) {
        unEquippedHosts.insert(nodeId);
        return;
    }

    int32_t nodeVectorIndex = nextNodeVectorIndex++;

    cModule* parentmod = getParentModule();
    if (!parentmod) error("Parent Module not found");

    cModuleType* nodeType = cModuleType::get(type.c_str());
    if (!nodeType) error("Module Type \"%s\" not found", type.c_str());

    //TODO: this trashes the vectsize member of the cModule, although nobody seems to use it
    cModule* mod = nodeType->create(name.c_str(), parentmod, nodeVectorIndex, nodeVectorIndex);
    mod->finalizeParameters();
    mod->getDisplayString().parse(displayString.c_str());
    mod->buildInside();
    mod->scheduleStart(simTime() + updateInterval);

    // pre-initialize TraCIMobility
    for (cModule::SubmoduleIterator iter(mod); !iter.end(); iter++) {
        cModule* submod = iter();
        ifInetTraCIMobilityCallPreInitialize(submod, nodeId, position, road_id, speed, angle);
        TraCIMobility* mm = dynamic_cast<TraCIMobility*>(submod);
        if (!mm) continue;
        mm->preInitialize(nodeId, position, road_id, speed, angle);
    }

    mod->callInitialize();
    hosts[nodeId] = mod;

    // post-initialize TraCIMobility
    for (cModule::SubmoduleIterator iter(mod); !iter.end(); iter++) {
        cModule* submod = iter();
        TraCIMobility* mm = dynamic_cast<TraCIMobility*>(submod);
        if (!mm) continue;
        mm->changePosition();
    }
}


void TraCI_App::vehiclesData()
{
    // get all lanes in the network
    list<string> myList = commandGetLaneList();

    for(list<string>::iterator i = myList.begin(); i != myList.end(); ++i)
    {
        // get all vehicles on lane i
        list<string> myList2 = commandGetLaneVehicleList( i->c_str() );

        for(list<string>::reverse_iterator k = myList2.rbegin(); k != myList2.rend(); ++k)
            saveVehicleData(k->c_str());
    }

    // increase index after writing data for all vehicles
    if (commandGetVehicleCount() > 0)
        index++;
}


void TraCI_App::saveVehicleData(string vID)
{
    double timeStep = (simTime()-updateInterval).dbl();
    string vType = commandGetVehicleTypeId(vID);
    string lane = commandGetVehicleLaneId(vID);
    double pos = commandGetVehicleLanePosition(vID);
    double speed = commandGetVehicleSpeed(vID);
    double accel = commandGetVehicleAccel(vID);
    int CFMode_Enum = commandGetVehicleCFMode(vID);
    string CFMode;

    enum CFMODES {
        Mode_Undefined,
        Mode_NoData,
        Mode_DataLoss,
        Mode_SpeedControl,
        Mode_GapControl,
        Mode_EmergencyBrake,
        Mode_Stopped
    };

    switch(CFMode_Enum)
    {
    case Mode_Undefined:
        CFMode = "Undefined";
        break;
    case Mode_NoData:
        CFMode = "NoData";
        break;
    case Mode_DataLoss:
        CFMode = "DataLoss";
        break;
    case Mode_SpeedControl:
        CFMode = "SpeedControl";
        break;
    case Mode_GapControl:
        CFMode = "GapControl";
        break;
    case Mode_EmergencyBrake:
        CFMode = "EmergencyBrake";
        break;
    case Mode_Stopped:
        CFMode = "Stopped";
        break;
    default:
        error("Not a valid CFModel!");
        break;
    }

    // get the timeGap setting
    double timeGapSetting = commandGetVehicleTimeGap(vID);

    // get the gap
    vector<string> vleaderIDnew = commandGetLeadingVehicle(vID, 900);
    string vleaderID = vleaderIDnew[0];
    double spaceGap = -1;

    if(vleaderID != "")
        spaceGap = atof( vleaderIDnew[1].c_str() );

    // calculate timeGap (if leading is present)
    double timeGap = -1;

    if(vleaderID != "" && speed != 0)
        timeGap = spaceGap / speed;

    VehicleData *tmp = new VehicleData(index, timeStep,
                                       vID.c_str(), vType.c_str(),
                                       lane.c_str(), pos,
                                       speed, accel, CFMode.c_str(),
                                       timeGapSetting, spaceGap, timeGap);
    Vec_vehiclesData.push_back(tmp);
}


void TraCI_App::vehiclesDataToFile()
{
    boost::filesystem::path filePath;

    if( ev.isGUI() )
    {
        filePath = "results/gui/vehicleData.txt";
    }
    else
    {
        // get the current run number
        int currentRun = ev.getConfigEx()->getActiveRunNumber();
        ostringstream fileName;

        if(useDetailedFilenames)
        {
            int TLMode = (*router->net->TLs.begin()).second->TLLogicMode;
            ostringstream filePrefix;
            filePrefix << router->totalVehicleCount << "_" << router->nonReroutingVehiclePercent << "_" << TLMode;
            fileName << filePrefix.str() << "_vehicleData.txt";
        }
        else
        {
            fileName << currentRun << "_vehicleData.txt";
        }

        filePath = "results/cmd/" + fileName.str();
    }

    FILE *filePtr = fopen (filePath.string().c_str(), "w");

    // write header
    fprintf (filePtr, "%-10s","index");
    fprintf (filePtr, "%-12s","timeStep");
    fprintf (filePtr, "%-15s","vehicleName");
    fprintf (filePtr, "%-17s","vehicleType");
    fprintf (filePtr, "%-12s","lane");
    fprintf (filePtr, "%-11s","pos");
    fprintf (filePtr, "%-12s","speed");
    fprintf (filePtr, "%-12s","accel");
    fprintf (filePtr, "%-20s","CFMode");
    fprintf (filePtr, "%-20s","timeGapSetting");
    fprintf (filePtr, "%-10s","SpaceGap");
    fprintf (filePtr, "%-10s\n\n","timeGap");

    int oldIndex = -1;

    // write body
    for(unsigned int k=0; k<Vec_vehiclesData.size(); k++)
    {
        if(oldIndex != Vec_vehiclesData[k]->index)
        {
            fprintf(filePtr, "\n");
            oldIndex = Vec_vehiclesData[k]->index;
        }

        fprintf (filePtr, "%-10d ", Vec_vehiclesData[k]->index);
        fprintf (filePtr, "%-10.2f ", Vec_vehiclesData[k]->time );
        fprintf (filePtr, "%-15s ", Vec_vehiclesData[k]->vehicleName);
        fprintf (filePtr, "%-15s ", Vec_vehiclesData[k]->vehicleType);
        fprintf (filePtr, "%-12s ", Vec_vehiclesData[k]->lane);
        fprintf (filePtr, "%-10.2f ", Vec_vehiclesData[k]->pos);
        fprintf (filePtr, "%-10.2f ", Vec_vehiclesData[k]->speed);
        fprintf (filePtr, "%-10.2f ", Vec_vehiclesData[k]->accel);
        fprintf (filePtr, "%-20s", Vec_vehiclesData[k]->CFMode);
        fprintf (filePtr, "%-20.2f ", Vec_vehiclesData[k]->timeGapSetting);
        fprintf (filePtr, "%-10.2f ", Vec_vehiclesData[k]->spaceGap);
        fprintf (filePtr, "%-10.2f \n", Vec_vehiclesData[k]->timeGap);
    }

    fclose(filePtr);
}


void TraCI_App::inductionLoops()
{
    // get all loop detectors
    list<string> str = commandGetLoopDetectorList();

    // for each loop detector
    for (list<string>::iterator it=str.begin(); it != str.end(); ++it)
    {
        // only if this loop detector detected a vehicle
        if( commandGetLoopDetectorCount(*it) == 1 )
        {
            vector<string>  st = commandGetLoopDetectorVehicleData(*it);

            string vehicleName = st.at(0);
            double entryT = atof( st.at(2).c_str() );
            double leaveT = atof( st.at(3).c_str() );
            double speed = commandGetLoopDetectorSpeed(*it);  // vehicle speed at current moment

            int counter = findInVector(Vec_loopDetectors, (*it).c_str(), vehicleName.c_str());

            // its a new entry, so we add it
            if(counter == -1)
            {
                LoopDetector *tmp = new LoopDetector( (*it).c_str(), vehicleName.c_str(), entryT, -1, speed, -1 );
                Vec_loopDetectors.push_back(tmp);
            }
            // if found, just update the leave time and leave speed
            else
            {
                Vec_loopDetectors[counter]->leaveTime = leaveT;
                Vec_loopDetectors[counter]->leaveSpeed = speed;
            }
        }
    }
}


void TraCI_App::inductionLoopToFile()
{
    boost::filesystem::path filePath;

    if( ev.isGUI() )
    {
        filePath = "results/gui/loopDetector.txt";
    }
    else
    {
        // get the current run number
        int currentRun = ev.getConfigEx()->getActiveRunNumber();
        ostringstream fileName;
        fileName << currentRun << "_loopDetector.txt";
        filePath = "results/cmd/" + fileName.str();
    }

    FILE *filePtr = fopen (filePath.string().c_str(), "w");

    // write header
    fprintf (filePtr, "%-20s","loopDetector");
    fprintf (filePtr, "%-20s","vehicleName");
    fprintf (filePtr, "%-20s","vehicleEntryTime");
    fprintf (filePtr, "%-20s","vehicleLeaveTime");
    fprintf (filePtr, "%-22s","vehicleEntrySpeed");
    fprintf (filePtr, "%-22s\n\n","vehicleLeaveSpeed");

    // write body
    for(unsigned int k=0; k<Vec_loopDetectors.size(); k++)
    {
        fprintf (filePtr, "%-20s ", Vec_loopDetectors[k]->detectorName);
        fprintf (filePtr, "%-20s ", Vec_loopDetectors[k]->vehicleName);
        fprintf (filePtr, "%-20.2f ", Vec_loopDetectors[k]->entryTime);
        fprintf (filePtr, "%-20.2f ", Vec_loopDetectors[k]->leaveTime);
        fprintf (filePtr, "%-20.2f ", Vec_loopDetectors[k]->entrySpeed);
        fprintf (filePtr, "%-20.2f ", Vec_loopDetectors[k]->leaveSpeed);
        fprintf (filePtr, "\n" );
    }

    fclose(filePtr);
}


int TraCI_App::findInVector(vector<LoopDetector *> Vec, const char *detectorName, const char *vehicleName)
{
    unsigned int counter;
    bool found = false;

    for(counter = 0; counter < Vec.size(); counter++)
    {
        if( strcmp(Vec[counter]->detectorName, detectorName) == 0 && strcmp(Vec[counter]->vehicleName, vehicleName) == 0)
        {
            found = true;
            break;
        }
    }

    if(!found)
        return -1;
    else
        return counter;
}

}
