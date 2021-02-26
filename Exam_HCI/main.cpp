// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

#include "common/common_utils/StrictMode.hpp"
STRICT_MODE_OFF
#ifndef RPCLIB_MSGPACK
#define RPCLIB_MSGPACK clmdep_msgpack
#endif // !RPCLIB_MSGPACK
#include "rpc/rpc_error.h"
STRICT_MODE_ON

#include "vehicles/car/api/CarRpcLibClient.hpp"
#include "common/common_utils/FileSystem.hpp"
#include <iostream>
#include <chrono>
using std::string;



int main()
{
    using namespace msr::airlib;

    // This assumes you are running DroneServer already on the same machine.
    // DroneServer must be running first.
    msr::airlib::CarRpcLibClient client;
    typedef common_utils::FileSystem FileSystem;

    try {
        client.confirmConnection();

        //enable API control
        client.enableApiControl(true);
        CarApiBase::CarControls controls;
        // std::cout << "Prem:\n\ta) per girare a sinistra\n\td) per girare a destra\n\tw) per andare avanti\n\ts) per andare indietro\n\tenter) per fermarti.\n per uscire dal loop premere e" << std::endl;
        controls.throttle = 0;
        controls.steering = 0.0f;
        client.setCarControls(controls);

        string print = "";
        while (true) {
            std::cin >> print;
            if (!print.compare("a")) {
                controls.steering = -1;
                client.setCarControls(controls);
            }
            if (!print.compare("d")) {
                controls.steering = 1;
                client.setCarControls(controls);
            }
            if (!print.compare("l")) {
                controls.steering = 0;
                client.setCarControls(controls);
            }
            if (!print.compare("w")) {
                controls.throttle = 0.5f;
                controls.brake = 0;
                controls.steering = 0;
                client.setCarControls(controls);
            }
            if (!print.compare("s")) {
                controls.throttle = -0.5f;
                controls.steering = 0;
                controls.brake = 1;
                client.setCarControls(controls);
            }
            client.setCarControls(controls);
        }

        client.setCarControls(CarApiBase::CarControls());
    }
    catch (rpc::rpc_error& e) {
        std::string msg = e.get_error().as<std::string>();
        // std::cout << "Exception raised by the API, something went wrong." << std::endl << msg << std::endl; std::cin.get();
    }

    return 0;
}
