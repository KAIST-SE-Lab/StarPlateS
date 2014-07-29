
#ifndef ApplVMANAGER_H
#define ApplVMANAGER_H

#include "ApplV_06_PlatoonMg.h"

namespace VENTOS {

class ApplVManager : public ApplVPlatoonMg
{
	public:
        ~ApplVManager();
		virtual void initialize(int stage);
        virtual void finish();

	protected:

        // NED variables (packet loss ratio)
        double droppT;
        string droppV;
        double plr;

        // NED variable
        bool SUMOvehicleDebug;
        bool modeSwitch;

        // NED variables (measurement error)
        bool measurementError;
        double errorGap;
        double errorRelSpeed;

        // Methods
        virtual void handleLowerMsg(cMessage*);
        virtual void handleSelfMsg(cMessage*);
        virtual void handlePositionUpdate(cObject*);

		virtual void onBeaconVehicle(BeaconVehicle*);
        virtual void onBeaconRSU(BeaconRSU*);
        virtual void onData(PlatoonMsg* wsm);

	private:
        bool dropBeacon(double time, string vehicle, double plr);
        void reportDropToStatistics(BeaconVehicle* wsm);
};
}

#endif
